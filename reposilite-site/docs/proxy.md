---
id: proxy
title: Proxy
sidebar_label: Proxy and caching
---

Our projects often use many repositories.
To simplify your build files, 
you can list all of these repositories in `proxy` section 
and Reposilite will also search for requested artifacts among them:

 ```json5
# List of proxied repositories.
# Reposilite will search for an artifact in remote repositories listed below, 
# if the requested artifact was not found.
proxied {
  https://repo.panda-lang.org
}
```

## Repository Caching

Proxied content from another repository is not stored in local repository by default.
To improve the stability of your build ecosystem, you can force Reposilite to store all of the proxied artifacts locally by enabling:

```json5
# Reposilite can store proxied artifacts locally to reduce response time and improve stability
storeProxied: true
```

**Note**: Remember about increasing disk quota! 
Caching may allocate thousands of artifacts, especially at the beginning - for the first few builds. 

```json5
# Control the maximum amount of data assigned to Reposilite instance
# Supported formats: 90%, 500MB, 10GB
diskQuota: 85%
```

Stable Reposilite instance should guarantee much better availability than any other public repository - even Maven Central repository.