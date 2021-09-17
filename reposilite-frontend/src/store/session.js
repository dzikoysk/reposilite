/*
 * Copyright (c) 2021 dzikoysk
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

import { reactive } from "vue"
import { createClient } from './client'

const defaultValue = ''
const nameKey = 'session-token-name'
const secretKey = 'session-token-secret'
const managerPermission = 'access-token:manager'
const readPermission = 'route:read'
const writePermission = 'route:write'

const token = reactive({
  name: defaultValue,
  secret: defaultValue
})

const defaultTokenInfo = {
  id: defaultValue,
  name: defaultValue,
  createdAt: defaultValue,
  permissions: [],
  routes: []
}

const session = reactive({
  tokenInfo: defaultTokenInfo
})

export default function useSession() {
  // move to session
  const updateToken = (name, secret) => {
    localStorage.setItem(nameKey, name)
    token.name = name
    localStorage.setItem(secretKey, secret)
    token.secret = secret
  }

  const logout = () => {
    updateToken(defaultValue, defaultValue)
    session.tokenInfo = defaultTokenInfo
  }

  const login = async (name, secret) => {
    try {
      const { client } = createClient()

      if (name == defaultValue) {
        throw new Error("Missing credentials")
      }

      const response = await client.auth.me(name, secret)
      updateToken(name, secret)
      session.tokenInfo = response.data
      return { token, session }
    } catch (error) {
      logout()
      throw error
    }
  }

  const fetchSession = () => {
    return login(
      localStorage.getItem(nameKey),
      localStorage.getItem(secretKey)
    )
  }

  const isLogged = () =>
    token.name != defaultValue

  const isManager = (tokenInfo) =>
    tokenInfo?.permissions?.find(entry => entry.identifier == managerPermission)
  
  return {
    token,
    session,
    login,
    logout,
    fetchSession,
    isLogged,
    isManager
  }
}