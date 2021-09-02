package com.reposilite.storage

import com.reposilite.shared.toPath
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal abstract class StorageProviderTest {

    protected lateinit var storageProvider: StorageProvider

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

}