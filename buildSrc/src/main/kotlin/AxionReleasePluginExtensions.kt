import org.gradle.kotlin.dsl.KotlinClosure2
import pl.allegro.tech.build.axion.release.domain.hooks.HookContext
import pl.allegro.tech.build.axion.release.domain.hooks.HooksConfig
import pl.allegro.tech.build.axion.release.domain.scm.ScmPosition

fun HooksConfig.fileUpdate(file: String, patternFromVersion: (String) -> String) {
    val patternClosure = KotlinClosure2(
        { version: String, _: HookContext ->
            patternFromVersion(version)
        }
    )
    pre("fileUpdate", mapOf("file" to file, "pattern" to patternClosure, "replacement" to patternClosure))
}

fun HooksConfig.commit(messageFromVersion: ((String) -> String)? = null) {
    if (messageFromVersion == null) {
        pre("commit")
    } else {
        pre("commit", KotlinClosure2(
            { v: String, _: ScmPosition ->
                messageFromVersion(v)
            }
        ))
    }
}