package com.reposilite.configuration.shared.api

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY

/*
 * JsonForms annotations
 */

@Retention
@Target(PROPERTY, FIELD)
annotation class Min(
    val min: Int
)

@Retention
@Target(PROPERTY, FIELD)
annotation class Max(
    val max: Int
)

@Retention
@Target(PROPERTY, FIELD)
annotation class Range(
    val min: Int,
    val max: Int
)

@Retention
@Target(PROPERTY, FIELD, CLASS)
annotation class Doc(
    val title: String,
    val description: String
)
