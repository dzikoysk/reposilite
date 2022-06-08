---
id: docker
title: Docker
---

You may use Docker image available on Docker Hub:

- [DockerHub Registry / Reposilite](https://hub.docker.com/r/dzikoysk/reposilite)

Reposilite defines two types of builds:

- `tag-based` - build is triggered per a new release, recommended for production environments
- `nightly` - build is triggered for each commit on main branch, it might be useful for testers and developers

First of all, you have to pull the image:

```yaml
# released builds, e.g. 3.0.0-alpha.26
$ docker pull dzikoysk/reposilite:3.0.0-alpha.26

# nightly builds
$ docker pull dzikoysk/reposilite:nightly
```

Then,
just run the image in interactive mode
_(to enable [interactive CLI](/guide/manual#interactive-cli))_:

```bash
$ docker run -it -v reposilite-data:/app/data -p 80:80 dzikoysk/reposilite:nightly
```

### Startup configuration

#### Data persistence

Reposilite stores data in `/app/data` directory by default. 
To make it persistent, use named volume with `-v` parameter:

```bash
$ docker run -it -v reposilite-data:/app/data -p 80:80 dzikoysk/reposilite
```

#### JVM properties

You can also pass custom configuration values using the environment variables:

```shell-session
$ docker run -e JAVA_OPTS='-Xmx128M' -p 8080:80 dzikoysk/reposilite
```

#### Reposilite properties

To pass custom parameters described in [setup#parameters](setup#parameters), use `REPOSILITE_OPTS` variable:

```shell-session
$ docker run -e REPOSILITE_OPTS='--local-configuration=/app/data/custom.cdn' -p 80:80 dzikoysk/reposilite
```

#### External configuration

You can mount external configuration files using the `--mount` parameter.
Before that, you have to make sure the the configuration file already exists on Docker host.
To launch Reposilite with a custom configuration, we have to mount proper file:

```bash
$ docker run -it \
  --mount type=bind,source=/etc/reposilite/configuration.cdn,target=/app/configuration.cdn \
  -e REPOSILITE_OPTS='--local-configuration=/app/configuration.cdn' \
  -v reposilite-data:/app/data
  -p 80:80
  dzikoysk/reposilite
```

### Using Docker Compose 

See the [default docker-compose.yml file](https://github.com/dzikoysk/reposilite/blob/main/docker-compose.yml) as a starting point for your configuration.

Because access tokens are created through the console command,
you should generate your first access token using `--token` startup parameter as follows:

1. Add `- REPOSILITE_OPTS=--token admin:secret` to the environment section in `docker-compose.yml` file. <br />
  **NOTE**: This is only needed for the first run and should be removed after creating a real security token.
2. Start the container with `docker-compose up -d`
3. Go to the address with your Reposilite instance to access the web dashboard.
4. Use your credentials specified in _REPOSILITE_OPTS_ to login _(e.g. admin / secret)_
5. Open the _'Console'_ tab. <br />
   **NOTE**: Your reverse proxy must also support and accept WebSocket connections.
6. Type `token-generate admin m` to create user named 'admin' with **m**anagement permission. 
   A strong password will be generated for you, so you should copy it. 
   If you really need to provide a custom one, use the `--secret=<your-password>` parameter. 
   Please note that this type of password creation is strongly discouraged. 
7. Tear down the container with `docker-compose down` and remove the _REPOSILITE_OPTS_ line with `--token admin:secret` parameter from your compose file.
8. Restart the container with `docker-compose up -d` and login with your credentials provided by the token-generate command. These credentials will be persisted.
