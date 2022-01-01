package com.reposilite

import org.junit.jupiter.api.Assertions.assertEquals

internal fun assertCollectionsEquals(first: Collection<Any?>, second: Collection<Any?>) {
    if (first.size == second.size && first.containsAll(second) && second.containsAll(first)) {
        return
    }

    assertEquals(first.sortedBy { it.toString() }, second.sortedBy { it.toString() }) // pretty printing
}