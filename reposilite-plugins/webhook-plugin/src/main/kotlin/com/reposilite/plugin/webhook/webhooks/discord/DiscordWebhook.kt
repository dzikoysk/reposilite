package com.reposilite.plugin.webhook.webhooks.discord

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location




fun sendDiscordEmbed(by: String, gav: Location, repo: Repository) {
    val url = "https://thisshouldbereplacedinproduction.example.com/webhook";

    val clientBuilder = WebhookClientBuilder(url);
    clientBuilder.setThreadFactory { job: Runnable? ->
        val thread = Thread(job)
        thread.name = "Reposilite"
        thread.isDaemon = true
        thread
    }
    clientBuilder.setWait(true);

    val client = clientBuilder.build();

    // Temp Description until I can figure out how to style embeds more,
    val embed = WebhookEmbedBuilder()
        .setColor(0xFF00EE)
        .setDescription("A new artifact named" + gav.getSimpleName() + " was uploaded to " + repo.name + " by " + by + "!")
        .build();

    client.send(embed)
        .thenAccept { message: ReadonlyMessage ->
            System.out.printf(
                "Reposilite Webhook: Discord embed sent with id: [%s]%n",
                message.id
            )
        }
}

fun sendDiscordMessage(by: String, gav: Location, repo: Repository) {
    val url = "https://thisshouldbereplacedinproduction.example.com/webhook";

    val clientBuilder = WebhookClientBuilder(url);
    clientBuilder.setThreadFactory { job: Runnable? ->
        val thread = Thread(job)
        thread.name = "Reposilite"
        thread.isDaemon = true
        thread
    }
    clientBuilder.setWait(true);
    val client = clientBuilder.build();

    val messageBuilder = WebhookMessageBuilder();
    messageBuilder.setUsername("Reposilite")
    messageBuilder.setAvatarUrl("https://raw.githubusercontent.com/dzikoysk/reposilite/main/reposilite-site-next/public/images/favicon.png")
    messageBuilder.setContent("A new artifact named" + gav.getSimpleName() + " was uploaded to " + repo.name + " by " + by + "!")
    client.send(messageBuilder.build())
        .thenAccept { message: ReadonlyMessage ->
            System.out.printf(
                "Reposilite Webhook: Discord message sent with id: [%s]%n",
                message.id
            )
        }
}