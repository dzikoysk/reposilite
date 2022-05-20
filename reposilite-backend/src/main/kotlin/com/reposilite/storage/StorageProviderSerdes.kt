package com.reposilite.storage

import com.fasterxml.classmate.ResolvedType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.deser.ValueInstantiator
import com.github.victools.jsonschema.generator.FieldScope
import com.github.victools.jsonschema.generator.SchemaGenerationContext
import com.reposilite.configuration.shared.EnumResolver
import com.reposilite.configuration.shared.SubtypeResolver
import java.util.ServiceLoader

class StorageProviderSerdes {

    companion object {

        private val STORAGE_PROVIDERS: Map<String, Class<out StorageProviderSettings>> =
            ServiceLoader.load(StorageProviderFactory::class.java).associate { factory -> factory.type to factory.settingsType }

    }

    class StorageProviderDeserializationProblemHandler : DeserializationProblemHandler() {

        override fun handleMissingInstantiator(ctx: DeserializationContext, type: Class<*>, instantiator: ValueInstantiator, parser: JsonParser, message: String): Any =
            when (type) {
                StorageProviderSettings::class.java -> parser.parseStorageProvider() ?: super.handleMissingInstantiator(ctx, type, instantiator, parser, message)
                else -> super.handleMissingInstantiator(ctx, type, instantiator, parser, message)
            }

        private fun JsonParser.parseStorageProvider(): Any? =
            codec?.let {
                val json = it.readTree<JsonNode>(this)
                val type = json.get("type").asText()
                return it.treeToValue(json, STORAGE_PROVIDERS[type])
            }

    }

    class StorageProviderEnumResolver : EnumResolver {

        override fun resolve(scope: FieldScope): Collection<String>? =
            if (scope.name == "type")
                STORAGE_PROVIDERS.filterValues { it == scope.declaringType.erasedType }.keys
            else null

    }

    class StorageProviderSubtypeResolver : SubtypeResolver {

        override fun resolve(declaredType: ResolvedType, context: SchemaGenerationContext): List<ResolvedType>? =
            if (declaredType.erasedType == StorageProviderSettings::class.java)
                STORAGE_PROVIDERS.values.map { context.typeContext.resolveSubtype(declaredType, it) }
            else null

    }

}
