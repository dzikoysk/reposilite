package com.reposilite.shared

import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder

fun and(vararg ops: SqlExpressionBuilder.() -> Op<Boolean>): Op<Boolean> =
    AndOp(ops.map { op -> Op.build(op) }.toList())