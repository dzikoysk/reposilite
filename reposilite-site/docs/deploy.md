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
        <id>my-cool-repository</id>
        <url>http://localhost:80/releases</url>
    </repository>
</distributionManagement>
```

To use generated token add, a new server in your [~/.m2/settings.xml](https://maven.apache.org/settings.html):

```xml
<servers>
    <server>
        <!-- Id has to match the id provided in pom.xml -->
        <id>my-cool-repository</id>
        <username>{alias}</username>
        <password>{token}</password>
    </server>
</servers>
```

If you've configured everything correctly, you should be able to deploy artifact using the following command:

```bash
$ mvn deploy
```

If you wish to deploy to several repositories, such as a snapshots and a releases repository, it is done like this:

`~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>my-cool-repository-releases</id>
        <username>{alias}</username>
        <password>{token}</password>
    </server>
    <server>
        <id>my-cool-repository-snapshots</id>
        <username>{alias}</username>
        <password>{token}</password>
    </server>
</servers>
```

And the following is added to your `pom.xml`:

```xml
<distributionManagement>
    <repository>
        <id>my-cool-repository-releases</id>
        <url>http://localhost:80/releases</url>
    </repository>
    <repository>
        <id>my-cool-repository-snapshots</id>
        <url>http://localhost:80/snapshots</url>
    </repository>
</distributionManagement>
```



### Gradle

Deploying with gradle is similar.
Note: please read the [gradle publishing docs](https://docs.gradle.org/current/userguide/publishing_maven.html).

If you are only deploying to a single repository, here is how to do it:

First, add your alias and token to your`~/.gradle/gradle.properties` (Warning: this should in your `GRADLE_USER_HOME`, *not* your project `gradle.properties`):

```properties
myCoolRepositoryUsername={alias}
myCoolRepositoryPassword={token}
```

Or, via command line properties:

```bash
./gradlew publish -PmyCoolRepositoryUsername={alias} -PmyCoolRepositoryPassword={token}
```



Next, add the `publishing` and `signing` gradle plugins, as well as the maven repository:

#### Kotlin

`build.gradle.kts`:

```kotlin
plugins {
    signing
    `maven-publish`
}

// ...

publishing {
    repositories {
        maven {
            name = "myCoolRepository"
            url = uri("https://my-cool-repository.com/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
```

#### Groovy

`build.gradle`:

```groovy
plugins {
    id 'signing'
    id 'maven-publish'
}

// ...

publishing {
    repositories {
        maven {
            name = "myCoolRepository"
            url = "https://my-cool-repository.com/releases"
            credentials(PasswordCredentials)
            authentication {
                basic(BasicAuthentication)
            }
        }
    }
}
```



To publish to several repositories, you must have two usernames/passwords (which may or may not be the same):

```properties
# Releases token & alias
myCoolRepositoryReleasesUsername={alias}
myCoolRepositoryReleasesPassword={token}

# Snapshots token & alias
myCoolRepositorySnapshotsUsername={alias}
myCoolRepositorySnapshotsPassword={token}
```

#### Kotlin

`build.gradle.kts`:

```kotlin
maven {
    name = "myCoolRepositoryReleases"
    url = uri("https://my-cool-repository.com/releases")
    credentials(PasswordCredentials::class)
    authentication {
        create<BasicAuthentication>("basic")
    }
}


maven {
    name = "myCoolRepositorySnapshots"
    url = uri("https://my-cool-repository.com/snapshots")
    credentials(PasswordCredentials::class)
    authentication {
        create<BasicAuthentication>("basic")
    }
}
```

#### Groovy

`build.gradle`:

```groovy
maven {
    name = "myCoolRepositoryReleases"
    url = "https://my-cool-repository.com/releases"
    credentials(PasswordCredentials)
    authentication {
        basic(BasicAuthentication)
    }
}


maven {
    name = "myCoolRepositorySnapshots"
    url = uri("https://my-cool-repository.com/snapshots")
    credentials(PasswordCredentials)
    authentication {
        basic(BasicAuthentication)
    }
}

```