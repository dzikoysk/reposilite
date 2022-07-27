package com.reposilite.storage

import com.reposilite.configuration.shared.api.SharedSettings

interface StorageProviderSettings : SharedSettings {
    val type: String
}
