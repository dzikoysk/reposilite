---
id: reposilite-2.x
title: Reposilite 2.x
---

Stable release of Reposilite 3.x has been finally released! 🎉 

### Summary
Reposilite 3.x is tested for a few months (alphas & RCs builds)
and quite a lot of users decided to roll out their instances to production already.
Anyway, there are some differences between 2.x and 3.x you should be aware of:

1. 3.x uses database to store state of your instance, not raw data files. <br/>
  [1.1. Stable targets: SQLite, MySQL/MariaDB <br/>
  1.2. Experimental targets: H2, PostgreSQL](/guide/setup#local-configuration) <br/>
2. Concept of primary repository _(paths rewriting)_ has been [removed](https://github.com/dzikoysk/reposilite/issues/505). <br/>
   If you need it for backwards compatibility, you can always imitate this behavior with custom repositories,
   but it's strongly recommended to move on from this approach. 
3. Frontend is fully independent application that you can disable or override.
   The small cosmetic flaw of this approach is that you'll see `/#/` in your URL, but overall it's just a healthy change.
   `Note:` It won't affect URLs of files located in repositories.
4. 3.x has slightly more advanced permissions system, 
   because each access token can contain multiple paths with different permissions: <br/>
   [4.1. Reposilite / Guide - Tokens](/guide/tokens) <br/>
   [4.2. Reposilite / Guide - Routes](/guide/routes)

Because a lot of properties are now located in the `Settings` tab in the web dashboard,
a good starting point to see what you can do with Reposilite 3.x is to generate your first access token:

* [Reposilite / Guide - Tokens :: Temporary tokens](/guide/tokens#temporary-tokens)

### Migration
`Note` Before an upgrade to the latest version, you should definitely consider a backup.
It's a good practice to be absolutely sure that you won't lose any important data in case of migration failure.

#### Repositories
Overall working directory structure stays the same, so your repositories and their content is fully compatible with 3.x.
If you'd like to see what files you can expect, take a look at:

* [Reposilite / Guide - Data structure](/guide/manual#data-structure)

#### Configuration
Reposilite 3.x has much more configuration possibilities, so unfortunately previous configuration is just incompatible. 
The major difference you can notice is that `configuration.cdn` file located in your working directory does not contain all properties you could find in 2.x.
That's because a lot of them is now configurable in web interface and those properties are stored in database.

To learn more about all possibilities to setup your Reposilite instance, take a look at:

* [Reposilite / Guide - Setup](/guide/setup)

#### Tokens
Reposilite 2.x stored access tokens & statistics in local files and those are incompatible with Reposilite 3.x. 
There's no option to migrate statistics, but I've prepared dedicated plugin that can help you with tokens:

* [Reposilite / Plugins - Migration plugin](/plugin/migration)
