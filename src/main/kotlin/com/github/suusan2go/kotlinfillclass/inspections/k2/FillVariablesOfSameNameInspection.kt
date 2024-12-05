package com.github.suusan2go.kotlinfillclass.inspections.k2

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol


class FillVariablesOfSameNameInspection : BaseFillClassInspection() {
    override fun getConstructorPromptDescription(): String = "Fill class constructor with variables of the same name"

    override fun getFunctionPromptDescription(): String = "Fill class constructor with variables of the same name"

    context(KaSession) override fun fillValue(signature: KaVariableSignature<KaValueParameterSymbol>): String? {
        return signature.name.asString()
    }
}