---
id: repositories
title: Repositories
sidebar_label: Repositories
---

By default, Reposilite generates two standard repositories:

* releases - the most popular repository type where we can push our artifacts
* snapshots - dedicated repository for artifacts 

## Custom repository 
You can also define a new one just adding it in the configuration:

```yaml
repositories:
  - "releases" # primary repository (declared as first)
  - "snapshots"
  - "custom_repository"
```

## Rewrite paths
Let's say, we have artifact `groupId/artifactId` located in the `releases` repository with some builds.
Maven will access this file using the standard path qualifier built on top of data provided in `pom.xml`:

* [localhost:80/releases/groupId/artifactId](http://localhost:80/releases/groupId/artifactId)

We can force Reposilite to support requests without primary repository name using this option:

```properties
rewritePathsEnabled: true
```

After that, we should be able to access `groupId/artifactId` using the following path:

* [localhost:80/groupId/artifactId](http://localhost:80/groupId/artifactId)

You may find it useful when you want to share link to your artifacts in a cleaner way.

## Latest info

Reposilite exposes extra qualifier to get information about the latest version of requested artifact:

* [localhost:80/groupId/artifactId/latest](http://localhost:80/groupId/artifactId/latest)

It might be helpful to fetch raw version of artifact to wrap it e.g. on your site.
