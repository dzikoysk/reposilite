---
id: plugin-api
title: Plugin API
---

Plugin system has been introduced in Reposilite 3.x and allows users to extend & customize their instances.

### Build system
Preferred build for plugins is [Gradle 7+](https://gradle.org/) with [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html). 

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "example.plugin"

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

application {
    mainClass.set("example.plugin.TestPluginKt")
}

dependencies {
    compileOnly("org.panda-lang:reposilite:3.0.0-rc.1")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("example-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}
```

### Sources

Every plugin has to provide `com.reposilite.plugin.api.ReposilitePlugin` implementation:

<CodeVariants>
  <CodeVariant name="example.plugin.TestPlugin.kt (Kotlin)">

```kotlin
@Plugin(name = "example")
class ExamplePlugin : ReposilitePlugin {

  override fun initialize(): Facade? {
    event { event: ReposiliteInitializeEvent ->
      logger.info("")
      logger.info("--- Example plugin")
      logger.info("Example plugin has been properly loaded")
    }

    return null
  }

}
```

  </CodeVariant>
  <CodeVariant name="example.plugin.TestPlugin.java (Java)">
  
```java
@Plugin(name = "test")
public final class ExamplePlugin extends ReposilitePlugin {

    @Override
    public @Nullable Facade initialize() {
        extensions().registerEvent(ReposiliteInitializeEvent.class, event -> {
            getLogger().info("");
            getLogger().info("--- Example plugin");
            getLogger().info("Example plugin has been properly loaded");
        });
        return null;
    }

}
```

  </CodeVariant>
</CodeVariants>

The last thing you need to do is a declaration of a service file in resources directory that tells what's the main class in your plugin:

* `/resources/META-INF/services/com.reposilite.plugin.api.ReposilitePlugin`:
```bash
example.ExamplePlugin
```

Then, just run `gradle shadowJar` task and put output file in `plugins` directory located in working directory of your Reposilite instance.

### API
List of classes and functions:

<Spoiler title="@Plugin" paddingX="5" paddingY="2">

Plugin annotation describes plugin using those properties:

| Property | Type | Description |
| :--: | :--: | :--: |
| name | String | Defines the name of plugin |
| version | String | Plugin version, by default the same as Reposilite dependency |
| dependencies | String[] | Array of required dependencies (builtin domains or plugins) |

~ [API / Plugin.java](https://github.com/dzikoysk/reposilite/blob/main/reposilite-backend/src/main/kotlin/com/reposilite/plugin/api/Plugin.java)

</Spoiler>

<Spoiler title="ReposilitePlugin" paddingX="5" paddingY="2">

Allows Reposilite to inject `Extensions` instance into your plugin, 
so you can communicate with other components of application.

_ReposilitePlugin_ exposes 2 callbacks:
1. `fun load(loader: PluginLoader)` - Called immediately when plugin is loaded in Reposilite
2. `fun initialize() -> Facade?` - Main method for your plugin,
   called when Reposilite will ensure that all required dependencies have been satisfied and everything works. You can also expose your own Facade implementation here if you want to expose some public API from your plugin.

~ [API / ReposilitePlugin.java](https://github.com/dzikoysk/reposilite/blob/main/reposilite-backend/src/main/kotlin/com/reposilite/plugin/api/ReposilitePlugin.java)

</Spoiler>

<Spoiler title="Extensions" paddingX="5" paddingY="2">

Extensions allows you to:
* Register event listeners
* Emit events
* Register facades
* Get external facades

~ [API / Extensions.java](https://github.com/dzikoysk/reposilite/blob/main/reposilite-backend/src/main/kotlin/com/reposilite/plugin/Extensions.kt)

</Spoiler>

### Facades

Facade is just a name for class that exposes public functions to other domains (API). The difference between `Facade` and `Service` is in the visibility - `Service` is meant to use and handle internal implementation, `Facade` for external users. You can find more about used architecture here:

* [Reposilite :: Backend - README](https://github.com/dzikoysk/reposilite/tree/main/reposilite-backend)