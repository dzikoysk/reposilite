/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.configuration.shared.api

import io.javalin.openapi.CustomAnnotation
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER

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
@Target(PROPERTY_GETTER, CLASS)
@CustomAnnotation
annotation class Doc(
    val title: String,
    val description: String
)
