package com.jxguo92.mykarooextension.extension

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

import kotlinx.coroutines.SupervisorJob

class MyKarooExtension : KarooExtension(EXTENSION_ID, EXTENSION_VERSION) {
    private val extensionJob = SupervisorJob()
    private val karooSystem by lazy { KarooSystemService(this) }

    override val types by lazy {
        DataFieldRegistry.create(karooSystem, extension, this, extensionJob)
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem.connect()
    }

    override fun onDestroy() {
        extensionJob.cancel()
        karooSystem.disconnect()
        super.onDestroy()
    }

    companion object {
        const val EXTENSION_ID = "my-karoo-extension"
        const val EXTENSION_VERSION = "0.1.0"
    }
}
