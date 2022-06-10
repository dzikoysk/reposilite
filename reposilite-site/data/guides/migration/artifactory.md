---
id: artifactory
title: Artifactory
---

### General

Reposilite works slightly different than Artifactory or Nexus, so make sure you've properly configured your instance and environment.
Guides you may find especially useful during migration:

* [Reposilite / Guide - Setup](/guide/setup) - How you can configure Reposilite instance
* [Reposilite / Guide - Tokens](/guide/tokens) - How to generate access to your instance
* [Reposilite / Guide - Deployment](/guide/deployment) - Deploy your first artifact to the Reposilite

### Export repository from Artifactory
Be sure to check the `.m2 compatible export` box to get an export that Reposilite can use. Also, we recommend excluding artifactory build metadata if your version of Artifactory has this feature.
    
![Artifactory - Export menu preview](https://user-images.githubusercontent.com/823828/167173140-6777ed00-d5e6-44b9-bf40-c337bae712a9.png)
    
Move the resulting export files to your new Reposilite server and extract them to the repository folder you would like them in.
Depending on the organization of your new repositories, you may need to move your artifacts out of parent folders to get them in the location you want them in your new repository.
   
* [Reposilite / Guide - Data Structure](/guide/manual#data-structure)

Configure the new repository in Reposilite, if you haven't already, making sure it is loading files from the correct directory.
You should be able to browse the artifacts once configuration reloads.
    
* [Reposilite / Guide - Custom repository configuration](/guide/repositories)

### Helpful Tips

#### Combining Releases / Snapshots

In some cases, you may want to merge separated release and snapshot repositories into one Reposilite repository. Rsync offers a useful way to do this and is installed by default on many unix systems. The following command safely merges releases in a `releases` folder into a `snapshots` folder containing snapshots. You can then move / rename the snapshots folder accordingly.

```bash
# Merge all files in releases into the snapshots folder
$ rsync -avhu --progress ./releases/ ./snapshots/

# Remove the releases folder
$ rm -rf ./releases

# Move the contents of the snapshots folder to your final target repository
$ mv ./snapshots/* /data/reposilite/repositories/my-migrated-repository/
```

#### Removing Artifactory Build Metadata Folders

If your Artifactory exports contain build metadata folders that you would prefer to remove, you can use the following command to help remove them. This command performs a find for all directories, in the current directory, ending in `.artifactory-metadata`, and removes them from the system.

```bash
$ rm -rf `find . -type d -name *.artifactory-metadata`
```
