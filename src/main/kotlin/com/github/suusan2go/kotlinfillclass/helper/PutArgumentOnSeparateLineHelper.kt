package com.github.suusan2go.kotlinfillclass.helper

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtValueArgumentList

object PutArgumentOnSeparateLineHelper {

    private val intentionClass: Class<*>? by lazy {
        try {
            Class.forName("org.jetbrains.kotlin.idea.intentions.ChopArgumentListIntention")
        } catch (e: ClassNotFoundException) {
            try {
                Class.forName("org.jetbrains.kotlin.idea.codeInsight.intentions.shared.ChopArgumentListIntention")
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }

    fun isAvailable(): Boolean = intentionClass != null

    fun applyTo(element: KtValueArgumentList, editor: Editor?) {
        val clazz = intentionClass ?: return
        val constructor = clazz.getConstructor()
        val intention = constructor.newInstance()
        val method = clazz.getMethod("applyTo", KtElement::class.java, Editor::class.java)
        method.invoke(intention, element, editor)
    }
}
