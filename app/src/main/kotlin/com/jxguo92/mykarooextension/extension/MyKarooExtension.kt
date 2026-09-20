package com.jxguo92.mykarooextension.extension

import io.hammerhead.karooext.extension.KarooExtension

class MyKarooExtension : KarooExtension(EXTENSION_ID, EXTENSION_VERSION) {
    private companion object {
        const val EXTENSION_ID = "my-karoo-extension"
        const val EXTENSION_VERSION = "0.1.0"
    }
}
