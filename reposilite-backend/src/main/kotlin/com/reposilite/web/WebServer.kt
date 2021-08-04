package com.reposilite.web

import com.reposilite.Reposilite

interface WebServer {

    fun start(reposilite: Reposilite)

    fun stop()

    fun isAlive(): Boolean

}