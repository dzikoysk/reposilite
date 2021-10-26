package com.reposilite.shared

import net.dzikoysk.cdn.model.Reference

/**
 * Executes the given block whenever any of the given references have been updated
 */
fun computed(vararg references: Reference<*>, block: () -> Unit) {
    references.forEach {
        it.subscribe { block() }
    }
}