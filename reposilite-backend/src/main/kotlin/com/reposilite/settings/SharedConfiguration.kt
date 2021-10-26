package com.reposilite.settings

import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import com.reposilite.shared.Validator
import net.dzikoysk.cdn.entity.Contextual
import net.dzikoysk.cdn.entity.Description
import net.dzikoysk.cdn.entity.DeserializationHandler
import panda.std.reactive.mutableReference
import panda.std.reactive.reference
import panda.utilities.StringUtils
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.Serializable

class SharedConfiguration : Serializable, DeserializationHandler<SharedConfiguration> {

    @Description("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #")
    @Description("#      Reposilite :: Shared      #")
    @Description("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #")
    @Description("")
    @Description("# Repository id used in Maven repository configuration")
    @JvmField
    val id = mutableReference("reposilite-repository")

    @Description("# Repository title")
    @JvmField
    val title = mutableReference("Reposilite Repository")

    @Description("# Repository description")
    @JvmField
    val description = mutableReference("Public Maven repository hosted through the Reposilite")

    @Description("# Link to organization's website")
    @JvmField
    val organizationWebsite = mutableReference("https://reposilite.com")

    @Description("# Link to organization's logo")
    @JvmField
    val organizationLogo = mutableReference("https://avatars.githubusercontent.com/u/88636591")

    @Description("# The Internet Content Provider License (also known as Bei'An)")
    @Description("# Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.")
    @Description("# In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.")
    @JvmField
    val icpLicense = mutableReference("")

    @Description("# Enable default frontend with dashboard")
    @JvmField
    val frontend = reference(true)

    @Description("# Enable Swagger (/swagger-docs) and Swagger UI (/swagger)")
    @JvmField
    val swagger = reference(false)

    @Description("# Custom base path")
    @JvmField
    val basePath = mutableReference("/")

    /* Repository properties */

    @Description("")
    @Description("# List of supported Maven repositories")
    @JvmField
    val repositories = mutableReference(mapOf(
        "releases" to RepositoryConfiguration(),
        "snapshots" to RepositoryConfiguration(),
        "private" to RepositoryConfiguration().also { it.visibility = PRIVATE }
    ))

    @Contextual
    class RepositoryConfiguration : Serializable {

        @Description("# Supported visibilities: public, hidden, private")
        @JvmField
        var visibility = RepositoryVisibility.PUBLIC

        @Description("# Does this repository accept redeployment of the same artifact version")
        @JvmField
        var redeployment = false

        @Description("")
        @Description("# Used storage type. Supported storage providers:")
        @Description("# > File system (local) provider. Supported flags:")
        @Description("# --quota 10GB = control the maximum amount of data stored in this repository. Supported formats: 90%, 500MB, 10GB (optional, by default: unlimited)")
        @Description("# --mount /mnt/releases = use custom directory to locate the repository data (optional, by default repositories are stored in repositories/{name} directory)")
        @Description("# Example usage:")
        @Description("# storageProvider: fs --quota 50GB")
        @Description("# > S3 provider. Supported flags:")
        @Description("# --endpoint = custom endpoint with which the S3 provider should communicate (optional)")
        @Description("# Example usage:")
        @Description("# storageProvider: s3 --endpoint custom.endpoint.com accessKey secretKey region bucket-name")
        @JvmField
        var storageProvider = "fs --quota 100%"

        @Command(name = "fs", description = ["Local file system (disk) storage provider settings"])
        internal class FSStorageProviderSettings : Validator() {
            @Option(names = ["-q", "--quota"], defaultValue = "100%")
            lateinit var quota: String
            @Option(names = ["-m", "--mount"], defaultValue = "")
            lateinit var mount: String
        }

        @Command(name = "s3", description = ["Amazon S3 storage provider settings"])
        internal class S3StorageProviderSettings : Validator() {
            @Option(names = ["-e", "--endpoint"], defaultValue = "")
            lateinit var endpoint: String
            @Parameters(index = "0", paramLabel = "<access-key>")
            lateinit var accessKey: String
            @Parameters(index = "1", paramLabel = "<secret-key>")
            lateinit var secretKey: String
            @Parameters(index = "2", paramLabel = "<region>")
            lateinit var region: String
            @Parameters(index = "3", paramLabel = "<bucket-name>")
            lateinit var bucketName: String
        }

        @Description("")
        @Description("# List of proxied repositories associated with this repository.")
        @Description("# Reposilite will search for a requested artifact in remote repositories listed below.")
        @Description("# Supported flags:")
        @Description("# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability")
        @Description("# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)")
        @Description("# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)")
        @Description("# Example usage:")
        @Description("# proxied [")
        @Description("#   https://repo.panda-lang.org/releases --store --connectTimeout=3 --readTimeout=15 --auth user:token")
        @Description("# ]")
        @JvmField
        var proxied = mutableListOf<String>()

        @Command(description = ["An entry representing one proxied host and its configuration"])
        class ProxiedHostConfiguration : Validator() {
            @Option(names = ["--store"])
            var store = false
            @Option(names = ["--connectTimeout"])
            var connectTimeout = 3
            @Option(names = ["--readTimeout"])
            var readTimeout = 15
            @Option(names = ["--authorization", "--auth"])
            var authorization: String? = null
        }

    }

    @Description("# Any kind of proxy services change real ip.")
    @Description("# The origin ip should be available in one of the headers.")
    @Description("# Nginx: X-Forwarded-For")
    @Description("# Cloudflare: CF-Connecting-IP")
    @Description("# Popular: X-Real-IP")
    @JvmField
    val forwardedIp = mutableReference("X-Forwarded-For")

    override fun handle(sharedConfiguration: SharedConfiguration): SharedConfiguration {
        var formattedBasePath = basePath.get()

        // verify base path
        if (!StringUtils.isEmpty(formattedBasePath)) {
            if (!formattedBasePath.startsWith("/")) {
                formattedBasePath = "/$formattedBasePath"
            }

            if (!formattedBasePath.endsWith("/")) {
                formattedBasePath += "/"
            }

            this.basePath.update(formattedBasePath)
        }

        return this
    }

}