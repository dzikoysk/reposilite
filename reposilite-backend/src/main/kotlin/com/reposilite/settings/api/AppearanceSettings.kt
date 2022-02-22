package com.reposilite.settings.api

import java.io.Serializable

data class AppearanceSettings(
    /** Repository id used in Maven repository configuration */
    val id: String,
    /** Repository title. */
    val title: String,
    /** Repository description. */
    val description: String,
    /** Link to organization's website. */
    val organizationWebsite: String,
    /** Link to organization's logo. */
    val organizationLogo: String
) : Serializable
