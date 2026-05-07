/*
 * Copyright (c) 2026 dzikoysk
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

package com.reposilite.shared

fun maskSecret(secret: String?): String {
    if (secret.isNullOrEmpty()) return "<empty>"
    val whitespace = Regex("\\s")
    val core = secret.trim()
    if (core.isEmpty()) return secret.replace(whitespace, "<whitespace>")
    val mid = if (core.length < 5) "*".repeat(core.length) else "${core.first()}${"*".repeat(core.length - 2)}${core.last()}"
    return secret.replace(core, mid).replace(whitespace, "<whitespace>")
}
