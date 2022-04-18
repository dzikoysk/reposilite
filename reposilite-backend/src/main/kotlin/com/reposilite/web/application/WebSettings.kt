package com.reposilite.web.application

import com.reposilite.settings.api.Doc
import net.dzikoysk.cdn.entity.Contextual

@Contextual
@Doc(title = "Web", description = "General web settings")
data class WebSettings(
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
)