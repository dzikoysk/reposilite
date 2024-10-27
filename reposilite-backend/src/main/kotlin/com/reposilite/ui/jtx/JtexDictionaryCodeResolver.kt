package com.reposilite.ui.jtx

import gg.jte.resolve.DirectoryCodeResolver
import java.nio.file.Path

class JtexDictionaryCodeResolver(path: Path) : DirectoryCodeResolver(path) {

    override fun resolveRequired(name: String?): String {
        val content = super.resolveRequired(name)
        return content
    }

}