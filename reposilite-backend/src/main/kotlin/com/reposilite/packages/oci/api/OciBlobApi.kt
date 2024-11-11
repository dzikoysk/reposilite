package com.reposilite.packages.oci.api

import java.io.InputStream

data class BlobResponse(
    val length: Int,
    val content: InputStream,
    val digest: String,
)
