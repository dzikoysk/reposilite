---
id: nexus-3
title: Nexus 3
---

The tool from [https://github.com/lbar/nexus3-export.git](https://github.com/lbar/nexus3-export.git) 
can be used to migrate files from Nexus 3.

### Get and build the tool

```bash
git clone https://github.com/lbar/nexus3-export.git
cd nexus3-export
mvn package -DskipTests
```

### Copy artifacts from Nexus

```bash
mkdir exported-data
java -jar target/nexus3-export-1.0.jar YOUR_NEXUS_URL YOUR_NEXUS_REPOSITORY_NAME exported-data

# OPTIONAL (largely depends on your Reposilite setup)
cd exported-data
chown --recursive reposilite:reposilite *
mv * /var/lib/reposilite/repositories/releases/
```
