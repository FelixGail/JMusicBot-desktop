package net.bjoernpetersen.deskbot.view

import javafx.fxml.FXMLLoader
import javafx.scene.Parent

inline fun <reified T : Controller> load(controller: T? = null): T {
    val loader = FXMLLoader(Charsets.UTF_8).apply {
        val type = T::class.java
        location = type.getResource(type.simpleName + ".fxml")
        resources = DeskBotInfo.resources
        setController(controller)
    }
    loader.load<Parent>()
    return loader.getController()
}
