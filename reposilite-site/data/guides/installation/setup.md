---
id: setup
title: Setup
---

Reposilite supports multiple use cases and scenarios, 
that's why there is several ways to configure it divided into 3 configuration layers:

* [Parameters](#parameters) - Startup configuration
* [Configuration - Local configuration](#local-configuration) - Immutable file-based configuration of the given Reposilite instance
* [Settings - Shared configuration](#shared-configuration) - Stores mutable state of Reposilite in database, it's shared between all Reposilite instances and supports hot-reloading of properties

You don't have to create configuration files manually,
Reposilite will generate it during the first startup,
but make sure that process is able to write to the disk.

## Parameters

Parameters are configuration properties passed to Reposilite through program arguments:

```bash
$ java -jar reposilite.jar --parameter=value
```

List of available parameters:

| Parameter | Default value | Description |
| :--: | :--: | :--: |
| `--help` | - | Displays help message with all parameters |
| `--version` | - | Display current version of Reposilite |
| `--working-directory` <br/> `-wd` | `./` | Sets custom working directory for this instance, so the location where Reposilite keeps local data |
| `--generate-configuration` <br/> `-gc` | - | Generate default template of the requested file. Supported templates: `configuration.cdn` for local configuration and `configuration.shared.json` for shared configuration |
| `--local-configuration`<br/>`--local-config`<br/>`-lc` | `configuration.cdn` | Sets custom location of local configuration file in CDN format. By default it's relative to working directory path, but you can also use absolute path. |
| `--local-configuration-mode`<br/>`--local-config-mode`<br/>`-lcm` | `auto` | Configuration modes description: [Configuration modes](#configuration-modes) |
| `--shared-configuration`<br/>`--shared-config`<br/>`-sc` | `configuration.shared.cdn` | Replaces database oriented storage of shared configuration with manually linked JSON file with all defined properties |
| `--hostname`<br/>`-h`| Value from [local configuration](#local-configuration) | Overrides hostname from local configuration |
| `--port`<br/>`-p` | Value from [local configuration](#local-configuration) | Overrides port from local configuration |
| `--token`<br/>`-t` | Empty | Create temporary token with the given credentials in `name:secret` format |
| `--test-env`<br/>`--debug`<br/>`-d` | - | Enables debug mode |

#### Configuration modes

Configuration mode describes how the given configuration should be processed by Reposilite.
Because Reposilite still evolves, 
there are introduced new properties over and over.
To simplify migration & update process, 
Reposilite supports automatic config migrations through [CDN](https://github.com/dzikoysk/cdn) library.
Sometimes you want to keep your configuration file immutable,
and to give a possibility to control this process, 
configuration modes where introduced. Supported configuration modes:

| Mode | Description |
| :--: | :--: |
| `auto` | Processes and updates configuration file automatically if there are missing entries |
| `none` | Disables automatic updates of configuration file, user has update such files manually in case of new properties introduced in further updates |

## Local configuration

Local configuration describes configuration of current Reposilite instance.
It mostly refers to infrastructure setup, such as hostname, port or database connection.

Example local configuration file in CDN format from [test workspace](https://github.com/dzikoysk/reposilite/tree/main/reposilite-backend/src/test/workspace):

<Spoiler title="configuration.cdn">

```yaml
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
#       Reposilite :: Local       #
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #

# Local configuration contains init params for current Reposilite instance.
# For more options, shared between instances, login to the dashboard with management token and visit 'Configuration' tab.

# Hostname
# The hostname can be used to limit which connections are accepted.
# Use 0.0.0.0 to accept connections from anywhere.
# 127.0.0.1 will only allow connections from localhost.
hostname: 0.0.0.0
# Port to bind
port: 8080
# Database configuration. Supported storage providers:
# - mysql localhost:3306 database user password
# - sqlite reposilite.db
# - sqlite --temporary
# Experimental providers (not covered with tests):
# - postgresql localhost:5432 database user password
# - h2 reposilite
database: sqlite reposilite.db

# Support encrypted connections
sslEnabled: false
# SSL port to bind
sslPort: 443
# Key store file to use.
# You can specify absolute path to the given file or use ${WORKING_DIRECTORY} variable.
keyStorePath: ${WORKING_DIRECTORY}/keystore.jks
# Key store password to use
keyStorePassword: reposilite
# Redirect http traffic to https
enforceSsl: false

# Max amount of threads used by core thread pool (min: 5)
# The web thread pool handles first few steps of incoming http connections, as soon as possible all tasks are redirected to IO thread pool.
webThreadPool: 32
# IO thread pool handles all tasks that may benefit from non-blocking IO (min: 2)
# Because most of tasks are redirected to IO thread pool, it might be a good idea to keep it at least equal to web thread pool.
ioThreadPool: 16
# Database thread pool manages open connections to database (min: 1)
# Embedded databases such as SQLite or H2 don't support truly concurrent connections, so the value will be always 1 for them if selected.
databaseThreadPool: 2
# Select compression strategy used by this instance.
# Using 'none' reduces usage of CPU & memory, but ends up with higher transfer usage.
# GZIP is better option if you're not limiting resources that much to increase overall request times.
# Available strategies: none, gzip
compressionStrategy: none

# Keep processed frontend files in memory to improve response time
cacheContent: true
# Amount of messages stored in cached logger.
cachedLogSize: 100
# Set custom base path for Reposilite instance.
# It's not recommended to mount Reposilite under custom base path
# and you should always prioritize subdomain over this option.
basePath: /
# Debug mode
debugEnabled: false
```

</Spoiler>

## Shared configuration

Settings (shared configuration) describes content of all your Reposilite instances, such as repositories or frontend customization. 
This configuration supports hot-reloading of properties and should be shared between all your Reposilite instances
to make sure that every instance behaves and offers the same content.

`Note` By default, 
Reposilite uses database to store shared configuration and synchronizes its state every 10 seconds.
Configuration synchronization is based on top of an interval, 
due to the lack of 3rd party service that could broadcast data between instances.

Shared configuration can be modified by access token with management permission through web interface in _Settings_ tab:

![Dashboard / Configuration](/images/guides/web-interface-configuration.png)

#### Shared configuration as file

Most users should use default configuration with shared configuration in database that offers support for hot-reloading and automatic updates, 
but for some of them it's not really what they want/can use. Some use cases where database based shared configuration may not be the best solution:

* Boot temporary Reposilite instance without preserved database file
* Specific environments with external configuration supervisors that don't want to share state between shared database

That's why Reposilite allows you to export shared configuration into JSON file and link it manually using `--shared-configuration` parameter. 
You can generate default template with `--generate-configuration=configuration.shared.json` parameter or just configure your instance through web dashboard and download configuration as JSON file.
Example output:

<Spoiler title="configuration.shared.json">

```json
{
  "web": {
    "swagger": "false",
    "forwardedIp": "X-Forwarded-For"
  },
  "repositories": {
    "repositories": {
      "releases": {
        "visibility": "PUBLIC",
        "redeployment": "false",
        "preserved": "-1",
        "storageProvider": {
          "type": "fs",
          "quota": "100%",
          "mount": ""
        },
        "proxied": "[]"
      },
      "snapshots": {
        "visibility": "PUBLIC",
        "redeployment": "false",
        "preserved": "-1",
        "storageProvider": {
          "type": "fs",
          "quota": "100%",
          "mount": ""
        },
        "proxied": "[]"
      },
      "private": {
        "visibility": "PRIVATE",
        "redeployment": "false",
        "preserved": "-1",
        "storageProvider": {
          "type": "fs",
          "quota": "100%",
          "mount": ""
        },
        "proxied": "[]"
      }
    }
  },
  "frontend": {
    "frontend": "true",
    "basePath": "/",
    "id": "reposilite-repository",
    "title": "Reposilite Repository",
    "description": "Public Maven repository hosted through the Reposilite",
    "organizationWebsite": "https://reposilite.com",
    "organizationLogo": "https://avatars.githubusercontent.com/u/88636591",
    "icpLicense": ""
  },
  "statistics": {
    "resolvedRequestsInterval": "MONTHLY"
  },
  "authentication": {
    "ldap": {
      "enabled": "false",
      "hostname": "ldap.domain.com",
      "port": "389",
      "baseDn": "dc=company,dc=com",
      "searchUserDn": "cn=reposilite,ou=admins,dc=domain,dc=com",
      "searchUserPassword": "reposilite-admin-secret",
      "userAttribute": "cn",
      "userFilter": "(&(objectClass=person)(ou=Maven Users))",
      "userType": "PERSISTENT"
    }
  }
}
```

</Spoiler>
