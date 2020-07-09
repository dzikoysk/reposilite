---
id: proxy
title: Proxy
sidebar_label: Proxy
---

Our projects often use many repositories.
To simplify your build files, 
you can list all of these repositories in `proxy` section 
and Reposilite will also search for requested artifacts among them:

 ```yaml
# List of proxied repositories.
# Reposilite will search for an artifact in remote repositories listed below, 
# if the requested artifact was not found.
# Note: URL cannot contains / at the end
proxied:
 - https://repo.panda-lang.org
```

## Repository Caching
`# TODO`