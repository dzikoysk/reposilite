# Reposilite Backend

### About

Backend is written in [Kotlin](https://kotlinlang.org/) and uses [Javalin](https://javalin.io/) as its main HTTP server 
with a couple of extensions developed within [Reposilite Playground](https://github.com/reposilite-playground) organization.
Main libraries used by project:

* [Javalin](https://javalin.io/) - HTTP server based on [Jetty](https://www.eclipse.org/jetty/)
* [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Reactive approach to handle async request and future responses
* [Exposed](https://github.com/JetBrains/Exposed) - SQLite, MySQL DSL support
* [AWS S3 Client](https://github.com/aws/aws-sdk-java-v2) - Remote file storage
* [Fuel](https://github.com/kittinunf/fuel) - Reactive HTTP client
* [Expressible](https://github.com/panda-lang/expressible) - Functional programming extensions, mostly used to work around `Result<Value, Error>` pattern
* [Picocli](https://picocli.info/) - Command line and command-like configuration properties support
* [JUnit 5](https://junit.org/junit5/) - Unit tests
* [Testcontainers](https://www.testcontainers.org/) - Integration test, requires Docker (for Windows you need Docker Desktop). 
  Don't worry if you're not able to run these tests locally - you can always make PR and we'll run it through GitHub Actions automatically :)
* See [build.gradle.kts](https://github.com/dzikoysk/reposilite/blob/main/reposilite-backend/build.gradle.kts) for more

Since 3.x Reposilite supports multiple infrastructure targets, it's written using design patterns known as 
[Domain Driven Development _(DDD)_](https://en.wikipedia.org/wiki/Domain-driven_design) & 
[Hexagonal architecture](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)) used in business to guarantee high quality of code organized by a well-know structure. 
What does it mean? Well, to simplify this aspect it mostly comes to the overall app layout and the way the code is organized. 
That's why you can expect to see this kind of sources in `reposilite-backend` module:

```bash
com.reposilite
  feature/                           # That's our domain, called 'feature'
    api/                             # Public API exposed by 'feature' domain
      ResponseDto.kt                 # E.g. a DTO with a response returned by FeatureFacade
    application/                     # Application layer classes that somehow configures the given domain
      FeatureWebConfiguration.kt     # Main configuration class used to register domain in Reposilite app
    infrastructure/                  # Infrastructure dependent implementations
      SQLRepository.kt               # E.g. an implementation based on SQL
    Repository.kt                    # Some abstract components to implement by various infrastructure impls
    FeatureFacade.kt                 # Unified class that contains all public methods exposed by the given domain
```

When you're trying to add something to Reposilite, try to think about every domain as a standalone module and follow this pattern :)
If you want learn more about it, visit some great dedicated articles, like e.g. [Organizing Layers Using Hexagonal Architecture, DDD, and Spring](https://www.baeldung.com/hexagonal-architecture-ddd-spring)

### Running 

You can run Reposilite in various ways, it depends what you expect:

* Run classes that end with `*Test.kt` to launch simple unit tests without launching Reposilite
* Run classes that end with `*InfrastructureTest.kt` to launch infrastructure tests that run Reposilite with required dependencies in Docker image
* Run Reposilite in test workspace with predefined configuration for IntelliJ - [Reposilite.run.xml](https://github.com/dzikoysk/reposilite/blob/main/.run/Reposilite.run.xml)
* Run all tests with Gradle - `gradle test`
* Build final fat JAR - `gradle clean build shadowJar`
* Build final fat JAR without tests - `gradle -x test clean build shadowJar`
