package com.suusan2go.kotlinfillclass.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtElementImpl
import org.jetbrains.kotlin.psi.KtFile

class FillClassIntention: IntentionAction {

    override fun getFamilyName(): String {
        return text
    }

    override fun getText(): String = "Fill class constructor"

    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val file = psiFile as KtFile
        file.add(KtElementImpl)
        file.psiRoots.iterator().forEach {
        }
        return
    }

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile): Boolean {
        return true
    }

    override fun startInWriteAction() = true
}