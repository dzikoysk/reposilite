package org.panda_lang.reposilite.token.infrastructure

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE

typealias Id = EntityID<Int>

object AccessTokenTable : IntIdTable("access_token") {

    val alias: Column<String> = varchar("alias", 512).uniqueIndex()
    val secret: Column<String> = varchar("secret", 512)
    val permissions: Column<String> = varchar("permissions", 32)

}

object RouteTable : IntIdTable("access_token_route") {

    val accessTokenId: Column<Id> = reference("access_token_id", AccessTokenTable.id, onDelete = CASCADE, onUpdate = CASCADE)
    val path: Column<String> = varchar("path", 2048)
    val permissions: Column<String> = varchar("permissions", 32)

}