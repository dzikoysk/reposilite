---
id: deployment
title: Deployment
---

The deploy phase adds your artifact to a remote repository automatically.

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
<servers>
  <server>
    <!-- Id has to match the id provided in pom.xml -->
    <id>my-domain-repository</id>
    <username>{token}</username>
    <password>{secret}</password>
  </server>
</servers>
```

If you've configured everything correctly, you should be able to deploy artifact using the following command:

```bash
$ mvn deploy
```

### Gradle

Deploying with Gradle is quite similar, read the official [gradle publishing docs](https://docs.gradle.org/current/userguide/publishing_maven.html) to learn more.

If you are only deploying to a single repository, here is how to do it.
Firstly, add your access token to your `~/.gradle/gradle.properties`.

<CodeVariants>
  <CodeVariant name="~/.gradle/gradle.properties">

```properties
myDomainRepositoryUsername={token}
myDomainRepositoryPassword={secret}
```

`Warning` This should be in your _GRADLE_USER_HOME_, *not* your project _gradle.properties_ file:

  </CodeVariant>
  <CodeVariant name="Via command line properties">
  
```bash
$ ./gradlew publish \
  -PmyDomainRepositoryUsername={token} \
  -PmyDomainRepositoryPassword={secret}
```

  </CodeVariant>
</CodeVariants>

Next, add the `publishing` and `signing` gradle plugins, as well as the maven repository:

<CodeVariants>
  <CodeVariant name="Gradle (Kts)">

```kotlin
plugins {
    signing
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "myDomainRepository"
            url = uri("https://repo.my-domain.com/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
```

  </CodeVariant>
  <CodeVariant name="Gradle (Groovy)">

```groovy
plugins {
  id 'signing'
  id 'maven-publish'
}
publishing {
  repositories {
      maven {
          name = "myDomainRepository"
          url = "https://repo.my-domain.com/releases"
          credentials(PasswordCredentials)
            authentication {
              basic(BasicAuthentication)
          }
      }
  }
}
```

  </CodeVariant>
</CodeVariants>

To publish to several repositories, you have to declare it for all repositories _(which may or may not be the same)_:

```properties
# Releases token & secret
myDomainRepositoryReleasesUsername={token}
myDomainRepositoryReleasesPassword={secret}

# Snapshots token & secret
myDomainRepositorySnapshotsUsername={token}
myDomainRepositorySnapshotsPassword={secret}
```

And declare multiple target in build file:

<CodeVariants>
  <CodeVariant name="Gradle (Kts)">

```kotlin
maven {
    name = "myDomainRepositoryReleases"
    url = uri("https://repo.my-domain.com/releases")
    credentials(PasswordCredentials::class)
    authentication {
        create<BasicAuthentication>("basic")
    }
}
maven {
    name = "myCoolRepositorySnapshots"
    url = uri("https://repo.my-domain.com/snapshots")
    credentials(PasswordCredentials::class)
    authentication {
        create<BasicAuthentication>("basic")
    }
}
```

  </CodeVariant>
  <CodeVariant name="Gradle (Groovy)">

```groovy
maven {
    name = "myDomainRepositoryReleases"
    url = "https://repo.my-domain.com/releases"
    credentials(PasswordCredentials)
    authentication {
        basic(BasicAuthentication)
    }
}
maven {
    name = "myCoolRepositorySnapshots"
    url = uri("https://repo.my-domain.com/snapshots")
    credentials(PasswordCredentials)
    authentication {
        basic(BasicAuthentication)
    }
}
```

  </CodeVariant>
</CodeVariants>
