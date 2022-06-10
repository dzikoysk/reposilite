# Contributing

This document describes how to contribute to the core [Reposilite](https://reposilite.org) project.
When contributing to this repository, 
please first discuss the change you wish to make via [issue](https://github.com/dzikoysk/reposilite/issues) as every pull request should address an issue.

## Creating an issue

Creating an issue to ask a question is fine. You can also join our Discord server for a little more fluent conversation:

<ul>
 <li>
  <a href="https://discord.gg/qGRqmGjUFX">
   <img alt="Discord Online" src="https://img.shields.io/discord/204728244434501632.svg" />
  </a>
  <a href="https://discord.gg/qGRqmGjUFX">
   <img alt="Discord" src="https://img.shields.io/badge/discord-reposilite-738bd7.svg?style=square" />
  </a>
 </li>
</ul>

Remember to include enough information if you're reporting a bug to simplify reproduction phase.

## Creating a PR
Every pull request will be considered :)

### How to increase the chance of having your PR merged

1. If related issue does not exist, create a new one to start a discussion aobut it
2. Try to write some tests for your change. There are a lot of examples in the `test` dir.
3. Format your code, so it looks somewhat like the rest of the source, don't make any extra changes like refactor that is not related to the subject of your PR.

## Running

Reposilite has 3 modules that use different tech stack. 
Visit README.md of each module to see details about how to run specific module and work with it:

* [Reposilite :: Backend](https://github.com/dzikoysk/reposilite/tree/main/reposilite-backend) - Sources of main Reposilite application
  * [Reposilite :: Plugins](https://github.com/dzikoysk/reposilite/tree/main/reposilite-plugins) - Plugin system related subprojects
* [Reposilite :: Frontend](https://github.com/dzikoysk/reposilite/tree/main/reposilite-frontend) - Sources of default Reposilite frontend in Vue
* [Reposilite :: Site](https://github.com/dzikoysk/reposilite/tree/main/reposilite-site) - Sources of [reposilite.com](https://reposilite.com/) website
