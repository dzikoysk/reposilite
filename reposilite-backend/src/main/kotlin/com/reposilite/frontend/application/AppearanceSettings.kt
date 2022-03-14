package com.reposilite.frontend.application

import java.io.Serializable

data class AppearanceSettings(
    /** Repository id used in Maven repository configuration */
    val id: String = "reposilite-repository",
    /** Repository title. */
    val title: String = "Reposilite Repository",
    /** Repository description. */
    val description: String = "Public Maven repository hosted through the Reposilite",
    /** Link to organization's website. */
    val organizationWebsite: String = "https://reposilite.com",
    /** Link to organization's logo. */
    val organizationLogo: String = "https://avatars.githubusercontent.com/u/88636591"
) : Serializable
