package com.reposilite.plugin

import java.util.Stack

fun toFlattenedDependencyGraph(declarations: Map<String, List<String>>): List<String> {
    val topologicalOrdering = mutableListOf<String>()
    val indegrees = declarations.mapValues { 0 }.toMutableMap()
    val nodesWithNoIncomingEdges = Stack<String>()

    declarations.forEach { (plugin, dependencies) -> dependencies
        .firstOrNull { declarations.containsKey(it).not() }
        ?.let { throw IllegalStateException("Unknown dependency '$it' in plugin '$plugin'") }
    }

    declarations
        .flatMap { (_, dependencies) -> dependencies }
        .forEach { indegrees.compute(it) { _, value -> (value ?: 0) + 1 } }

    declarations
        .filter { (node, _) -> indegrees[node] == 0 }
        .forEach { (node, _) -> nodesWithNoIncomingEdges.push(node) }

    while (nodesWithNoIncomingEdges.size > 0) {
        val node = nodesWithNoIncomingEdges.pop()
        topologicalOrdering.add(node)

        declarations[node]!!.forEach { neighbor ->
            if (indegrees.compute(neighbor) { _, value -> (value ?: 1) -1 } == 0) {
                nodesWithNoIncomingEdges.push(neighbor)
            }
        }
    }

    if (topologicalOrdering.size != declarations.size) {
        (declarations.keys - topologicalOrdering.toSet()).asSequence()
            .map { deepSearch(declarations, emptySet(), it) }
            .firstOrNull()
            ?.let { throw IllegalStateException("Circular dependency between ${it.joinToString(" -> ")}") }
            ?: throw IllegalStateException("Graph has a circular dependency that cannot be traced.")
    }

    return topologicalOrdering.reversed()
}

private fun deepSearch(declarations: Map<String, List<String>>, track: Set<String>, current: String): List<String>? =
    declarations[current]!!.asSequence()
        .map { dependency ->
            if (track.contains(dependency))
                track.toMutableList().also { it.add(dependency) }
            else
                deepSearch(declarations, track.toMutableSet().also { it.add(dependency) }, dependency)
        }
        .filterNotNull()
        .firstOrNull()