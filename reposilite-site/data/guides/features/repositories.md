---
id: repositories
title: Repositories
---

Reposilite 3.7 supports multiple repository providers. Maven and generic file repositories are built in, and plugins can register additional providers without changing the server router.

By default, the Maven provider generates three standard repositories:

* `releases` - the most popular repository type where we can push our artifacts
* `snapshots` - dedicated repository for snapshot artifacts (with `-SNAPSHOT` suffix)
* `private` - dedicated repository for private artifacts

You don't have to use them, 
but it's a common practice to follow standardized `releases` - `snapshots` naming convention for public repositories.

### Repository configuration
Each repository has individual configuration, so you can define different rules for each of them:

#### Redeployment
By default, Reposilite does not allow to redeploy artifacts to the same repository.
If you try to deploy the same artifact twice, 
you'll get [409 Conflict](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/409) response.
You can enable redeployment in the repository configuration.

#### Hidden repositories
If the visibility of a repository is set to `HIDDEN`, it will not be shown in the dashboard,
but you can download artifacts from it, for example with gradle.
Only if you are using a proper [access token](authorization#access-token), it will be shown in the dashboard.

#### Private repositories
If the visibility of a repository is set to `PRIVATE`,
you can only interact (download, index, deploy) with the repository using a proper [access token](authorization#access-token).

#### Storage providers
Each repository has individual storage provider configuration,
so some of them can use [S3-compatible object storage](/guide/s3) and others can use local filesystem.

#### Mirrors
Reposilite can mirror (proxy) other repositories:
- [Guide / Mirrors](/guide/mirrors) - how to configure mirrors

### Custom repository
You can also define a new one by just adding it in the configuration:

![Configuration](/images/guides/settings-repositories.png)

### Generic file repositories

A generic repository stores arbitrary files without applying Maven metadata, checksum, snapshot, or artifact-layout rules. Its content API uses the same stable repository-first URL shape:

```text
GET    /{repository}
HEAD   /{repository}
GET    /{repository}/{path}
HEAD   /{repository}/{path}
PUT    /{repository}/{path}
POST   /{repository}/{path}
DELETE /{repository}/{path}
```

For example, `PUT /downloads/releases/application.tar.gz` uploads a file and `GET` on the same URL downloads it. Writes require a token with write access to the route. Visibility, route permissions, filesystem quotas, S3 storage, directory browsing, and redeployment rules work in the same way as for Maven repositories.

Generic repositories are configured and consumed through direct repository URLs in 3.7. The dashboard's file browser remains Maven-specific, so it does not list generic repositories yet.

Generic repositories have their own `generic` settings domain and do not need a `type` field in Maven settings:

```json
{
  "generic": {
    "repositories": [
      {
        "id": "downloads",
        "visibility": "PUBLIC",
        "redeployment": false,
        "storageProvider": {
          "type": "fs",
          "quota": "10GB"
        }
      }
    ]
  }
}
```

Repository ids must be a single non-blank URL path segment. Keep them unique across providers: if multiple
providers expose the same id, that repository URL returns 404 until the conflict is removed.
