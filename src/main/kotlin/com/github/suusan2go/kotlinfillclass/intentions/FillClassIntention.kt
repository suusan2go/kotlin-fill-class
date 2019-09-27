package com.github.suusan2go.kotlinfillclass.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor

class FillClassIntention : SelfTargetingIntention<KtValueArgumentList>(KtValueArgumentList::class.java, "Fill class constructor") {
    override fun isApplicableTo(element: KtValueArgumentList, caretOffset: Int): Boolean {
        val descriptor = element.descriptor() ?: return false
        if (descriptor.valueParameters.size == element.arguments.size) return false
        text = if (descriptor is ClassConstructorDescriptor) "Fill class constructor" else "Fill function"
        return true
    }

    override fun applyTo(element: KtValueArgumentList, editor: Editor?) {
        val parameters = element.descriptor()?.valueParameters ?: return
        element.fillArguments(parameters)
    }

    private fun KtValueArgumentList.descriptor(): FunctionDescriptor? {
        val calleeExpression = getStrictParentOfType<KtCallExpression>()?.calleeExpression ?: return null
        val context = calleeExpression.analyze(BodyResolveMode.PARTIAL)
        val descriptor = calleeExpression.getReferenceTargets(context).firstOrNull() as? FunctionDescriptor
        if (descriptor is JavaCallableMemberDescriptor) return null
        return descriptor.takeIf { it is ClassConstructorDescriptor || it is SimpleFunctionDescriptor }
    }

    private fun KtValueArgumentList.fillArguments(parameters: List<ValueParameterDescriptor>) {
        val arguments = this.arguments
        val argumentNames = arguments.mapNotNull { it.getArgumentName()?.asName?.identifier }
        val factory = KtPsiFactory(this)
        parameters.forEachIndexed { index, parameter ->
            if (arguments.size > index && !arguments[index].isNamed()) return@forEachIndexed
            if (parameter.name.identifier in argumentNames) return@forEachIndexed
            val added = addArgument(createDefaultValueArgument(parameter, factory))
            val argumentExpression = added.getArgumentExpression()
            if (argumentExpression is KtQualifiedExpression) {
                ShortenReferences.DEFAULT.process(argumentExpression)
            }
        }
    }

    private fun createDefaultValueArgument(parameter: ValueParameterDescriptor, factory: KtPsiFactory): KtValueArgument {
        val type = parameter.type
        val defaultValue = when {
            KotlinBuiltIns.isBoolean(type) -> "false"
            KotlinBuiltIns.isChar(type) -> "''"
            KotlinBuiltIns.isDouble(type) -> "0.0"
            KotlinBuiltIns.isFloat(type) -> "0.0f"
            KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isLong(type) || KotlinBuiltIns.isShort(type) -> "0"
            KotlinBuiltIns.isCollectionOrNullableCollection(type) -> "arrayOf()"
            KotlinBuiltIns.isNullableAny(type) -> "null"
            KotlinBuiltIns.isString(type) -> "\"\""
            KotlinBuiltIns.isListOrNullableList(type) -> "listOf()"
            KotlinBuiltIns.isSetOrNullableSet(type) -> "setOf()"
            KotlinBuiltIns.isMapOrNullableMap(type) -> "mapOf()"
            type.isMarkedNullable -> "null"
            else -> null
        }
        if (defaultValue != null) {
            return factory.createArgument(factory.createExpression(defaultValue), parameter.name)
        }

        val descriptor = type.constructor.declarationDescriptor as? LazyClassDescriptor
        val modality = descriptor?.modality
        if (descriptor?.kind == ClassKind.ENUM_CLASS || modality == Modality.ABSTRACT || modality == Modality.SEALED) {
            return factory.createArgument(null, parameter.name)
        }

        val fqName = descriptor?.importableFqName?.asString()
        val valueParameters =
                descriptor?.constructors?.firstOrNull { it is ClassConstructorDescriptor }?.valueParameters
        val argumentExpression = if (fqName != null && valueParameters != null) {
            (factory.createExpression("$fqName()")).also {
                val callExpression = it as? KtCallExpression ?: (it as? KtQualifiedExpression)?.callExpression
                callExpression?.valueArgumentList?.fillArguments(valueParameters)
            }
        } else {
            null
        }
        return factory.createArgument(argumentExpression, parameter.name)
    }
}
