package net.bjoernpetersen.deskbot.impl

import net.bjoernpetersen.musicbot.spi.util.BrowserOpener
import java.net.URL

class ConsolePrinterBrowserOpener : BrowserOpener {
    override fun openDocument(url: URL) {
        println("Please visit the following URL: $url")
    }
}
