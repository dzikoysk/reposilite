package com.reposilite

import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext

internal class ReposiliteLocalIntegrationJunitExtension : Extension, BeforeEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        context?.also {
            val instance = it.requiredTestInstance
            val type = instance::class.java

            type.getField("_extensionInitialized").set(instance, true)
            type.getField("_database").set(instance, "sqlite reposilite.db")
            type.getField("_storageProvider").set(instance, "fs")
        }
    }

}