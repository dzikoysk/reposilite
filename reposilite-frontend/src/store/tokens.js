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

import { ref } from 'vue'
import { useSession } from './session'
import { createSuccessToast, createErrorToast } from '../helpers/toast'

const { client } = useSession()
const tokens = ref([])

const errorMessage = (error) =>
  error?.response?.data?.message || `${error}`

const tokenIsManager = (token) =>
  (token.permissions || []).some(permission => permission.identifier === 'access-token:manager')

const groupRoutes = (token) => {
  const byPath = {}
  ;(token.routes || []).forEach(route => {
    const entry = byPath[route.path] || { path: route.path, read: false, write: false }
    if (route.permission?.identifier === 'route:read') entry.read = true
    if (route.permission?.identifier === 'route:write') entry.write = true
    byPath[route.path] = entry
  })
  return Object.values(byPath)
}

const groupedToRequestRoutes = (grouped) =>
  grouped
    .filter(route => route.read || route.write)
    .map(route => ({ path: route.path, permissions: [route.read && 'r', route.write && 'w'].filter(Boolean) }))

const toMs = (value) =>
  Array.isArray(value) ? new Date(value[0], value[1] - 1, value[2]).getTime()
    : typeof value === 'number' ? (value < 1e12 ? value * 1000 : value)
    : new Date(value).getTime()

const toUpdateRequest = (token) => ({
  description: token.description || '',
  permissions: tokenIsManager(token) ? ['access-token:manager'] : [],
  routes: groupedToRequestRoutes(groupRoutes(token)),
  expiresAt: token.expiresAt ? new Date(toMs(token.expiresAt)).toISOString() : null,
})

const fetchTokens = () =>
  client.value.tokens.list()
    .then(response => { tokens.value = response.data })
    .catch(error => createErrorToast(errorMessage(error)))

const createToken = (name, { type, description = '' }) =>
  client.value.tokens.createOrUpdate(name, {
    type, secretType: 'RAW', secret: null, description, permissions: [], routes: [], expiresAt: null,
  })
    .then(response => fetchTokens().then(() => response.data))

const saveToken = (token, overrides, successMessage) =>
  client.value.tokens.update(token.name, { ...toUpdateRequest(token), ...overrides })
    .then(() => fetchTokens())
    .then(() => { createSuccessToast(successMessage); return true })
    .catch(error => { createErrorToast(errorMessage(error)); return false })

const saveTokenMeta = (token, { description, manager, expiresAt }) =>
  saveToken(token, {
    description: description.trim(),
    permissions: manager ? ['access-token:manager'] : [],
    expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
  }, `Token '${token.name}' updated`)

const saveRoute = (token, route, originalPath) => {
  const replaced = new Set([route.path, originalPath].filter(Boolean))
  return saveToken(token, {
    routes: groupedToRequestRoutes([
      ...groupRoutes(token).filter(existing => !replaced.has(existing.path)),
      route,
    ]),
  }, `Route '${route.path}' saved`)
}

const removeRoute = (token, path) =>
  saveToken(token, {
    routes: groupedToRequestRoutes(groupRoutes(token).filter(route => route.path !== path)),
  }, `Route '${path}' removed`)

const deleteToken = (name) =>
  client.value.tokens.delete(name)
    .then(() => fetchTokens())
    .then(() => createSuccessToast(`Token '${name}' has been deleted`))
    .catch(error => createErrorToast(errorMessage(error)))

const regenerateSecret = (name) =>
  client.value.tokens.regenerateSecret(name).then(response => response.data)

export function useTokens() {
  return {
    tokens,
    fetchTokens,
    createToken,
    saveTokenMeta,
    saveRoute,
    removeRoute,
    deleteToken,
    regenerateSecret,
    groupRoutes,
    tokenIsManager,
    toMs,
    errorMessage
  }
}
