<!--
  - Copyright (c) 2020-2026 dzikoysk
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup>
import { ref, computed, watchEffect } from 'vue'
import { createErrorToast, createSuccessToast, createWarningToast } from '../../helpers/toast'
import { useSession } from '../../store/session'
import { useTokens } from '../../store/tokens'
import TrashIcon from '../icons/TrashIcon.vue'
import ViewHeader from '../util/ViewHeader.vue'

const { client } = useSession()
const { tokens, fetchTokens, saveRoute, removeRoute, deleteToken, groupRoutes, tokenIsManager, errorMessage } = useTokens()

// a user is a token named after the e-mail, its paths are the maximum for tokens the user creates (user#label)
const users = computed(() => tokens.value.filter(token => token.name.includes('@') && !token.name.includes('#')))
const tokensOf = (user) => tokens.value.filter(token => token.name.startsWith(`${user.name}#`))

const newUser = ref({ email: '', path: '', read: true, write: false })
const newPath = ref({})
const confirming = ref(null)

const normalizePath = (path) => {
  const value = (path || '').trim()
  return value === '' || value.startsWith('/') ? value : '/' + value
}

const routesOf = (draft) => {
  const path = normalizePath(draft.path)
  if (path === '' || (!draft.read && !draft.write)) return []
  return [{ path, permissions: [draft.read && 'r', draft.write && 'w'].filter(Boolean) }]
}

const createUser = () => {
  const email = newUser.value.email.trim().toLowerCase()
  if (!/^[^\s:#@]+@[^\s:#@]+$/.test(email)) { createWarningToast('Enter a valid e-mail address'); return }
  if (tokens.value.some(token => token.name === email)) { createWarningToast(`User '${email}' already exists`); return }
  client.value.tokens.createOrUpdate(email, {
    type: 'PERSISTENT', secretType: 'RAW', secret: null, description: 'OIDC user', permissions: [], routes: routesOf(newUser.value), expiresAt: null,
  })
    .then(() => fetchTokens())
    .then(() => { createSuccessToast(`User '${email}' created`); newUser.value = { email: '', path: '', read: true, write: false } })
    .catch(error => createErrorToast(errorMessage(error)))
}

const addPath = (user) => {
  const draft = newPath.value[user.name]
  const path = normalizePath(draft.path)
  if (path === '') return
  if (!draft.read && !draft.write) { createWarningToast('Select read and/or write'); return }
  saveRoute(user, { path, read: !!draft.read, write: !!draft.write })
    .then(ok => { if (ok) newPath.value[user.name] = { path: '', read: true, write: false } })
}

const deleteUser = (user) =>
  Promise.all(tokensOf(user).map(token => client.value.tokens.delete(token.name)))
    .then(() => deleteToken(user.name))
    .then(() => { confirming.value = null })
    .catch(error => createErrorToast(errorMessage(error)))

watchEffect(() => users.value.forEach(user => {
  if (!newPath.value[user.name]) newPath.value[user.name] = { path: '', read: true, write: false }
}))

fetchTokens()
</script>

<template>
  <div class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Users"
      description="Users sign in with OpenID Connect and create their own tokens."
    >
      <template #note>
        Paths of a user are the maximum for every token the user creates, removing a path applies to these tokens immediately.
      </template>
    </ViewHeader>

    <div class="overflow-hidden rounded-lg bg-white text-sm text-gray-600 dark:bg-gray-900 dark:text-gray-300">
      <form
        class="flex flex-wrap items-center gap-2 border-b border-gray-200 px-4.5 py-3.5 dark:border-gray-800"
        @submit.prevent="createUser"
      >
        <input
          v-model="newUser.email"
          type="email"
          class="h-9 min-w-56 flex-1 rounded-md border border-gray-300 bg-white px-3 text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400"
          placeholder="E-mail, e.g. john@company.com"
          aria-label="E-mail of the new user"
        >
        <input
          v-model="newUser.path"
          class="h-9 min-w-40 rounded-md border border-gray-300 bg-white px-3 font-mono text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400"
          placeholder="/releases"
          aria-label="First path of the new user"
        >
        <label class="inline-flex items-center gap-1"><input
          v-model="newUser.read"
          type="checkbox"
        > read</label>
        <label class="inline-flex items-center gap-1"><input
          v-model="newUser.write"
          type="checkbox"
        > write</label>
        <button
          type="submit"
          class="h-9 whitespace-nowrap rounded-md bg-blue-700 px-3.5 font-medium text-white hover:bg-blue-800 <sm:w-full"
        >
          + Add user
        </button>
      </form>

      <p
        v-if="users.length === 0"
        class="px-4.5 py-3 text-gray-500"
      >
        No users yet. Users are also created automatically on their first sign-in, without paths.
      </p>
      <div
        v-for="user in users"
        :key="user.name"
        class="flex flex-col gap-2 border-b border-gray-200 px-4.5 py-3 dark:border-gray-800"
      >
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-semibold text-gray-800 dark:text-gray-100">{{ user.name }}</span>
          <span
            v-if="tokenIsManager(user)"
            class="rounded-full bg-blue-100 px-1.5 py-0.5 text-[0.7rem] text-blue-700 dark:bg-blue-900 dark:text-blue-200"
          >manager</span>
          <span class="flex-1 text-xs text-gray-500">{{ tokensOf(user).length }} token(s)</span>
          <template v-if="confirming === user.name">
            <button
              type="button"
              class="text-xs font-medium text-red-600 dark:text-red-400"
              @click="deleteUser(user)"
            >
              Delete user and tokens
            </button>
            <button
              type="button"
              class="text-xs text-gray-500 dark:text-gray-400"
              @click="confirming = null"
            >
              Cancel
            </button>
          </template>
          <button
            v-else
            type="button"
            class="text-gray-500 hover:text-red-600 dark:text-gray-400 dark:hover:text-red-400"
            title="Delete user"
            :aria-label="`Delete user ${user.name}`"
            @click="confirming = user.name"
          >
            <TrashIcon
              class="h-4 w-4"
              aria-hidden="true"
            />
          </button>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <span
            v-for="route in groupRoutes(user)"
            :key="route.path"
            class="inline-flex items-center gap-1.5 rounded-md bg-gray-100 px-2 py-1 dark:bg-gray-800"
          >
            <code class="font-mono text-gray-800 dark:text-gray-100">{{ route.path }}</code>
            <span class="text-xs">{{ [route.read && 'read', route.write && 'write'].filter(Boolean).join(' + ') }}</span>
            <button
              type="button"
              class="text-gray-400 hover:text-red-600"
              :aria-label="`Remove ${route.path} from ${user.name}`"
              @click="removeRoute(user, route.path)"
            >×</button>
          </span>
          <form
            v-if="newPath[user.name]"
            class="inline-flex flex-wrap items-center gap-1.5"
            @submit.prevent="addPath(user)"
          >
            <input
              v-model="newPath[user.name].path"
              class="h-7 w-36 rounded-md border border-gray-300 bg-white px-2 font-mono text-xs text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
              placeholder="/path"
              :aria-label="`New path for ${user.name}`"
            >
            <label class="inline-flex items-center gap-1 text-xs"><input
              v-model="newPath[user.name].read"
              type="checkbox"
            > r</label>
            <label class="inline-flex items-center gap-1 text-xs"><input
              v-model="newPath[user.name].write"
              type="checkbox"
            > w</label>
            <button
              type="submit"
              class="h-7 rounded-md border border-gray-300 px-2 text-xs dark:border-gray-700"
            >
              Add path
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
