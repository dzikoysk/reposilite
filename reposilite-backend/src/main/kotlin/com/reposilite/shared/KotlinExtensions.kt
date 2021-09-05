package com.reposilite.shared

import java.util.concurrent.atomic.AtomicBoolean

fun AtomicBoolean.peek(block: () -> Unit) {
    if (this.get()) {
        block()
    }
}