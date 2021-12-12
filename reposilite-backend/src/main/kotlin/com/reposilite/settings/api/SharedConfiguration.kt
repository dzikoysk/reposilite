package com.reposilite.settings.api

import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import com.reposilite.shared.extensions.Validator
import net.dzikoysk.cdn.entity.Contextual
import net.dzikoysk.cdn.entity.Description
import net.dzikoysk.cdn.serdes.DeserializationHandler
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
    val id = reference("reposilite-repository")

    @Description("# Repository title")
    @JvmField
    val title = reference("Reposilite Repository")

    @Description("# Repository description")
    @JvmField
    val description = reference("Public Maven repository hosted through the Reposilite")

    @Description("# Link to organization's website")
    @JvmField
    val organizationWebsite = reference("https://reposilite.com")

    @Description("# Link to organization's logo")
    @JvmField
    val organizationLogo = reference("https://avatars.githubusercontent.com/u/88636591")

    @Description("# The Internet Content Provider License (also known as Bei'An)")
    @Description("# Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.")
    @Description("# In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.")
    @JvmField
    val icpLicense = reference("")

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
    val repositories = reference(mapOf(
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
        @Description("# --mount /mnt/releases = use custom directory to locate the repository data (optional, by default it's './repositories/{name}')")
        @Description("# Example usage:")
        @Description("# storageProvider: fs --quota 50GB")
        @Description("# > S3 provider. Supported flags:")
        @Description("# --endpoint = overwrite the AWS endpoint (optional)")
        @Description("# --access-key = overwrite AWS access-key used to authenticate (optional)")
        @Description("# --secret-key = overwrite AWS secret-key used to authenticate (optional)")
        @Description("# --region = overwrite AWS region (optional)")
        @Description("# See software.amazon.awssdk.services.s3.S3Client for default values")
        @Description("# Example usage:")
        @Description("# storageProvider: s3 bucket-name --endpoint custom.endpoint.com --access-key accessKey --secret-key secretKey --region region")
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
            @Parameters(index = "0", paramLabel = "<bucket-name>")
            lateinit var bucketName: String
            @Option(names = ["-e", "--endpoint"], defaultValue = "")
            lateinit var endpoint: String
            @Option(names = ["-a", "--access-key"], defaultValue = "")
            lateinit var accessKey: String
            @Option(names = ["-s", "--secret-key"], defaultValue = "")
            lateinit var secretKey: String
            @Option(names = ["-r", "--region"], defaultValue = "")
            lateinit var region: String
        }

        @Description("")
        @Description("# List of proxied repositories associated with this repository.")
        @Description("# Reposilite will search for a requested artifact in remote repositories listed below.")
        @Description("# Supported flags:")
        @Description("# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability")
        @Description("# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)")
        @Description("# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)")
        @Description("# --allow=<group prefix> - An allowed artifact group. Can be specified multiple times. If none are given, all artifacts can be obtained from this proxy.")
        @Description("# Example usage:")
        @Description("# proxied [")
        @Description("#   https://repo.panda-lang.org/releases --store --connectTimeout=3 --readTimeout=15 --auth user:token --allow=com.reposilite  --proxy host:ip")
        @Description("#   local-repository-name")
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
            @Option(names = ["--allow", "--allowGroup"], arity = "0..*")
            var allowedGroups = emptyArray<String>()
            @Option(names = ["--proxy"])
            var proxy = ""
        }

    }

    /* Statistics */

    @Description("")
    @Description("# Statistics module configuration")
    @JvmField
    val statistics = reference(StatisticsConfiguration())

    @Contextual
    class StatisticsConfiguration : Serializable {

        @Description("# How often Reposilite should divide recorded requests into separated groups.")
        @Description("# With higher precision you can get more detailed timestamps, but it'll increase database size.")
        @Description("# It's not that important for small repos with low traffic, but public instances should not use daily interval.")
        @Description("# Available modes: daily, weekly, monthly, yearly")
        @JvmField
        var resolvedRequestsInterval = "monthly"

    }

    /* Utilities */

    @Description("")
    @Description("# Any kind of proxy services change real ip.")
    @Description("# The origin ip should be available in one of the headers.")
    @Description("# Nginx: X-Forwarded-For")
    @Description("# Cloudflare: CF-Connecting-IP")
    @Description("# Popular: X-Real-IP")
    @JvmField
    val forwardedIp = reference("X-Forwarded-For")

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