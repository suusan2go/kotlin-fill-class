package com.github.suusan2go.kotlinfillclass.intentions

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

class FillClassIntentionTest : BasePlatformTestCase() {
    fun `test fill class constructor`() {
        doAvailableTest("""
            class User(val name: String, val age: Int)
            fun test() {
                User(<caret>)
            }
        """, """
            class User(val name: String, val age: Int)
            fun test() {
                User(name = "", age = 0)
            }
        """)
    }

    fun `test can't fill class constructor`() {
        doUnavailableTest("""
            class User(val name: String, val age: Int)
            fun test() {
                User("", 0<caret>)
            }
        """)
    }

    fun `test fill function`() {
        doAvailableTest("""
            class User(val name: String, val age: Int)
            fun foo(s: String, t: Int, u: User) {}
            fun test() {
                foo(<caret>)
            }
        """, """
            class User(val name: String, val age: Int)
            fun foo(s: String, t: Int, u: User) {}
            fun test() {
                foo(s = "", t = 0, u = User(name = "", age = 0))
            }
        """, "Fill function")
    }

    fun `test can't fill function`() {
        doUnavailableTest("""
            fun foo(s: String, t: Int) {}            
            fun test() {
                foo("", 0<caret>)
            }
        """)
    }

    fun `test fill for non primitive types`() {
        doAvailableTest("""
            class A(a1: String, a2: Int)
            class B(b1: Int, b2: String, a: A)
            class C
            class D(a: A, b: B, c: C, r: Runnable)
            fun test() {
                D(<caret>)
            }
        """, """
            class A(a1: String, a2: Int)
            class B(b1: Int, b2: String, a: A)
            class C
            class D(a: A, b: B, c: C, r: Runnable)
            fun test() {
                D(a = A(a1 = "", a2 = 0), b = B(b1 = 0, b2 = "", a = A(a1 = "", a2 = 0)), c = C(), r =)
            }
        """)
    }

    private val intention = FillClassIntention()

    private fun doAvailableTest(before: String, after: String, intentionText: String = "Fill class constructor") {
        checkCaret(before)
        myFixture.configureByText(KotlinFileType.INSTANCE, before.trimIndent())
        myFixture.launchAction(intention)
        check(intentionText == intention.text) {
            "Intention text mismatch. [expected]$intentionText [actual]${intention.text}"
        }
        myFixture.checkResult(after.trimIndent())
    }

    private fun doUnavailableTest(before: String) {
        checkCaret(before)
        myFixture.configureByText(KotlinFileType.INSTANCE, before.trimIndent())
        check(intention.familyName !in myFixture.availableIntentions.mapNotNull { it.familyName }) {
            "Intention should not be available"
        }
    }

    private fun checkCaret(before: String) {
        check("<caret>" in before) {
            "Please, add `<caret>` marker to\n$before"
        }
    }
}
