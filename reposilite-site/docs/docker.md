---
id: docker
title: Docker
sidebar_label: Docker
---

Reposilite defines two types of builds:
* tag-based - build is triggered per a new release, recommended for production environments
* nightly - build is triggered for each commit on master branch, it might be useful for testers and developers

## Installation

First of all, you have to pull the image from [DockerHub](https://hub.docker.com/r/dzikoysk/reposilite):

```shell-session
// released builds, e.g. 2.9.22-SNAPSHOT
$ docker pull dzikoysk/reposilite:2.9.22-SNAPSHOT

// nightly builds
$ docker pull dzikoysk/reposilite:nightly
```

Then, 
just run the image in interactive mode 
*(to support [interactive CLI](install#interactive-cli))*:

```console
$ docker run -it -v reposilite-data:/app/data -p 80:80 dzikoysk/reposilite:nightly
```

### Data persistence
Reposilite stores data in `/app/data` directory. 
Since 2.7.0, 
Reposilite marks this path as externally mounted volume by default.
To use named volume which can be reused by Docker,
run docker with `-v` parameter:

```console
$ docker run -it -v reposilite-data:/app/data -p 80:80 dzikoysk/reposilite
```

### Custom properties

You can also pass custom configuration values using the environment variables:

```shell-session
$ docker run -e JAVA_OPTS='-Xmx128M -Dreposilite.port=8080' -p 8080:80 dzikoysk/reposilite
```

You can find list of configuration properties in [configuration](configuration) chapter.

### Custom parameters
To pass custom parameters described in [installation#properties](install#properties), use `REPOSILITE_OPTS` variable:

```shell-session
$ docker run -e REPOSILITE_OPTS='--config=/app/data/custom.cdn' -p 80:80 dzikoysk/reposilite
```

**Note:** Default Dockerfile changes Reposilite working directory to `/app/data/` and marks it as volume.
It is not recommended to modify this property.

### External configuration
You can mount external configuration file using the `--mount` and `--config` parameter.
Before that, you have to make sure the the configuration file already exists on Docker host. 
To launch Reposilite with a custom configuration, we have to mount proper file:

```console
$ docker run -it \
  --mount type=bind,source=/etc/reposilite/reposilite.cdn,target=/app/reposilite-host.cdn \
  -e REPOSILITE_OPTS='--config=/app/reposilite-host.cdn' \
  -v reposilite-data:/app/data
  -p 80:80
  dzikoysk/reposilite
```

As you can see, it is not best solution and as long as possible, 
you should override default configuration values using the [custom properties](#custom-properties).
