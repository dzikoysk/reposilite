package com.reposilite.frontend.application

import com.reposilite.settings.api.Doc
import java.io.Serializable

@Doc(title = "Appearance", description = "Appearance settings")
data class AppearanceSettings(
    /** Enable default frontend with dashboard */
    @Doc(title = "Frontend", description = "Enable default frontend with dashboard")
    val frontend: Boolean = true,
    /** Custom base path */
    @Doc(title = "Custom Base Path", description = "Custom base path the frontend is located at.")
    val basePath: String = "/",
    /** Repository id used in Maven repository configuration */
    @Doc(title = "Id", description = "Repository id used in Maven repository configuration")
    val id: String = "reposilite-repository",
    /** Repository title. */
    @Doc(title = "Title", description = "The title displayed on the frontend homepage.")
    val title: String = "Reposilite Repository",
    /** Repository description. */
    @Doc(title = "Description", description = "The description displayed on the frontend homepage.")
    val description: String = "Public Maven repository hosted through the Reposilite",
    /** Link to organization's website. */
    @Doc(title = "Organisation Website", description = "Link to organization's website.")
    val organizationWebsite: String = "https://reposilite.com",
    /** Link to organization's logo. */
    @Doc(title = "Organisation Logo", description = "Link to organization's logo.")
    val organizationLogo: String = "https://avatars.githubusercontent.com/u/88636591",
    /**
     * Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
     * In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.
     */
    @Doc(title = "ICP License", description = """
        Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
        In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.
    """)
    val icpLicense: String = "",
) : Serializable
