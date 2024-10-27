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

package com.reposilite.token

import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.token.api.AccessTokenDetails
import panda.std.Result
import panda.std.Result.supplyThrowing
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.readText

class ExportService {

    fun exportToFile(tokens: Collection<AccessTokenDetails>, toFile: Path): Path =
        Files.writeString(toFile, DEFAULT_OBJECT_MAPPER.writeValueAsString(tokens), TRUNCATE_EXISTING, CREATE)

    fun importFromFile(fromFile: Path): Result<Collection<AccessTokenDetails>, Exception> =
        supplyThrowing { DEFAULT_OBJECT_MAPPER.readValue<List<AccessTokenDetails>>(fromFile.readText()) }

}