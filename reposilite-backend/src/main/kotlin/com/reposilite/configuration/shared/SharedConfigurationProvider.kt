package com.reposilite.configuration.shared

interface SharedConfigurationProvider {

    fun updateConfiguration(content: String)

    fun fetchConfiguration(): String

    fun isUpdateRequired(): Boolean

    fun isMutable(): Boolean

    fun name(): String

}
