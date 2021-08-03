package org.panda_lang.reposilite.maven

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.panda_lang.reposilite.config.Configuration.RepositoryConfiguration
import org.panda_lang.reposilite.maven.api.DeployRequest
import org.panda_lang.reposilite.maven.api.RepositoryVisibility
import org.panda_lang.reposilite.shared.FileType.FILE

internal class MavenFacadeTest : MavenSpec() {

    override fun repositories() = mapOf(
        "public" to RepositoryConfiguration().also {
            it.visibility = RepositoryVisibility.PUBLIC
        },
        "hidden" to RepositoryConfiguration().also {
            it.visibility = RepositoryVisibility.HIDDEN
        },
        "private" to RepositoryConfiguration().also {
            it.visibility = RepositoryVisibility.PRIVATE
        }
    )

    @ParameterizedTest
    @EnumSource(RepositoryVisibility::class)
    fun `should deploy file under the given path`(visibility: RepositoryVisibility) {
        // given: an uri and file to store
        val repository = visibility.name.lowercase()
        val gav = "/org/panda-lang/reposilite/3.0.0/reposilite-3.0.0.jar"
        val by = "dzikoysk@127.0.0.1"
        val file = "content".byteInputStream()

        // when: the following file is deployed
        val fileDetails = mavenFacade.deployFile(DeployRequest(repository, gav, by, file))
            .onError { fail { it.toString() } }
            .get()

        // then: file has been successfully stored
        assertEquals(FILE, fileDetails.type)
        assertEquals("reposilite-3.0.0.jar", fileDetails.name)
        assertFalse(fileDetails.isReadable())
    }

}