/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.auth

import panda.std.Result
import panda.std.asError
import panda.std.asSuccess

private const val NUL_CHARACTER = '\u0000'

internal class Secret private constructor(val value: String) {

    companion object {

        fun create(value: String): Result<Secret, String> =
            when {
                value.isNotBlank() && NUL_CHARACTER !in value -> Secret(value).asSuccess()
                else -> "Secret must not be blank or contain NUL characters".asError()
            }

    }

}
