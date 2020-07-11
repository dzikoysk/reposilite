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

## Parameters
Some of the properties have to be set through the command-line parameters.

### Working directory
To declare custom working directory for Reposilite instance,
you should use `--working-directory` *(alias: `-wd`)* parameter:

```bash
$ java -jar reposilite.jar --working-directory=/app/data
```

### Configuration file
You may also declare custom location of configuration file using the `--config` *(alias: `-cfg`)* parameter:

```bash
$ java reposilite.jar --config=/etc/reposilite/reposilite.yml
```

You don't have to create this file manually,
Reposilite will generate it during the first startup, 
but make sure that you've granted `write` permission.

### Properties
Using the system properties, 
you can also override values from the loaded configuration.
See [configuration#system-properties](./configuration#system-properties) to learn more.