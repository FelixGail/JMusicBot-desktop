package net.bjoernpetersen.deskbot.view

import net.bjoernpetersen.deskbot.lifecycle.Lifecyclist
import net.bjoernpetersen.deskbot.localization.YamlResourceBundle
import java.util.ResourceBundle

object DeskBotInfo {
    val resources: ResourceBundle
        get() = ResourceBundle.getBundle("net.bjoernpetersen.deskbot.view.DeskBot", YamlResourceBundle.Control)
    var runningInstance: Lifecyclist? = null
}
