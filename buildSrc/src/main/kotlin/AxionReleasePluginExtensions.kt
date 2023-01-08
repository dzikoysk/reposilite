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

import org.gradle.kotlin.dsl.KotlinClosure2
import pl.allegro.tech.build.axion.release.domain.hooks.HookContext
import pl.allegro.tech.build.axion.release.domain.hooks.HooksConfig
import pl.allegro.tech.build.axion.release.domain.scm.ScmPosition

fun HooksConfig.fileUpdate(file: String, patternFromVersion: (String) -> String) {
    val patternClosure = KotlinClosure2(
        { version: String, _: HookContext ->
            patternFromVersion(version)
        }
    )
    pre("fileUpdate", mapOf("file" to file, "pattern" to patternClosure, "replacement" to patternClosure))
}

fun HooksConfig.commit(messageFromVersion: ((String) -> String)? = null) {
    if (messageFromVersion == null) {
        pre("commit")
    } else {
        pre("commit", KotlinClosure2(
            { v: String, _: ScmPosition ->
                messageFromVersion(v)
            }
        ))
    }
}