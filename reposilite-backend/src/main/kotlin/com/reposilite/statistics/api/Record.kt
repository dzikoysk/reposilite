/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.statistics.api

enum class RecordType {
    REQUEST,
    UNKNOWN
}

fun findRecordTypeByName(name: String) : RecordType? =
    RecordType.values().find { it.name.equals(name, ignoreCase = true) }

data class RecordIdentifier(
    val type: RecordType,
    val identifier: String
)

data class Record(
    val type: RecordType,
    val identifier: String,
    val count: Long
)