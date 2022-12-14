package com.github.suusan2go.kotlinfillclass.inspections

class FillEmptyValuesInspection(
    withoutDefaultValues: Boolean = false,
    withoutDefaultArguments: Boolean = false,
    withTrailingComma: Boolean = false,
    putArgumentsOnSeparateLines: Boolean = false,
    movePointerToEveryArgument: Boolean = true
) : BaseFillClassInspection(
    withoutDefaultValues,
    withoutDefaultArguments,
    withTrailingComma,
    putArgumentsOnSeparateLines,
    movePointerToEveryArgument
) {
    override fun shouldGenerateDummyValues(): Boolean {
        return false
    }

    override fun getPromptTitle(): String {
        return "Fill with empty values"
    }
}