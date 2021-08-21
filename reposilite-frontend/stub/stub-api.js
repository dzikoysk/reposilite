const express = require('express')

const respond = (response) =>
  (_, res) => res.send(response)

const basicAuth = (req) => {
  const b64auth = (req.get('Authorization') || '').split(' ')[1] || ''
  return Buffer.from(b64auth, 'base64').toString().split(':')
}

const createFileDetails = (name) =>
  ({ type: 'FILE', name })

const createDirectoryDetails = (name, files) =>
  ({ type: 'DIRECTORY', name, files })

express()
  .get('/', (req, res) =>
    res.send('Reposilite stub API')
  )
  .use((_, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*')
    res.setHeader('Access-Control-Allow-Headers', '*')
    next()
  })
  .get('/auth/me', (req, res) => {
    const [login, password] = basicAuth(req)

    if (login != 'alias' || password != 'secret') {
      res.status(401).send('Invalid credentials')
      return
    }

    res.send({
      id: 1,
      alias: 'alias',
      createdAt: Date.now(),
      permissions: ['access-token:manager'],
      routes: [{ path: '/', permissions: [ 'route:read', 'route:write' ] }]
    })
  })
  .get('/api/maven/details', respond(
    createDirectoryDetails('/', [
      createDirectoryDetails('releases'),
      createDirectoryDetails('snapshots')
    ])
  ))
  .get('/api/maven/details/snapshots', respond(
    createDirectoryDetails('/snapshot', [])
  ))
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