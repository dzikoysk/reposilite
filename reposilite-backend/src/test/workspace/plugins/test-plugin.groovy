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

        return null
    }

}