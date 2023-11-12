---
id: repositories
title: Repositories
---

By default, Reposilite generates three standard repositories:

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
