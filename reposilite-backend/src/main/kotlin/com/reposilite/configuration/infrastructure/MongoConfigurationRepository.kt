package com.reposilite.configuration.infrastructure

import com.reposilite.configuration.ConfigurationRepository
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.time.Instant

class MongoConfigurationRepository(private val mongoClient: MongoClient, private val databaseName: String) : ConfigurationRepository {

    private val database: MongoDatabase = mongoClient.getDatabase(databaseName)
    private val collection: MongoCollection<Document> = database.getCollection("settings")

    override fun saveConfiguration(name: String, configuration: String) {
        val document = Document("name", name)
            .append("updateDate", Instant.now().toString())
            .append("content", configuration)
        collection.insertOne(document)
    }

    override fun findConfiguration(name: String): String? {
        val document = collection.find(Document("name", name)).first()
        return document?.getString("content")
    }

    override fun findConfigurationUpdateDate(name: String): Instant? {
        val document = collection.find(Document("name", name)).first()
        return document?.getString("updateDate")?.let { Instant.parse(it) }
    }

}
