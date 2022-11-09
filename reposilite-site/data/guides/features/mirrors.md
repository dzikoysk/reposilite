---
id: mirrors
title: Mirrors
---

Our projects often use many repositories.
To simplify your build files, 
you can list all of these repositories in `Mirrored repositories` section 
and Reposilite will also search for requested artifacts among them:

![Mirrored Repositories](/images/guides/mirrored-repositories.png)

**Note**: Remember about increasing disk quota! 
Caching may allocate thousands of artifacts, especially at the beginning - for the first few builds. 

![Disk Quota](/images/guides/disk-quota.png)

Stable Reposilite instance should guarantee much better availability than any other public repository - even Maven Central repository.