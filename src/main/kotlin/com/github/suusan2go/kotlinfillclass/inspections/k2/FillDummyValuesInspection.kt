package com.github.suusan2go.kotlinfillclass.inspections.k2

import com.github.suusan2go.kotlinfillclass.inspections.ValueGenerator
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType

class FillDummyValuesInspection : FillEmptyValuesInspection() {
    override val constructorPromptDescription: String
        get() = "Fill class constructor with dummy values"

    override val functionPromptDescription: String
        get() = "Fill function with dummy values"

    override val alwaysFillArgumentValues: Boolean
        get() = true

    override fun fillValue(
        session: KaSession,
        signature: KaVariableSignature<KaValueParameterSymbol>,
    ): String? {
        with(session) {
            val type = signature.returnType
            val paramName = signature.name.asString()
            if (type !is KaClassType) {
                return null
            }
            return when {
                type.isCharType -> "'${ValueGenerator.getRandomChar()}'"
                type.isDoubleType -> "${ValueGenerator.getRandomNumber()}.${ValueGenerator.getRandomNumber()}"
                type.isFloatType -> "${ValueGenerator.getRandomNumber()}.${ValueGenerator.getRandomNumber()}f"
                type.isIntType ||
                    type.isLongType ||
                    type.isShortType -> "${ValueGenerator.randomNumFor(paramName)}"
                type.isCharSequenceType ||
                    type.isStringType -> "\"${ValueGenerator.randomStringFor(paramName)}\""
                else -> super@FillDummyValuesInspection.fillValue(session, signature)
            }
        }
    }
}
