package com.github.suusan2go.kotlinfillclass.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import javax.swing.JComponent

class FillClassInspection(
    @JvmField var withoutDefaultValues: Boolean = false,
    @JvmField var withoutDefaultArguments: Boolean = false,
    @JvmField var withTrailingComma: Boolean = false
) : AbstractKotlinInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) = valueArgumentListVisitor(fun(element: KtValueArgumentList) {
        val descriptor = element.descriptor() ?: return
        if (descriptor.valueParameters.size == element.arguments.size) return
        val description = if (descriptor is ClassConstructorDescriptor) "Fill class constructor" else "Fill function"
        val fix = FillClassFix(
            description = description,
            withoutDefaultValues = withoutDefaultValues,
            withoutDefaultArguments = withoutDefaultArguments,
            withTrailingComma = withTrailingComma
        )
        holder.registerProblem(element, description, fix)
    })

    override fun createOptionsPanel(): JComponent {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Fill arguments without default values", "withoutDefaultValues")
        panel.addCheckbox("Do not fill default arguments", "withoutDefaultArguments")
        panel.addCheckbox("Append trailing comma", "withTrailingComma")
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
    private val withoutDefaultValues: Boolean,
    private val withoutDefaultArguments: Boolean,
    private val withTrailingComma: Boolean
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
            if (withoutDefaultArguments && parameter.declaresDefaultValue()) return@forEachIndexed
            val added = addArgument(createDefaultValueArgument(parameter, factory))
            val argumentExpression = added.getArgumentExpression()
            if (argumentExpression is KtQualifiedExpression || argumentExpression is KtLambdaExpression) {
                ShortenReferences.DEFAULT.process(argumentExpression)
            }
            if (argumentExpression != null && withTrailingComma && index == parameters.lastIndex) {
                argumentExpression.addCommaAfter(factory)
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
        val defaultValue = type.defaultValue()
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

    private fun KotlinType.defaultValue(): String? = when {
        KotlinBuiltIns.isBoolean(this) -> "false"
        KotlinBuiltIns.isChar(this) -> "''"
        KotlinBuiltIns.isDouble(this) -> "0.0"
        KotlinBuiltIns.isFloat(this) -> "0.0f"
        KotlinBuiltIns.isInt(this) || KotlinBuiltIns.isLong(this) || KotlinBuiltIns.isShort(this) -> "0"
        KotlinBuiltIns.isCollectionOrNullableCollection(this) -> "arrayOf()"
        KotlinBuiltIns.isNullableAny(this) -> "null"
        KotlinBuiltIns.isString(this) -> "\"\""
        KotlinBuiltIns.isListOrNullableList(this) -> "listOf()"
        KotlinBuiltIns.isSetOrNullableSet(this) -> "setOf()"
        KotlinBuiltIns.isMapOrNullableMap(this) -> "mapOf()"
        this.isFunctionType -> lambdaDefaultValue()
        this.isMarkedNullable -> "null"
        else -> null
    }

    private fun KotlinType.lambdaDefaultValue(): String = buildString {
        append("{")
        if (arguments.size > 2) {
            val validator = CollectingNameValidator()
            val lambdaParameters = arguments.dropLast(1).joinToString(postfix = "->") {
                val type = it.type
                val name = KotlinNameSuggester.suggestNamesByType(type, validator, "param")[0]
                validator.addName(name)
                val typeText = type.constructor.declarationDescriptor?.importableFqName?.asString() ?: type.toString()
                val nullable = if (type.isMarkedNullable) "?" else ""
                "$name: $typeText$nullable"
            }
            append(lambdaParameters)
        }
        append("}")
    }

    private fun PsiElement.addCommaAfter(factory: KtPsiFactory) {
        val comma = factory.createComma()
        parent.addAfter(comma, this)
    }
}
