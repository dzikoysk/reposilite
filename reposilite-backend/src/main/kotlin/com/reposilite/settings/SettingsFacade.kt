package com.reposilite.settings

import com.reposilite.journalist.Journalist

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val sharedConfigurationService: SharedConfigurationService
) {

    fun verifySharedConfiguration() =
        sharedConfigurationService.verifySharedConfiguration()

    fun loadSharedConfiguration(): SharedConfiguration =
        sharedConfigurationService.loadSharedConfiguration()

}