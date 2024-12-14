package com.github.suusan2go.kotlinfillclass.inspections.k2

import com.github.suusan2go.kotlinfillclass.helper.K2SupportHelper
import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_MOVE_POINTER_TO_EVERY_ARGUMENT
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_PUT_ARGUMENTS_ON_SEPARATE_LINES
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_WITHOUT_DEFAULT_ARGUMENTS
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_WITHOUT_DEFAULT_VALUES
import com.github.suusan2go.kotlinfillclass.inspections.BaseFillClassInspection.Companion.LABEL_WITH_TRAILING_COMMA
import com.intellij.codeInspection.ProblemsHolder
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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiEditorUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinModCommandQuickFix
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsight.utils.isEnum
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import javax.swing.JComponent

@OptIn(KaAllowAnalysisOnEdt::class)
abstract class BaseFillClassInspection(
    @JvmField var withoutDefaultValues: Boolean = false,
    @JvmField var withoutDefaultArguments: Boolean = false,
    @JvmField var withTrailingComma: Boolean = false,
    @JvmField var putArgumentsOnSeparateLines: Boolean = false,
    @JvmField var movePointerToEveryArgument: Boolean = true,
) : KotlinApplicableInspectionBase.Simple<KtValueArgumentList, BaseFillClassInspection.Context>() {
    data class Context(
        val functionName: String,
        val candidates: List<String>,
        val isConstructor: Boolean,
    )

    abstract fun getConstructorPromptDescription(): String

    abstract fun getFunctionPromptDescription(): String

    override fun getProblemDescription(
        element: KtValueArgumentList,
        context: Context,
    ): String {
        return if (context.isConstructor) {
            getConstructorPromptDescription()
        } else {
            getFunctionPromptDescription()
        }
    }

    override fun createOptionsPanel(): JComponent? {
        if (!K2SupportHelper.isK2PluginEnabled()) return null
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
    ) = valueArgumentListVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun getApplicableRanges(element: KtValueArgumentList): List<TextRange> = ApplicabilityRange.self(element)

    context(KaSession)
    override fun prepareContext(element: KtValueArgumentList): Context? {
        if (!K2SupportHelper.isK2PluginEnabled()) return null
        val callElement = element.parent as? KtCallElement ?: return null
        return callElement.findCandidates().takeIf { it.isNotEmpty() }?.let {
            Context(
                functionName = it[0].partiallyAppliedSymbol.signature.toString(),
                candidates =
                    it.map { call ->
                        callElement.calleeExpression?.text + (
                            call.symbol.psi?.getChildOfType<KtParameterList>()?.text
                                ?: call.partiallyAppliedSymbol.signature.valueParameters.joinToString(
                                    prefix = "(",
                                    postfix = ")",
                                    separator = ", ",
                                    transform = { sig -> "${sig.name.identifier}: ${sig.returnType.symbol?.classId?.asFqNameString()}" },
                                )
                        )
                    },
                isConstructor = it[0].symbol is KaConstructorSymbol,
            )
        }
    }

    context(KaSession)
    private fun KtCallElement.findCandidates(): List<KaFunctionCall<*>> {
        return findCandidates(valueArguments.size)
    }

    context(KaSession)
    private fun KtElement.findCandidates(argumentSize: Int): List<KaFunctionCall<*>> {
        val resolvedCall = resolveToCall()
        if (resolvedCall is KaErrorCallInfo) {
            val candidates =
                resolvedCall.candidateCalls
                    .mapNotNull { it as? KaFunctionCall<*> }
                    .filter { ktCall ->
                        ktCall.symbol.origin !in
                            listOf(
                                KaSymbolOrigin.JAVA_SOURCE,
                                KaSymbolOrigin.JAVA_LIBRARY,
                                KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY,
                                KaSymbolOrigin.JS_DYNAMIC,
                            ) && ktCall.partiallyAppliedSymbol.signature.valueParameters
                                .filterNot { it.symbol.isVararg }.size > argumentSize
                    }
            return candidates
        }
        return emptyList()
    }

    override fun isApplicableByPsi(element: KtValueArgumentList): Boolean {
        return true
    }

    override fun createQuickFix(
        element: KtValueArgumentList,
        context: Context,
    ) = object : KotlinModCommandQuickFix<KtValueArgumentList>() {
        override fun getFamilyName() =
            if (context.isConstructor) {
                getConstructorPromptDescription()
            } else {
                getFunctionPromptDescription()
            }

        override fun applyFix(
            project: Project,
            element: KtValueArgumentList,
            updater: ModPsiUpdater,
        ) {
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
    }

    private fun createListPopup(
        context: Context,
        argumentList: KtValueArgumentList,
        lambdaArgument: KtLambdaArgument?,
        project: Project,
    ): BaseListPopupStep<String> {
        val functionIndexes =
            context.candidates
                .mapIndexed { index, valueParams -> valueParams to index }
                .sortedBy { it.first.length }
                .toMap()

        return object : BaseListPopupStep<String>("Choose Function", functionIndexes.keys.toList()) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(
                selectedValue: String,
                finalChoice: Boolean,
            ): PopupStep<*>? {
                if (finalChoice) {
                    return doFinalStep {
                        val editor = PsiEditorUtil.findEditor(argumentList)
                        val ac = ActionContext.from(editor, argumentList.containingFile)
                        allowAnalysisOnEdt {
                            analyze(argumentList) {
                                val modCommand =
                                    ModCommand.psiUpdate(ac) { eu ->
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

    context(KaSession)
    private fun KtValueArgumentList.fillArgumentsAndFormat(
        ktCall: KaFunctionCall<*>,
        updater: ModPsiUpdater?,
        lambdaArgument: KtLambdaArgument?,
    ) {
        fillArgumentsAndFormat(ktCall.partiallyAppliedSymbol.signature.valueParameters, updater, lambdaArgument)
    }

    context(KaSession)
    private fun KtValueArgumentList.fillArgumentsAndFormat(
        parameters: List<KaVariableSignature<KaValueParameterSymbol>>,
        updater: ModPsiUpdater?,
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

        // 3. Remove full qualifiers and import references
        // This should be run after PutArgumentOnSeparateLineHelper
        findElementsInArgsByType<KtQualifiedExpression>(argumentSize)
            .forEach { ShortenReferencesFacility.getInstance().shorten(it) }
        findElementsInArgsByType<KtLambdaExpression>(argumentSize)
            .forEach { ShortenReferencesFacility.getInstance().shorten(it) }

        // 4. Set argument placeholders
        // This should be run on final state
        if (!isPreviewSession() && movePointerToEveryArgument && updater != null) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            startToReplaceArguments(argumentSize, updater)
        }
    }

    context(KaSession)
    private fun KtValueArgumentList.createArguments(
        factory: KtPsiFactory,
        parameters: List<KaVariableSignature<KaValueParameterSymbol>>,
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

    context(KaSession)
    private fun createDefaultValueArgument(
        parameter: KaVariableSignature<KaValueParameterSymbol>,
        factory: KtPsiFactory,
    ): KtValueArgument {
        if (withoutDefaultValues) {
            return factory.createArgument(null, parameter.name)
        }
        val value = fillValue(parameter)
        if (value != null) {
            return factory.createArgument(factory.createExpression(value), parameter.name)
        }

        val returnType = parameter.returnType
        if (returnType.isEnum() ||
            returnType.symbol?.modality in listOf(KaSymbolModality.ABSTRACT, KaSymbolModality.SEALED)
        ) {
            return factory.createArgument(null, parameter.name)
        }
        val fqName =
            returnType.symbol?.classId?.asFqNameString()
                ?: return factory.createArgument(null, parameter.name)
        var argumentExpression: KtExpression? = null
        if (returnType.symbol?.psi is KtObjectDeclaration) {
            argumentExpression = factory.createExpression(fqName)
        } else {
            // FIXME: Fill constructor
//            argumentExpression = factory.createExpression("${fqName}()").apply {
//                val resolvedCall = findCandidates(0).firstOrNull()
//                if (resolvedCall != null) {
//                    findDescendantOfType<KtValueArgumentList> { true }?.fillArgumentsAndFormat(resolvedCall, null, null)
//                }
//            }
            argumentExpression = factory.createExpression("$fqName()")
        }
        return factory.createArgument(argumentExpression, parameter.name)
    }

    context(KaSession)
    protected abstract fun fillValue(signature: KaVariableSignature<KaValueParameterSymbol>): String?

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

    private fun KtValueArgumentList.startToReplaceArguments(
        startIndex: Int,
        updater: ModPsiUpdater,
    ) {
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
