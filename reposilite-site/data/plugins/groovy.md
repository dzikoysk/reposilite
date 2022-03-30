---
id: groovy
title: Groovy
description: Utility plugin that enables scripting API in Groovy
official: true
repository: dzikoysk/reposilite
authors: [ 'dzikoysk' ]
---

Utility plugin that enables scripting API in Groovy. In general, you should probably use Java/Kotlin for better experience and performance, but that's still an option - especially useful for quick prototyping of dirty scripts.

Before you'll start using Groovy, you should definitely take a look at:
* [Guide / Developers - Plugin API](/guide/plugin-api)

Then, you can just writing your extension in Groovy. 
Standard implementation of `ReposilitePlugin` should look like this:

```groovy
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin

@Plugin(name = "test", version = "1.0.0")
class TestPlugin extends ReposilitePlugin {

    @Override
    Facade initialize() {
        extensions().registerEvent(ReposiliteInitializeEvent.class, event -> {
            getLogger().info("Hello from Groovy")
        })

        return new TestFacade()
    }

}

class TestFacade implements Facade {

}
```

You can now just put `test-plugin.groovy` script in `$working-directory/plugins/` directory
and Groovy plugin should load your script during startup.