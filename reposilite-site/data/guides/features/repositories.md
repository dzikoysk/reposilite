---
id: repositories
title: Repositories
---

By default, Reposilite generates three standard repositories:

* releases - the most popular repository type where we can push our artifacts
* snapshots - dedicated repository for snapshot artifacts 
* private - dedicated repository for private artifacts

## Custom repository 
You can also define a new one by just adding it in the configuration:

![Configuration](/images/guides/settings-repositories.png)

#### Hidden repositories
If the visibility of a repository is set to `HIDDEN`, it will not be shown in the dashboard,
but you can download artifacts from it, for example with gradle.
Only if you are using a proper [access token](authorization#access-token), it will be shown in the dashboard.

#### Private repositories
If the visibility of a repository is set to `PRIVATE`,
you can only interact (download, index, deploy) with the repository using a proper [access token](authorization#access-token).