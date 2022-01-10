---
id: docker-compose
title: Docker Compose
sidebar_label: Docker Compose
---

## Installation

See the [docker-compose.yml example file](../docker-compose.yml) for a starting example.

## Notes

The security token (login) is created through a console command. To be able to create a security token with a Docker Compose setup, a little setup is needed:

- add `- REPOSILITE_OPTS=--token admin:secret` to the `docker-compose.yml` file. **NOTE**: this is only needed for the first run and should be removed after creating a real security token!
- start the container with `docker-compose up -d`
- access the URL for Reposilite. This depends on the hosts configuration (reverse proxy, etc.)
- use your credentials from `REPOSILITE_OPTS` to login (ie. admin / secret)
- open the "Console" tab. **NOTE**: your reverse proxy must also support and accept WebSockets!
- type `token-generate -s=yourSuperSecretPassword admin m` to create user named "admin" with the password "yourSuperSecretPassword" and **m**anagement permissions
- tear down the container with `docker-compose down` and remove the `REPOSILITE_OPTS` line or the `--token admin:secret` part from your `docker-compose.yml` file
- restart the container with `docker-compose up -d` and login with your credentials provided by the `token-generate` command. These credentials will be persisted.
