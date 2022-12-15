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

    override fun getConstructorPromptTitle(): String {
        return "Fill constructor"
    }
    override fun getFunctionPromptTitle(): String {
        return "Fill function"
    }
}
