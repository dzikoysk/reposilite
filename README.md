# NanoMaven [![Build Status](https://travis-ci.org/dzikoysk/NanoMaven.svg?branch=master)](https://travis-ci.org/dzikoysk/NanoMaven)
Lightweight repository manager for Maven artifacts. 
It is a simple solution to replace managers like Nexus, Archiva or Artifactory. 
As a successor of NanoMaven, you should also check the [Reposilite](https://github.com/panda-lang/reposilite) - enhanced repository management software mainly dedicated for Maven artifacts.

![Preview](https://user-images.githubusercontent.com/4235722/78812901-73b8c680-79cc-11ea-95d5-9763a53e4240.png)

#### Features
* [x] Working repository manager
* [x] Multiple repositories under a single URL
* [x] Deploy
* [x] Authorization
* [x] Console
* [ ] Proxy for the specified remote repositories
* [x] Front page
* [ ] Statistics
* [ ] Code quality

#### Download
Releases: [GitHub Downloads](https://github.com/dzikoysk/NanoMaven/releases)

#### Commands & Configuration
List of available management commands

```bash
NanoMaven 2.0.0 Commands:
  help - List available commands
  tokens - List all generated tokens
  keygen <path> <alias> - Generate a new access token for the given path
  rs - Reinstall all artifacts
  stop - Shutdown server
```

Configuration

```yaml
# ~~~~~~~~~~~~~~~~~~~~~~ #
#       Nano Maven       #
# ~~~~~~~~~~~~~~~~~~~~~~ #

# General Repository Name
repositoryName: NanoMaven Repository

# Hostname
hostname: ''
# Port
port: 80

# Include a repository names in the path
repositoryPathEnabled: false
# Enable directory indexing
indexingEnabled: true

# Root directories of repositories
repositories:
  - releases
  - snapshots

# Nested Maven
nestedMaven: true
# External Maven directory (if 'nested-maven': false)
externalMaven: /usr/local/share/java/maven33

# Accept deployment connections
deployEnabled: true
```

#### Guide
To deploy artifacts we have to generate `access token` assigned to the given path. Example usages:

```bash
keygen / admin
19:55:20.692 INFO | Generated new access token for admin (/)
19:55:20.692 INFO | AW7-kaXSSXTRVL_Ip9v7ruIiqe56gh96o1XdSrqZCyTX2vUsrZU3roVOfF-YYF-y
19:55:20.723 INFO | Stored tokens: 1

keygen /org/panda-lang/nanomaven nanomaven
19:56:09.109 INFO | Generated new access token for nanomaven (/org/panda-lang/nanomaven)
19:56:09.109 INFO | OFnV-2GiZeX0cHpeDvuLo0xjUpU5wNUcpkR4521fG68U9anfqNwKsVkFcQUCK4yk
19:56:09.114 INFO | Stored tokens: 2
```

To use generated token add a new server in your `./m2/settings.xml`  

```xml
<server>
  <id>{repository-id}</id>
  <username>{alias}</username>
  <password>{token}</password>
</server>
```
#### Maven builds
You can also use maven builds to embed NanoMaven in your application

```xml
<dependency>
    <groupId>org.panda-lang</groupId>
    <artifactId>nanomaven</artifactId>
    <version>2.0.0</version>
</dependency>

<repository>
    <name>Panda Repository</name>
    <id>panda-repository</id>
    <url>https://repo.panda-lang.org/</url>
</repository>
```
