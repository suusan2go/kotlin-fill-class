package com.github.suusan2go.kotlinfillclass.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.util.constructors


class FillClassIntention : SelfTargetingIntention<KtValueArgumentList>(KtValueArgumentList::class.java, "Fill class constructor") {
    override fun isApplicableTo(element: KtValueArgumentList, caretOffset: Int): Boolean {
        val parameters = element.getValueParameters() ?: return false
        return element.arguments.size != parameters.size
    }

    override fun applyTo(element: KtValueArgumentList, editor: Editor?) {
        val parameters = element.getValueParameters() ?: return
        createParameterSetterExpression(element, parameters)
    }

    private fun KtValueArgumentList.getValueParameters(): MutableList<ValueParameterDescriptor>? {
        val callExpression = getStrictParentOfType<KtCallExpression>() ?: return null
        val calleeExpression = callExpression.calleeExpression ?: return null
        val analysisResult = calleeExpression.analyzeAndGetResult()
        val classDescriptor = calleeExpression
                .getReferenceTargets(analysisResult.bindingContext)
                .mapNotNull { (it as? ConstructorDescriptor)?.containingDeclaration }
                .distinct()
                .singleOrNull() ?: return null
        return classDescriptor.constructors.first().valueParameters
    }

    override fun startInWriteAction() = true

    private fun createParameterSetterExpression(element: KtValueArgumentList, parameters: List<ValueParameterDescriptor>) {
        val arguments = element.arguments
        val argumentNames = arguments.map { it.getArgumentName()?.asName?.identifier }.filterNotNull()
        val factory = KtPsiFactory(element.project)
        parameters.forEachIndexed { index, parameter ->
            if (arguments.size > index && !arguments[index].isNamed()) return@forEachIndexed
            if (argumentNames.contains(parameter.name.identifier)) return@forEachIndexed
            val newArgument = factory.createArgument(
                    expression = factory.createExpression(createDefaultValueFromParameter(parameter)),
                    name = parameter.name
            )
            element.addArgument(newArgument)
        }
    }

    private fun createDefaultValueFromParameter(parameter: ValueParameterDescriptor): String {
        val type = parameter.type
        return when {
            KotlinBuiltIns.isBoolean(type) -> "false"
            KotlinBuiltIns.isChar(type) -> "''"
            KotlinBuiltIns.isDouble(type) -> "0.0"
            KotlinBuiltIns.isFloat(type) -> "0.0f"
            KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isLong(type) || KotlinBuiltIns.isShort(type) -> "0"
            KotlinBuiltIns.isCollectionOrNullableCollection(type) -> "emptyArray()"
            KotlinBuiltIns.isNullableAny(type) -> "null"
            KotlinBuiltIns.isString(type) -> "\"\""
            KotlinBuiltIns.isListOrNullableList(type) -> "emptyList()"
            KotlinBuiltIns.isSetOrNullableSet(type) -> "emptySet()"
            type.isMarkedNullable -> "null"
            else -> ""
        }
    }
}
