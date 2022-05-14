package com.reposilite.configuration.shared

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.Facade
import com.reposilite.status.FailureFacade
import panda.std.Result
import panda.std.asError
import panda.std.ok
import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference

class SharedConfigurationFacade(
    private val journalist: Journalist,
    private val schemaGenerator: SchemaGenerator,
    private val failureFacade: FailureFacade
) : Facade {

    private val domains = mutableMapOf<Class<*>, MutableReference<*>>()
    private val configHandlers = mutableMapOf<String, SharedSettingsReference<*>>()

    fun <S : SharedSettings> createDomainSettings(settingsInstance : S): MutableReference<S> =
        mutableReference(settingsInstance).also {
            domains[settingsInstance.javaClass] = it
            registerSettingsWatcher(settingsInstance.javaClass, it)
        }

    private fun <T : SharedSettings> registerSettingsWatcher(handler: SharedSettingsReference<T>): SharedSettingsReference<T> =
        handler.also {
            if (it.name in configHandlers) throw IllegalArgumentException("There are already settings with that name! Please report to the plugin author.")
            configHandlers[it.name] = it
        }

    private fun <T : SharedSettings> registerSettingsWatcher(type: Class<T>, reference: MutableReference<T>): SharedSettingsReference<T> =
        registerSettingsWatcher(type, reference::get) { reference.update(it) }

    private fun <T : SharedSettings> registerSettingsWatcher(type: Class<T>, getter: () -> T, setter: (T) -> Unit): SharedSettingsReference<T> =
        registerSettingsWatcher(DefaultSharedSettingsReference(type, schemaGenerator, getter, setter))

    internal fun updateSharedSettings(content: String): Result<Unit, Collection<Pair<SharedSettingsReference<*>, Exception>>> {
        val updateResult = Result.attempt { DEFAULT_OBJECT_MAPPER.readTree(content) }
            .map { node -> names().filter { node.has(it) }.associateWith { node.get(it) } }
            .orElseGet { emptyMap() }
            .mapKeys { (name) -> getSettingsReference<SharedSettings>(name)!! }
            .mapValues { (ref, obj) -> DEFAULT_OBJECT_MAPPER.readValue(obj.toString(), ref.type) }
            .map { (ref, settings) -> ref to updateSharedSettings(ref.name, settings)!! }

        updateResult
            .filter { (_, result) -> result.isOk }
            .forEach { (ref) -> journalist.logger.info("Shared configuration | Domain '${ref.name}' has been loaded from database") }

        val failures = updateResult
            .filter { (_, result) -> result.isErr }

        failures.forEach { (ref, result) ->
            journalist.logger.error("Shared configuration | Cannot update '${ref.name}' due to ${result.error}")
            journalist.logger.debug("Shared configuration | Source:")
            journalist.logger.debug(content)
            failureFacade.throwException("Shared configuration", result.error)
        }

        return failures
            .map { (ref, result) -> ref to result.error }
            .takeIf { it.isNotEmpty() }
            ?.asError()
            ?: ok()
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : SharedSettings> updateSharedSettings(name: String, body: S): Result<S, out Exception>? =
        getSettingsReference<S>(name)?.update(body)

    inline fun <reified T> getDomainSettings(): MutableReference<T> =
        getDomainSettings(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <S> getDomainSettings(settingsClass: Class<S>): MutableReference<S> =
        domains[settingsClass] as MutableReference<S>

    @Suppress("UNCHECKED_CAST")
    fun <S : SharedSettings> getSettingsReference(name: String): SharedSettingsReference<S>? =
        configHandlers[name] as? SharedSettingsReference<S>

    fun names(): Collection<String> =
        configHandlers.keys

}
