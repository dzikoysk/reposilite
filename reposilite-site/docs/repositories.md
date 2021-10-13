---
id: repositories
title: Repositories
sidebar_label: Repositories
---

By default, Reposilite generates three standard repositories:

* releases - the most popular repository type where we can push our artifacts
* snapshots - dedicated repository for snapshot artifacts 
* private - dedicated repository for private artifacts

## Custom repository 
You can also define a new one just adding it in the configuration:

```json5
repositories {
  releases {
    ...
  }
  snapshots {
    ...
  }
  private {
    ...
  }
  custom_repository {
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
}
```

## Hidden repositories
If the visibility of a repository is set to `HIDDEN`, it will not be shown in the dashboard,
but you can download artifacts from it, for example with gradle.
Only if you are using a proper [access token](authorization#access-token), it will be shown in the dashboard.

## Private repositories
If the visibility of a repository is set to `PRIVATE`,
you can only interact (download, index, deploy) with the repository using a proper [access token](authorization#access-token).