package com.reposilite.web.application

import com.reposilite.settings.shared.Doc
import com.reposilite.settings.shared.SharedSettings

@Doc(title = "Web", description = "General web settings")
data class WebSettings(
    @Doc(title = "Forwarded IP", description = """
        Any kind of proxy services change real ip.
        The origin ip should be available in one of the headers. <br />
        Nginx: X-Forwarded-For <br />
        Cloudflare: CF-Connecting-IP <br />
        Popular: X-Real-IP
    """)
    val forwardedIp: String = "X-Forwarded-For",
) : SharedSettings
