# Reposilite [![Build Status](https://travis-ci.org/dzikoysk/reposilite.svg?branch=master)](https://travis-ci.org/dzikoysk/reposilite)
Reposilite *(formerly NanoMaven)* - lightweight repository manager for Maven artifacts. 
It is a simple solution to replace managers like Nexus, Archiva or Artifactory. 

![Preview](https://user-images.githubusercontent.com/4235722/82151266-f732c680-985a-11ea-842c-bb53acad794b.png)

#### Features
* [x] Working Maven repository manager *(example: [repo.panda-lang.org](https://repo.panda-lang.org))*
* [x] Docker image *(repository: [dzikoysk/reposilite](https://hub.docker.com/r/dzikoysk/reposilite))*
* [x] Authorization *(deploy and downloads)*
* [x] Deploy *(using tokens and BCrypt)*
* [x] Customizable front page
* [x] [dependabot](https://dependabot.com/) supported
* [x] Multiple repositories under a single URL
* [x] CLI
* [x] Snapshots
* [x] Proxy for the specified remote repositories
* [x] Multithreading
* [ ] Statistics
* [ ] Admin panel

#### Installation
Releases: [GitHub Downloads](https://github.com/dzikoysk/reposilite/releases) <br>
Images: [DockerHub Repository](https://hub.docker.com/r/dzikoysk/reposilite) <br>
Requirements: 
* Java 8+
* RAM 8MB+

| Amount | Description |
|:------:|-------------|
| *8MB* | Tiny repository for personal projects |
| *16MB* - *32MB* | *--------------------^------------------* + CI + Proxy |
| *48MB - 128MB* | Tiny public repository *(recommended)* |
| *128MB+* | Public repository | 

To launch Reposilite with defined amount of RAM, use `Xmx` parameter:
```bash
$ java -Xmx<Amount>M -jar reposilite.jar
```
If you will not define the memory size, Reposilite will probably use around *~30MB to ~250MB*.
You may also use Reposilite through the docker image:

```bash
$ docker pull dzikoysk/reposilite
```

#### Guide
List of available management commands

```bash
Reposilite 2.4.1 Commands:
  help - List available commands
  status - Display metrics
  tokens - List all generated tokens
  keygen <path> <alias> - Generate a new access token for the given path
  purge - Clear cache
  stop - Shutdown server
```

To deploy artifacts we have to generate `access token` assigned to the given path. Example usages:

```bash
keygen / admin
19:55:20.692 INFO | Generated new access token for admin (/)
19:55:20.692 INFO | AW7-kaXSSXTRVL_Ip9v7ruIiqe56gh96o1XdSrqZCyTX2vUsrZU3roVOfF-YYF-y
19:55:20.723 INFO | Stored tokens: 1

keygen /org/panda-lang/reposilite reposilite
19:56:09.109 INFO | Generated new access token for reposilite (/org/panda-lang/reposilite)
19:56:09.109 INFO | OFnV-2GiZeX0cHpeDvuLo0xjUpU5wNUcpkR4521fG68U9anfqNwKsVkFcQUCK4yk
19:56:09.114 INFO | Stored tokens: 2
```

To use generated token add a new server in your `~/m2/settings.xml`  

```xml
<server>
  <id>{repository-id}</id>
  <username>{alias}</username>
  <password>{token}</password>
</server>
```

#### FAQ
