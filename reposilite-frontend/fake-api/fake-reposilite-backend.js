/* eslint-disable no-unused-vars */

/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createRequire } from "module"
const require = createRequire(import.meta.url)

const express = require("express")
const expressWs = require("express-ws")
const bodyParser = require('body-parser')
const crypto = require("crypto")

const {
  respond,
  authorized,
  invalidCredentials,
  sendMessage,
  createFileDetails,
  createDirectoryDetails,
  generateDayWiseTimeSeries
} = require("./extensions.cjs")

const application = express()
expressWs(application)

let uploadedFiles = []
let mavenSettingsSchema = require('./maven-settings-schema.json')
let mavenSettingsEntity = require('./maven-settings-entity.json')

let uptime = 1000
let memory = 20
let threads = 10
let failures = [
  [
    'failure /api/maven/releases/com/example/app/1.0.0/app-1.0.0.jar',
    '  by NullPointerException: Cannot invoke "Repository.getName()" because "repository" is null',
    '  at com.reposilite.maven.MavenFacade.findFile(MavenFacade.kt:88)',
    '  at com.reposilite.maven.MavenFacade.findDetails(MavenFacade.kt:120)',
    '  at com.reposilite.maven.infrastructure.MavenEndpoints.findFile(MavenEndpoints.kt:64)',
  ].join('\n'),
  [
    'failure /api/badge/latest/releases/com/example/lib',
    '  by IllegalStateException: No versions found for the requested artifact',
    '  at com.reposilite.badge.BadgeFacade.findLatestVersion(BadgeFacade.kt:52)',
    '  at com.reposilite.badge.infrastructure.BadgeEndpoints.latestBadge(BadgeEndpoints.kt:41)',
  ].join('\n'),
]

const todayLocalDate = () => {
  const now = new Date()
  return [now.getFullYear(), now.getMonth() + 1, now.getDate()]
}
const permissionObject = (value) =>
  value === "m" || value === "access-token:manager"
    ? { identifier: "access-token:manager", shortcut: "m" }
    : { identifier: value, shortcut: value }
const routePermissionObject = (shortcut) =>
  shortcut === "w"
    ? { identifier: "route:write", shortcut: "w" }
    : { identifier: "route:read", shortcut: "r" }

let tokenSequence = 2
let accessTokens = [
  {
    identifier: { type: "PERSISTENT", value: 1 },
    name: "admin",
    createdAt: [2023, 1, 1],
    description: "Primary management token",
    expiresAt: null,
    permissions: [{ identifier: "access-token:manager", shortcut: "m" }],
    routes: [],
  },
  {
    identifier: { type: "PERSISTENT", value: 2 },
    name: "ci-deployer",
    createdAt: [2023, 2, 15],
    description: "Used by the release pipeline",
    expiresAt: null,
    permissions: [],
    routes: [
      { path: "/releases", permission: { identifier: "route:read", shortcut: "r" } },
      { path: "/releases", permission: { identifier: "route:write", shortcut: "w" } },
      { path: "/snapshots", permission: { identifier: "route:read", shortcut: "r" } },
    ],
  },
]

setInterval(() => {
  memory += Math.random() * 10
  threads += 1
  uptime += 5000
  if (failures.length < 6) {
    failures.push([
      `failure /api/maven/releases/com/example/app/1.0.${failures.length}/app.jar`,
      '  by RuntimeException: Simulated failure emitted by the fake backend',
      '  at com.reposilite.fake.Simulator.tick(Simulator.kt:10)',
    ].join('\n'))
  }
}, 5000)

const statisticsBaseDate = new Date().getTime() - (19 * 86400000)
const statisticsSeries = [
  {
    name: 'Releases',
    data: generateDayWiseTimeSeries(statisticsBaseDate, 20, {
      min: 10,
      max: 60
    })
  },
  {
    name: 'Snapshots',
    data: generateDayWiseTimeSeries(statisticsBaseDate, 20, {
      min: 10,
      max: 20
    })
  },
  {
    name: 'Maven Central',
    data: generateDayWiseTimeSeries(statisticsBaseDate, 20, {
      min: 10,
      max: 15
    })
  }
]

const resolvedArtifacts = {
  releases: [
    { gav: 'com/example/app/2.4.1/app-2.4.1.jar', count: 48120 },
    { gav: 'org/panda-lang/utilities/1.8.4/utilities-1.8.4.jar', count: 39540 },
    { gav: 'com/example/lib/1.9.0/lib-1.9.0.jar', count: 31205 },
    { gav: 'com/example/app/2.4.0/app-2.4.0.jar', count: 22870 },
    { gav: 'net/dzikoysk/cdn/1.14.4/cdn-1.14.4.jar', count: 18930 },
    { gav: 'com/example/app/maven-metadata.xml', count: 15540 },
    { gav: 'com/example/lib/1.8.2/lib-1.8.2.jar', count: 11020 },
    { gav: 'io/javalin/javalin/6.1.3/javalin-6.1.3.jar', count: 8640 },
  ]
}

application
  .get("/", (req, res) => res.send("Reposilite stub API"))
  .use((req, res, next) => {
    console.log("Requested fake " + req.method + " " + req.url)
    res.setHeader("Access-Control-Allow-Origin", "*")
    res.setHeader("Access-Control-Allow-Headers", "*")
    res.setHeader(
      "Access-Control-Allow-Methods",
      "PUT, PATCH, POST, GET, HEAD, DELETE, OPTIONS"
    )
    next()
  })
  .use(express.text())
  .use(bodyParser.raw({ limit: '100mb', extended: true }))
  .use(bodyParser.json())
  .get('/api/settings/domains', (req, res) => res.send(['maven']))
  .get('/api/settings/schema/maven', (req, res) => res.send(mavenSettingsSchema))
  .get('/api/settings/domain/maven', (req, res) => res.send(mavenSettingsEntity))
  .put('/api/settings/domain/maven', (req, res) => { mavenSettingsEntity = req.body; res.send("") })
  .get('/api/tokens', (req, res) =>
    authorized(req, () => res.send(accessTokens), () => invalidCredentials(res))
  )
  .get('/api/tokens/:name', (req, res) =>
    authorized(req, () => {
      const token = accessTokens.find(entry => entry.name === req.params.name)
      token ? res.send(token) : res.status(404).send({ status: 404, message: "Token not found" })
    }, () => invalidCredentials(res))
  )
  .put('/api/tokens/:name', (req, res) =>
    authorized(req, () => {
      const name = req.params.name
      const existing = accessTokens.find(entry => entry.name === name)
      const secret = req.body.secret || crypto.randomBytes(24).toString("hex")
      const permissions = (req.body.permissions || []).map(permissionObject)
      const routes = (req.body.routes || []).flatMap(route =>
        (route.permissions || []).map(permission => ({ path: route.path, permission: routePermissionObject(permission) }))
      )
      const token = {
        identifier: existing ? existing.identifier : { type: req.body.type || "PERSISTENT", value: ++tokenSequence },
        name,
        createdAt: todayLocalDate(),
        description: req.body.description || "",
        expiresAt: req.body.expiresAt ? Math.floor(Date.parse(req.body.expiresAt) / 1000) : null,
        permissions,
        routes,
      }
      accessTokens = accessTokens.filter(entry => entry.name !== name).concat(token)
      res.send({ accessToken: token, permissions, routes, secret })
    }, () => invalidCredentials(res))
  )
  .patch('/api/tokens/:name', (req, res) =>
    authorized(req, () => {
      const name = req.params.name
      const existing = accessTokens.find(entry => entry.name === name)
      if (!existing) return res.status(404).send({ status: 404, message: "Token not found" })
      const token = { ...existing }
      if (req.body.description != null) token.description = req.body.description
      if (req.body.permissions != null) token.permissions = req.body.permissions.map(permissionObject)
      if (req.body.routes != null) token.routes = req.body.routes.flatMap(route =>
        (route.permissions || []).map(permission => ({ path: route.path, permission: routePermissionObject(permission) }))
      )
      if ('expiresAt' in req.body) token.expiresAt = req.body.expiresAt ? Math.floor(Date.parse(req.body.expiresAt) / 1000) : null
      accessTokens = accessTokens.filter(entry => entry.name !== name).concat(token)
      res.send(token)
    }, () => invalidCredentials(res))
  )
  .delete('/api/tokens/:name', (req, res) =>
    authorized(req, () => {
      accessTokens = accessTokens.filter(entry => entry.name !== req.params.name)
      res.send("")
    }, () => invalidCredentials(res))
  )
  .post('/api/tokens/:name/secret', (req, res) =>
    authorized(req, () => {
      const token = accessTokens.find(entry => entry.name === req.params.name)
      token
        ? res.send(crypto.randomBytes(24).toString("hex"))
        : res.status(404).send({ status: 404, message: "Token not found" })
    }, () => invalidCredentials(res))
  )
  .get(
    "/api/maven/details/snapshots",
    respond(createDirectoryDetails("/snapshot", []))
  )
  .get("/api/maven/details/private", (req, res) => {
    authorized(
      req,
      () =>
        res.send(
          createDirectoryDetails("/private", [createDirectoryDetails("1.0.0")])
        ),
      () => invalidCredentials(res)
    )
  })
  .get("/private/maven-metadata.xml", (req, res) => {
    authorized(
      req,
      () =>
        res.send(`
      <metadata>
        <groupId>default</groupId>
        <artifactId>private</artifactId>
        <versioning>
          <release>1.0.0</release>
          <versions>
            <version>1.0.0</version>
          </versions>
        </versioning>
      </metadata>
      `),
      () => invalidCredentials(res)
    )
  })
  .get(
    "/api/maven/details/filled",
    respond(
      createDirectoryDetails(
        "/filled",
        Array(80)
          .fill(undefined)
          .map(() => {
            const a = crypto.randomBytes(10).toString('hex').substring(0, 1 + Math.round(1 * Math.random()))
            const b = crypto.randomBytes(10).toString('hex').substring(0, 1 + Math.round(3 * Math.random()))
            const c = crypto.randomBytes(10).toString('hex').substring(0, 1 + Math.round(6 * Math.random()))
            const d = crypto.randomBytes(10).toString('hex').substring(0, 1 + Math.round(9 * Math.random()))
            return createDirectoryDetails(a + '-' + b + '-' + c + '-' + d)
          })
          .concat(
            Array(10)
              .fill(undefined)
              .map(() => createFileDetails(crypto.randomBytes(7).toString('hex'),"text/html", 4096))
          )
      )
    )
  )
  .get(
    "/api/maven/details/releases",
    respond(
      createDirectoryDetails("/releases", [createDirectoryDetails("gav")])
    )
  )
  .get(
    "/api/maven/details/releases/gav",
    respond(
      createDirectoryDetails("/releases/gav", [
        createDirectoryDetails("0.1.0"),
        createDirectoryDetails("1.0.0"),
        createFileDetails("maven-metadata.xml", "text/xml", 4096),
      ])
    )
  )
  .get(
    "/api/maven/details/releases/gav/1.0.0",
    respond(
      createDirectoryDetails("/releases/gav/1.0.0", [
        createFileDetails("gav-1.0.0.jar", "application/jar-archive", 1337),
        createFileDetails("gav-1.0.0.jar.md5", "text/plain", 5),
      ])
    )
  )
  .get(
    "/api/maven/details/releases/gav/0.1.0",
    respond(
      createDirectoryDetails("/releases/gav/0.1.0", [
        createFileDetails("gav-0.1.0.jar", "application/jar-archive", 1337),
      ])
    )
  )
  .get("/releases/gav/1.0.0/gav-1.0.0.jar", respond("content"))
  .get("/releases/gav/0.1.0/gav-0.1.0.jar", respond("content"))
  .get(
    "/releases/gav/maven-metadata.xml",
    respond(`
  <metadata>
    <groupId>g.a.v</groupId>
    <artifactId>gav</artifactId>
    <versioning>
      <release>1.0.0</release>
      <versions>
        <version>0.1.0</version>
        <version>1.0.0</version>
      </versions>
    </versioning>
  </metadata>
  `)
  )
  .get("/api/auth/me", (req, res) => {
    authorized(
      req,
      () =>
        res.send({
          accessToken: {
            id: 1,
            name: "name",
            createdAt: Date.now(),
            description: "Description",
          },
          permissions: [
            {
              identifier: "access-token:manager"
            }
          ],
          routes: [
            {
              path: "/",
              permission: {
                identifier: "route:read",
              },
            },
            {
              path: "/",
              permission: {
                identifier: "route:write",
              },
            },
          ],
        }),
      () => invalidCredentials(res)
    )
  })
  .get("/api/status/instance", (req, res) => {
    authorized(
      req,
      () => {
        res.send({
          version: '3.2.0',
          latestVersion: '<unknown>',
          uptime: uptime,
          usedMemory: memory,
          maxMemory: '32',
          usedThreads: threads,
          maxThreads: 64,
          failuresCount: failures.length
        })
      },
      () => invalidCredentials(res)
    )
  })
  .get("/api/status/snapshots", (req, res) => {
    authorized(
      req,
      () => {
        res.send([
          {
            at: new Date().getTime() - (1000 * 60),
            memory: 20,
            threads: 11
          },
          {
            at: new Date().getTime(),
            memory: 10,
            threads: 5
          }
        ])
      },
      () => invalidCredentials(res)
    )
  })
  .get("/api/status/failures", (req, res) => {
    authorized(
      req,
      () => res.send(failures),
      () => invalidCredentials(res)
    )
  })
  .get("/api/status/health", (req, res) => res.send({ status: "UP" }))
  .get("/api/statistics/resolved/all", (req, res) => {
    authorized(
      req,
      () =>
        res.send({
          statisticsEnabled: true,
          repositories: statisticsSeries
        }),
      () => invalidCredentials(res)
    )
  })
  .get("/api/statistics/resolved/unique", (req, res) => {
    authorized(
      req,
      () => res.send(Object.values(resolvedArtifacts).flat().length),
      () => invalidCredentials(res)
    )
  })
  .get(/^\/api\/statistics\/resolved\/phrase\/(\d+)\/([^/]+)\/(.*)$/, (req, res) => {
    authorized(
      req,
      () => {
        const limit = parseInt(req.params[0], 10) || 20
        const phrase = req.params[2].toLowerCase()
        const pool = resolvedArtifacts[req.params[1]] || []
        const requests = pool
          .filter(entry => entry.gav.toLowerCase().includes(phrase))
          .sort((a, b) => b.count - a.count)
          .slice(0, limit)
        res.send({
          sum: requests.reduce((sum, entry) => sum + entry.count, 0),
          requests
        })
      },
      () => invalidCredentials(res)
    )
  })
  .ws("/api/console/sock", (connection) => {
    let authenticated = false

    connection.on("message", (message) => {
      if (message == "Authorization:name:secret") {
        sendMessage(connection, "DEBUG | Authorized")
        authenticated = true
      }

      if (!authenticated || message == "stop") {
        sendMessage(connection, "Connection closed")
        connection.close()
        return
      }

      sendMessage(connection, "INFO | Response: " + message)
    })
  })
  .get("/api/maven/details", (req, res) => {
    const repositories = createDirectoryDetails("/", [
      createDirectoryDetails("releases"),
      createDirectoryDetails("snapshots"),
      createDirectoryDetails("filled"),
    ])
    authorized(req, () =>
      repositories.files.push(createDirectoryDetails("private"))
    )
    res.send(repositories)
  })
  .put(/.*/, (req, res) => {
    authorized(
      req,
      () => {
        uploadedFiles.push({
          file: req.url,
          content: req.body
        })
        console.log(`File ${req.url} has been uploaded`)
      },
      () => invalidCredentials(res)
    )
  })
  .delete(/.*/, (req, res) => {
    authorized(
      req,
      () => {
        uploadedFiles = uploadedFiles.filter(entry => entry.file == req.url)
        console.log(`File ${req.url} has been deleted`)
      },
      () => invalidCredentials(res)
    )
  })
  .get(/.*/, (req, res) =>
    res.status(404).send({
      status: 404,
      message: "Not found",
    })
  )
  .listen(8887)

console.log("Reposilite stub API started on port 8887")
