package com.github.suusan2go.kotlinfillclass.inspections

import com.github.suusan2go.kotlinfillclass.helper.PutArgumentOnSeparateLineHelper
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import javax.swing.JComponent

class FillDummyValuesInspection : BaseFillClassInspection(withoutDefaultValues = false) {

    override fun getConstructorPromptTitle(): String {
        return "Fill class constructor with dummy values"
    }

    override fun getFunctionPromptTitle(): String {
        return "Fill function with dummy values"
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
        return FillDummyValueFix(
            description,
            withoutDefaultValues,
            withoutDefaultArguments,
            withTrailingComma,
            putArgumentsOnSeparateLines,
            movePointerToEveryArgument,
        )
    }
}

class FillDummyValueFix(
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
    override fun fillValue(descriptor: ValueParameterDescriptor): String? {
        val type = descriptor.type
        val paramName = descriptor.name.asString()
        return when {
            KotlinBuiltIns.isChar(type) -> "'${ValueGenerator.getRandomChar()}'"
            KotlinBuiltIns.isDouble(type) -> "${ValueGenerator.getRandomNumber()}.${ValueGenerator.getRandomNumber()}"
            KotlinBuiltIns.isFloat(type) -> "${ValueGenerator.getRandomNumber()}.${ValueGenerator.getRandomNumber()}f"
            KotlinBuiltIns.isInt(type) ||
                KotlinBuiltIns.isLong(type) ||
                KotlinBuiltIns.isShort(type) -> "${ValueGenerator.randomNumFor(paramName)}"

            KotlinBuiltIns.isCharSequence(type) ||
                KotlinBuiltIns.isString(type) -> "\"${ValueGenerator.randomStringFor(paramName)}\""

            else -> super.fillValue(descriptor)
        }
    }
}
