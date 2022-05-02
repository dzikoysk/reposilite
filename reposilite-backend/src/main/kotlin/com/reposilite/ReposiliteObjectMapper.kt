package com.reposilite

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.deser.ValueInstantiator
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.reposilite.shared.extensions.ContentTypeSerializer
import com.reposilite.storage.StorageProviderFactory
import com.reposilite.storage.application.StorageProviderSettings
import io.javalin.http.ContentType
import java.util.ServiceLoader

object ReposiliteObjectMapper {

    val DEFAULT_OBJECT_MAPPER: ObjectMapper = JsonMapper.builder()
        .addModule(JavaTimeModule())
        .addModule(SimpleModule().also {
            it.addSerializer(ContentType::class.java, ContentTypeSerializer())
        })
        .build()
        .registerKotlinModule()
        .setSerializationInclusion(Include.NON_NULL)
        .addHandler(object: DeserializationProblemHandler() {// TODO: move this to a proper place
            override fun handleMissingInstantiator(
                ctxt: DeserializationContext?,
                instClass: Class<*>?,
                valueInsta: ValueInstantiator?,
                p: JsonParser?,
                msg: String?
            ): Any {
                if (instClass == StorageProviderSettings::class.java) {
                    val storageProviders = ServiceLoader.load(StorageProviderFactory::class.java).associate { it.type to it.settingsType }
                    return p?.codec?.let {
                        val json = it.readTree<JsonNode>(p)
                        val type = json.get("type").asText()
                        return it.treeToValue(json, storageProviders[type])
                    } ?: super.handleMissingInstantiator(ctxt, instClass, valueInsta, p, msg)
                }
                return super.handleMissingInstantiator(ctxt, instClass, valueInsta, p, msg)
            }
        })

}