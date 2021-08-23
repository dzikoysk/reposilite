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
    console.log('Requested fake ' + req.method + ' ' + req.url)
    res.setHeader('Access-Control-Allow-Origin', '*')
    res.setHeader('Access-Control-Allow-Headers', '*')
    next()
  })
  .get('/api/auth/me', (req, res) => {
    authorized(req,
      () => res.send({
        id: 1,
        name: 'name',
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
  .get('/releases/gav/gav.jar', respond('content'))
  .get('*', (req, res) => res.status(404).send({
    status: 404,
    message: 'Not found'
  }))
  .listen(80)

console.log('Reposilite stub API started on port 80')