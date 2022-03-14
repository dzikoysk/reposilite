---
id: deploy
title: Deploy
sidebar_label: Deploy
---

The deploy phase adds your artifact to a remote repository automatically.
Before we get to that, you have to make sure that the deploy feature is enabled in Repository:

```properties
# Accept deployment connections
deployEnabled: true
```

Now, you should determine in your `pom.xml` the target repository where Maven should upload artifact.
Let's say we want to deploy artifact to the `releases` repository:

```xml
<distributionManagement>
    <repository>
        <id>local-repository</id>
        <url>http://localhost:80/releases</url>
    </repository>
</distributionManagement>
```

To use generated token add, a new server in your [~/m2/settings.xml](https://maven.apache.org/settings.html):

```xml
<server>
  <!-- Id has to match the id provided in pom.xml -->
  <id>local-repository</id>
  <username>{alias}</username>
  <password>{token}</password>
</server>
```

If you've configured everything correctly, you should be able to deploy artifact using the following command:

```bash
$ mvn deploy
```

### Gradle

Some users experienced issues related to the invalid auth method chosen by Gradle.
To avoid such a problem, it is recommended to provide `BasicAuthentication` in repository properties.

```groovy
maven {
    url "http://domain.com/releases"
    credentials {
        username = alias
        password = token
    }
    authentication {
        basic(BasicAuthentication)
    }
}
```