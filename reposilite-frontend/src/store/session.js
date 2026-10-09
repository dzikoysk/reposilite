/*
 * Copyright (c) 2020-2026 dzikoysk
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

import { computed, ref, watchEffect } from "vue"
import { createClient } from './client'
import usePlaceholders from './placeholders'

const { oauth2Provider } = usePlaceholders()

const token = ref({
  name: localStorage.getItem('token-name') || '',
  secret: localStorage.getItem('token-secret') || '',
})
const setToken = (name, secret) => {
  token.value = { name, secret }
}

watchEffect(() => {
  localStorage.setItem('token-name', token.value.name)
  localStorage.setItem('token-secret', token.value.secret)
})

/**
 * @type {(undefined|{
 *    accessToken: {
 *      identifier: {
 *        type: string,
 *        value: number
 *      },
 *      name: string,
 *      createdAt: string,
 *      description: string
 *    },
 *    permissions: {
 *      identifier: string,
 *      shortcut: string
 *    }[],
 *    routes: {
 *      path: string,
 *      permission: {
 *        identifier: string,
 *        shortcut: string
*       }
 *    }[]
 *  })}
 */
const defaultDetails = undefined
const details = ref(defaultDetails)

const logout = () => {
  if (oauth2Provider && isLogged.value) {
    client.value.auth.oauth2.closeSession().catch(() => {})
  }
  setToken('', '')
  details.value = undefined
}

const login = (name, secret) =>
  createClient(name, secret)
    .auth
    .me()
    .then(response => {
      setToken(name, secret)
      details.value = response.data
    })

const client = computed(() => createClient(token.value.name, token.value.secret))

const takeQueryParameter = (name) => {
  const url = new URL(window.location.href)
  const value = url.searchParams.get(name)
  if (value !== null) {
    url.searchParams.delete(name)
    window.history.replaceState(window.history.state, '', url)
  }
  return value
}

const oauth2Error = (message) =>
  Promise.reject({ oauth2: true, message })

// Identity provider redirects back to the dashboard with one-time code that has to be exchanged for session credentials
const initializeSession = () => {
  const error = takeQueryParameter('oauth2_error')
  const code = takeQueryParameter('oauth2_code')

  if (error) {
    return oauth2Error(error)
  }

  if (code) {
    return createClient()
      .auth
      .oauth2
      .openSession(code)
      .then(response => login(response.data.name, response.data.secret))
      .catch(error => oauth2Error(error.response?.data?.message || error.message))
  }

  return login(token.value.name, token.value.secret)
}

const isLogged = computed(() => details.value !== undefined)

const isManager = computed(() => {
  return details.value
    ?.permissions
    ?.some(entry => entry.identifier === 'access-token:manager') === true
})

const hasPermissionTo = (path, permission) => {
  if (isManager.value) {
    return true
  }
  return details.value
    ?.routes
    ?.find(route => path.startsWith(route.path) && route.permission.identifier === permission) !== undefined
}

export function useSession() {
  return {
    token,
    details,
    login,
    logout,
    isLogged,
    client,
    isManager,
    hasPermissionTo,
    initializeSession
  }
}
