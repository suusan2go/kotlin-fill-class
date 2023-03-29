package com.github.suusan2go.kotlinfillclass.inspections

import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getParameterForArgument
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class BaseFillClassInspection(
    @JvmField var withoutDefaultValues: Boolean = false,
    @JvmField var withoutDefaultArguments: Boolean = false,
    @JvmField var withTrailingComma: Boolean = false,
    @JvmField var putArgumentsOnSeparateLines: Boolean = false,
    @JvmField var movePointerToEveryArgument: Boolean = true,
) : AbstractKotlinInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ) = valueArgumentListVisitor(fun(element: KtValueArgumentList) {
        val callElement = element.parent as? KtCallElement ?: return
        val (_, descriptor) = callElement.analyze() ?: return
        if (descriptor.valueParameters.size == callElement.valueArguments.size) return
        val description =
            if (descriptor is ClassConstructorDescriptor) getConstructorPromptTitle() else getFunctionPromptTitle()
        val fix = createFillClassFix(
            description = description,
            withoutDefaultValues = withoutDefaultValues,
            withoutDefaultArguments = withoutDefaultArguments,
            withTrailingComma = withTrailingComma,
            putArgumentsOnSeparateLines = putArgumentsOnSeparateLines,
            movePointerToEveryArgument = movePointerToEveryArgument,
        )
        holder.registerProblem(element, description, fix)
    })

    abstract fun getConstructorPromptTitle(): String
    abstract fun getFunctionPromptTitle(): String

    open fun createFillClassFix(
        description: String,
        withoutDefaultValues: Boolean,
        withoutDefaultArguments: Boolean,
        withTrailingComma: Boolean,
        putArgumentsOnSeparateLines: Boolean,
        movePointerToEveryArgument: Boolean,
    ): FillClassFix = FillClassFix(
        description = description,
        withoutDefaultValues = withoutDefaultValues,
        withoutDefaultArguments = withoutDefaultArguments,
        withTrailingComma = withTrailingComma,
        putArgumentsOnSeparateLines = putArgumentsOnSeparateLines,
        movePointerToEveryArgument = movePointerToEveryArgument,
    )

    companion object {
        const val LABEL_WITHOUT_DEFAULT_VALUES = "Fill arguments without default values"
        const val LABEL_WITHOUT_DEFAULT_ARGUMENTS = "Do not fill default arguments"
        const val LABEL_WITH_TRAILING_COMMA = "Append trailing comma"
        const val LABEL_PUT_ARGUMENTS_ON_SEPARATE_LINES = "Put arguments on separate lines"
        const val LABEL_MOVE_POINTER_TO_EVERY_ARGUMENT = "Move pointer to every argument"
    }
}

private fun KtCallElement.analyze(): Pair<ResolvedCall<out CallableDescriptor>, FunctionDescriptor>? {
    val resolvedCall = calleeExpression?.resolveToCall() ?: return null
    val descriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null
    if (descriptor is JavaCallableMemberDescriptor) return null
    return resolvedCall to descriptor
}

open class FillClassFix(
    private val description: String,
    private val withoutDefaultValues: Boolean,
    private val withoutDefaultArguments: Boolean,
    private val withTrailingComma: Boolean,
    private val putArgumentsOnSeparateLines: Boolean,
    private val movePointerToEveryArgument: Boolean,
) : LocalQuickFix {
    override fun getName() = description

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val argumentList = descriptor.psiElement as? KtValueArgumentList ?: return
        val (resolvedCall, functionDescriptor) = argumentList.parent.safeAs<KtCallElement>()?.analyze() ?: return
        argumentList.fillArguments(functionDescriptor.valueParameters, resolvedCall)
    }

    private fun KtValueArgumentList.fillArguments(
        parameters: List<ValueParameterDescriptor>,
        resolvedCall: ResolvedCall<out CallableDescriptor>? = null,
    ) {
        val arguments = this.arguments
        val argumentSize = arguments.size
        val argumentNames = arguments.mapNotNull { it.getArgumentName()?.asName?.identifier }

        val lambdaArgument = parent.safeAs<KtCallElement>()?.lambdaArguments?.singleOrNull()
        val parameterForLambdaArgument = lambdaArgument?.let { resolvedCall?.getParameterForArgument(it) }

        val factory = KtPsiFactory(this)
        val needsTrailingComma = withTrailingComma && !hasTrailingComma()
        parameters.forEachIndexed { index, parameter ->
            if (parameter == parameterForLambdaArgument) return@forEachIndexed
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
                PutArgumentOnSeparateLineHelper.applyTo(this, editor)
            }
            if (movePointerToEveryArgument) {
                startToReplaceArguments(argumentSize, editor)
            }
        }
    }

    private fun createDefaultValueArgument(
        parameter: ValueParameterDescriptor,
        factory: KtPsiFactory,
    ): KtValueArgument {
        if (withoutDefaultValues) {
            return factory.createArgument(null, parameter.name)
        }

        val value = fillValue(parameter)
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

    protected open fun fillValue(descriptor: ValueParameterDescriptor): String? {
        val type = descriptor.type
        return when {
            KotlinBuiltIns.isBoolean(type) -> "false"
            KotlinBuiltIns.isChar(type) -> "''"
            KotlinBuiltIns.isDouble(type) -> "0.0"
            KotlinBuiltIns.isFloat(type) -> "0.0f"
            KotlinBuiltIns.isInt(type) ||
                KotlinBuiltIns.isLong(type) ||
                KotlinBuiltIns.isShort(type) -> "0"

            KotlinBuiltIns.isCollectionOrNullableCollection(type) -> "arrayOf()"
            KotlinBuiltIns.isNullableAny(type) -> "null"
            KotlinBuiltIns.isString(type) -> "\"\""
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
