@file:Suppress("UnstableApiUsage")

package com.github.suusan2go.kotlinfillclass.inspections

import com.github.suusan2go.kotlinfillclass.helper.K2SupportHelper
import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.computeAllNames
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.ifEmpty

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
        if (K2SupportHelper.isK2PluginEnabled()) return
        val callElement = element.parent as? KtCallElement ?: return
        val descriptors = analyze(callElement).ifEmpty { return }
        val description = if (descriptors.any { descriptor -> descriptor is ClassConstructorDescriptor }) {
            getConstructorPromptTitle()
        } else {
            getFunctionPromptTitle()
        }
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

private fun analyze(call: KtCallElement): List<FunctionDescriptor> {
    val context = call.analyze(BodyResolveMode.PARTIAL)
    val resolvedCall = call.calleeExpression?.getResolvedCall(context)
    val descriptors = if (resolvedCall != null) {
        val descriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return emptyList()
        listOf(descriptor)
    } else {
        call.calleeExpression?.mainReference?.multiResolve(false).orEmpty().mapNotNull {
            val func = it.element as? KtFunction ?: return@mapNotNull null
            val descriptor = func.descriptor as? FunctionDescriptor ?: return@mapNotNull null
            descriptor
        }
    }
    val argumentSize = call.valueArguments.size
    return descriptors.filter { descriptor ->
        descriptor !is JavaCallableMemberDescriptor &&
            descriptor.valueParameters.filterNot { it.isVararg }.size > argumentSize
    }
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
        val call = argumentList.parent as? KtCallElement ?: return
        val descriptors = analyze(call).ifEmpty { return }

        val lambdaArgument = call.lambdaArguments.singleOrNull()
        val editor = argumentList.findExistingEditor()
            ?: ImaginaryEditor(project, argumentList.containingFile.viewProvider.document)
        if (descriptors.size == 1 || editor is ImaginaryEditor) {
            argumentList.fillArgumentsAndFormat(
                descriptor = descriptors.first(),
                editor = editor,
                lambdaArgument = lambdaArgument,
            )
        } else {
            val listPopup = createListPopup(argumentList, lambdaArgument, descriptors, editor)
            JBPopupFactory.getInstance().createListPopup(listPopup).showInBestPositionFor(editor)
        }
    }

    private fun createListPopup(
        argumentList: KtValueArgumentList,
        lambdaArgument: KtLambdaArgument?,
        descriptors: List<FunctionDescriptor>,
        editor: Editor,
    ): BaseListPopupStep<String> {
        val functionName = descriptors.first().let { descriptor ->
            if (descriptor is ClassConstructorDescriptor) {
                descriptor.containingDeclaration.name.asString()
            } else {
                descriptor.name.asString()
            }
        }
        val functions = descriptors
            .sortedBy { descriptor -> descriptor.valueParameters.size }
            .associateBy { descriptor ->
                val key = descriptor.valueParameters.joinToString(
                    separator = ", ",
                    prefix = "$functionName(",
                    postfix = ")",
                    transform = { "${it.name}: ${it.type}" },
                )
                key
            }
        return object : BaseListPopupStep<String>("Choose Function", functions.keys.toList()) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    val parameters = functions[selectedValue]?.valueParameters.orEmpty()
                    CommandProcessor.getInstance().runUndoTransparentAction {
                        runWriteAction {
                            argumentList.fillArgumentsAndFormat(parameters, editor, lambdaArgument)
                        }
                    }
                }
                return PopupStep.FINAL_CHOICE
            }
        }
    }

    private fun KtValueArgumentList.fillArgumentsAndFormat(
        descriptor: FunctionDescriptor,
        editor: Editor,
        lambdaArgument: KtLambdaArgument?,
    ) {
        fillArgumentsAndFormat(descriptor.valueParameters, editor, lambdaArgument)
    }

    private fun KtValueArgumentList.fillArgumentsAndFormat(
        parameters: List<ValueParameterDescriptor>,
        editor: Editor,
        lambdaArgument: KtLambdaArgument? = null,
    ) {
        val argumentSize = arguments.size
        val factory = KtPsiFactory(this.project)
        fillArguments(factory, parameters, editor, lambdaArgument)

        // post-fill process

        // 1. Put arguments on separate lines
        if (putArgumentsOnSeparateLines) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            if (this.arguments.isNotEmpty()) {
                PutArgumentOnSeparateLineHelper.applyTo(this, editor)
            }
            findElementsInArgsByType<KtValueArgumentList>(argumentSize)
                .filter { it.arguments.isNotEmpty() }
                .forEach { PutArgumentOnSeparateLineHelper.applyTo(it, editor) }
        }

        // 2. Add trailing commas
        if (withTrailingComma) {
            addTrailingCommaIfNeeded(factory)
            findElementsInArgsByType<KtValueArgumentList>(argumentSize)
                .forEach { it.addTrailingCommaIfNeeded(factory) }
        }

        // 3. Remove full qualifiers and import references
        // This should be run after PutArgumentOnSeparateLineHelper
        findElementsInArgsByType<KtQualifiedExpression>(argumentSize)
            .forEach { ShortenReferences.DEFAULT.process(it) }
        findElementsInArgsByType<KtLambdaExpression>(argumentSize)
            .forEach { ShortenReferences.DEFAULT.process(it) }

        // 4. Set argument placeholders
        // This should be run on final state
        if (editor !is ImaginaryEditor && movePointerToEveryArgument) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            startToReplaceArguments(argumentSize, editor)
        }
    }

    private fun KtValueArgumentList.fillArguments(
        factory: KtPsiFactory,
        parameters: List<ValueParameterDescriptor>,
        editor: Editor,
        lambdaArgument: KtLambdaArgument? = null,
    ) {
        val arguments = this.arguments
        val argumentNames = arguments.mapNotNull { it.getArgumentName()?.asName?.identifier }

        val lastIndex = parameters.size - 1
        parameters.forEachIndexed { index, parameter ->
            if (lambdaArgument != null && index == lastIndex && parameter.type.isFunctionType) return@forEachIndexed
            if (arguments.size > index && !arguments[index].isNamed()) return@forEachIndexed
            if (parameter.name.identifier in argumentNames) return@forEachIndexed
            if (parameter.isVararg) return@forEachIndexed
            if (withoutDefaultArguments && parameter.declaresDefaultValue()) return@forEachIndexed
            addArgument(createDefaultValueArgument(parameter, factory, editor))
        }
    }

    private fun createDefaultValueArgument(
        parameter: ValueParameterDescriptor,
        factory: KtPsiFactory,
        editor: Editor,
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
        val valueParameters = descriptor?.constructors
            ?.sortedByDescending { it.isPrimary } // primary constructor first
            ?.firstOrNull { it is ClassConstructorDescriptor }
            ?.valueParameters
        val argumentExpression = if (fqName != null && valueParameters != null) {
            (factory.createExpression("$fqName()")).also {
                val callExpression = it as? KtCallExpression ?: (it as? KtQualifiedExpression)?.callExpression
                callExpression?.valueArgumentList?.fillArguments(factory, valueParameters, editor)
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
            KotlinBuiltIns.isCharSequence(type) ||
                KotlinBuiltIns.isString(type) -> "\"\""

            KotlinBuiltIns.isListOrNullableList(type) -> "listOf()"
            KotlinBuiltIns.isSetOrNullableSet(type) -> "setOf()"
            KotlinBuiltIns.isMapOrNullableMap(type) -> "mapOf()"
            type.isFunctionType -> type.lambdaDefaultValue()
            type.isEnum() -> type.firstEnumValueOrNull()
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

    private fun KotlinType.firstEnumValueOrNull(): String? {
        val names = this.memberScope.computeAllNames() ?: return null
        for (name in names) {
            val descriptor = this.memberScope.getContributedClassifier(name, NoLookupLocation.FROM_IDE)
                ?: continue
            if (descriptor.defaultType.supertypes().contains(this)) {
                return descriptor.fqNameOrNull()?.asString() ?: continue
            }
        }
        return null
    }

    private inline fun <reified T : KtElement> KtValueArgumentList.findElementsInArgsByType(argStartOffset: Int): List<T> {
        return this.arguments.subList(argStartOffset, this.arguments.size).flatMap { argument ->
            argument.collectDescendantsOfType<T>()
        }
    }

    private fun KtValueArgumentList.addTrailingCommaIfNeeded(factory: KtPsiFactory) {
        if (this.arguments.isNotEmpty() && !this.hasTrailingComma()) {
            val comma = factory.createComma()
            this.addAfter(comma, this.arguments.last())
        }
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
                val commaOffset = if (argument.text.lastOrNull() == ',') 1 else 0
                val endOffset = argument.textRangeIn(this).endOffset - commaOffset
                templateBuilder.replaceRange(TextRange(endOffset, endOffset), "")
            }
        }
        templateBuilder.run(editor, true)
    }
}
