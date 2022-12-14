package com.github.suusan2go.kotlinfillclass.inspections

class FillDummyValuesInspection(
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
}