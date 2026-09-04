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

package com.reposilite.generic

import com.reposilite.repository.api.RepositoryAccessMode
import com.reposilite.repository.api.RepositoryInfo
import com.reposilite.storage.StorageProvider

internal const val GENERIC_REPOSITORY_TYPE = "generic"

class GenericRepository internal constructor(
    val name: String,
    val visibility: RepositoryAccessMode,
    val redeployment: Boolean,
    val storageProvider: StorageProvider,
) {

    internal val info = RepositoryInfo(name, visibility)

}
