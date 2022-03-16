package com.reposilite.settings.api

import com.fasterxml.jackson.databind.JsonNode
import com.github.victools.jsonschema.generator.FieldScope
import com.github.victools.jsonschema.generator.MethodScope
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.generator.TypeScope
import com.reposilite.storage.application.FileSystemStorageProviderSettings
import com.reposilite.storage.application.StorageProviderSettings
import com.reposilite.storage.application.S3StorageProviderSettings
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Retention
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class Min(val min: Int)

@Retention
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class Max(val max: Int)

@Retention
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class Range(val min: Int, val max: Int)

@Retention
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class Doc(val title: String, val description: String)


interface SettingsHandler<T> {
    val name: String
    val type: Class<T>
    val schema: JsonNode
    fun get(): T
    fun update(value: T)

    companion object {
        private val generator = SchemaGenerator(SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, SCHEMA_OPTION_PRESET).with(SettingsModule()).build())

        @JvmStatic
        fun <T> of(name: String, type: Class<T>, getter: Supplier<T>, updater: Consumer<T>): SettingsHandler<T> = object : SettingsHandler<T> {
            override val name = name
            override val type = type
            override val schema = generator.generateSchema(type)
            override fun get(): T = getter.get()
            override fun update(value: T): Unit = updater.accept(value)
        }
    }
}

val FieldScope.kProperty: KProperty1<*, *>?
    get() = this.declaringType.erasedType.kotlin.memberProperties.find { member -> member.name == this.name }

val MethodScope.kProperty: KProperty1<*, *>?
    get() = this.declaringType.erasedType.kotlin.memberProperties.find { member -> member.getter.name == this.name }

val FieldScope.doc: Doc?
    get() = this.getAnnotationConsideringFieldAndGetterIfSupported(Doc::class.java) ?: this.kProperty?.findAnnotation()

val MethodScope.doc: Doc?
    get() = this.getAnnotationConsideringFieldAndGetterIfSupported(Doc::class.java) ?: this.kProperty?.findAnnotation()

val TypeScope.doc: Doc?
    get() = this.type.erasedType.getAnnotation(Doc::class.java)

private val SCHEMA_OPTION_PRESET = OptionPreset(
    Option.SCHEMA_VERSION_INDICATOR,
    Option.ADDITIONAL_FIXED_TYPES,
    Option.FLATTENED_ENUMS,
    Option.FLATTENED_OPTIONALS,
    Option.VALUES_FROM_CONSTANT_FIELDS,
    Option.PUBLIC_NONSTATIC_FIELDS,
    Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
    Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
    Option.ALLOF_CLEANUP_AT_THE_END,
    Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES,
    Option.PLAIN_DEFINITION_KEYS,
    Option.EXTRA_OPEN_API_FORMAT_VALUES,
    Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS,
    Option.GETTER_METHODS,
    Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT,
    Option.DEFINITIONS_FOR_ALL_OBJECTS
)

class SettingsModule: com.github.victools.jsonschema.generator.Module {
    override fun applyToConfigBuilder(builder: SchemaGeneratorConfigBuilder) {
        builder.forTypesInGeneral().withSubtypeResolver { declaredType, context -> when (declaredType.erasedType) {
            StorageProviderSettings::class.java -> listOf(
                context.typeContext.resolveSubtype(declaredType, FileSystemStorageProviderSettings::class.java),
                context.typeContext.resolveSubtype(declaredType, S3StorageProviderSettings::class.java)
            )
            else -> null
        }}

        builder.forFields().withEnumResolver { if (it.name == "type") when(it.declaringType.erasedType) {
            FileSystemStorageProviderSettings::class.java -> listOf("fs")
            S3StorageProviderSettings::class.java -> listOf("s3")
            else -> null
        } else null }

        builder.forFields().withNumberInclusiveMinimumResolver { field -> field.getAnnotationConsideringFieldAndGetterIfSupported(Min::class.java)?.min?.toBigDecimal() }
        builder.forFields().withNumberInclusiveMaximumResolver { field -> field.getAnnotationConsideringFieldAndGetterIfSupported(Max::class.java)?.max?.toBigDecimal() }
        builder.forFields().withNumberInclusiveMinimumResolver { field -> field.getAnnotationConsideringFieldAndGetterIfSupported(Range::class.java)?.min?.toBigDecimal() }
        builder.forFields().withNumberInclusiveMaximumResolver { field -> field.getAnnotationConsideringFieldAndGetterIfSupported(Range::class.java)?.max?.toBigDecimal() }

        builder.forFields().withTitleResolver { field -> field.doc?.title }
        builder.forMethods().withTitleResolver { methods -> methods.doc?.title }
        builder.forTypesInGeneral().withTitleResolver { scope -> scope.doc?.title }

        builder.forFields().withDescriptionResolver { field -> field.doc?.description?.trimIndent() }
        builder.forMethods().withDescriptionResolver { methods -> methods.doc?.description?.trimIndent() }
        builder.forTypesInGeneral().withDescriptionResolver { scope -> scope.doc?.description?.trimIndent() }

        builder.forFields().withNullableCheck { field -> field.kProperty?.returnType?.isMarkedNullable }
    }
}
