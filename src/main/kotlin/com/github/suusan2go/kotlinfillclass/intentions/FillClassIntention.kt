package com.github.suusan2go.kotlinfillclass.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class FillClassIntention : SelfTargetingIntention<KtValueArgumentList>(KtValueArgumentList::class.java, "Fill class constructor") {
    override fun isApplicableTo(element: KtValueArgumentList, caretOffset: Int): Boolean {
        val descriptor = element.descriptor() ?: return false
        if (descriptor.valueParameters.size == element.arguments.size) return false
        text = (if (descriptor is ClassConstructorDescriptor) "Fill class constructor" else "Fill function")
        return true
    }

    override fun applyTo(element: KtValueArgumentList, editor: Editor?) {
        val parameters = element.descriptor()?.valueParameters ?: return
        createParameterSetterExpression(element, parameters)
    }

    private fun KtValueArgumentList.descriptor(): FunctionDescriptor? {
        val calleeExpression = getStrictParentOfType<KtCallExpression>()?.calleeExpression ?: return null
        val context = calleeExpression.analyze(BodyResolveMode.PARTIAL)
        val descriptor = calleeExpression.getReferenceTargets(context).firstOrNull() as? FunctionDescriptor
        return descriptor.takeIf { it is ClassConstructorDescriptor || it is SimpleFunctionDescriptor }
    }

    private fun createParameterSetterExpression(element: KtValueArgumentList, parameters: List<ValueParameterDescriptor>) {
        val arguments = element.arguments
        val argumentNames = arguments.mapNotNull { it.getArgumentName()?.asName?.identifier }
        val factory = KtPsiFactory(element.project)
        parameters.forEachIndexed { index, parameter ->
            if (arguments.size > index && !arguments[index].isNamed()) return@forEachIndexed
            if (parameter.name.identifier in argumentNames) return@forEachIndexed
            val defaultValue = createDefaultValueFromParameter(parameter)
            val newArgument = factory.createArgument(factory.createExpression(defaultValue), parameter.name)
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
            else -> "TODO()"
        }
    }
}
