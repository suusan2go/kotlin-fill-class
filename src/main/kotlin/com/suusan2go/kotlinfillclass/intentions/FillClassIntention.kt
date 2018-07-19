package com.suusan2go.kotlinfillclass.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.types.typeUtil.*


class FillClassIntention(
        element: KtCallExpression
): KotlinQuickFixAction<KtCallExpression>(element) {

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