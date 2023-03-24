package com.github.suusan2go.kotlinfillclass.inspections

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * [ValueGenerator] tests
 * Each value is randomly generated, so repeat 10 times.
 */
class ValueGeneratorTest : BasePlatformTestCase() {

    fun `test randomNumFor should generate positive integer number`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomNumFor("") in 0 until 10000)
        }
    }

    fun `test randomNumFor should generate year-like integer number`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomNumFor("year") in 1000 until 2100)
        }
    }

    fun `test randomStringFor should generate single word`() {
        repeat(10) {
            val result = ValueGenerator.randomStringFor("")
            assertTrue(result.matches("^[a-zA-Z0-9]+\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate url string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("url").matches("^https?://[a-z0-9.-]+/.*".toRegex()))
        }
    }

    fun `test randomStringFor should generate city string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("city").matches("^[A-Z][a-z']*( [A-Z][a-z']*)*\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate country string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("country").matches("^[A-Z].+\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate email string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("email").matches("^[a-z0-9._-]+@[a-z0-9.-]+\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate phone string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("phone").matches("^\\(\\d+\\) \\d+-\\d+\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate state string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("state").matches("^[A-Z].+\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate zip code string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("zip").matches("^\\d{5}\$".toRegex()))
        }
    }

    fun `test randomStringFor should generate name string`() {
        repeat(10) {
            assertTrue(ValueGenerator.randomStringFor("name").matches("^[A-Z][a-z]* [A-Z][a-z]*\$".toRegex()))
        }
    }

    fun `test getRandomNumber should generate some number`() {
        repeat(10) {
            assertTrue(ValueGenerator.getRandomNumber() >= 0)
        }
    }

    fun `test getRandomChar should generate english alphabet character`() {
        repeat(10) {
            assertTrue(ValueGenerator.getRandomChar() in 'A'..'Z')
        }
    }
}
