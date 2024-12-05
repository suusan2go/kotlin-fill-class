package com.github.suusan2go.kotlinfillclass.inspections.k2

import com.github.suusan2go.kotlinfillclass.inspections.ValueGenerator
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType


class FillDummyValuesInspection : FillEmptyValuesInspection() {
    override fun getConstructorPromptDescription(): String = "Fill class constructor with dummy values"

    override fun getFunctionPromptDescription(): String = "Fill function with dummy values"

    context(KaSession) override fun fillValue(signature: KaVariableSignature<KaValueParameterSymbol>): String? {
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
            else -> super.fillValue(signature)
        }
    }
}