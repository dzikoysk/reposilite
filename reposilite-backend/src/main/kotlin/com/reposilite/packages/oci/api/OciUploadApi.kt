package com.reposilite.packages.oci.api

data class UploadState(
    val sessionId: String,
    val name: String,
    var uploadedData: ByteArray,
    var bytesReceived: Int,
    val createdAt: String
)
