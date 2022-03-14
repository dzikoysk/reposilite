package com.reposilite.frontend.application

import com.reposilite.settings.api.Doc
import java.io.Serializable

@Doc(title = "Appearance", description = "Appearance settings")
data class AppearanceSettings(
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
    val organizationLogo: String = "https://avatars.githubusercontent.com/u/88636591"
) : Serializable
