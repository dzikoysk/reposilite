package com.reposilite.storage

import com.reposilite.configuration.shared.SharedSettings

interface StorageProviderSettings : SharedSettings {
    val type: String
}