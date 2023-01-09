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
} = require("./extensions")

const application = express()
expressWs(application)

let uploadedFiles = []
let mavenSettingsSchema = require('./maven-settings-schema.json')
let mavenSettingsEntity = require('./maven-settings-entity.json')

let uptime = 1000
let memory = 20
let threads = 10
let failures = 0

setInterval(() => {
  memory += Math.random() * 10
  threads += 1
  uptime += 5000
  failures += 1
}, 5000)

const statisticsSeries = [
  {
    name: 'Releases',
    data: generateDayWiseTimeSeries(new Date('11 Feb 2022 GMT').getTime(), 20, {
      min: 10,
      max: 60
    })
  },
  {
    name: 'Snapshots',
    data: generateDayWiseTimeSeries(new Date('11 Feb 2022 GMT').getTime(), 20, {
      min: 10,
      max: 20
    })
  },
  {
    name: 'Maven Central',
    data: generateDayWiseTimeSeries(new Date('11 Feb 2022 GMT').getTime(), 20, {
      min: 10,
      max: 15
    })
  }
]

application
  .get("/", (req, res) => res.send("Reposilite stub API"))
  .use((req, res, next) => {
    console.log("Requested fake " + req.method + " " + req.url)
    res.setHeader("Access-Control-Allow-Origin", "*")
    res.setHeader("Access-Control-Allow-Headers", "*")
    res.setHeader(
      "Access-Control-Allow-Methods",
      "PUT, POST, GET, HEAD, DELETE, OPTIONS"
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
          permissions: [{ identifier: "access-token:manager" }],
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
          failuresCount: failures
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
  .put("*", (req, res) => {
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
  .delete("*", (req, res) => {
    authorized(
      req,
      () => {
        uploadedFiles = uploadedFiles.filter(entry => entry.file == req.url)
        console.log(`File ${req.url} has been deleted`)
      },
      () => invalidCredentials(res)
    )
  })
  .get("*", (req, res) =>
    res.status(404).send({
      status: 404,
      message: "Not found",
    })
  )
  .listen(8080)

console.log("Reposilite stub API started on port 80")
