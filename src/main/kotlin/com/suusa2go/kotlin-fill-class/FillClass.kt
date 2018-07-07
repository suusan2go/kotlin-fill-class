import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

import com.sun.javafx.scene.CameraHelper.project

class FillClass : AnAction("Hello") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getProject()
        Messages.showMessageDialog(project, "Hello world!", "Greeting", Messages.getInformationIcon())
    }
}