---
id: customization
title: Customization
sidebar_label: Customization
---

At this moment frontend does not have wide range of customizable options.

## Base path
By default, 
Reposilite assumes that application is available at root of the given domain, 
e.g. `repo.domain.com`. 
To bind Reposilite to some custom base path, let's say `domain.com/repo`,
you have to specify it in the configuration to fix incorrect paths:

```properties
basePath: "/repo"
```

## Header
Header content might be modified using the following properties:

```properties
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
```

As a result of these cosmetic changes, we can see:

![Customization new](/img/customization-new.png)