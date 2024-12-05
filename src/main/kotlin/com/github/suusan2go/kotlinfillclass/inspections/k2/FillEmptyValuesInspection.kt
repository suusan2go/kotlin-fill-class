package com.github.suusan2go.kotlinfillclass.inspections.k2

import com.intellij.psi.PsiEnumConstant
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinNameSuggester
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.codeinsight.utils.isEnum
import org.jetbrains.kotlin.idea.codeinsight.utils.isNullableAnyType
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType


open class FillEmptyValuesInspection : BaseFillClassInspection() {
    override fun getConstructorPromptDescription(): String = "Fill class constructor"

    override fun getFunctionPromptDescription(): String = "Fill function"

    context(KaSession) override fun fillValue(signature: KaVariableSignature<KaValueParameterSymbol>): String? {
        val type = signature.returnType
        if (type !is KaClassType) {
            return null
        }
        return when {
            type.isBooleanType -> "false"
            type.isCharType -> "''"
            type.isDoubleType -> "0.0"
            type.isFloatType -> "0.0f"
            type.isIntType ||
                    type.isLongType ||
                    type.isShortType -> "0"

            type.isArrayOrPrimitiveArray -> "arrayOf()"
            type.isNullableAnyType() -> "null"
            type.isCharSequenceType ||
                    type.isStringType -> "\"\""

            type.classId in listOf(
                StandardClassIds.List,
                StandardClassIds.MutableList,
                StandardClassIds.Collection
            ) -> "listOf()"

            type.classId in listOf(StandardClassIds.Set, StandardClassIds.MutableSet) -> "setOf()"
            type.classId in listOf(StandardClassIds.Map, StandardClassIds.MutableMap) -> "mapOf()"
            type.isFunctionType -> type.lambdaDefaultValue()
            type.isEnum() -> type.firstEnumValueOrNull()
            type.isMarkedNullable -> "null"
            else -> null
        }
    }

    context(KaSession) private fun KaClassType.lambdaDefaultValue(): String = buildString {
        append("{")
        if (typeArguments.size > 2) {
            val validator = CollectingNameValidator()
            val lambdaParameters = typeArguments.dropLast(1).joinToString(postfix = "->") {
                val type = it.type ?: return@joinToString ""
                val suggester = KotlinNameSuggester()
                val name = KotlinNameSuggester.suggestNameByName(suggester.suggestTypeNames(type).first(), validator)
                validator.addName(name)
                val typeText = (type as? KaClassType)?.classId?.asFqNameString()
                val nullable = if (type.isMarkedNullable) "?" else ""
                "$name: $typeText$nullable"
            }
            append(lambdaParameters)
        }
        append("}")
    }

    private fun KaClassType.firstEnumValueOrNull(): String? {
        val psi = symbol.psi ?: return null
        return (psi as? KtClass)?.declarations?.firstOrNull()?.kotlinFqName?.asString() // Kotlin Enum
            ?: psi.getChildrenOfType<PsiEnumConstant>().firstOrNull()?.kotlinFqName?.asString() // Java Enum
    }
}