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
# Title displayed by frontend
title: "#onlypanda"
# Description displayed by frontend
description: "Public Maven repository hosted through the Reposilite"
# Accent color used by frontend
accentColor: "#2fd4aa"
```

As a result of these cosmetic changes, we can see:

![Customization old](/img/customization-old.png)