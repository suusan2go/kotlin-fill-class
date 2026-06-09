package com.github.suusan2go.kotlinfillclass.inspections.k2

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol

class FillVariablesOfSameNameInspection : BaseFillClassInspection() {
    override val constructorPromptDescription: String
        get() = "Fill class constructor with variables of the same name"

    override val functionPromptDescription: String
        get() = "Fill function with variables of the same name"

    override val alwaysFillArgumentValues: Boolean
        get() = true

    override fun fillValue(
        session: KaSession,
        signature: KaVariableSignature<KaValueParameterSymbol>,
    ): String? = signature.name.asString()
}
