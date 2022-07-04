---
id: sources
title: Sources
---

Actively developed projects & modules by [@dzikoysk](https://github.com/dzikoysk) that powers Reposilite 3.x:

* [dzikoysk / Reposilite](https://github.com/dzikoysk/reposilite) - Main project repository
  * [Backend](https://github.com/dzikoysk/reposilite/tree/main/reposilite-backend) - Main sources of Reposilite
  * [Frontend](https://github.com/dzikoysk/reposilite/tree/main/reposilite-frontend) - Dashboard implementation in [Vue 3](https://vuejs.org/)
  * [Plugins](https://github.com/dzikoysk/reposilite/tree/main/reposilite-plugins) - Official extensions to Reposilite
  * [Website](https://github.com/dzikoysk/reposilite/tree/main/reposilite-site) - [reposilite.com](https://reposilite.com/) site sources in [Next.js](https://nextjs.org/) ([React](https://reactjs.org/))
* [Reposilite Playground / *](https://github.com/reposilite-playground/) - Dedicated organization for Reposilite with several internal utility libraries
  * [Javalin RFCs](https://github.com/reposilite-playground/javalin-rfcs) - Set of extension methods, alternative routing & coroutines plugin for Javalin
  * [Javalin OpenApi](https://github.com/reposilite-playground/javalin-openapi) - Reimplemented OpenApi plugin for Javalin based on top of annotation processing with a support for [Swagger](https://swagger.io/) & [ReDoc](https://github.com/Redocly/redoc)
  * [Journalist](https://github.com/reposilite-playground/journalist) - Tiny logging abstraction that provides non-static loggers, with support for SL4J
  * [Exposed Upsert](https://github.com/reposilite-playground/exposed-upsert) - Implements missing upsert functionality for [Exposed](https://github.com/JetBrains/Exposed) framework
* [dzikoysk / CDN](https://github.com/dzikoysk/cdn) - Configuration library used by Reposilite to handle `.cdn` format
* [panda-lang / Expressible](https://github.com/panda-lang/expressible) - Dependency free utility library for Java & Kotlin, dedicated for functional codebases that require enhanced response handling. 
* [javalin / Javalin](https://github.com/javalin/javalin) - Simple web framework behind Reposilite, received a few patches addressed by Reposilite

### State of sources

Reposilite 1.x and 2.x were written in Java, but in 3.x the time has come for Kotlin.
The decision to move to Kotlin was caused by a several factors:

1. Sources consistency - Main sources, unit & integration tests and build script files are fully written in Kotlin.
   We're glad we could finally get rid of Groovy.
2. Extensibility - Reposilite is built on top of [Javalin](https://javalin.io/), web framework already written in Kotlin. 
   It's relatively small library, so we often need to extend it with a bit of custom methods.
   To avoid mess created by static utility methods, we're extending base objects with extension functions.
3. State of Java - Quite primitive generic types implementation, mutable collections, rotting std, 
   nullable black holes and lack of significant changes since JDK8
   made the overall experience worse for standard web application speaking of representation of business logic.  
4. Nullability - A possibility to replace _Optionals_ with nullable types and overall better support on language level reduces complexity.
5. Functional programming - Suited to FP-like codebase on syntax & std level.
6. Stable - Finally, Gradle 7.x+ and Kotlin 1.4+ are pretty damn stable for our needs.  
7. Relatively easy - There is no big difference between Kotlin and Java,
   every open-minded Java developer should be able to write in Kotlin within an hour.

If you're Java developer that never had contact with Kotlin, check our [Kotlin guide](/guide/kotlin) in a context of Reposilite sources!
