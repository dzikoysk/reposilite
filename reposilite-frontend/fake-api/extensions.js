/*
 * Copyright (c) 2022 dzikoysk
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
  connection.send(`${new Date().toDateString()} ${message}`)

const createFileDetails = (name, contentType, contentLength) =>
  ({ type: 'FILE', name, contentType, contentLength })

const createDirectoryDetails = (name, files) =>
  ({ type: 'DIRECTORY', name, files })

module.exports = {
  respond,
  basicAuth,
  authorized,
  invalidCredentials,
  sendMessage,
  createFileDetails,
  createDirectoryDetails,
}