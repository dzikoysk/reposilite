package com.reposilite.packages.maven.api

import java.io.InputStream
import org.apache.commons.codec.digest.DigestUtils

enum class Checksum(val extension: String, val generate: (InputStream) -> String) {
    MD5("md5", { DigestUtils.md5Hex(it) }),
    SHA1("sha1", { DigestUtils.sha1Hex(it) }),
    SHA256("sha256", { DigestUtils.sha256Hex(it) }),
    SHA512("sha512", { DigestUtils.sha512Hex(it) })
}