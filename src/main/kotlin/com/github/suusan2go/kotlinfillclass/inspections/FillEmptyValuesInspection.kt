package com.github.suusan2go.kotlinfillclass.inspections

import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import javax.swing.JComponent

class FillEmptyValuesInspection : BaseFillClassInspection() {

    override fun getConstructorPromptTitle(): String {
        return "Fill class constructor"
    }

    override fun getFunctionPromptTitle(): String {
        return "Fill function"
    }

    override fun createOptionsPanel(): JComponent {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox(LABEL_WITHOUT_DEFAULT_VALUES, "withoutDefaultValues")
        panel.addCheckbox(LABEL_WITHOUT_DEFAULT_ARGUMENTS, "withoutDefaultArguments")
        panel.addCheckbox(LABEL_WITH_TRAILING_COMMA, "withTrailingComma")
        if (PutArgumentOnSeparateLineHelper.isAvailable()) {
            panel.addCheckbox(LABEL_PUT_ARGUMENTS_ON_SEPARATE_LINES, "putArgumentsOnSeparateLines")
        }
        panel.addCheckbox(LABEL_MOVE_POINTER_TO_EVERY_ARGUMENT, "movePointerToEveryArgument")
        return panel
    }
}
