package com.github.suusan2go.kotlinfillclass.inspections

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

class FillClassInspectionTest : BasePlatformTestCase() {
    fun `test fill class constructor`() {
        doAvailableTest(
            """
            class User(val name: String, val age: Int)
            fun test() {
                User(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int)
            fun test() {
                User(name = "", age = 0)
            }
        """
        )
    }

    fun `test can't fill class constructor`() {
        doUnavailableTest(
            """
            class User(val name: String, val age: Int)
            fun test() {
                User("", 0<caret>)
            }
        """
        )
    }

    fun `test fill function`() {
        doAvailableTest(
            """
            class User(val name: String, val age: Int)
            fun foo(s: String, t: Int, u: User) {}
            fun test() {
                foo(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int)
            fun foo(s: String, t: Int, u: User) {}
            fun test() {
                foo(s = "", t = 0, u = User(name = "", age = 0))
            }
        """,
            "Fill function"
        )
    }

    fun `test can't fill function`() {
        doUnavailableTest(
            """
            fun foo(s: String, t: Int) {}            
            fun test() {
                foo("", 0<caret>)
            }
        """
        )
    }

    fun `test fill for non primitive types`() {
        doAvailableTest(
            """
            class A(a1: String, a2: Int)
            class B(b1: Int, b2: String, a: A)
            class C
            class D(a: A, b: B, c: C, r: Runnable)
            fun test() {
                D(<caret>)
            }
        """,
            """
            class A(a1: String, a2: Int)
            class B(b1: Int, b2: String, a: A)
            class C
            class D(a: A, b: B, c: C, r: Runnable)
            fun test() {
                D(a = A(a1 = "", a2 = 0), b = B(b1 = 0, b2 = "", a = A(a1 = "", a2 = 0)), c = C(), r =)
            }
        """
        )
    }

    fun `test don't add default value for enum,abstract,sealed`() {
        doAvailableTest(
            """
            enum class A(val a: String) {
                Foo("foo"), Bar("bar"), Baz("baz");
            }
            sealed class B(val b: String)
            abstract class C(val c: String)
            class Test(a: A, b: B, c: C)
            fun test() {
                Test(<caret>)
            }
        """,
            """
            enum class A(val a: String) {
                Foo("foo"), Bar("bar"), Baz("baz");
            }
            sealed class B(val b: String)
            abstract class C(val c: String)
            class Test(a: A, b: B, c: C)
            fun test() {
                Test(a =, b =, c =)
            }
        """
        )
    }

    fun `test add import directives`() {
        val dependency = """
            package com.example

            class A
            class B(a: A)
        """
        doAvailableTest(
            """
            import com.example.B
            
            val b = B(<caret>)
        """,
            """
            import com.example.A
            import com.example.B
            
            val b = B(a = A())
        """,
            dependencies = listOf(dependency)
        )
    }

    fun `test call java constructor`() {
        val javaDependency = """
            public class Java {
                public Java(String str) {
                }
            }
        """
        doUnavailableTest(
            """
            fun test() {
                Java(<caret>)
            }
        """,
            javaDependencies = listOf(javaDependency)
        )
    }

    fun `test call java method`() {
        val javaDependency = """
            public class Java {
                public Java(String str) {
                }
            
                public void foo(Java java) {
                }
            }
        """
        doUnavailableTest(
            """
            fun test() {
                Java("").foo(<caret>)
            }
        """,
            javaDependencies = listOf(javaDependency)
        )
    }

    fun `test fill super type call entry`() {
        doAvailableTest(
            """
            open class C(p1: Int, p2: Int)
            class D : C(<caret>)
        """,
            """
            open class C(p1: Int, p2: Int)
            class D : C(p1 = 0, p2 = 0)
        """
        )
    }

    fun `test extension function`() {
        doAvailableTest(
            """
            class Foo
            fun Foo.foo(x: Int, y: Int) {}
            fun test() {
                Foo().foo(<caret>)
            }
        """,
            """
            class Foo
            fun Foo.foo(x: Int, y: Int) {}
            fun test() {
                Foo().foo(x = 0, y = 0)
            }
        """,
            "Fill function"
        )
    }

    fun `test imported extension function`() {
        doAvailableTest(
            """
            import Bar.foo
            class Foo
            object Bar {
                fun Foo.foo(x: Int, y: Int) {}
            }
            fun test() {
                Foo().foo(<caret>)
            }
        """,
            """
            import Bar.foo
            class Foo
            object Bar {
                fun Foo.foo(x: Int, y: Int) {}
            }
            fun test() {
                Foo().foo(x = 0, y = 0)
            }
        """,
            "Fill function"
        )
    }

    fun `test fill class constructor without default values`() {
        doAvailableTest(
            """
            class User(val name: String, val age: Int)
            fun test() {
                User(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int)
            fun test() {
                User(name =, age =)
            }
        """,
            withoutDefaultValues = true
        )
    }

    fun `test do not fill default arguments`() {
        doAvailableTest(
            """
            class User(val name: String, val age: Int = 0)
            fun test() {
                User(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int = 0)
            fun test() {
                User(name = "")
            }
        """,
            withoutDefaultArguments = true
        )
    }

    fun `test fill lambda arguments`() {
        val dependency = """
            package foo

            class A

            class B(f1: () -> Unit, f2: (Int) -> String, f3: (Int, String?, A) -> String)
        """.trimIndent()

        doAvailableTest(
            """
            import foo.B
            
            fun test() {
                B(<caret>)
            }
        """,
            """
            import foo.A
            import foo.B
            
            fun test() {
                B(f1 = {}, f2 = {}, f3 = { i: Int, s: String?, a: A -> })
            }
        """,
            withoutDefaultArguments = true, dependencies = listOf(dependency)
        )
    }

    fun `test do not add trailing comma`() {
        doAvailableTest(
            """
            class User(val name: String, val age: Int = 0)
            fun test() {
                User(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int = 0)
            fun test() {
                User(name = "", age = 0)
            }
        """,
            withTrailingComma = false
        )
    }

    fun `test add trailing comma`() {
        doAvailableTest(
            """
            class User(val name: String, val age: Int = 0)
            fun test() {
                User(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int = 0)
            fun test() {
                User(name = "", age = 0,)
            }
        """,
            withTrailingComma = true
        )
    }

    fun `test do not add trailing comma if trailing comma already exists`() {
        doAvailableTest(
            """
             fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
             fun test() {
                 foo(
                     a = 0,
                     b = 0,<caret>
                 )
             }
         """,
            """
             fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
             fun test() {
                 foo(
                     a = 0,
                     b = 0, c = 0, d = 0,
                 )
             }
         """,
            problemDescription = "Fill function",
            withTrailingComma = true
        )
    }

    fun `test do not put arguments on separate lines`() {
        doAvailableTest(
            """
            fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
            fun test() {
                foo(<caret>)
            }
        """,
            """
            fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
            fun test() {
                foo(a = 0, b = 0, c = 0, d = 0)
            }
        """,
            problemDescription = "Fill function",
            putArgumentsOnSeparateLines = false
        )
    }

    fun `test put arguments on separate lines`() {
        doAvailableTest(
            """
            fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
            fun test() {
                foo(<caret>)
            }
        """,
            """
            fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
            fun test() {
                foo(a = 0,
                        b = 0,
                        c = 0,
                        d = 0)
            }
        """,
            problemDescription = "Fill function",
            putArgumentsOnSeparateLines = true
        )
    }

    fun `test put arguments on separate lines with existing arguments`() {
        doAvailableTest(
            """
            fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
            fun test() {
                foo(a = 1, b = 2<caret>)
            }
        """,
            """
            fun foo(a: Int, b: Int, c: Int, d: Int) = a + b + c + d
            fun test() {
                foo(a = 1,
                        b = 2,
                        c = 0,
                        d = 0)
            }
        """,
            problemDescription = "Fill function",
            putArgumentsOnSeparateLines = true
        )
    }

    fun `test move pointer to every argument`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 0<caret>, y = 0, z = 0)
        """,
            problemDescription = "Fill function",
            movePointerToEveryArgument = true
        )
    }

    fun `test move pointer to every argument with existing arguments`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 1<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 1, y = 0<caret>, z = 0)
        """,
            problemDescription = "Fill function",
            movePointerToEveryArgument = true
        )
    }

    fun `test move pointer to every argument with putArgumentsOnSeparateLines`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 0<caret>,
                    y = 0,
                    z = 0)
        """,
            problemDescription = "Fill function",
            movePointerToEveryArgument = true,
            putArgumentsOnSeparateLines = true
        )
    }

    fun `test move pointer to every argument with withoutDefaultValues`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x =<caret>, y =, z =)
        """,
            problemDescription = "Fill function",
            movePointerToEveryArgument = true,
            withoutDefaultValues = true
        )
    }

    private fun doAvailableTest(
        before: String,
        after: String,
        problemDescription: String = "Fill class constructor",
        dependencies: List<String> = emptyList(),
        javaDependencies: List<String> = emptyList(),
        withoutDefaultValues: Boolean = false,
        withoutDefaultArguments: Boolean = false,
        withTrailingComma: Boolean = false,
        putArgumentsOnSeparateLines: Boolean = false,
        movePointerToEveryArgument: Boolean = false,
    ) {
        val highlightInfo = doHighlighting(
            before,
            problemDescription,
            dependencies,
            javaDependencies,
            withoutDefaultValues,
            withoutDefaultArguments,
            withTrailingComma,
            putArgumentsOnSeparateLines,
            movePointerToEveryArgument,
        )
        check(highlightInfo != null) { "Problems should be detected at caret" }
        myFixture.launchAction(myFixture.findSingleIntention(problemDescription))
        myFixture.checkResult(after.trimIndent())
    }

    private fun doUnavailableTest(
        before: String,
        problemDescription: String = "Fill class constructor",
        dependencies: List<String> = emptyList(),
        javaDependencies: List<String> = emptyList(),
        withoutDefaultValues: Boolean = false,
        withoutDefaultArguments: Boolean = false,
        withTrailingComma: Boolean = false,
        putArgumentsOnSeparateLines: Boolean = false,
        movePointerToEveryArgument: Boolean = false
    ) {
        val highlightInfo = doHighlighting(
            before,
            problemDescription,
            dependencies,
            javaDependencies,
            withoutDefaultValues,
            withoutDefaultArguments,
            withTrailingComma,
            putArgumentsOnSeparateLines,
            movePointerToEveryArgument,
        )
        check(highlightInfo == null) { "No problems should be detected at caret" }
    }

    private fun doHighlighting(
        code: String,
        problemDescription: String = "Fill class constructor",
        dependencies: List<String>,
        javaDependencies: List<String>,
        withoutDefaultValues: Boolean,
        withoutDefaultArguments: Boolean,
        withTrailingComma: Boolean,
        putArgumentsOnSeparateLines: Boolean,
        movePointerToEveryArgument: Boolean,
    ): HighlightInfo? {
        check("<caret>" in code) { "Please, add `<caret>` marker to\n$code" }

        dependencies.forEachIndexed { index, dependency ->
            myFixture.configureByText("dependency$index.kt", dependency.trimIndent())
        }
        javaDependencies.forEachIndexed { index, dependency ->
            myFixture.configureByText("dependency$index.java", dependency.trimIndent())
        }
        myFixture.configureByText(KotlinFileType.INSTANCE, code.trimIndent())

        val inspection = FillEmptyValuesInspection(
            withoutDefaultValues = withoutDefaultValues,
            withoutDefaultArguments = withoutDefaultArguments,
            withTrailingComma = withTrailingComma,
            putArgumentsOnSeparateLines = putArgumentsOnSeparateLines,
            movePointerToEveryArgument = movePointerToEveryArgument,
        )
        myFixture.enableInspections(inspection)

        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)
        val inspectionProfile = inspectionProfileManager.currentProfile
        val state = inspectionProfile.getToolDefaultState(inspection.shortName, project)
        state.level = HighlightDisplayLevel.WARNING

        val caretOffset = myFixture.caretOffset
        return myFixture.doHighlighting().singleOrNull {
            it.description == problemDescription && caretOffset in it.startOffset..it.endOffset
        }
    }
}
