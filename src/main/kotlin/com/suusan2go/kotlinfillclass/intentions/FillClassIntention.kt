package com.suusan2go.kotlinfillclass.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.toLightClassOrigin
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType


class FillClassIntention: IntentionAction {

    override fun getFamilyName(): String {
        return text
    }

    override fun getText(): String = "Fill class constructor"

    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val caretModel = editor.caretModel
        val offset = caretModel.offset
        val ktFile = psiFile as KtFile
        val psiElement = ktFile.findElementAt(offset)

        psiElement!!.node.elementType
        val file = PsiManager.getInstance(project).findFile(psiFile.virtualFile) as KtFile
        val factory = KtPsiFactory(project = project)
        val argument = factory.createStringTemplate("hogehoge")

        file!!.node.addChild(argument.node)
        return
    }



    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile): Boolean {
        return true
    }

    override fun startInWriteAction() = true
}