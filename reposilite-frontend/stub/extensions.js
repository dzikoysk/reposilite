const respond = (response) =>
  (_, res) => res.send(response)

const basicAuth = (req) => {
  const b64auth = (req.get('Authorization') || '').split(' ')[1] || ''
  return Buffer.from(b64auth, 'base64').toString().split(':')
}

const authorized = (req, success, failure) => {
  const [login, password] = basicAuth(req)

  if (login == 'name' && password == 'secret') {
    console.log('Authorization successful for request ' + req.url)
    success()
  } else failure && failure()
}

const invalidCredentials = (res) =>
  res.status(401).send('Invalid credentials')

const sendMessage = (connection, message) =>
  connection.send(new Date().toDateString() + " | " + message)

const createFileDetails = (name) =>
  ({ type: 'FILE', name })

const createDirectoryDetails = (name, files) =>
  ({ type: 'DIRECTORY', name, files })

module.exports = [
  respond,
  basicAuth,
  authorized,
  invalidCredentials,
  sendMessage,
  createFileDetails,
  createDirectoryDetails,
]