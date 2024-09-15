package com.github.suusan2go.kotlinfillclass.inspections.k2

import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_MOVE_POINTER_TO_EVERY_ARGUMENT
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_PUT_ARGUMENTS_ON_SEPARATE_LINES
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_WITHOUT_DEFAULT_ARGUMENTS
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_WITHOUT_DEFAULT_VALUES
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_WITH_TRAILING_COMMA
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ex.ProblemDescriptorImpl
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandExecutor
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.util.PsiEditorUtil
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.calls.KtErrorCallInfo
import org.jetbrains.kotlin.analysis.api.calls.KtSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.calls.symbol
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinNameSuggester
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.AbstractKotlinApplicableInspectionWithContext
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.KotlinApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.applicabilityRanges
import org.jetbrains.kotlin.idea.codeinsight.utils.isEnum
import org.jetbrains.kotlin.idea.codeinsight.utils.isNullableAnyType
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
@OptIn(KtAllowAnalysisOnEdt::class)
abstract class BaseFillClassInspection(
    @JvmField var withoutDefaultValues: Boolean = false,
    @JvmField var withoutDefaultArguments: Boolean = false,
    @JvmField var withTrailingComma: Boolean = false,
    @JvmField var putArgumentsOnSeparateLines: Boolean = false,
    @JvmField var movePointerToEveryArgument: Boolean = true,
) : AbstractKotlinApplicableInspectionWithContext<KtValueArgumentList, BaseFillClassInspection.Context>() {

    data class Context(
        val functionName: String,
        val candidates: List<String>,
        val isConstructor: Boolean,
    )

    abstract fun getConstructorPromptDescription(): String

    abstract fun getFunctionPromptDescription(): String

    override fun getProblemDescription(element: KtValueArgumentList, context: Context): String {
        return if (context.isConstructor) {
            getConstructorPromptDescription()
        } else {
            getFunctionPromptDescription()
        }
    }

    override fun createOptionsPanel(): JComponent {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox(LABEL_WITHOUT_DEFAULT_VALUES, "withoutDefaultValues")
        panel.addCheckbox(LABEL_WITHOUT_DEFAULT_ARGUMENTS, "withoutDefaultArguments")
        panel.addCheckbox(LABEL_WITH_TRAILING_COMMA, "withTrailingComma")
        if (PutArgumentOnSeparateLineHelper.isAvailable()) {
            panel.addCheckbox(LABEL_PUT_ARGUMENTS_ON_SEPARATE_LINES, "putArgumentsOnSeparateLines")
        }
        panel.addCheckbox(LABEL_MOVE_POINTER_TO_EVERY_ARGUMENT, "movePointerToEveryArgument")
        return panel
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor = valueArgumentListVisitor(fun(element: KtValueArgumentList) {
        visitTargetElement(element, holder, isOnTheFly)
    })

    override fun getApplicabilityRange(): KotlinApplicabilityRange<KtValueArgumentList> =
        applicabilityRanges { element: KtValueArgumentList ->
            listOf(element.textRangeIn(element))
        }

    context(KtAnalysisSession) override fun prepareContext(element: KtValueArgumentList): Context? {
        val callExpression = element.parent as? KtCallExpression ?: return null
        return callExpression.findCandidates().takeIf { it.isNotEmpty() }?.let {
            Context(
                functionName = it[0].partiallyAppliedSymbol.signature.toString(),
                candidates = it.map { call ->
                    callExpression.calleeExpression?.text + (call.symbol.psi?.getChildOfType<KtParameterList>()?.text
                        ?: call.partiallyAppliedSymbol.signature.valueParameters.joinToString(
                            prefix = "(",
                            postfix = ")",
                            separator = ", ",
                            transform = { sig -> "${sig.name.identifier}: ${sig.returnType.asStringForDebugging()}" },
                        ))
                },
                isConstructor = it[0].symbol is KtConstructorSymbol
            )
        }
    }

    context(KtAnalysisSession) private fun KtCallElement.findCandidates(): List<KtSimpleFunctionCall> {
        val resolvedCall = resolveCall()
        val argumentSize = valueArguments.size
        if (resolvedCall is KtErrorCallInfo) {
            val candidates = resolvedCall.candidateCalls
                .mapNotNull { it as? KtSimpleFunctionCall }
                .filter { ktCall ->
                    ktCall.symbol.origin !in listOf(
                        KtSymbolOrigin.JAVA,
                        KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY,
                        KtSymbolOrigin.JS_DYNAMIC
                    ) && ktCall.partiallyAppliedSymbol.signature.valueParameters.filterNot { it.symbol.isVararg }.size > argumentSize
                }
            return candidates
        }
        return emptyList()
    }

    override fun isApplicableByPsi(element: KtValueArgumentList): Boolean {
        return true
    }

    override fun apply(element: KtValueArgumentList, context: Context, project: Project, updater: ModPsiUpdater) {
        val call = element.parent as? KtCallElement ?: return
        val lambdaArgument = call.lambdaArguments.singleOrNull()
        if (context.candidates.size == 1 || isPreviewSession()) {
            allowAnalysisOnEdt {
                analyze(element) {
                    val candidates = call.findCandidates()
                    element.fillArgumentsAndFormat(
                        ktCall = candidates.first(),
                        updater = updater,
                        lambdaArgument = lambdaArgument,
                    )
                }
            }
        } else {
            val listPopup = createListPopup(context, element, lambdaArgument, project)
            invokeLater {
                JBPopupFactory.getInstance().createListPopup(listPopup).showInFocusCenter()
            }
        }
    }

    private fun createListPopup(
        context: Context,
        argumentList: KtValueArgumentList,
        lambdaArgument: KtLambdaArgument?,
        project: Project,
    ): BaseListPopupStep<String> {
        val functionIndexes = context.candidates
            .mapIndexed { index, valueParams -> valueParams to index }
            .sortedBy { it.first.length }
            .toMap()

        return object : BaseListPopupStep<String>("Choose Function", functionIndexes.keys.toList()) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    return doFinalStep {
                        val editor = PsiEditorUtil.findEditor(argumentList)
                        val ac = ActionContext.from(
                            ProblemDescriptorImpl(
                                argumentList,
                                argumentList,
                                "",
                                emptyArray(),
                                ProblemHighlightType.INFORMATION,
                                false,
                                argumentList.range,
                                true
                            )
                        )
                        allowAnalysisOnEdt {
                            analyze(argumentList) {
                                val modCommand = ModCommand.psiUpdate(ac) { eu ->
                                    val e = eu.getWritable(argumentList)
                                    val call = e.parent as? KtCallElement ?: return@psiUpdate
                                    val index = functionIndexes[selectedValue] ?: return@psiUpdate
                                    val parameters = call.findCandidates()[index]
                                    e.fillArgumentsAndFormat(parameters, eu, lambdaArgument)
                                }

                                CommandProcessor.getInstance().executeCommand(project, {
                                    ModCommandExecutor.getInstance().executeInteractively(ac, modCommand, editor)
                                }, "", null)
                            }
                        }

                    }
                }

                return PopupStep.FINAL_CHOICE
            }
        }
    }

    context(KtAnalysisSession) private fun KtValueArgumentList.fillArgumentsAndFormat(
        ktCall: KtSimpleFunctionCall,
        updater: ModPsiUpdater,
        lambdaArgument: KtLambdaArgument?,
    ) {
        fillArgumentsAndFormat(ktCall.partiallyAppliedSymbol.signature.valueParameters, updater, lambdaArgument)
    }

    context(KtAnalysisSession) private fun KtValueArgumentList.fillArgumentsAndFormat(
        parameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
        updater: ModPsiUpdater,
        lambdaArgument: KtLambdaArgument? = null,
    ) {
        val document = containingFile.viewProvider.document
        val argumentSize = arguments.size
        val factory = KtPsiFactory(this.project)
        val newArguments = createArguments(factory, parameters, lambdaArgument)

        for (newArgument in newArguments) {
            addArgument(newArgument)
        }

        // post-fill process

        // 1. Put arguments on separate lines
        if (putArgumentsOnSeparateLines) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)

            val editor = ImaginaryEditor(project, document)
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

        // 3. Set argument placeholders
        // This should be run on final state
        if (!isPreviewSession() && movePointerToEveryArgument) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            startToReplaceArguments(argumentSize, updater)
        }
    }

    context(KtAnalysisSession) private fun KtValueArgumentList.createArguments(
        factory: KtPsiFactory,
        parameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
        lambdaArgument: KtLambdaArgument? = null,
    ): List<KtValueArgument> {
        val arguments = this.arguments
        val argumentNames = arguments.mapNotNull { it.getArgumentName()?.asName?.identifier }

        val lastIndex = parameters.size - 1
        return parameters.mapIndexedNotNull { index, parameter ->
            if (lambdaArgument != null && index == lastIndex && parameter.returnType is KtFunctionalType) return@mapIndexedNotNull null
            if (arguments.size > index && !arguments[index].isNamed()) return@mapIndexedNotNull null
            if (parameter.name.identifier in argumentNames) return@mapIndexedNotNull null
            if (parameter.symbol.isVararg) return@mapIndexedNotNull null
            if (withoutDefaultArguments && parameter.symbol.hasDefaultValue) return@mapIndexedNotNull null
            createDefaultValueArgument(parameter, factory)
        }
    }

    context(KtAnalysisSession) private fun createDefaultValueArgument(
        parameter: KtVariableLikeSignature<KtValueParameterSymbol>,
        factory: KtPsiFactory,
    ): KtValueArgument {
        if (withoutDefaultValues) {
            return factory.createArgument(null, parameter.name)
        }
        val value = fillValue(parameter)
        if (value != null) {
            return factory.createArgument(factory.createExpression(value), parameter.name)
        }
        // TODO: implement
        return factory.createArgument(null, parameter.name)

//        val descriptor = parameter.returnType.constructor.declarationDescriptor as? LazyClassDescriptor
//        val modality = descriptor?.modality
//        if (descriptor?.kind == ClassKind.ENUM_CLASS || modality == Modality.ABSTRACT || modality == Modality.SEALED) {
//            return factory.createArgument(null, parameter.name)
//        }
//
//        val fqName = descriptor?.importableFqName?.asString()
//        val valueParameters = descriptor?.constructors
//            ?.sortedByDescending { it.isPrimary } // primary constructor first
//            ?.firstOrNull { it is ClassConstructorDescriptor }
//            ?.valueParameters
//        val argumentExpression = if (fqName != null && valueParameters != null) {
//            (factory.createExpression("$fqName()")).also {
//                val callExpression = it as? KtCallExpression ?: (it as? KtQualifiedExpression)?.callExpression
//                callExpression?.valueArgumentList?.fillArguments(factory, valueParameters, updater)
//            }
//        } else {
//            null
//        }
//        return factory.createArgument(argumentExpression, parameter.name)
    }

    context(KtAnalysisSession) protected open fun fillValue(signature: KtVariableLikeSignature<KtValueParameterSymbol>): String? {
        val type = signature.returnType
        if (type !is KtNonErrorClassType) {
            return null
        }
        return when {
            type.isBoolean -> "false"
            type.isChar -> "''"
            type.isDouble -> "0.0"
            type.isFloat -> "0.0f"
            type.isInt ||
                type.isLong ||
                type.isShort -> "0"

            type.isArrayOrPrimitiveArray() -> "arrayOf()"
            type.isNullableAnyType() -> "null"
            type.isCharSequence ||
                type.isString -> "\"\""

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

    context(KtAnalysisSession) private fun KtNonErrorClassType.lambdaDefaultValue(): String = buildString {
        append("{")
        if (ownTypeArguments.size > 2) {
            val validator = CollectingNameValidator()
            val lambdaParameters = ownTypeArguments.dropLast(1).joinToString(postfix = "->") {
                val type = it.type ?: return@joinToString ""
                val suggester = KotlinNameSuggester()
                val name = KotlinNameSuggester.suggestNameByName(suggester.suggestTypeNames(type).first(), validator)
                validator.addName(name)
                val typeText = (type as? KtNonErrorClassType)?.let { it.classId.asFqNameString() }
                val nullable = if (type.isMarkedNullable) "?" else ""
                "$name: $typeText$nullable"
            }
            append(lambdaParameters)
        }
        append("}")
    }

    private fun KtNonErrorClassType.firstEnumValueOrNull(): String? {
        val psi = classSymbol.psi ?: return null
        return (psi as? KtClass)?.declarations?.firstOrNull()?.kotlinFqName?.asString() // Kotlin Enum
            ?: psi.getChildrenOfType<PsiEnumConstant>().firstOrNull()?.kotlinFqName?.asString() // Java Enum
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

    private fun KtValueArgumentList.startToReplaceArguments(startIndex: Int, updater: ModPsiUpdater) {
        val templateBuilder = updater.templateBuilder()
        arguments.drop(startIndex).forEach { argument ->
            val argumentExpression = argument.getArgumentExpression()
            if (argumentExpression != null) {
                templateBuilder.field(argumentExpression, argumentExpression.text)
            } else {
                templateBuilder.field(argument.lastChild, "")
            }
        }
    }

    private fun isPreviewSession(): Boolean {
        return Throwable().stackTrace.any {
            it.className == "com.intellij.modcommand.ModCommandQuickFix" &&
                it.methodName == "generatePreview"
        }
    }

    private fun KtDotQualifiedExpression.deleteQualifier(): KtExpression? {
        val selectorExpression = selectorExpression ?: return null
        return this.replace(selectorExpression) as KtExpression
    }
}
