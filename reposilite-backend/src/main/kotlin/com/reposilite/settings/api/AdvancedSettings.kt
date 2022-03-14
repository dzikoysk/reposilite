package com.reposilite.settings.api

import java.io.Serializable

@Doc(title = "Advanced", description = "Advanced settings")
data class AdvancedSettings(
    /** Custom base path */
    @Doc(title = "Custom Base Path", description = "Custom base path the frontend is located at.")
    val basePath: String = "/",
    /** Enable default frontend with dashboard */
    @Doc(title = "Frontend", description = "Enable default frontend with dashboard")
    val frontend: Boolean = true,
    /** Enable Swagger (/swagger-docs) and Swagger UI (/swagger). */
    @Doc(title = "Swagger", description = "Enable Swagger (/swagger-docs) and Swagger UI (/swagger).")
    val swagger: Boolean = false,
    /**
     * Any kind of proxy services change real ip.
     * The origin ip should be available in one of the headers.
     * Nginx: X-Forwarded-For
     * Cloudflare: CF-Connecting-IP
     * Popular: X-Real-IP
     */
    @Doc(title = "Forwarded IP", description = """Any kind of proxy services change real ip.
The origin ip should be available in one of the headers.
Nginx: X-Forwarded-For
Cloudflare: CF-Connecting-IP
Popular: X-Real-IP""")
    val forwardedIp: String = "X-Forwarded-For",
    /**
     * Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
     * In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.
     */
    @Doc(title = "ICP License", description = """Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.""")
    val icpLicense: String = ""
) : Serializable
