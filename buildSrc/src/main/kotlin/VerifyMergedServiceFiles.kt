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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.ZipFile

@DisableCachingByDefault(because = "Verification task has no outputs")
abstract class VerifyMergedServiceFiles : DefaultTask() {

    @get:Classpath
    abstract val serviceSources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archiveFile: RegularFileProperty

    @TaskAction
    fun verifyServiceFiles() {
        val expectedServices = collectServices(serviceSources.files)
        val actualServices = collectServices(listOf(archiveFile.get().asFile))
        val missingServices = expectedServices
            .mapValues { (service, expectedProviders) ->
                expectedProviders - actualServices.getOrDefault(service, emptySet())
            }
            .filterValues(Set<String>::isNotEmpty)

        if (missingServices.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Shaded JAR is missing service providers:")
                    missingServices.toSortedMap().forEach { (service, providers) ->
                        appendLine("- $service")
                        providers.sorted().forEach { provider -> appendLine("  - $provider") }
                    }
                }.trimEnd()
            )
        }

        logger.lifecycle(
            "Verified {} providers across {} service descriptors in {}",
            expectedServices.values.sumOf(Set<String>::size),
            expectedServices.size,
            archiveFile.get().asFile.name
        )
    }

    private fun collectServices(files: Iterable<File>): Map<String, Set<String>> =
        buildMap<String, MutableSet<String>> {
            files.forEach { file ->
                when {
                    file.isDirectory -> collectFromDirectory(file, this)
                    file.isFile && file.extension in ARCHIVE_EXTENSIONS -> collectFromArchive(file, this)
                }
            }
        }

    private fun collectFromDirectory(root: File, services: MutableMap<String, MutableSet<String>>) {
        val servicesDirectory = root.resolve(SERVICES_PATH)

        if (!servicesDirectory.isDirectory) {
            return
        }

        servicesDirectory
            .walkTopDown()
            .filter(File::isFile)
            .forEach { serviceFile ->
                val servicePath = "$SERVICES_PATH/${serviceFile.relativeTo(servicesDirectory).invariantSeparatorsPath}"
                collectProviders(servicePath, serviceFile.readText(), services)
            }
    }

    private fun collectFromArchive(archive: File, services: MutableMap<String, MutableSet<String>>) {
        ZipFile(archive).use { zipFile ->
            zipFile.entries().asSequence()
                .filterNot { entry -> entry.isDirectory }
                .filter { entry -> entry.name.startsWith("$SERVICES_PATH/") }
                .forEach { entry ->
                    val content = zipFile.getInputStream(entry).bufferedReader().use { reader -> reader.readText() }
                    collectProviders(entry.name, content, services)
                }
        }
    }

    private fun collectProviders(
        servicePath: String,
        content: String,
        services: MutableMap<String, MutableSet<String>>
    ) {
        if (servicePath == GROOVY_EXTENSION_MODULE) {
            return
        }

        content.lineSequence()
            .map { line -> line.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .forEach { provider -> services.getOrPut(servicePath, ::mutableSetOf).add(provider) }
    }

    private companion object {
        const val SERVICES_PATH = "META-INF/services"
        const val GROOVY_EXTENSION_MODULE = "$SERVICES_PATH/org.codehaus.groovy.runtime.ExtensionModule"
        val ARCHIVE_EXTENSIONS = setOf("jar", "zip")
    }
}
