package com.reposilite.storage

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.deser.ValueInstantiator
import java.util.ServiceLoader

class StorageProviderDeserializationProblemHandler : DeserializationProblemHandler() {

    private val storageProviders = ServiceLoader.load(StorageProviderFactory::class.java).associate { factory -> factory.type to factory.settingsType }

    override fun handleMissingInstantiator(ctx: DeserializationContext, type: Class<*>, instantiator: ValueInstantiator, parser: JsonParser, message: String): Any =
        when (type) {
            StorageProviderSettings::class.java -> parser.parseStorageProvider() ?: super.handleMissingInstantiator(ctx, type, instantiator, parser, message)
            else -> super.handleMissingInstantiator(ctx, type, instantiator, parser, message)
        }

    private fun JsonParser.parseStorageProvider(): Any? =
        codec?.let {
            val json = it.readTree<JsonNode>(this)
            val type = json.get("type").asText()
            return it.treeToValue(json, storageProviders[type])
        }

}