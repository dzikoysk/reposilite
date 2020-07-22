---
id: cdn
title: CDN
sidebar_label: CDN
---

We often use CDN services, like e.g. Cloudflare to proxy traffic. 
By default, these services use some kind of cache policy which causes a lot of random issues.
To avoid these problem, you have to exclude Reposilite from cached resources, 
for instance - if you use Cloudflare - you can set `Cache Level` to `Bypass` through the custom page rules:

![Cache Bypass](/img/cloudflare-cache-bypass.png)

Related issue: [GH-156](https://github.com/dzikoysk/reposilite/issues/156)