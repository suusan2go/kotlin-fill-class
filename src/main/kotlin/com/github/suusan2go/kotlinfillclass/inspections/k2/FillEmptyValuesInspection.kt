package com.github.suusan2go.kotlinfillclass.inspections.k2


class FillEmptyValuesInspection : BaseFillClassInspection() {
    override fun getConstructorPromptDescription(): String = "Fill class constructor"

    override fun getFunctionPromptDescription(): String = "Fill function"
}