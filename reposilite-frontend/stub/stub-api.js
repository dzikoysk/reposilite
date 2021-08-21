const express = require('express')

const [
  respond,
  basicAuth,
  authorized,
  invalidCredentials,
  createFileDetails,
  createDirectoryDetails
] = require('./extensions')

express()
  .get('/', (req, res) =>
    res.send('Reposilite stub API')
  )
  .use((req, res, next) => {
    console.log('Requested fake ' + req.url)
    res.setHeader('Access-Control-Allow-Origin', '*')
    res.setHeader('Access-Control-Allow-Headers', '*')
    next()
  })
  .get('/auth/me', (req, res) => {
    authorized(req,
      () => res.send({
        id: 1,
        alias: 'alias',
        createdAt: Date.now(),
        permissions: ['access-token:manager'],
        routes: [{ path: '/', permissions: [ 'route:read', 'route:write' ] }]
      }),
      () => invalidCredentials(res)
    )
   })
  .get('/api/maven/details', (req, res) => {
    const repositories = createDirectoryDetails('/', [
      createDirectoryDetails('releases'),
      createDirectoryDetails('snapshots')
    ])

    authorized(req,
      () => repositories.files.push(createDirectoryDetails('private'))
    )
    
    res.send(repositories)
  })
  .get('/api/maven/details/snapshots', respond(
    createDirectoryDetails('/snapshot', [])
  ))
  .get('/api/maven/details/private', (req, res) => {
    authorized(req,
      () => res.send(createDirectoryDetails('/private', [
        createDirectoryDetails("something")
      ])),
      () => invalidCredentials(res)
    )
  })
  .get('/api/maven/details/releases', respond(
    createDirectoryDetails('/releases', [
      createDirectoryDetails('gav')
    ])
  ))
  .get('/api/maven/details/releases/gav', respond(
    createDirectoryDetails('/releases/gav', [
      createFileDetails('gav.jar')
    ])
  ))
  .listen(80)

console.log('Reposilite stub API started on port 80')