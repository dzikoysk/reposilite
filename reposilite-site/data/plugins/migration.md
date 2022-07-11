---
id: migration
title: Migration
description: Migrate your tokens from 2.x to 3.x
official: true
repository: dzikoysk/reposilite
authors: [ 'dzikoysk' ]
maven: 'maven.reposilite.com'
groupId: 'com.reposilite.plugin'
artifactId: 'migration-plugin'
---

The migration plugin is dedicated for Reposilite 2.x users that want to migrate their tokens to Reposilite 3.x.
This plugins picks up `tokens.dat` file from a working directory 
and converts it to `tokens.json` file in JSON scheme compatible with `token-import` command described in [Guide :: Tokens / Export and Import](/guide/tokens#export-and-import) guide.

```bash
Migration | Reposilite 2.x 'tokens.dat' file found, the migration procedure has started.
Migration | 3 token(s) found in 'tokens.dat' file.
Migration | 3 token(s) have been exported to 'tokens.json' file.
Migration | Run 'token-import tokens.json' command to import those tokens.
Migration | This plugin is no longer needed, you can remove it.
```
