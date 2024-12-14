package com.github.suusan2go.kotlinfillclass.inspections

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [ValueGenerator] tests
 * Each value is randomly generated, so repeat 10 times.
 */
class ValueGeneratorTest {

    @Test
    fun `test randomNumFor should generate positive integer number`() {
        repeat(1000) {
            assertTrue(ValueGenerator.randomNumFor("") in 0 until 10000)
        }
    }

    @Test
    fun `test randomNumFor should generate year-like integer number`() {
        repeat(1000) {
            assertTrue(ValueGenerator.randomNumFor("year") in 1000 until 2100)
        }
    }

    @Test
    fun `test randomStringFor should generate single word`() {
        repeat(1000) {
            val result = ValueGenerator.randomStringFor("")
            assertThat(result).matches("^[a-zA-Z0-9]+\$")
        }
    }

    @Test
    fun `test randomStringFor should generate url string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("url")).matches("^https?://[a-z0-9.-]+/.*")
        }
    }

    @Test
    fun `test randomStringFor should generate city string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("city")).matches("^[A-Za-zà' ]*\$")
        }
    }

    @Test
    fun `test randomStringFor should generate country string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("country")).matches("^[A-Z].+\$")
        }
    }

    @Test
    fun `test randomStringFor should generate email string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("email")).matches("^[a-z0-9._-]+@[a-z0-9.-]+\$")
        }
    }

    @Test
    fun `test randomStringFor should generate phone string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("phone")).matches("^\\(\\d+\\) \\d+-\\d+\$")
        }
    }

    @Test
    fun `test randomStringFor should generate state string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("state")).matches("^[A-Z].+\$")
        }
    }

    @Test
    fun `test randomStringFor should generate zip code string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("zip")).matches("^\\d{5}\$")
        }
    }

    @Test
    fun `test randomStringFor should generate name string`() {
        repeat(1000) {
            assertThat(ValueGenerator.randomStringFor("name")).matches("^[A-Za-z]+ [A-Za-z' ]+\$")
        }
    }

    @Test
    fun `test getRandomNumber should generate some number`() {
        repeat(1000) {
            assertTrue(ValueGenerator.getRandomNumber() >= 0)
        }
    }

    @Test
    fun `test getRandomChar should generate english alphabet character`() {
        repeat(1000) {
            assertTrue(ValueGenerator.getRandomChar() in 'A'..'Z')
        }
    }
}
