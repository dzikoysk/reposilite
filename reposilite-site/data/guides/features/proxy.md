---
id: proxy
title: Proxy
---

Our projects often use many repositories.
To simplify your build files, 
you can list all of these repositories in `proxy` section 
and Reposilite will also search for requested artifacts among them:

 ```json5
# List of proxied repositories associated with this repository.
# Reposilite will search for a requested artifact in remote repositories listed below.
# Supported flags:
# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability
# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)
# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)
proxied: [
  https://maven.reposilite.com/releases --store 
]
```

**Note**: Remember about increasing disk quota! 
Caching may allocate thousands of artifacts, especially at the beginning - for the first few builds. 

```json5
storageProvider: fs --quota 85%
```

Stable Reposilite instance should guarantee much better availability than any other public repository - even Maven Central repository.