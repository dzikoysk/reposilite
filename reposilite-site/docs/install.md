---
id: install
title: Installation
sidebar_label: Installation
---

You can download standalone version of Reposilite from GitHub releases page:

* [GitHub Downloads](https://github.com/dzikoysk/reposilite/releases)

You may also use Docker image available on Docker Hub:

* [DockerHub Repository](https://hub.docker.com/r/dzikoysk/reposilite)

## Requirements
* Java 8+
* RAM 8MB+

| Amount | Description |
|:------:|-------------|
| *8MB* | Tiny repository for personal projects |
| *16MB* - *32MB* | *--------------------^------------------* + CI + Proxy |
| *48MB - 128MB* | Tiny public repository *(recommended)* |
| *128MB+* | Public repository | 


## Running
To launch Reposilite with defined amount of RAM, use `Xmx` parameter, for instance:
```bash
$ java -Xmx32M -jar reposilite.jar
```
If you will not define the memory size, Reposilite will probably use around *~40MB to ~250MB*.

### Preview
If Reposilite has been launched properly,
you should be able to see its frontend located under the default http://localhost:80 address:

![Preview](/img/about-preview.png)

## Docker

You may also use Reposilite through the docker image:

```bash
$ docker pull dzikoysk/reposilite
```

You can also pass custom configuration values using the environment variables:

```bash
$ docker run -e JAVA_OPTS='-Xmx128M -Dreposilite.port="8080"' reposilite
```

You'll find more about configuration options in the next chapter.