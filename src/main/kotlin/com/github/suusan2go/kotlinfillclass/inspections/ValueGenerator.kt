package com.github.suusan2go.kotlinfillclass.inspections

import com.thedeanda.lorem.LoremIpsum

object ValueGenerator {
    private var staticNum = 0

    internal fun randomNumFor(paramName: String): Int {
        return when {
            paramName.contains("year", ignoreCase = true) -> (1980..2023).random()
            else -> (1111..9999).random()
        }
    }

    internal fun randomStringFor(_paramName: String): String {
        val lorem = LoremIpsum.getInstance()
        val paramName = _paramName.lowercase()
        return when {
            // names
            paramName.contains("url", ignoreCase = true) -> lorem.url
            paramName.contains("city", ignoreCase = true) -> lorem.city
            paramName.contains("country", ignoreCase = true) -> lorem.country
            paramName.contains("email", ignoreCase = true) -> lorem.email
            paramName.contains("phone", ignoreCase = true) -> lorem.phone
            paramName.contains("state", ignoreCase = true) -> lorem.stateFull
            paramName.contains("zip", ignoreCase = true) -> lorem.zipCode
            paramName.contains("name", ignoreCase = true) -> lorem.name
            else -> lorem.getWords(0)
        }
    }

    fun getRandomNumber() = staticNum++
}