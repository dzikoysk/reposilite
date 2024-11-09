package com.reposilite.packages.oci.api

data class UploadState(
    val sessionId: String,
    val name: String,
    val uploadedData: ByteArray,
    val bytesReceived: Int,
    val createdAt: String
)
