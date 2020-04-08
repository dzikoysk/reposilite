# NanoMaven [![Build Status](https://travis-ci.org/dzikoysk/NanoMaven.svg?branch=master)](https://travis-ci.org/dzikoysk/NanoMaven)
Lightweight repository manager for Maven artifacts. 
It is a simple solution to replace managers like Nexus, Archiva or Artifactory. 
As a successor of NanoMaven, you should also check the [Reposilite](https://github.com/panda-lang/reposilite) - enhanced repository management software mainly dedicated for Maven artifacts.

![obraz](https://user-images.githubusercontent.com/4235722/78812901-73b8c680-79cc-11ea-95d5-9763a53e4240.png)

#### Download
Releases: [GitHub Downloads](https://github.com/dzikoysk/NanoMaven/releases)

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

#### Configuration
```yaml
# ~~~~~~~~~~~~~~~~~~~~~~ #
#       Nano Maven       #
# ~~~~~~~~~~~~~~~~~~~~~~ #

# General Repository Name
repository-name: NanoMaven Repository

# Hostname
hostname: ''
# Port
port: 80

# Include a repository names in the path
repository-path-enabled: false
# Enable directory indexing
indexing-enabled: true

# Nested Maven
nested-maven: true
# External Maven directory (if 'nested-maven': false)
external-maven: /usr/local/share/java/maven33

# Accept deployment connections
deploy-enabled: true
# Require authorization
authorization-enabled: true
# Administrator accounts
administrators:
- dzikoysk
```

#### Commands
Commands can be invoked from the console
```bash
NanoMaven 2.0.0 Commands:
  help - List available commands
  tokens - List all generated tokens
  keygen <path> <alias> - Generate a new access token for the given path
  rs - Reinstall all artifacts
  stop - Shutdown server
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
