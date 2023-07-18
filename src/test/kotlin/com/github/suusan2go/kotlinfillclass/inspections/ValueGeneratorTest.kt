package com.github.suusan2go.kotlinfillclass.inspections

import com.github.suusan2go.kotlinfillclass.inspections.matchers.RegexMatcher
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.hamcrest.MatcherAssert.assertThat

/**
 * [ValueGenerator] tests
 * Each value is randomly generated, so repeat 10 times.
 */
class ValueGeneratorTest : BasePlatformTestCase() {

    fun `test randomNumFor should generate positive integer number`() {
        repeat(1000) {
            assertTrue(ValueGenerator.randomNumFor("") in 0 until 10000)
        }
    }

    fun `test randomNumFor should generate year-like integer number`() {
        repeat(1000) {
            assertTrue(ValueGenerator.randomNumFor("year") in 1000 until 2100)
        }
    }

    fun `test randomStringFor should generate single word`() {
        repeat(1000) {
            val result = ValueGenerator.randomStringFor("")
            assertThat(result, RegexMatcher("^[a-zA-Z0-9]+\$"))
        }
    }

    fun `test randomStringFor should generate url string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("url"), RegexMatcher("^https?://[a-z0-9.-]+/.*"))
        }
    }

    fun `test randomStringFor should generate city string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("city"), RegexMatcher("^[A-Za-zà' ]*\$"))
        }
    }

    fun `test randomStringFor should generate country string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("country"), RegexMatcher("^[A-Z].+\$"))
        }
    }

    fun `test randomStringFor should generate email string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("email"), RegexMatcher("^[a-z0-9._-]+@[a-z0-9.-]+\$"))
        }
    }

    fun `test randomStringFor should generate phone string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("phone"), RegexMatcher("^\\(\\d+\\) \\d+-\\d+\$"))
        }
    }

    fun `test randomStringFor should generate state string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("state"), RegexMatcher("^[A-Z].+\$"))
        }
    }

    fun `test randomStringFor should generate zip code string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("zip"), RegexMatcher("^\\d{5}\$"))
        }
    }

    fun `test randomStringFor should generate name string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("name"), RegexMatcher("^[A-Za-z]+ [A-Za-z' ]+\$"))
        }
    }

    fun `test getRandomNumber should generate some number`() {
        repeat(1000) {
            assertTrue(ValueGenerator.getRandomNumber() >= 0)
        }
    }

    fun `test getRandomChar should generate english alphabet character`() {
        repeat(1000) {
            assertTrue(ValueGenerator.getRandomChar() in 'A'..'Z')
        }
    }
}
