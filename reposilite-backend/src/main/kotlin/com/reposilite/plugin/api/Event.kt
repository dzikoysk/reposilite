package com.reposilite.plugin.api

interface Event

fun interface EventListener<E : Event> {

    fun onCall(event: E)

    fun priority(): Double = Priorities.DEFAULT

}

object Priorities {

    val HIGH = 0.0
    val DEFAULT = 1.0
    val LOW = 2.0

}