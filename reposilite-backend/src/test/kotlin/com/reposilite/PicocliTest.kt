package com.reposilite

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

internal class PicocliTest {

    @Command
    class Foo : Runnable {

        @Option(names = ["--store"])
        var store = false

        override fun run() { }

    }

    @Test
    fun `should support flags`() {
        val foo = Foo()

        val commandLine = CommandLine(foo)
        val args = arrayOf("--store")
        commandLine.execute(*args)

        assertTrue(foo.store)
    }

}