package com.github.suusan2go.kotlinfillclass.inspections.k2

import org.jetbrains.kotlin.psi.KtValueArgumentList

class FillEmptyValuesInspection : BaseFillClassInspection() {
    override fun getActionFamilyName(): String = "Fill class constructor / function"

    override fun getConstructorPromptDescription(): String = "Fill class constructor"

    override fun getFunctionPromptDescription(): String = "Fill function"
}