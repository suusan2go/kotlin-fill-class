package com.suusan2go.kotlinfillclass.intentions

import com.intellij.codeInsight.intention.IntentionAction
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


class FillClassIntention: IntentionAction {

    override fun getFamilyName(): String {
        return text
    }

    override fun getText(): String = "Fill class constructor"

    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val ktFile = psiFile as KtFile
        val element = ktFile.findElementAt(editor.caretModel.offset)

        val clazz =  PsiTreeUtil.getParentOfType(element, KtLightClass::class.java, false)
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