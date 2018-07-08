package com.suusan2go.kotlinfillclass.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

class FillClassIntention: IntentionAction {
    override fun getFamilyName(): String {
        return text
    }

    override fun getText(): String = "Fill class constructor"

    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        return
    }

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile): Boolean {
        return psiFile is KtFile
    }

    override fun startInWriteAction() = true
}