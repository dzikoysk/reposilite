<div align="center">
 <h1>Reposilite</h1>
 <div>
  <a href="https://github.com/dzikoysk/reposilite/actions/workflows/gradle.yml">
   <img alt="Reposilite CI" src="https://github.com/dzikoysk/reposilite/actions/workflows/gradle.yml/badge.svg" />
  </a>
  <a href="https://github.com/dzikoysk/reposilite/releases">
   <img src="https://repo.panda-lang.org/api/badge/latest/releases/org/panda-lang/reposilite?color=40c14a&name=Reposilite&prefix=v" />
  </a>
  <a href="https://codecov.io/gh/dzikoysk/reposilite">
   <img alt="CodeCov" src="https://codecov.io/gh/dzikoysk/reposilite/branch/main/graph/badge.svg?token=9flNHTSJpp" />
  </a>
  <a href="https://hub.docker.com/r/dzikoysk/reposilite">
   <img alt="Docker Pulls" src="https://img.shields.io/docker/pulls/dzikoysk/reposilite.svg?label=pulls&logo=docker" />
  </a>
  <!--
  <a href="(https://www.codefactor.io/repository/github/dzikoysk/reposilite/overview/main">
   <img alt="CodeFactor" src="https://www.codefactor.io/repository/github/dzikoysk/reposilite/badge/main" />
  </a>
  -->
  <a href="https://discord.gg/qGRqmGjUFX">
   <img alt="Discord" src="https://img.shields.io/badge/discord-reposilite-738bd7.svg?style=square" />
  </a>
  <a href="https://discord.gg/qGRqmGjUFX">
   <img alt="Discord Online" src="https://img.shields.io/discord/204728244434501632.svg" />
  </a>
 </div>
 <br>
 <div>
  Lightweight and easy-to-use repository manager for Maven based artifacts in JVM ecosystem. 
This is simple, extensible and scalable self-hosted solution to replace managers like Nexus, Archiva or Artifactory, with reduced resources consumption. 
 </div>
 <br>
 <div>
  <a href="https://reposilite.com">Website</a>
  |
  <a href="https://reposilite.com/docs/about">Official Guide</a>
  |
  <a href="https://github.com/dzikoysk/reposilite/releases">GitHub Releases</a>
  |
  <a href="https://hub.docker.com/r/dzikoysk/reposilite">DockerHub Images</a>
  |
  <a href="https://panda-lang.org/support">Support</a>
  |
  <a href="https://repo.panda-lang.org">Demo</a>
 </div>
 <br>
 <img alt="Preview" src="https://user-images.githubusercontent.com/4235722/133891983-966e5c6d-97b1-48cc-b754-6e88117ee4f7.png" />
 <br>
 <br>
 <strong>
  ⛔ Main sources refers to the alpha version of Reposilite 3.x that is under heavy development.
  <br>
  If you're looking for docs and sources of Reposilite 2.x, visit 2.x branch: <a href="https://github.com/dzikoysk/reposilite/tree/2.x">Reposilite 2.x</a>
 </strong>
</div>

### Installation
To run Reposilite for your personal needs you should assign around 16MB of RAM and at least Java 8+ installed. <br>
For huge public repositories you can adjust memory limit and even size of used thread pools in the configuration.

```bash
# Launching a standalone JAR file
$ java -Xmx16M -jar reposilite-3.0.0-alpha.21.jar

# Using a Docker
$ docker pull dzikoysk/reposilite:3.0.0-alpha.21
```

Visit official guide to read more about extra parameters and configuration details.

### Publications
* [Reposilite - Official Guide](https://reposilite.com/docs/about)
* [Publishing your artifacts to the Reposilite - a new self-hosted repository manager ](https://dev.to/dzikoysk/publishing-your-artifacts-to-the-reposilite-a-new-self-hosted-repository-manager-3n0h)

Users' stories

* [Reposilite - Reddit Thread](https://www.reddit.com/r/java/comments/k8i2m0/reposilite_alternative_lightweight_maven/)
* [Looking for simple repository manager by David Kihato](https://kihats.medium.com/custom-self-hosted-maven-repository-cbb778031f68)

### Supporters
Thanks to all contributors and people that decided to donate the project ❤️

<table>
 <tr><td>Monthly</td><td><a href="https://github.com/sponsors/dzikoysk">GitHub Sponsors</a></td></tr>
 <tr>
  <td>One time</td>
  <td>
   <a href="https://github.com/zzmgck">zzmgck</a> with $190, 
   <a href="https://github.com/milkyway0308">milkyway0308</a> with $20,
   <a href="https://github.com/alexwhb">alexwhb</a> with $15, 
   <a href="https://github.com/escv">escv</a> with $10,
   <a href="https://github.com/EthanDevelops">EthanDevelops</a> with $6, 
   Rob with $5
  </td>
 </tr>
</table>

And maybe someday... also you will `\(^-^)/`

### For developers

Recommended tool to develop backend module is IntelliJ IDE, for frontend it might be e.g. VSC. 

```bash
# Run only backend through CLI
$ ./gradlew run

# Run only frontend
$ cd reposilite-frontend && npm i && npm run full

# Run only Reposilite site
$ cd reposilite-site/website && npm i && npm run start
```

#### Stack

Reposilite 3.x
* Reposilite Backend: Kotlin with Coroutines + Javalin + Exposed + JUnit + _(DDD + Hexagonal Architecture)_
* Reposilite Frontend: Vue3 + Vite + Windicss
* Reposilite Site: React.js + Docusaurus

Reposilite 2.x
* Reposilite Backend: Java + Javalin (Jetty) + Groovy (JUnit) + _(DDD)_
* Reposilite Frontend: Vue2 + Pug + Stylus + Tailwindcss
* Reposilite Site: React.js + Docusaurus 

Reposilite 1.x
* Reposilite: Java + NanoHTTPD
