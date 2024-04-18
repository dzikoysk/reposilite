---
id: maven
title: Maven
---

### Maven

You should determine in your `pom.xml` the target repository where Maven should upload artifact.
Let's say we want to deploy artifact to the `releases` repository:

```xml
<distributionManagement>
    <repository>
        <id>my-domain-repository</id>
        <url>http://repo.my-domain.com/releases</url>
    </repository>
</distributionManagement>
```

To use generated token, add a new server in your [~/.m2/settings.xml](https://maven.apache.org/settings.html):

```xml
<settings>
  <servers>
    <server>
      <!-- Id has to match the id provided in pom.xml -->
      <id>my-domain-repository</id>
      <username>{secret}</username>
      <password>{token}</password>
    </server>
  </servers>
</settings>
```

If you've configured everything correctly, you should be able to deploy artifact using the following command:

```bash
$ mvn deploy
```
