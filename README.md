# NanoMaven [![Build Status](https://travis-ci.org/dzikoysk/NanoMaven.svg?branch=master)](https://travis-ci.org/dzikoysk/NanoMaven)
Lightweight repository manager for Maven artifacts

#### Download
Releases: [GitHub Downloads](https://github.com/dzikoysk/NanoMaven/releases)

#### Features
* [x] Working repository manager
* [x] Multiple repositories under a single URL
* [x] Deploy
* [x] Authorization
* [ ] Proxy for the specified remote repositories
* [ ] Front page
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
NanoMaven 1.0.4 Commands:   
   help - List available commands
   users - List all registered users
   projects - List all added projects
   add-user <username> <password> - Add user
   add-project <repository>.<groupId>/<artifactId> - Add project extra data
   add-member <repository>.<groupId>/<artifactId> <username> - Add user to the specified project
   reinstall-artifacts (rs) - Reinstall all artifacts
```

#### Maven builds
You can also use maven to embed NanoMaven in your application

```xml
<dependency>
    <groupId>org.panda-lang</groupId>
    <artifactId>nanomaven</artifactId>
    <version>1.0.4</version>
</dependency>

<repository>
    <name>Panda Repository</name>
    <id>panda-repository</id>
    <url>https://repo.panda-lang.org/</url>
</repository>
```
