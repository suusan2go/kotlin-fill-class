package com.github.suusan2go.kotlinfillclass.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import javax.swing.JComponent

class FillClassInspection(
    @JvmField var withoutDefaultValues: Boolean = false
) : AbstractKotlinInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) = valueArgumentListVisitor(fun(element: KtValueArgumentList) {
        val descriptor = element.descriptor() ?: return
        if (descriptor.valueParameters.size == element.arguments.size) return
        val description = if (descriptor is ClassConstructorDescriptor) "Fill class constructor" else "Fill function"
        val fix = FillClassFix(description, withoutDefaultValues)
        holder.registerProblem(element, description, fix)
    })

    override fun createOptionsPanel(): JComponent {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Fill arguments without default values", "withoutDefaultValues")
        return panel
    }
}

private fun KtValueArgumentList.descriptor(): CallableDescriptor? {
    val calleeExpression = getStrictParentOfType<KtCallElement>()?.calleeExpression ?: return null
    val descriptor = calleeExpression.resolveToCall()?.resultingDescriptor
    if (descriptor is JavaCallableMemberDescriptor) return null
    return descriptor.takeIf { it is ClassConstructorDescriptor || it is SimpleFunctionDescriptor }
}

class FillClassFix(
    private val description: String,
    private val withoutDefaultValues: Boolean
) : LocalQuickFix {
    override fun getName() = description

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtValueArgumentList ?: return
        val parameters = element.descriptor()?.valueParameters ?: return
        element.fillArguments(parameters)
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

    private fun createDefaultValueArgument(
        parameter: ValueParameterDescriptor,
        factory: KtPsiFactory
    ): KtValueArgument {
        if (withoutDefaultValues) {
            return factory.createArgument(null, parameter.name)
        }

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
