---
id: sbt
title: SBT
---

### SBT
SBT is a default build tool for [Scala](https://www.scala-lang.org/) and there are some noticeable changes in package naming convention, but at the end it works on top of the Maven protocol.
All you need is to link your repository and provide valid credentials to [token](/guide/tokens) that has access to [routes](/guide/routes) used by your project.

```groovy
// Standard
publishTo := Some("Reposilite" at "https://maven.reposilite.com/releases")
credentials += Credentials("Reposilite", "maven.reposilite.com", "token", "secret")

// For localhost
publishTo := Some("Reposilite" at "http://localhost:8080/releases")
credentials += Credentials("Reposilite", "localhost", "token", "secret")
```

For more advanced configurations, take a look at official publishing guide for SBT:

* [Scala SBT / Publishing](https://www.scala-sbt.org/1.x/docs/Publishing.html)