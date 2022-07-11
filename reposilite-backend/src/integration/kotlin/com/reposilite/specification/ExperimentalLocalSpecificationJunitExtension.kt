/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.specification

import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Integrations used in local stack:
 * - H2
 * - Local file system
 */
internal class ExperimentalLocalSpecificationJunitExtension : Extension, BeforeEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        context?.also {
            val instance = it.requiredTestInstance
            val type = instance::class.java

            type.getField("_extensionInitialized").set(instance, true)
            type.getField("_database").set(instance, "h2 --temporary")
            type.getField("_storageProvider").set(instance, FileSystemStorageProviderSettings())
        }
    }

}