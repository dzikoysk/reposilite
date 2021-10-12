---
id: configuration
title: Configuration
sidebar_label: Configuration
---

There are two ways to pass custom properties to your Reposilite instance:

* Through the `reposilite.cdn` configuration file
* Using the [system properties](#system-properties) *(overrides values from configuration)*

Detailed description of properties is located in the following chapters:
* [Repositories](./repositories)
* [Authorization](./authorization)
* [Proxy](./proxy)
* [Customization](./customization)

## Default configuration
The standard configuration of Reposilite should be generated
by Reposilite during your first start and it should look like this:

```json5
# ~~~~~~~~~~~~~~~~~~~~~~ #
#       Reposilite       #
# ~~~~~~~~~~~~~~~~~~~~~~ #

# Hostname
# The hostname can be used to limit which connections are accepted.
# Use 0.0.0.0 to accept connections from anywhere.
# 127.0.0.1 will only allow connections from localhost.
hostname: 0.0.0.0
# Port to bind
port: 80
# Database. Supported storage providers:
# - sqlite reposilite.db
# - sqlite --temporary
# - mysql localhost:3306 database user password
database: sqlite reposilite.db

# Repository id used in Maven repository configuration
id: reposilite-repository
# Repository title
title: Reposilite Repository
# Repository description
description: Public Maven repository hosted through the Reposilite
# Link to organization's website
organizationWebsite: https://reposilite.com
# Link to organization's logo
organizationLogo: https://avatars.githubusercontent.com/u/88636591
# The Internet Content Provider License (also known as Bei'An)
# Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
# In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.
icpLicense: ""
# Enable default frontend with dashboard
frontend: true
# Custom base path
basePath: /
# Keep processed frontend files in memory to improve response time
cacheContent: true
# Enable Swagger (/swagger-docs) and Swagger UI (/swagger)
swagger: false

# List of supported Maven repositories.
# First directory on the list is the main (primary) repository.
# To mark a repository as private, add the "--private" flag
repositories {
releases {
# Supported visibilities: public, hidden, private
visibility: PUBLIC
# Does this repository accept redeployment of the same artifact version
redeployment: false

# Used storage type. Supported storage providers:
# > File system (local) provider. Supported flags:
# --quota 10GB = control the maximum amount of data stored in this repository. (Supported formats: 90%, 500MB, 10GB)
# Example usage:
# storageProvider: fs --quota 50GB
# > S3 provider. Supported flags:
# --endpoint = custom endpoint with which the S3 provider should communicate (optional)
# Example usage:
# storageProvider: s3 --endpoint custom.endpoint.com accessKey secretKey region bucket-name
storageProvider: fs --quota 100%

# List of proxied repositories associated with this repository.
# Reposilite will search for a requested artifact in remote repositories listed below.
# Supported flags:
# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability
# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)
# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)
# Example usage:
# proxied [
#   https://repo.panda-lang.org/releases --store --connectTimeout=3 --readTimeout=15 --auth user:token
# ]
proxied: []
}
snapshots {
# Supported visibilities: public, hidden, private
visibility: PUBLIC
# Does this repository accept redeployment of the same artifact version
redeployment: false

# Used storage type. Supported storage providers:
# > File system (local) provider. Supported flags:
# --quota 10GB = control the maximum amount of data stored in this repository. (Supported formats: 90%, 500MB, 10GB)
# Example usage:
# storageProvider: fs --quota 50GB
# > S3 provider. Supported flags:
# --endpoint = custom endpoint with which the S3 provider should communicate (optional)
# Example usage:
# storageProvider: s3 --endpoint custom.endpoint.com accessKey secretKey region bucket-name
storageProvider: fs --quota 100%

# List of proxied repositories associated with this repository.
# Reposilite will search for a requested artifact in remote repositories listed below.
# Supported flags:
# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability
# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)
# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)
# Example usage:
# proxied [
#   https://repo.panda-lang.org/releases --store --connectTimeout=3 --readTimeout=15 --auth user:token
# ]
proxied: []
}
private {
# Supported visibilities: public, hidden, private
visibility: PRIVATE
# Does this repository accept redeployment of the same artifact version
redeployment: false

# Used storage type. Supported storage providers:
# > File system (local) provider. Supported flags:
# --quota 10GB = control the maximum amount of data stored in this repository. (Supported formats: 90%, 500MB, 10GB)
# Example usage:
# storageProvider: fs --quota 50GB
# > S3 provider. Supported flags:
# --endpoint = custom endpoint with which the S3 provider should communicate (optional)
# Example usage:
# storageProvider: s3 --endpoint custom.endpoint.com accessKey secretKey region bucket-name
storageProvider: fs --quota 100%

# List of proxied repositories associated with this repository.
# Reposilite will search for a requested artifact in remote repositories listed below.
# Supported flags:
# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability
# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)
# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)
# Example usage:
# proxied [
#   https://repo.panda-lang.org/releases --store --connectTimeout=3 --readTimeout=15 --auth user:token
# ]
proxied: []
}
}

# Support encrypted connections
sslEnabled: false
# SSL port to bind
sslPort: 443
# Key store file to use.
# You can specify absolute path to the given file or use ${WORKING_DIRECTORY} variable.
keyStorePath: ${WORKING_DIRECTORY}/keystore.jks
# Key store password to use
keyStorePassword: ""
# Redirect http traffic to https
enforceSsl: false

# Max amount of threads used by core thread pool (min: 4)
# The web thread pool handles first few steps of incoming http connections, as soon as possible all tasks are redirected to IO thread pool.
webThreadPool: 32
# IO thread pool handles all tasks that may benefit from non-blocking IO (min: 2)
# Because most of tasks are redirected to IO thread pool, it might be a good idea to keep it at least equal to web thread pool.
ioThreadPool: 16

# Amount of messages stored in cached logger.
cachedLogSize: 100
# Any kind of proxy services change real ip.
# The origin ip should be available in one of the headers.
# Nginx: X-Forwarded-For
# Cloudflare: CF-Connecting-IP
# Popular: X-Real-IP
forwardedIp: X-Forwarded-For
# Debug mode
debugEnabled: false
```

Customized version of configuration file can be found in test workspace: [reposilite.cdn](https://github.com/dzikoysk/reposilite/blob/master/reposilite-backend/src/test/workspace/reposilite.cdn)

## System properties
Passing properties through the system properties is especially useful, 
when we want to use Docker image. The parameter must be provided in the following structure:

```properties
-Dreposilite.propertyName=propertyValue
-Dreposilite.propertyName=arrayValue1,arrayValue2
```

For instance:

```bash
$ java -Xmx32M -Dreposilite.port=8080 -jar reposilite.jar
```


## Parameters
Some of the properties have to be set through the command-line parameters.

### Working directory
To declare custom working directory for Reposilite instance,
you should use `--working-directory` *(alias: `-wd`)* parameter:

```console
$ java -jar reposilite.jar --working-directory=/app/data
```

### Configuration file
You may also declare custom location of configuration file using the `--config` *(alias: `-cfg`)* parameter:

```console
$ java -jar reposilite.jar --config=/etc/reposilite/reposilite.cdn
```

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

```console
$ java -Dtinylog.writerFile.file=/etc/reposilite/log.txt -jar reposilite.jar
```