subprojects {
    group = "com.reposilite.plugin"

    publishing {
        publications {
            create<MavenPublication>("library") {
                from(components.getByName("java"))
                // Gradle generator does not support <repositories> section from Maven specification.
                // ~ https://github.com/gradle/gradle/issues/15932
                pom.withXml {
                    val repositories = asNode().appendNode("repositories")
                    project.repositories.findAll(closureOf<Any> {
                        if (this is MavenArtifactRepository && this.url.toString().startsWith("https")) {
                            val repository = repositories.appendNode("repository")
                            repository.appendNode("id", this.name)
                            repository.appendNode("url", this.url.toString())
                        }
                    })
                }
            }
        }
    }
}