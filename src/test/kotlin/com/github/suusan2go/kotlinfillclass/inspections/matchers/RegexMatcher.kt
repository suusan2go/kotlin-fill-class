package com.github.suusan2go.kotlinfillclass.inspections.matchers

import org.hamcrest.CustomTypeSafeMatcher
import org.intellij.lang.annotations.Language

class RegexMatcher(@Language("RegExp") val regex: String) :
    CustomTypeSafeMatcher<String>("matches regex $regex") {
    override fun matchesSafely(item: String?): Boolean {
        return item?.matches(regex.toRegex()) ?: false
    }
}
