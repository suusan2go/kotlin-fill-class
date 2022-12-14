package com.github.suusan2go.kotlinfillclass.inspections

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.intentions.ChopArgumentListIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import javax.swing.JComponent

abstract class BaseFillClassInspection(
    @JvmField var withoutDefaultValues: Boolean = false,
    @JvmField var withoutDefaultArguments: Boolean = false,
    @JvmField var withTrailingComma: Boolean = false,
    @JvmField var putArgumentsOnSeparateLines: Boolean = false,
    @JvmField var movePointerToEveryArgument: Boolean = true,
) : AbstractKotlinInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) = valueArgumentListVisitor(fun(element: KtValueArgumentList) {
        val descriptor = element.descriptor() ?: return
        if (descriptor.valueParameters.size == element.arguments.size) return
        val description = getPromptTitle()
        val fix = FillClassFix(
            description = description,
            withoutDefaultValues = withoutDefaultValues,
            withoutDefaultArguments = withoutDefaultArguments,
            withTrailingComma = withTrailingComma,
            putArgumentsOnSeparateLines = putArgumentsOnSeparateLines,
            movePointerToEveryArgument = movePointerToEveryArgument,
            shouldGenerateDummyValues = shouldGenerateDummyValues()
        )
        holder.registerProblem(element, description, fix)
    })

    override fun createOptionsPanel(): JComponent {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Fill arguments without default values", "withoutDefaultValues")
        panel.addCheckbox("Do not fill default arguments", "withoutDefaultArguments")
        panel.addCheckbox("Append trailing comma", "withTrailingComma")
        panel.addCheckbox("Put arguments on separate lines", "putArgumentsOnSeparateLines")
        panel.addCheckbox("Move pointer to every argument", "movePointerToEveryArgument")
        return panel
    }

    abstract fun shouldGenerateDummyValues(): Boolean
    abstract fun getPromptTitle(): String
}

private fun KtValueArgumentList.descriptor(): FunctionDescriptor? {
    val calleeExpression = getStrictParentOfType<KtCallElement>()?.calleeExpression ?: return null
    val descriptor = calleeExpression.resolveToCall()?.resultingDescriptor as? FunctionDescriptor ?: return null
    if (descriptor is JavaCallableMemberDescriptor) return null
    return descriptor
}

class FillClassFix(
    private val description: String,
    private val withoutDefaultValues: Boolean,
    private val withoutDefaultArguments: Boolean,
    private val withTrailingComma: Boolean,
    private val putArgumentsOnSeparateLines: Boolean,
    private val movePointerToEveryArgument: Boolean,
    private val shouldGenerateDummyValues: Boolean,
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
        val argumentSize = arguments.size
        val argumentNames = arguments.mapNotNull { it.getArgumentName()?.asName?.identifier }
        val factory = KtPsiFactory(this)
        val needsTrailingComma = withTrailingComma && !hasTrailingComma()
        parameters.forEachIndexed { index, parameter ->
            if (arguments.size > index && !arguments[index].isNamed()) return@forEachIndexed
            if (parameter.name.identifier in argumentNames) return@forEachIndexed
            if (withoutDefaultArguments && parameter.declaresDefaultValue()) return@forEachIndexed

            val added = addArgument(createDefaultValueArgument(parameter, factory))
            val argumentExpression = added.getArgumentExpression()
            if (argumentExpression is KtQualifiedExpression || argumentExpression is KtLambdaExpression) {
                ShortenReferences.DEFAULT.process(argumentExpression)
            }
            if (needsTrailingComma && index == parameters.lastIndex) {
                argumentExpression?.addCommaAfter(factory)
            }
        }
        val editor = findExistingEditor()
        if (editor != null) {
            if (putArgumentsOnSeparateLines || movePointerToEveryArgument) {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            }
            if (putArgumentsOnSeparateLines) {
                ChopArgumentListIntention().applyTo(this, editor)
            }
            if (movePointerToEveryArgument) {
                startToReplaceArguments(argumentSize, editor)
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

        val value = parameter.fillValue()
        if (value != null) {
            return factory.createArgument(factory.createExpression(value), parameter.name)
        }

        val descriptor = parameter.type.constructor.declarationDescriptor as? LazyClassDescriptor
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

    private fun ValueParameterDescriptor.fillValue(): String? {
        val type = this.type
        val paramName = this.name.asString()
        return when {
            KotlinBuiltIns.isBoolean(type) -> "false"
            KotlinBuiltIns.isChar(type) -> if (shouldGenerateDummyValues) {
                "'${ValueGenerator.getRandomChar()}'"
            } else {
                "''"
            }

            KotlinBuiltIns.isDouble(type) -> if (shouldGenerateDummyValues) {
                "${ValueGenerator.getRandomNumber()}.${ValueGenerator.getRandomNumber()}"
            } else {
                "0.0"
            }

            KotlinBuiltIns.isFloat(type) -> if (shouldGenerateDummyValues) {
                "${ValueGenerator.getRandomNumber()}.${ValueGenerator.getRandomNumber()}f"
            } else {
                "0.0f"
            }

            KotlinBuiltIns.isInt(type) ||
                    KotlinBuiltIns.isLong(type) ||
                    KotlinBuiltIns.isShort(type) -> if (shouldGenerateDummyValues) {
                "${ValueGenerator.randomNumFor(paramName)}"
            } else {
                "0"
            }

            KotlinBuiltIns.isCollectionOrNullableCollection(type) -> "arrayOf()"
            KotlinBuiltIns.isNullableAny(type) -> "null"
            KotlinBuiltIns.isString(type) -> if (shouldGenerateDummyValues) {
                "\"${ValueGenerator.randomStringFor(paramName)}\""
            } else {
                "\"\""
            }

            KotlinBuiltIns.isListOrNullableList(type) -> "listOf()"
            KotlinBuiltIns.isSetOrNullableSet(type) -> "setOf()"
            KotlinBuiltIns.isMapOrNullableMap(type) -> "mapOf()"
            type.isFunctionType -> type.lambdaDefaultValue()
            type.isMarkedNullable -> "null"
            else -> null
        }
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

    private fun KtValueArgumentList.hasTrailingComma() =
        rightParenthesis?.getPrevSiblingIgnoringWhitespaceAndComments(withItself = false)?.node?.elementType == KtTokens.COMMA

    private fun KtValueArgumentList.startToReplaceArguments(startIndex: Int, editor: Editor) {
        val templateBuilder = TemplateBuilderImpl(this)
        arguments.drop(startIndex).forEach { argument ->
            val argumentExpression = argument.getArgumentExpression()
            if (argumentExpression != null) {
                templateBuilder.replaceElement(argumentExpression, argumentExpression.text)
            } else {
                val endOffset = argument.textRangeIn(this).endOffset
                templateBuilder.replaceRange(TextRange(endOffset, endOffset), "")
            }
        }
        templateBuilder.run(editor, true)
    }
}
