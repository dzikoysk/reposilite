package com.reposilite.storage

import com.reposilite.maven.api.DocumentInfo
import com.reposilite.shared.FileType.FILE
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.toPath
import com.reposilite.storage.specification.StorageProviderSpecification
import io.javalin.http.ContentType.APPLICATION_JAR
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal abstract class StorageProviderTest : StorageProviderSpecification() {

    @Test
    fun `should store and return valid resource` () {
        // given: a destination path to the resource and its content
        val path = "/directory/file.data".toPath()
        val content = "content".toByteArray()

        // when: resource is put in storage and then requested
        val putResponse = storageProvider.putFile(path, content.inputStream())

        // then: put request should succeed
        assertOk(putResponse)
        assertTrue(storageProvider.exists(path))

        // when: stored resource is requested
        val fetchResponse = storageProvider.getFile(path)

        // then: provider should return proper resource
        assertOk(fetchResponse)
        assertArrayEquals(content, fetchResponse.get().readBytes())
    }

    @Test
    fun `should return error if non-existing resource has been requested` () {
        // given: some path to the resource that doesn't exist
        val resource = "/not/found.data".toPath()

        // when: non-existing resource is requested
        val nonExistingResource = storageProvider.getFile(resource)

        // then: response should contain error
        assertError(nonExistingResource)
    }

    @Test
    fun `should return valid file details` () {
        // given: a destination path to the resource and its content
        val path = "/directory/file.jar".toPath()
        val content = "content".toByteArray()
        storageProvider.putFile(path, content.inputStream())

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