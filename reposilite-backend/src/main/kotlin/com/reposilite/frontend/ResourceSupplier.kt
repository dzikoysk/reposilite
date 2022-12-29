package com.reposilite.frontend

import panda.std.Result
import java.io.IOException
import java.io.InputStream

fun interface ResourceSupplier {
    fun supply(): Result<InputStream, IOException>
}