package com.reposilite.plugin.webhook.api

// Core
import club.minnced.discord.webhook.WebhookClientBuilder;
// Embed
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
// Message
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
// Config



fun sendEmbed(embed: WebhookEmbed) {
    // TODO: Make this a conig value reorganize code to use one function in the init file.
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
    client.send(embed)
        .thenAccept { message: ReadonlyMessage ->
            System.out.printf(
                "Reposilite Webhook: Discord message sent with id: [%s]%n",
                message.id
            )
        }
}

fun sendMessage(messageBuilder: WebhookMessageBuilder) {
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
    client.send(messageBuilder.build());
}