package com.reposilite.storage.application

import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import com.reposilite.storage.s3.S3StorageProviderSettings
import net.dzikoysk.cdn.CdnSettings
import net.dzikoysk.cdn.CdnUtils
import net.dzikoysk.cdn.model.Element
import net.dzikoysk.cdn.model.Section
import net.dzikoysk.cdn.serdes.Composer
import net.dzikoysk.cdn.serdes.TargetType
import net.dzikoysk.cdn.serdes.TargetType.ClassTargetType
import net.dzikoysk.cdn.serdes.composers.ContextualComposer
import panda.std.Result
import java.lang.Exception

class StorageProviderSettingsComposer : Composer<StorageProviderSettings> {

    private val contextualComposer = ContextualComposer()

    override fun serialize(
        settings: CdnSettings?,
        description: MutableList<String>?,
        key: String?,
        type: TargetType?,
        entity: StorageProviderSettings?
    ): Result<out Element<*>, out Exception> =
        contextualComposer.serialize(settings, description, key, type, entity)

    override fun deserialize(
        settings: CdnSettings?,
        source: Element<*>?,
        type: TargetType?,
        defaultValue: StorageProviderSettings?,
        entryAsRecord: Boolean
    ): Result<StorageProviderSettings, Exception> {
        val section = source!! as Section

        val target = when (section.getString("type").orNull()) {
            "fs" -> ClassTargetType(FileSystemStorageProviderSettings::class.java)
            "s3" -> ClassTargetType(S3StorageProviderSettings::class.java)
            else -> throw IllegalArgumentException("Unsupported type $section")
        }

        return CdnUtils.findComposer(settings, target, null)
            .flatMap { it.deserialize(settings, source, target, defaultValue, entryAsRecord) }
            .map { it as StorageProviderSettings }
    }

}