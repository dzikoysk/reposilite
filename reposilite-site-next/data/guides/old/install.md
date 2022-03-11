---
id: install
title: Installation
sidebar_label: Installation
---

You can download standalone version of Reposilite from GitHub releases page:

- [GitHub Downloads](https://github.com/dzikoysk/reposilite/releases)

You may also use Docker image available on Docker Hub:

- [DockerHub Repository](https://hub.docker.com/r/dzikoysk/reposilite)

## Requirements

- Java 8+
- RAM 12MB+

|     Amount      | Description                                            |
| :-------------: | ------------------------------------------------------ |
|     _12MB_      | Tiny repository for personal projects                  |
| _16MB_ - _32MB_ | _--------------------^------------------_ + CI + Proxy |
| _48MB - 128MB_  | Tiny public repository _(recommended)_                 |
|    _128MB+_     | Public repository                                      |

## Running

To launch Reposilite with defined amount of RAM, use `-Xmx` parameter, for instance:

```bash
$ java -Xmx32M -jar reposilite.jar
```

If you will not define the memory size, Reposilite will probably use around _~40MB to ~250MB_.

### Interactive CLI

Reposilite exposes interactive console directly in a terminal and it awaits for an input.
Type `help` and learn more about available commands.

![Interactive CLI](https://user-images.githubusercontent.com/4235722/93831263-aac91e80-fc72-11ea-8316-50b1e37cf16f.gif)

**Note**: Your first access token has to be generated through the terminal or provided as a command line argument.  
Read more about tokens and token management commands here: [Authorization](authorization#generate-token).

### Structure

Reposilite stores data in current [working directory](#working-directory),
by default it is a place where you've launched it.

```shell-session
user@host ~/workspace: java -jar reposilite.jar
```

```bash
~workspace/
+--repositories/        The root directory for all declared repositories
   +--private/          Default private repository
   +--releases/         Default repository for releases
   +--snapshots/        Default repository for snapshot releases
+--static/              Static website content
+--log.txt              Mirror of log from CLI
+--reposilite.jar       Application file
+--reposilite.cdn       Configuration file
+--reposilite.db        Data file containing stats and tokens (only if sqlite is used as database)
```

To separate data files and configuration from application, use [parameters](configuration#parameters).

### Interface

If Reposilite has been launched properly,
you should be able to see its frontend located under the default http://localhost:80/#/ address:

![Preview](/img/about-preview.png)

To access the console, sign in using the button in the upper right corner.

### Further reading

- [Generate token to access dashboard](authorization#generate-token)
- [Deploy artifact](deploy)

External publications:

- [DEV.to article - Install & deploy your first artifact](https://dev.to/dzikoysk/publishing-your-artifacts-to-the-reposilite-a-new-self-hosted-repository-manager-3n0h)
