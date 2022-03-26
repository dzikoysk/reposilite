---
id: manual
title: Manual
---

You can download standalone version of Reposilite from GitHub releases page:

- [GitHub / Reposilite :: Releases](https://github.com/dzikoysk/reposilite/releases)

Requirements: `System: Windows, Linux AMD/ARM`, `JVM: 8-18`, `Memory: 16MB+`

Recommended memory ranges:

|     Amount      | Description                                            |
| :-------------: | ------------------------------------------------------ |
|     _16MB_      | Tiny repository for personal projects                  |
|     _32MB_      | _--------------------^----------------_ + CI + Proxy |
| _64MB - 128MB_  | Tiny public repository _(recommended)_                 |
|    _128MB+_     | Public repository                                      |

### Running

To launch Reposilite with defined amount of RAM, use `-Xmx` parameter, for instance:

```bash
$ java -Xmx32M -jar reposilite.jar
```

### Interactive CLI

Reposilite exposes interactive console directly in a terminal and it awaits for an input.
Type `help` and learn more about available commands.

![Interactive CLI](/images/guides/interactive-cli.gif)

`Note` Your first access token has to be generated through the terminal or provided as a command line argument.  
Read more about tokens and token management commands in [Guide / Tokens](/guide/tokens).

### Web interface

If Reposilite has been launched properly,
you should be able to see its frontend located under the default http://localhost:80/#/ address.

![Web Interface Preview](/images/guides/web-interface-preview.png)

### Data structure

Reposilite stores data in working directory,
by default it is a place where you've launched it.

```shell-session
user@host ~/workspace: java -jar reposilite.jar
```

```bash
~workspace/
+--logs/              List of 10 latest log files
+--plugins/           Directory with all external plugins to load
+--repositories/      The root directory for all declared repositories
   +--private/        Default private repository
   +--releases/       Default repository for releases
   +--snapshots/      Default repository for snapshot releases
+--static/            Static website content
+--configuration.cdn  Configuration file
+--latest.log         Log from the latest launch of Reposilite instance
+--reposilite.jar     Application file
+--reposilite.db      Data file containing stats and tokens (only if embedded database enabled)
```

To separate data files and configuration from application, use [parameters](configuration#parameters).
