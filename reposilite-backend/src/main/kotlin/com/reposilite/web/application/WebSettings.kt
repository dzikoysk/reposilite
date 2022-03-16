package com.reposilite.web.application

import com.reposilite.settings.api.Doc
import java.io.Serializable

@Doc(title = "Web", description = "General web settings")
data class WebSettings(
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
    @Doc(title = "Forwarded IP", description = """
        Any kind of proxy services change real ip.
        The origin ip should be available in one of the headers.
        Nginx: X-Forwarded-For
        Cloudflare: CF-Connecting-IP
        Popular: X-Real-IP
    """)
    val forwardedIp: String = "X-Forwarded-For",
) : Serializable
