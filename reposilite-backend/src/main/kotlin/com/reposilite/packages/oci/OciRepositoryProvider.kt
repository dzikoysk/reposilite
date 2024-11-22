package com.reposilite.packages.oci

class OciRepositoryProvider {

    private val repositories = mutableMapOf<String, OciRepository>()

    fun getRepositories(): Collection<OciRepository> = repositories.values

}