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

    private fun createParameterSetterExpression(parameters: List<ValueParameterDescriptor>): String {
        var result = ""
        parameters.forEach { parameter ->
            var parameterString = "${parameter.name.identifier} = ${createDefaultValueFromParameter(parameter)},\n".let {
                if(parameters.last() == parameter) it.replace(",\n","")
            }
            result = "$result$parameterString"
        }
        return result
    }

    private fun createDefaultValueFromParameter(parameter: ValueParameterDescriptor): String {
        val type = parameter.type
        val des = type.constructor.declarationDescriptor!!
        return when {
            KotlinBuiltIns.isBoolean(type) -> "false"
            KotlinBuiltIns.isChar(type) -> "''"
            KotlinBuiltIns.isDouble(type) -> "0.0d"
            KotlinBuiltIns.isFloat(type) -> "0.0f"
            KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isLong(type) || KotlinBuiltIns.isShort(type) -> "0"
            KotlinBuiltIns.isCollectionOrNullableCollection(type) -> "emptyArray()"
            KotlinBuiltIns.isNullableAny(type) -> "null"
            KotlinBuiltIns.isString(type) -> "\"\""
            KotlinBuiltIns.isListOrNullableList(type) -> "emptyList()"
            KotlinBuiltIns.isSetOrNullableSet(type) -> "emptySet()"
            type.isMarkedNullable -> "null"
            else -> ""
        }
    }
}