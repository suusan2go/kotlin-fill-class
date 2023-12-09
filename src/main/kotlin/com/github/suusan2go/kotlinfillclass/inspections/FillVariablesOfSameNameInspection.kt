package com.github.suusan2go.kotlinfillclass.inspections

import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import javax.swing.JComponent

class FillVariablesOfSameNameInspection : BaseFillClassInspection(withoutDefaultValues = false) {

    override fun getConstructorPromptTitle(): String {
        return "Fill class constructor with variables of the same name"
    }

    override fun getFunctionPromptTitle(): String {
        return "Fill class constructor with variables of the same name"
    }

    override fun createOptionsPanel(): JComponent {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox(LABEL_WITHOUT_DEFAULT_ARGUMENTS, "withoutDefaultArguments")
        panel.addCheckbox(LABEL_WITH_TRAILING_COMMA, "withTrailingComma")
        if (PutArgumentOnSeparateLineHelper.isAvailable()) {
            panel.addCheckbox(LABEL_PUT_ARGUMENTS_ON_SEPARATE_LINES, "putArgumentsOnSeparateLines")
        }
        panel.addCheckbox(LABEL_MOVE_POINTER_TO_EVERY_ARGUMENT, "movePointerToEveryArgument")
        return panel
    }

    override fun createFillClassFix(
        description: String,
        withoutDefaultValues: Boolean,
        withoutDefaultArguments: Boolean,
        withTrailingComma: Boolean,
        putArgumentsOnSeparateLines: Boolean,
        movePointerToEveryArgument: Boolean,
    ): FillClassFix {
        return FillSameValueFix(
            description,
            withoutDefaultValues,
            withoutDefaultArguments,
            withTrailingComma,
            putArgumentsOnSeparateLines,
            movePointerToEveryArgument,
        )
    }
}

class FillSameValueFix(
    description: String,
    withoutDefaultValues: Boolean,
    withoutDefaultArguments: Boolean,
    withTrailingComma: Boolean,
    putArgumentsOnSeparateLines: Boolean,
    movePointerToEveryArgument: Boolean,
) : FillClassFix(
    description,
    withoutDefaultValues,
    withoutDefaultArguments,
    withTrailingComma,
    putArgumentsOnSeparateLines,
    movePointerToEveryArgument,
) {
    override fun fillValue(descriptor: ValueParameterDescriptor): String {
        return descriptor.name.asString()
    }
}
