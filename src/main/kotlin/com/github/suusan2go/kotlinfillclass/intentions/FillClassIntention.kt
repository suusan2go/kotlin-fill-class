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
        val callExpression = element.getStrictParentOfType<KtCallExpression>() ?: return false
        val calleeExpression = callExpression.calleeExpression ?: return false
        val analysisResult = calleeExpression.analyzeAndGetResult()
        val classDescriptor = calleeExpression
                .getReferenceTargets(analysisResult.bindingContext)
                .mapNotNull { (it as? ConstructorDescriptor)?.containingDeclaration }
                .distinct()
                .singleOrNull() ?: return false
        val parameters = classDescriptor.constructors.first().valueParameters
        return element.arguments.size != parameters.size
    }

    override fun applyTo(element: KtValueArgumentList, editor: Editor?) {
        val callExpression = element.getStrictParentOfType<KtCallExpression>() ?: return
        val calleeExpression = callExpression.calleeExpression ?: return
        val analysisResult = calleeExpression.analyzeAndGetResult()
        val classDescriptor = calleeExpression
                .getReferenceTargets(analysisResult.bindingContext)
                .mapNotNull { (it as? ConstructorDescriptor)?.containingDeclaration }
                .distinct()
                .singleOrNull() ?: return
        val parameters = classDescriptor.constructors.first().valueParameters

        val factory = KtPsiFactory(project = element.project)
        val argument = factory.createExpression("""${classDescriptor.name.identifier}(
            ${createParameterSetterExpression(parameters)}
            )""".trimMargin())
        callExpression.replace(argument)
        return
    }

    override fun startInWriteAction() = true

    private fun createParameterSetterExpression(parameters: List<ValueParameterDescriptor>): String {
        var result = ""
        parameters.forEach { parameter ->
            var parameterString = "${parameter.name.identifier} = ${createDefaultValueFromParameter(parameter)},\n".let {
                if(parameters.last() == parameter) return@let it.replace(",\n","")
                it
            }
            result = "$result$parameterString"
        }
        return result
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
