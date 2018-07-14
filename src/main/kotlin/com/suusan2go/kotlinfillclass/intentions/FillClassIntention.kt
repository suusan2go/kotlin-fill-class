package com.suusan2go.kotlinfillclass.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClassOrigin
import org.jetbrains.kotlin.parsing.KotlinParser
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.getLanguage
import com.intellij.refactoring.changeSignature.LanguageChangeSignatureDetector
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreateCallableMemberFromUsageFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreateConstructorFromSuperTypeCallActionFactory


class FillClassIntention(
        element: KtConstructorCalleeExpression
): KotlinQuickFixAction<KtConstructorCalleeExpression>(element) {

    override fun getFamilyName(): String {
        return text
    }

    override fun getText(): String = "Fill class constructor"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        CreateConstructorFromSuperTypeCallActionFactory
        val clazz =  PsiTreeUtil.getParentOfType(element, KtElement::class.java, false)
        val factory = KtPsiFactory(project = project)
        val argument = factory.createStringTemplate("hogehoge")
        element.replace(argument)
        return
    }

    override fun startInWriteAction() = true
}