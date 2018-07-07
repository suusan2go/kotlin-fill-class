package com.suusan2go.kotlinfillclass.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class FillClassIntention: IntentionAction {
    override fun getFamilyName(): String {
        return getText()
    }

    override fun getText(): String = "Fill class constructor"

    override fun invoke(p0: Project, p1: Editor?, p2: PsiFile?) {
        return
    }

    override fun isAvailable(p0: Project, p1: Editor?, p2: PsiFile?): Boolean {
        return true
    }

    override fun startInWriteAction() = true
}