---
id: javadoc
title: Javadoc
description: Host JavaDoc pages automatically using JAR files located in repositories
official: true
repository: dzikoysk/reposilite
authors: [ 'TOTHT0MI', 'dzikoysk' ]
url: 'maven.reposilite.com'
gav: 'com.reposilite.plugin.javadoc-plugin'
---

Host JavaDoc pages automatically using JAR files (`*-javadoc.jar`) located in repositories.
Javadoc file is extracted during the first request to `$working-directory/javadocs` directory.

To access javadocs, you have to visit `repo.domain.com/javadoc/<gav>`. 
For instance, using Reposilite Javadocs:

* [panda-lang repository / reposilite-3.0.0-rc.4 javadocs](https://maven.reposilite.com/javadoc/releases/com/reposilite/reposilite/3.0.0-rc.4/)
