package com.reposilite.plugin.api

interface Event

@FunctionalInterface
fun interface EventListener<E : Event> {

    fun onCall(event: E)

    fun priority(): Double = Priorities.DEFAULT

}

object Priorities {

    const val HIGH = 0.0
    const val DEFAULT = 1.0
    const val LOW = 2.0

}