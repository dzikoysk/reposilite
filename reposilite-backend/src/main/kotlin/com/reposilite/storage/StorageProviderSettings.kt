package com.reposilite.storage

import com.reposilite.settings.shared.SharedSettings

interface StorageProviderSettings : SharedSettings {
    val type: String
}