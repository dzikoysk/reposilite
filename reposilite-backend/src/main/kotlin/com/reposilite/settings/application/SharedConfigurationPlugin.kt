package com.reposilite.settings.application

import com.reposilite.maven.RepositoryVisibility
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.infrastructure.SharedConfigurationEndpoints
import com.reposilite.settings.SharedConfigurationFacade
import com.reposilite.settings.api.*
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import com.reposilite.token.AccessTokenType
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "sharedconfig")
class SharedConfigurationPlugin: ReposilitePlugin() {
    override fun initialize(): SharedConfigurationFacade {
        val sharedConfigurationFacade = SharedConfigurationFacade()
        event { event: ReposiliteInitializeEvent ->
            val sharedConfiguration = event.reposilite.extensions.facade<SettingsFacade>().sharedConfiguration
            with (sharedConfigurationFacade) {
                registerHandler(SettingsHandler.of("appearance", AppearanceSettings::class.java, sharedConfiguration::getAppearanceSettingsDTO, sharedConfiguration::updateFromAppearanceSettingsDTO))
                registerHandler(SettingsHandler.of("advanced", AdvancedSettings::class.java, sharedConfiguration::getAdvancedSettingsDTO, sharedConfiguration::updateFromAdvancedSettingsDTO))
                registerHandler(SettingsHandler.of("repositories", RepositoriesSettings::class.java, sharedConfiguration::getRepositoriesSettingsDTO, sharedConfiguration::updateFromRepositoriesSettingsDTO))
                registerHandler(SettingsHandler.of("statistics", StatisticsSettings::class.java, sharedConfiguration::getStatisticsSettingsDTO, sharedConfiguration::updateFromStatisticsSettingsDTO))
                registerHandler(SettingsHandler.of("ldap", LdapSettings::class.java, sharedConfiguration::getLdapSettingsDTO, sharedConfiguration::updateFromLdapSettingsDTO))
                registerHandler(SettingsHandler.of("all", Settings::class.java, sharedConfiguration::getSettingsDTO, sharedConfiguration::updateFromSettingsDTO))
            }
        }
        event { event: RoutingSetupEvent ->
            event.registerRoutes(SharedConfigurationEndpoints(sharedConfigurationFacade))
        }
        return sharedConfigurationFacade
    }
}

private fun parseStorageProvider(storageProvider: String): RepositorySettings.StorageProvider {
    if (storageProvider.startsWith("fs")) {
        val configuration = loadCommandBasedConfiguration(SharedConfiguration.RepositoryConfiguration.FSStorageProviderSettings(), storageProvider).configuration
        return FSStorageProviderSettings(configuration.quota, configuration.mount)
    } else if (storageProvider.startsWith("s3")) {
        val configuration = loadCommandBasedConfiguration(SharedConfiguration.RepositoryConfiguration.S3StorageProviderSettings(), storageProvider).configuration
        return S3StorageProviderSettings(configuration.bucketName, configuration.endpoint, configuration.accessKey, configuration.secretKey, configuration.region)
    }
    throw RuntimeException("unsupported storage provider")
}

private fun parseProxied(proxied: String): RepositorySettings.ProxiedRepository = loadCommandBasedConfiguration(SharedConfiguration.RepositoryConfiguration.ProxiedHostConfiguration(), proxied).let { RepositorySettings.ProxiedRepository(reference = it.name, store = it.configuration.store, connectTimeout = it.configuration.connectTimeout.toLong(), readTimeout = it.configuration.readTimeout.toLong(), authorization = parseAuthorisation(it.configuration.authorization), allowedGroups = it.configuration.allowedGroups.toList()) }

private fun parseAuthorisation(authorization: String?): RepositorySettings.ProxiedRepository.Authorization? = authorization?.split(':')?.let { RepositorySettings.ProxiedRepository.Authorization(it[0], it[1]) }

private fun SharedConfiguration.updateFromStatisticsSettingsDTO(settings: StatisticsSettings): StatisticsSettings {
    statistics.update(SharedConfiguration.StatisticsConfiguration().also { it.resolvedRequestsInterval = settings.resolvedRequestsInterval.name.lowercase() })
    return getStatisticsSettingsDTO()
}

private fun formatStorageProvider(storageProvider: RepositorySettings.StorageProvider): String = when (storageProvider.type) {
    "fs" -> with (storageProvider as FSStorageProviderSettings) {
        var o = type
        if (quota.isNotEmpty()) {
            o += " --quota $quota"
        }
        if (mount.isNotEmpty()) {
            o += " --mount $mount"
        }
        o
    }
    "s3" -> with (storageProvider as S3StorageProviderSettings) {
        var o = "$type $bucketName"
        if (endpoint.isNotEmpty()) {
            o += " --endpoint $endpoint"
        }
        if (accessKey.isNotEmpty()) {
            o += " --access-key $accessKey"
        }
        if (secretKey.isNotEmpty()) {
            o += " --secret-key $secretKey"
        }
        if (region.isNotEmpty()) {
            o += " --region $region"
        }
        o
    }
    else -> throw RuntimeException("unsupported storage provider")
}

private fun formatProxied(proxiedRepository: RepositorySettings.ProxiedRepository): String = with(proxiedRepository) {
    var o = reference
    if (store) {
        o += " --store"
    }
    if (connectTimeout != 3L) {
        o += " --connectTimeout $connectTimeout"
    }
    if (readTimeout != 15L) {
        o += " --readTimeout $readTimeout"
    }
    if (authorization != null) {
        o += " --auth ${authorization.name}:${authorization.token}"
    }
    allowedGroups.forEach { o += " --allow=$it" }
    o
}

private fun SharedConfiguration.updateFromSettingsDTO(settings: Settings): Settings {
    updateFromAppearanceSettingsDTO(settings.appearance)
    updateFromAdvancedSettingsDTO(settings.advanced)
    updateFromRepositoriesSettingsDTO(RepositoriesSettings(settings.repositories))
    updateFromStatisticsSettingsDTO(settings.statistics)
    updateFromLdapSettingsDTO(settings.ldap)
    return getSettingsDTO()
}

private fun SharedConfiguration.updateFromLdapSettingsDTO(settings: LdapSettings): LdapSettings {
    ldap.update(SharedConfiguration.LdapConfiguration().apply {
        enabled = settings.enabled
        hostname = settings.hostname
        port = settings.port
        baseDn = settings.baseDn
        searchUserDn = settings.searchUserDn
        searchUserPassword = settings.searchUserPassword
        userAttribute = settings.userAttribute
        userFilter = settings.userFilter
        userType = AccessTokenType.valueOf(settings.userType.name)
    })
    return getLdapSettingsDTO()
}

private fun SharedConfiguration.updateFromRepositoriesSettingsDTO(settings: RepositoriesSettings): RepositoriesSettings {
    this.repositories.update(settings.repositories.mapValues {
        val repositoryConfiguration = SharedConfiguration.RepositoryConfiguration()
        repositoryConfiguration.preserved = it.value.preserved
        repositoryConfiguration.redeployment = it.value.redeployment
        repositoryConfiguration.visibility = RepositoryVisibility.valueOf(it.value.visibility.name)
        repositoryConfiguration.storageProvider = formatStorageProvider(it.value.storageProvider)
        repositoryConfiguration.proxied = it.value.proxied.map(::formatProxied).toMutableList()
        repositoryConfiguration
    })
    return getRepositoriesSettingsDTO()
}

private fun SharedConfiguration.updateFromAdvancedSettingsDTO(settings: AdvancedSettings): AdvancedSettings {
    basePath.update(settings.basePath)
    frontend.update(settings.frontend)
    swagger.update(settings.swagger)
    forwardedIp.update(settings.forwardedIp)
    icpLicense.update(settings.icpLicense)
    return getAdvancedSettingsDTO()
}

private fun SharedConfiguration.updateFromAppearanceSettingsDTO(settings: AppearanceSettings): AppearanceSettings {
    id.update(settings.id)
    title.update(settings.title)
    description.update(settings.description)
    organizationLogo.update(settings.organizationLogo)
    organizationWebsite.update(settings.organizationWebsite)
    return getAppearanceSettingsDTO()
}

private fun SharedConfiguration.RepositoryConfiguration.toDTO(): RepositorySettings = RepositorySettings(visibility = RepositorySettings.Visibility.valueOf(visibility.name), redeployment = redeployment, preserved = preserved, storageProvider = parseStorageProvider(storageProvider), proxied = proxied.map { parseProxied(it) })

private fun SharedConfiguration.getSettingsDTO(): Settings = Settings(appearance = getAppearanceSettingsDTO(), advanced = getAdvancedSettingsDTO(), repositories = getRepositoriesSettingsDTO().repositories, statistics = getStatisticsSettingsDTO(), ldap = getLdapSettingsDTO())

private fun SharedConfiguration.getLdapSettingsDTO(): LdapSettings = with(ldap.get()) { LdapSettings(enabled = enabled, hostname = hostname, port = port, baseDn = baseDn, searchUserDn = searchUserDn, searchUserPassword = searchUserPassword, userAttribute = userAttribute, userFilter = userFilter, userType = LdapSettings.UserType.valueOf(userType.name)) }

private fun SharedConfiguration.getStatisticsSettingsDTO(): StatisticsSettings = StatisticsSettings(resolvedRequestsInterval = StatisticsSettings.ResolvedRequestsInterval.valueOf(statistics.get().resolvedRequestsInterval.uppercase()))

private fun SharedConfiguration.getRepositoriesSettingsDTO(): RepositoriesSettings = repositories.map { RepositoriesSettings(repositories = it.mapValues { entry -> entry.value.toDTO() }) }

private fun SharedConfiguration.getAdvancedSettingsDTO(): AdvancedSettings = AdvancedSettings(basePath = basePath.get(), frontend = frontend.get(), swagger = swagger.get(), forwardedIp = forwardedIp.get(), icpLicense = icpLicense.get())

private fun SharedConfiguration.getAppearanceSettingsDTO(): AppearanceSettings = AppearanceSettings(id = id.get(), title = title.get(), description = description.get(), organizationWebsite = organizationWebsite.get(), organizationLogo = organizationLogo.get())
