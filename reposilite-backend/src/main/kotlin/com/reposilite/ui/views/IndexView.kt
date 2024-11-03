package com.reposilite.ui.views

data class IndexView(
    val title: String,
    val description: String,
    val logo: String,
    val website: String,
    val highlightedProjects: List<Project>,
    val repositories: List<Repository>,
    val browsedRepository: BrowsedRepository? = null,
) {
    data class Project(
        val name: String,
        val description: String,
        val routes: List<Route>
    ) {
        fun getSupportedPackages(): Set<String> = routes.map { it.repository.type }.toSet()
    }
    data class Route(
        val repository: Repository,
        val path: String,
    )
    data class Repository(
        val name: String,
        val type: String
    )
    data class BrowsedRepository(
        val name: String,
        val files: List<Entry>
    ) {
        interface Entry

        data class File(
            val name: String,
            val size: String,
        ) : Entry

        data class Directory(
            val name: String,
        ) : Entry
    }
    data class GroupedRepository(
        val type: String,
        val repositories: List<Repository>
    )
    fun getGroupedRepositories(): List<GroupedRepository> =
        repositories
            .groupBy { it.type }
            .entries
            .map { GroupedRepository(type = it.key, repositories = it.value) }
            .sortedByDescending { it.repositories.size }
}