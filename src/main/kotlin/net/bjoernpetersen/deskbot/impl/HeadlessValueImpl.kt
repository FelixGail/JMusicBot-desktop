package net.bjoernpetersen.deskbot.impl

import com.google.inject.AbstractModule
import com.google.inject.name.Names

class HeadlessValueImpl(private val headless: Boolean) : AbstractModule() {

    override fun configure() {
        bind(Boolean::class.java).annotatedWith(Names.named("Headless")).toInstance(headless)
    }
}
