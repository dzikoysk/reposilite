---
id: docker
title: Docker
sidebar_label: Docker
---

## Docker

You may also use Reposilite through the docker image:

```bash
$ docker pull dzikoysk/reposilite
```

You can also pass custom configuration values using the environment variables:

```bash
$ docker run -e JAVA_OPTS='-Xmx128M ' dzikoysk/reposilite
```

You'll find more about configuration options in the next chapter.