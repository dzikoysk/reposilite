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
To launch Reposilite with defined amount of RAM, use `-Xmx` parameter, for instance:

```console
$ java -Xmx32M -jar reposilite.jar
```

If you will not define the memory size, Reposilite will probably use around *~40MB to ~250MB*.

### Structure
Reposilite stores data in current [working directory](#working-directory),
by default it is a place where you've launched it.

```shell-session
user@host ~/workspace: java -jar reposilite.jar
```

```bash
~workspace/
+--repositories/        The root directory for all declared repositories
   +--/releases         Default repository for releases
   +--/snapshots        Default repository for snapshot releases
+--log.txt              Mirror of log from CLI
+--reposilite.jar       Application file
+--reposilite.yml       Configuration file
+--stats.yml            Data file containing stats records
+--tokens.yml           Data file containing stored tokens
```

To separate data files and configuration from application, use [parameters](#parameters).

### Preview
If Reposilite has been launched properly,
you should be able to see its frontend located under the default http://localhost:80 address:

![Preview](/img/about-preview.png)

### Interactive CLI
Reposilite exposes interactive console in terminal.
Type `help` and learn more.

## Parameters
Some of the properties have to be set through the command-line parameters.

### Working directory
To declare custom working directory for Reposilite instance,
you should use `--working-directory` *(alias: `-wd`)* parameter:

```console
$ java -jar reposilite.jar --working-directory=/app/data
```

### Configuration file
You may also declare custom location of configuration file using the `--config` *(alias: `-cfg`)* parameter:

```console
$ java -jar reposilite.jar --config=/etc/reposilite/reposilite.yml
```

You don't have to create this file manually,
Reposilite will generate it during the first startup, 
but make sure that you've granted `write` permission.

### Properties
Using the system properties, 
you can also override values from the loaded configuration.
See [configuration#system-properties](./configuration#system-properties) to learn more.

### Log file
Reposilite uses [tinylog](https://tinylog.org) as logging library. 
To change location of log file, use [system properties](https://tinylog.org/v2/configuration/#configuration):

```console
$ java -Dtinylog.writerFile.file=/etc/reposilite/log.txt -jar reposilite.jar
```