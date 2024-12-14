package com.github.suusan2go.kotlinfillclass.inspections.k2

import com.github.suusan2go.kotlinfillclass.inspections.util.PluginTestUtils
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.util.Disposer
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinFileType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for [FillVariablesOfSameNameInspection].
 * Add system property `idea.kotlin.plugin.use.k2=true` to run this test.
 */
class FillVariablesOfSameNameInspectionTest {
    private var _myFixture: CodeInsightTestFixture? = null
    private val myFixture: CodeInsightTestFixture
        get() = requireNotNull(_myFixture)

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        _myFixture = PluginTestUtils.createMyFixture(testInfo)
        myFixture.setUp()
    }

    @AfterEach
    fun tearDown() {
        myFixture.tearDown()
        _myFixture = null
    }

    @Test
    fun `test fill class constructor`() {
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
                User(name = name, age = age, pass = pass)
            }
        """,
        )
    }

    @Test
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

    @Test
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
                foo(s = s, t = t, u = u)
            }
        """,
            "Fill class constructor with variables of the same name",
        )
    }

    @Test
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

    @Test
    fun `test can't fill function with lambda argument`() {
        doUnavailableTest(
            """
            fun foo(a: Int, b: String, block: () -> Unit) {}
            fun main() {
                foo(1, "b"<caret>) {}
            }
        """,
            "Fill class constructor with variables of the same name\"",
        )
    }

    @Test
    fun `test fill function with lambda argument`() {
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
                foo(1, b = b) {}
            }
        """,
            "Fill class constructor with variables of the same name",
        )
    }

    @Test
    fun `test fill function with lambda argument and trailing comma`() {
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
                foo(
                    1,
                    b = b,
                ) {}
            }
        """,
            "Fill class constructor with variables of the same name",
            withTrailingComma = true,
            putArgumentsOnSeparateLines = true,
        )
    }

    @Test
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
                D(a = a, b = b, c = c, r = r)
            }
        """,
        )
    }

    @Test
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
                Test(emotion = emotion)
            }
        """,
        )
    }

    @Test
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
            import com.example.Test
            
            fun test() {
                Test(emotion = emotion)
            }
        """,
            dependencies = listOf(dependency),
        )
    }

    @Test
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
                Test(emotion = emotion)
            }
        """,
            dependencies = listOf(dependency),
            javaDependencies = listOf(javaDependency),
        )
    }

    @Test
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
                Test(b = b, c = c)
            }
        """,
        )
    }

    @Test
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
            import com.example.B
            
            val b = B(a = a)
        """,
            dependencies = listOf(dependency),
        )
    }

    @Test
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

    @Test
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

    @Test
    fun `test fill super type call entry`() {
        doAvailableTest(
            """
            open class C(p1: Int, p2: Int)
            class D : C(<caret>)
        """,
            """
            open class C(p1: Int, p2: Int)
            class D : C(p1 = p1, p2 = p2)
        """,
        )
    }

    @Test
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
                Foo().foo(x = x, y = y)
            }
        """,
            "Fill class constructor with variables of the same name",
        )
    }

    @Test
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
                Foo().foo(x = x, y = y)
            }
        """,
            "Fill class constructor with variables of the same name",
        )
    }

    @Test
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
                User(name = name)
            }
        """,
            withoutDefaultArguments = true,
        )
    }

    @Test
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
            import foo.B
            
            fun test() {
                B(f1 = f1, f2 = f2, f3 = f3)
            }
        """,
            dependencies = listOf(dependency),
            withoutDefaultArguments = true,
        )
    }

    @Test
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
                User(name = name, age = age)
            }
        """,
            withTrailingComma = false,
        )
    }

    @Test
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
                User(name = name, age = age,)
            }
        """,
            withTrailingComma = true,
        )
    }

    @Test
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
                     b = 0, c = c, d = d,
                 )
             }
         """,
            problemDescription = "Fill class constructor with variables of the same name",
            withTrailingComma = true,
        )
    }

    @Test
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
                foo(a = a, b = b, c = c, d = d)
            }
        """,
            problemDescription = "Fill class constructor with variables of the same name",
            putArgumentsOnSeparateLines = false,
        )
    }

    @Test
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
                foo(
                    a = a,
                    b = b,
                    c = c,
                    d = d
                )
            }
        """,
            problemDescription = "Fill class constructor with variables of the same name",
            putArgumentsOnSeparateLines = true,
        )
    }

    @Test
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
                foo(
                    a = 1,
                    b = 2,
                    c = c,
                    d = d
                )
            }
        """,
            problemDescription = "Fill class constructor with variables of the same name",
            putArgumentsOnSeparateLines = true,
        )
    }

    @Test
    fun `test move pointer to every argument`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = x<caret>, y = y, z = z)
        """,
            problemDescription = "Fill class constructor with variables of the same name",
            movePointerToEveryArgument = true,
        )
    }

    @Test
    fun `test move pointer to every argument with existing arguments`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 1<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(x = 1, y = y<caret>, z = z)
        """,
            problemDescription = "Fill class constructor with variables of the same name",
            movePointerToEveryArgument = true,
        )
    }

    @Test
    fun `test move pointer to every argument with putArgumentsOnSeparateLines`() {
        doAvailableTest(
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(<caret>)
        """,
            """
            fun foo(x: Int, y: Int, z: Int) = x + y + z
            val bar = foo(
                x = x<caret>,
                y = y,
                z = z
            )
        """,
            problemDescription = "Fill class constructor with variables of the same name",
            putArgumentsOnSeparateLines = true,
            movePointerToEveryArgument = true,
        )
    }

    private fun doAvailableTest(
        before: String,
        after: String,
        problemDescription: String = "Fill class constructor with variables of the same name",
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
        problemDescription: String = "Fill class constructor with variables of the same name",
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
        problemDescription: String = "Fill class constructor with variables of the same name",
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
        val targetCode = code.trimIndent()
        myFixture.configureByText(KotlinFileType.INSTANCE, targetCode)

        val inspection = FillVariablesOfSameNameInspection().apply {
            this.withoutDefaultArguments = withoutDefaultArguments
            this.withTrailingComma = withTrailingComma
            this.putArgumentsOnSeparateLines = putArgumentsOnSeparateLines
            this.movePointerToEveryArgument = movePointerToEveryArgument
        }
        myFixture.enableInspections(inspection)

        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(myFixture.project)
        val inspectionProfile = inspectionProfileManager.currentProfile
        val state = inspectionProfile.getToolDefaultState(inspection.shortName, myFixture.project)
        state.level = HighlightDisplayLevel.WARNING

        val caretOffset = targetCode.indexOf("<caret>")
        return myFixture.doHighlighting().singleOrNull {
            it.inspectionToolId == "FillVariablesOfSameName" &&
                    it.description == problemDescription &&
                    caretOffset in it.startOffset..it.endOffset
        }
    }

    data class JavaDependency(val className: String, @Language("java") val source: String)
}
