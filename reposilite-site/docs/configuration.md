---
id: configuration
title: Configuration
sidebar_label: Configuration
---

There are two ways to pass custom properties to your Reposilite instance:

* Through the `reposilite.yml` configuration file
* Using the [system properties](#system-properties) *(overrides values from configuration)*

Detailed description of properties is located in the following chapters:
* [Repositories](./repositories)
* [Authorization](./authorization)
* [Proxy](./proxy)
* [Customization](./customization)

## Default configuration
The standard configuration of Reposilite should be generated
by Reposilite during your first start and it should look like this:

```yml
# ~~~~~~~~~~~~~~~~~~~~~~ #
#       Reposilite       #
# ~~~~~~~~~~~~~~~~~~~~~~ #

# Hostname
hostname: ""
# Port
port: 80
# Debug
debugEnabled: false

# List of supported Maven repositories.
# First directory on the list is the main repository.
repositories:
  - "releases"
  - "snapshots"

# List of proxied repositories.
# Reposilite will search for an artifact in remote repositories listed below, if the requested artifact was not found.
# Note: URL cannot contains / at the end
proxied: []
# - https://repo.panda-lang.org

# Accept deployment connections
deployEnabled: true
# Allow to omit name of the main repository in request
# e.g. /org/panda-lang/reposilite will be redirected to /releases/org/panda-lang/reposilite
rewritePathsEnabled: true
# Require authentication of all requests (download, head requests)
# This option should be set to 'false', if you are hosting public repository
fullAuthEnabled: false
# If you don't want to display content of your repositories,
# you can just disable indexing
indexingEnabled: true
# List of management tokens used by dashboard to access extra options.
# (By default, people are allowed to use standard dashboard options related to the associated path)
managers: []
# - root

# Title displayed by frontend
title: "#onlypanda"
# Description displayed by frontend
description: "Public Maven repository hosted through the Reposilite"
# Accent color used by frontend
accentColor: "#2fd4aa"
```

## System properties
Passing properties through the system properties is especially useful, 
when we want to use Docker image. The parameter must be provided in the following structure:

```properties
-Dreposilite.propertyName="propertyValue"
-Dreposilite.propertyName="arrayValue1,arrayValue2"
```

For instance:

```bash
$ java -Xmx32M -Dreposilite.port="8080" -jar reposilite.jar
```