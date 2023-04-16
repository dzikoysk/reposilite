package com.reposilite.plugin.checksum

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.PreResolveEvent
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.checksum.ChecksumPlugin.ChecksumType.MD5
import com.reposilite.plugin.checksum.ChecksumPlugin.ChecksumType.SHA1
import com.reposilite.plugin.checksum.ChecksumPlugin.ChecksumType.SHA256
import com.reposilite.plugin.checksum.ChecksumPlugin.ChecksumType.SHA512
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.storage.api.Location
import org.apache.commons.codec.digest.DigestUtils

@Plugin(name = "checksum", dependencies = ["maven"])
class ChecksumPlugin : ReposilitePlugin() {

    enum class ChecksumType(val extension: String) {
        MD5(".md5"),
        SHA1(".sha1"),
        SHA256(".sha256"),
        SHA512(".sha512");
    }

    override fun initialize(): Facade? {
        val mavenFacade = facade<MavenFacade>()
        val checksums = ChecksumType.values()

        fun Location.getChecksum(): ChecksumType? =
            checksums.find { endsWith(it.extension) }

        event { (accessToken, repository, checksumGav): PreResolveEvent ->
            if (repository.mirrorHosts.isNotEmpty()) {
                logger.debug("Checksum | ${repository.name} uses mirrors")
                return@event
            }

            val checksum = checksumGav.getChecksum() ?: run {
                logger.debug("Checksum | $checksumGav is not a checksum file")
                return@event
            }

            val file = checksumGav.getParent().resolve(checksumGav.getSimpleName().substring(0, checksumGav.getSimpleName().length - checksum.extension.length))

            if (file.getChecksum() != null) {
                logger.debug("Checksum | Cannot generate checksum for existing checksum file: $file")
                return@event
            }

            if (repository.storageProvider.exists(checksumGav)) {
                logger.debug("Checksum | $checksumGav already exists")
                return@event // checksum already exists
            }

            val (_, data) = mavenFacade
                .findFile(
                    LookupRequest(
                        accessToken = accessToken,
                        repository = repository.name,
                        gav = file
                    )
                )
                .orNull() // ignore files that don't exist
                ?: run {
                    logger.debug("Checksum | $file does not exist")
                    return@event
                }

            val generatedChecksum = data.use {
                when (checksum) {
                    MD5 -> DigestUtils.md5(data)
                    SHA1 -> DigestUtils.sha1(data)
                    SHA256 -> DigestUtils.sha256(data)
                    SHA512 -> DigestUtils.sha512(data)
                }
            }

            repository.storageProvider.putFile(checksumGav, generatedChecksum.inputStream())
                .peek { logger.debug("Checksum | Generated checksum ${checksum.name} for $file") }
                .onError { logger.debug("Checksum | Cannot generate checksum ${checksum.name} for $file due to $it") }
        }

        return null
    }

}