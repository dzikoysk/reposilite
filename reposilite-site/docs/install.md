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
* RAM 12MB+

| Amount | Description |
|:------:|-------------|
| *12MB* | Tiny repository for personal projects |
| *16MB* - *32MB* | *--------------------^------------------* + CI + Proxy |
| *48MB - 128MB* | Tiny public repository *(recommended)* |
| *128MB+* | Public repository | 


## Running
To launch Reposilite with defined amount of RAM, use `-Xmx` parameter, for instance:

```console
$ java -Xmx32M -jar reposilite.jar
```

If you will not define the memory size, Reposilite will probably use around *~40MB to ~250MB*.

### Interactive CLI
Reposilite exposes interactive console directly in a terminal and it awaits for an input.
Type `help` and learn more about available commands.

![Interactive CLI](https://user-images.githubusercontent.com/4235722/93831263-aac91e80-fc72-11ea-8316-50b1e37cf16f.gif)

**Note**: Your first access token has to be generated through the terminal. 
Read more about tokens and keygen command here: [Authorization](authorization#generate-token).

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
+--reposilite.cdn       Configuration file
+--stats.yml            Data file containing stats records
+--tokens.yml           Data file containing stored tokens
```

To separate data files and configuration from application, use [parameters](configuration#parameters).

### Interface
If Reposilite has been launched properly,
you should be able to see its frontend located under the default http://localhost:80 address:

![Preview](/img/about-preview.png)

To access dashboard visit http://localhost:80/dashboard or click on the feather icon.

### Further reading

* [Generate token to access dashboard](authorization#generate-token)
* [Deploy artifact](deploy)

External publications:

* [DEV.to article - Install & deploy your first artifact](https://dev.to/dzikoysk/publishing-your-artifacts-to-the-reposilite-a-new-self-hosted-repository-manager-3n0h)