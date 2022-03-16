package com.reposilite.plugin.webhook.listener

import com.reposilite.maven.Repository
import com.reposilite.plugin.webhook.api.sendDiscordEmbed
import com.reposilite.plugin.webhook.api.sendDiscordMessage
import com.reposilite.storage.api.Location


fun webhookEvent(by: String, gav: Location, repo: Repository) {
    val webhookService = "discord" // Should be removed
    val webhookType = "message" // Should be removed
    if (webhookService === "discord") {
        if (webhookType === "message") {
            sendDiscordMessage(by, gav, repo)
        } else if (webhookService === "embed") {
            sendDiscordEmbed(by, gav, repo)
        } else {
            println("Reposilite Webhook: Invaild Webhook Type found, using embed.")
            sendDiscordEmbed(by, gav, repo)
        }
    } else {
        // Send the Slack message/embed.
        println("Reposilite Webhook: Slack is not yet implemented or there was an invalid embed service!")
    }
}