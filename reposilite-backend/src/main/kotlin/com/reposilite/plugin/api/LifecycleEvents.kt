package com.reposilite.plugin.api

import com.reposilite.Reposilite

class ReposiliteInitializeEvent(val reposilite: Reposilite) : Event

class ReposilitePostInitializeEvent(val reposilite: Reposilite) : Event

class ReposiliteDisposeEvent(val reposilite: Reposilite) : Event