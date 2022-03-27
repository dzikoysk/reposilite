---
id: configuration
title: Configuration
sidebar_label: Configuration
---

There are two ways to pass custom properties to your Reposilite instance:

- Through the `reposilite.cdn` configuration file
- Using the [system properties](#system-properties) _(overrides values from configuration)_

Detailed description of properties is located in the following chapters:

- [Repositories](./repositories)
- [Authorization](./authorization)
- [Proxy](./proxy)
- [Customization](./customization)

## Default configuration

Customized version of configuration file can be found in test workspace: [reposilite.cdn](https://github.com/dzikoysk/reposilite/blob/master/reposilite-backend/src/test/workspace/reposilite.cdn)

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

```bash
$ java -Dtinylog.writerFile.file=/etc/reposilite/log.txt -jar reposilite.jar
```
