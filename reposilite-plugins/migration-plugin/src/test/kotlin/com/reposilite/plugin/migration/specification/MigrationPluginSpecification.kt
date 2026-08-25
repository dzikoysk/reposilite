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

package com.reposilite.plugin.migration.specification

import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.plugin.migration.MigrationPlugin
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal open class MigrationPluginSpecification {

    @TempDir
    lateinit var workingDirectory: File

    private val logger = InMemoryLogger()
    private val extensions = Extensions(AggregatedLogger(logger, PrintStreamLogger(System.out, System.err)))
    protected val migrationPlugin = MigrationPlugin()

    init {
        ReposilitePluginAccessor.injectExtension(migrationPlugin, extensions)
    }

    fun workingDirectory(): Path =
        workingDirectory.toPath()

    fun resource(test: String, file: String): String =
        MigrationPluginSpecification::class.java.getResourceAsStream("/$test - $file")!!.readBytes().decodeToString()

}