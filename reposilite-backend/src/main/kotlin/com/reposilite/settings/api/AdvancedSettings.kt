package com.reposilite.settings.api

import java.io.Serializable

data class AdvancedSettings(
    /** Custom base path */
    val basePath: String,
    /** Enable default frontend with dashboard */
    val frontend: Boolean,
    /** Enable Swagger (/swagger-docs) and Swagger UI (/swagger). */
    val swagger: Boolean,
    /** Any kind of proxy services change real ip.
The origin ip should be available in one of the headers.
Nginx: X-Forwarded-For
Cloudflare: CF-Connecting-IP
Popular: X-Real-IP */
    val forwardedIp: String,
    /** Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter. */
    val icpLicense: String
) : Serializable
