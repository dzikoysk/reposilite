package com.reposilite

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.reposilite.shared.extensions.ContentTypeSerializer
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
        .apply { ServiceLoader.load(DeserializationProblemHandler::class.java).forEach { addHandler(it) } }

}