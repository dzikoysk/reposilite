package com.reposilite.storage.filesystem

import com.reposilite.storage.api.FileType.FILE
import com.reposilite.storage.type
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.name

internal class UsageTracker(
    private val rootDirectory: Path,
) {
    private val counterFile: Path = rootDirectory.resolve(".local/usage/${rootDirectory.name}")
    private val lock = Any()
    @Volatile
    private var loadedUsage: Long = loadUsage()

    fun getUsage(): Long = loadedUsage

    fun addDelta(delta: Long) {
        synchronized(lock) {
            loadedUsage += delta
            if (loadedUsage < 0) {
                loadedUsage = recalculate()
            } else {
                counterFile.createParentDirectories()
                counterFile.toFile().writeText("$loadedUsage\n")
            }
        }
    }

    private fun loadUsage(): Long =
        try {
            if (counterFile.exists()) {
                counterFile.toFile().readText().trim().toLongOrNull()?.takeIf { it >= 0 }
                    ?: recalculate()
            } else {
                recalculate()
            }
        } catch (e: Exception) {
            recalculate()
        }

    private fun recalculate(): Long {
        val size = Files.walk(rootDirectory).use { stream ->
            stream.filter { it.type() == FILE }
                .mapToLong { it.fileSize() }
                .sum()
        }
        counterFile.createParentDirectories()
        counterFile.toFile().writeText("$size\n")
        return size
    }
}
