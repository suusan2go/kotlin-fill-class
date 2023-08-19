package com.github.suusan2go.kotlinfillclass.inspections

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinFileType

class FillDummyValueInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        mockkObject(ValueGenerator)
    }

    override fun tearDown() {
        super.tearDown()
        unmockkObject(ValueGenerator)
    }

    fun `test fill class constructor`() {
        every { ValueGenerator.randomStringFor("name") } returns "John Smith"
        every { ValueGenerator.randomNumFor("age") } returns 1234
        every { ValueGenerator.randomStringFor("pass") } returns "password"
        doAvailableTest(
            """
            class User(val name: String, val age: Int, val pass: CharSequence)
            fun test() {
                User(<caret>)
            }
        """,
            """
            class User(val name: String, val age: Int, val pass: CharSequence)
            fun test() {
                User(name = "John Smith", age = 1234, pass = "password")
            }
        """,
        )
    }

    fun `test can't fill class constructor`() {
        doUnavailableTest(
            """
            class User(val name: String, val age: Int)
            fun test() {
                User("", 0<caret>)
            }
        """,
        )
    }

    fun `test fill function`() {
        every { ValueGenerator.randomStringFor("name") } returns "John Smith"
        every { ValueGenerator.randomStringFor("s") } returns "Foo"
        every { ValueGenerator.randomNumFor("age") } returns 1234
        every { ValueGenerator.randomNumFor("t") } returns 2345
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
                foo(s = "Foo", t = 2345, u = User(name = "John Smith", age = 1234))
            }
        """,
            "Fill function with dummy values",
        )
    }

    fun `test can't fill function`() {
        doUnavailableTest(
            """
            fun foo(s: String, t: Int) {}            
            fun test() {
                foo("", 0<caret>)
            }
        """,
        )
    }

    fun `test can't fill function with lambda argument`() {
        doUnavailableTest(
            """
            fun foo(a: Int, b: String, block: () -> Unit) {}
            fun main() {
                foo(1, "b"<caret>) {}
            }
        """,
            "Fill function with dummy values",
        )
    }

    fun `test fill function with lambda argument`() {
        every { ValueGenerator.randomStringFor("b") } returns "Foo"
        doAvailableTest(
            """
            fun foo(a: Int, b: String, block: () -> Unit) {}
            fun main() {
                foo(1<caret>) {}
            }
        """,
            """
            fun foo(a: Int, b: String, block: () -> Unit) {}
            fun main() {
                foo(1, b = "Foo") {}
            }
        """,
            "Fill function with dummy values",
        )
    }

    fun `test fill function with lambda argument and trailing comma`() {
        every { ValueGenerator.randomStringFor("b") } returns "Foo"
        doAvailableTest(
            """
            fun foo(a: Int, b: String, block: () -> Unit) {}
            fun main() {
                foo(1<caret>) {}
            }
        """,
            """
            fun foo(a: Int, b: String, block: () -> Unit) {}
            fun main() {
                foo(1,
                        b = "Foo",
                ) {}
            }
        """,
            "Fill function with dummy values",
            withTrailingComma = true,
            putArgumentsOnSeparateLines = true,
        )
    }

    fun `test fill for non primitive types`() {
        every { ValueGenerator.randomStringFor(any()) } answers {
            "Foo${firstArg<String>()}"
        }
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                D(a = A(a1 = "Fooa1", a2 = 3057), b = B(b1 = 3087, b2 = "Foob2", a = A(a1 = "Fooa1", a2 = 3057)), c = C(), r =)
            }
        """,
        )
    }

    fun `test add default value for enum`() {
        doAvailableTest(
            """
            enum class EmotionType(val description: String) {
                HAPPY("happy"), SAD("sad"), ANGRY("angry");
            }
            class Test(emotion: EmotionType)
            fun test() {
                Test(<caret>)
            }
        """,
            """
            enum class EmotionType(val description: String) {
                HAPPY("happy"), SAD("sad"), ANGRY("angry");
            }
            class Test(emotion: EmotionType)
            fun test() {
                Test(emotion = EmotionType.HAPPY)
            }
        """,
        )
    }

    fun `test add default value for enum from dependency`() {
        val dependency = """
            package com.example
            
            class Test(emotion: EmotionType)
            enum class EmotionType(val description: String) {
                HAPPY("happy"), SAD("sad"), ANGRY("angry");
            }
        """
        doAvailableTest(
            """
            import com.example.Test
            
            fun test() {
                Test(<caret>)
            }
        """,
            """
            import com.example.EmotionType
            import com.example.Test
            
            fun test() {
                Test(emotion = EmotionType.HAPPY)
            }
        """,
            dependencies = listOf(dependency),
        )
    }

    fun `test add default value for java enum`() {
        val dependency = """
            package com.example
            import EmotionType
            class Test(emotion: EmotionType)
        """

        val javaDependency = JavaDependency(
            className = "EmotionType",
            source = """
                public enum EmotionType {
                    HAPPY("happy"), SAD("sad"), ANGRY("angry");
                    public String description;
                    EmotionType(String description) { this.description = description; }
                }
            """,
        )
        doAvailableTest(
            """
            import com.example.Test
            
            fun test() {
                Test(<caret>)
            }
        """,
            """
            import com.example.Test
            
            fun test() {
                Test(emotion = EmotionType.HAPPY)
            }
        """,
            dependencies = listOf(dependency),
            javaDependencies = listOf(javaDependency),
        )
    }

    fun `test don't add default value for abstract,sealed`() {
        doAvailableTest(
            """
            sealed class B(val b: String)
            abstract class C(val c: String)
            class Test(b: B, c: C)
            fun test() {
                Test(<caret>)
            }
        """,
            """
            sealed class B(val b: String)
            abstract class C(val c: String)
            class Test(b: B, c: C)
            fun test() {
                Test(b =, c =)
            }
        """,
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
            dependencies = listOf(dependency),
        )
    }

    fun `test call java constructor`() {
        val javaDependency = JavaDependency(
            className = "Java",
            source = """ 
                public class Java {
                    public Java(String str) {
                    }
                }
            """,
        )
        doUnavailableTest(
            """
            fun test() {
                Java(<caret>)
            }
        """,
            javaDependencies = listOf(javaDependency),
        )
    }

    fun `test call java method`() {
        val javaDependency = JavaDependency(
            className = "Java",
            source = """
                public class Java {
                    public Java(String str) {
                    }
                
                    public void foo(Java java) {
                    }
                }
            """,
        )
        doUnavailableTest(
            """
            fun test() {
                Java("").foo(<caret>)
            }
        """,
            javaDependencies = listOf(javaDependency),
        )
    }

    fun `test fill super type call entry`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
        doAvailableTest(
            """
            open class C(p1: Int, p2: Int)
            class D : C(<caret>)
        """,
            """
            open class C(p1: Int, p2: Int)
            class D : C(p1 = 3521, p2 = 3522)
        """,
        )
    }

    fun `test extension function`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                Foo().foo(x = 120, y = 121)
            }
        """,
            "Fill function with dummy values",
        )
    }

    fun `test imported extension function`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                Foo().foo(x = 120, y = 121)
            }
        """,
            "Fill function with dummy values",
        )
    }

    fun `test do not fill default arguments`() {
        every { ValueGenerator.randomStringFor("name") } returns "John Smith"
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
                User(name = "John Smith")
            }
        """,
            withoutDefaultArguments = true,
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
            dependencies = listOf(dependency),
            withoutDefaultArguments = true,
        )
    }

    fun `test do not add trailing comma`() {
        every { ValueGenerator.randomStringFor("name") } returns "John Smith"
        every { ValueGenerator.randomNumFor("age") } returns 1234
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
                User(name = "John Smith", age = 1234)
            }
        """,
            withTrailingComma = false,
        )
    }

    fun `test add trailing comma`() {
        every { ValueGenerator.randomStringFor("name") } returns "John Smith"
        every { ValueGenerator.randomNumFor("age") } returns 1234
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
                User(name = "John Smith", age = 1234, )
            }
        """,
            withTrailingComma = true,
        )
    }

    fun `test do not add trailing comma if trailing comma already exists`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                     b = 0, c = 99, d = 100,
                 )
             }
         """,
            problemDescription = "Fill function with dummy values",
            withTrailingComma = true,
        )
    }

    fun `test do not put arguments on separate lines`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                foo(a = 97, b = 98, c = 99, d = 100)
            }
        """,
            problemDescription = "Fill function with dummy values",
            putArgumentsOnSeparateLines = false,
        )
    }

    fun `test put arguments on separate lines`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                foo(a = 97,
                        b = 98,
                        c = 99,
                        d = 100)
            }
        """,
            problemDescription = "Fill function with dummy values",
            putArgumentsOnSeparateLines = true,
        )
    }

    fun `test put arguments on separate lines with existing arguments`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
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
                        c = 99,
                        d = 100)
            }
        """,
            problemDescription = "Fill function with dummy values",
            putArgumentsOnSeparateLines = true,
        )
    }

    fun `test move pointer to every argument`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 120<caret>, y = 121, z = 122)
        """,
            problemDescription = "Fill function with dummy values",
            movePointerToEveryArgument = true,
        )
    }

    fun `test move pointer to every argument with existing arguments`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 1<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 1, y = 121<caret>, z = 122)
        """,
            problemDescription = "Fill function with dummy values",
            movePointerToEveryArgument = true,
        )
    }

    fun `test move pointer to every argument with putArgumentsOnSeparateLines`() {
        every { ValueGenerator.randomNumFor(any()) } answers {
            firstArg<String>().hashCode()
        }
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 120<caret>,
                    y = 121,
                    z = 122)
        """,
            problemDescription = "Fill function with dummy values",
            putArgumentsOnSeparateLines = true,
            movePointerToEveryArgument = true,
        )
    }

    private fun doAvailableTest(
        before: String,
        after: String,
        problemDescription: String = "Fill class constructor with dummy values",
        dependencies: List<String> = emptyList(),
        javaDependencies: List<JavaDependency> = emptyList(),
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
        problemDescription: String = "Fill class constructor with dummy values",
        dependencies: List<String> = emptyList(),
        javaDependencies: List<JavaDependency> = emptyList(),
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
            withoutDefaultArguments,
            withTrailingComma,
            putArgumentsOnSeparateLines,
            movePointerToEveryArgument,
        )
        check(highlightInfo == null) { "No problems should be detected at caret" }
    }

    private fun doHighlighting(
        code: String,
        problemDescription: String = "Fill class constructor with dummy values",
        dependencies: List<String>,
        javaDependencies: List<JavaDependency>,
        withoutDefaultArguments: Boolean,
        withTrailingComma: Boolean,
        putArgumentsOnSeparateLines: Boolean,
        movePointerToEveryArgument: Boolean,
    ): HighlightInfo? {
        check("<caret>" in code) { "Please, add `<caret>` marker to\n$code" }

        dependencies.forEachIndexed { index, dependency ->
            myFixture.configureByText("dependency$index.kt", dependency.trimIndent())
        }
        javaDependencies.forEach { (className, source) ->
            myFixture.configureByText("$className.java", source.trimIndent())
        }
        myFixture.configureByText(KotlinFileType.INSTANCE, code.trimIndent())

        val inspection = FillDummyValuesInspection().apply {
            this.withoutDefaultArguments = withoutDefaultArguments
            this.withTrailingComma = withTrailingComma
            this.putArgumentsOnSeparateLines = putArgumentsOnSeparateLines
            this.movePointerToEveryArgument = movePointerToEveryArgument
        }
        myFixture.enableInspections(inspection)

        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)
        val inspectionProfile = inspectionProfileManager.currentProfile
        val state = inspectionProfile.getToolDefaultState(inspection.shortName, project)
        state.level = HighlightDisplayLevel.WARNING

        val caretOffset = myFixture.caretOffset
        return myFixture.doHighlighting().singleOrNull {
            it.inspectionToolId == "FillDummyValues" &&
                it.description == problemDescription &&
                caretOffset in it.startOffset..it.endOffset
        }
    }

    data class JavaDependency(val className: String, @Language("java") val source: String)
}
