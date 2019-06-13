package com.github.suusan2go.kotlinfillclass.intentions

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

class FillClassIntentionTest : LightPlatformCodeInsightFixtureTestCase() {
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
                foo(s = "", t = 0, u =)
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
