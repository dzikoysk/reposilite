package com.reposilite.storage

import com.reposilite.maven.api.DocumentInfo
import com.reposilite.shared.FileType.FILE
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.toPath
import io.javalin.http.ContentType.APPLICATION_JAR
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal abstract class StorageProviderTest : StorageProviderSpec() {

    @Test
    fun `should store and return valid resource` () {
        // given: a destination path to the resource and its content
        val path = "/directory/file.data".toPath()
        val content = "content".toByteArray()

        // when: non-existing resource is requested
        val nonExistingResource = storageProvider.getFile(path)

        // then: response should contain error
        assertError(nonExistingResource)

        // when: resource is put in storage and then requested
        val putResponse = storageProvider.putFile(path, content)

        // then: put request should succeed
        assertOk(putResponse)

        // when: stored resource is requested
        val fetchResponse = storageProvider.getFile(path)

        // then: provider should return proper resource
        assertOk(fetchResponse)
        assertArrayEquals(content, fetchResponse.get().readBytes())
    }

    @Test
    fun `should return valid file details` () {
        // given: a destination path to the resource and its content
        val path = "/directory/file.jar".toPath()
        val content = "content".toByteArray()
        storageProvider.putFile(path, content)

        // when: file details are requested
        val response = storageProvider.getFileDetails(path)

        // then: response should contain expected file details
        val fileDetails = assertOk(response) as DocumentInfo
        assertEquals(FILE, fileDetails.type)
        assertEquals(path.getSimpleName(), fileDetails.name)
        assertEquals(content.size.toLong(), fileDetails.contentLength)
        assertEquals(APPLICATION_JAR, fileDetails.contentType)
    }

}