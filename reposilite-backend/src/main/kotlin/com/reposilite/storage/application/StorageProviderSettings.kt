package com.reposilite.storage.application

import net.dzikoysk.cdn.entity.Contextual

@Contextual
interface StorageProviderSettings {
    val type: String
}