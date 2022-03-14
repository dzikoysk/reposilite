---
id: cloudflare
title: Cloudflare
---

We often use CDN services, like e.g. Cloudflare to proxy traffic. 
By default, these services use some kind of cache policy which causes a lot of random issues.
To avoid these problems, you have to exclude Reposilite from cached resources.


If you use Cloudflare - you can set `Cache Level` property to `Bypass` through the custom page rules:

![Cache Bypass](/images/guides/cloudflare-cache-bypass.png)

Associated issue on GitHub: [GH-156 Wrong artifact is being downloaded](https://github.com/dzikoysk/reposilite/issues/156)