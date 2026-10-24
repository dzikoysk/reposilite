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
import { ref, computed } from 'vue'
import { useClipboard } from '@vueuse/core'
import { createErrorToast, createSuccessToast, createWarningToast } from '../../helpers/toast'
import { useSession } from '../../store/session'
import { useTokens } from '../../store/tokens'
import CopiedIcon from '../icons/CopiedIcon.vue'
import CopyIcon from '../icons/CopyIcon.vue'
import TrashIcon from '../icons/TrashIcon.vue'
import ViewHeader from '../util/ViewHeader.vue'

const { client, details, token: session } = useSession()
const { groupRoutes, toMs, errorMessage } = useTokens()
const { copy: copyText } = useClipboard()

const tokens = ref([])
const label = ref('')
// paths of the user are the maximum, every new token may get all or a subset of them
const allowedRoutes = computed(() => groupRoutes(details.value || {}))
const selected = ref(Object.fromEntries(allowedRoutes.value.map(route => [route.path, { read: route.read, write: route.write }])))
const created = ref(null)
const copied = ref(false)
const confirming = ref(null)

const labelOf = (token) => token.name.substring(token.name.indexOf('#') + 1)
const formatDate = (value) => value ? new Date(toMs(value)).toLocaleDateString() : 'never'
const isExpired = (token) => token.expiresAt && toMs(token.expiresAt) < Date.now()

const fetchMyTokens = () =>
  client.value.auth.myTokens()
    .then(response => { tokens.value = response.data })
    .catch(error => createErrorToast(errorMessage(error)))

const create = () => {
  const value = label.value.trim()
  if (!/^[A-Za-z0-9._-]{1,32}$/.test(value)) { createWarningToast("Label must have 1-32 characters: letters, digits, '.', '_' or '-'"); return }
  const routes = Object.entries(selected.value)
    .filter(([, permissions]) => permissions.read || permissions.write)
    .map(([path, permissions]) => ({ path, permissions: [permissions.read && 'r', permissions.write && 'w'].filter(Boolean) }))
  client.value.auth.createMyToken(value, routes)
    .then(response => {
      copied.value = false
      created.value = { name: response.data.accessToken.name, secret: response.data.secret }
      label.value = ''
      return fetchMyTokens()
    })
    .catch(error => createErrorToast(errorMessage(error)))
}

const revoke = (token) =>
  client.value.auth.deleteMyToken(labelOf(token))
    .then(() => createSuccessToast(`Token '${labelOf(token)}' revoked`))
    .then(() => { confirming.value = null; return fetchMyTokens() })
    .catch(error => createErrorToast(errorMessage(error)))

const copySecret = async () => {
  await copyText(created.value.secret)
  copied.value = true
}

const mavenSnippet = computed(() => created.value && `<server>
  <id>reposilite</id>
  <username>${created.value.name}</username>
  <password>${created.value.secret}</password>
</server>`)

const gradleSnippet = computed(() => created.value && `reposiliteUsername=${created.value.name}
reposilitePassword=${created.value.secret}`)

fetchMyTokens()
</script>

<template>
  <div class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Tokens"
      :description="`Tokens of ${session.name} for Maven, Gradle and other build tools.`"
    >
      <template #note>
        The secret is shown only once. Tokens expire automatically, create a new one when it does.
      </template>
    </ViewHeader>

    <div class="overflow-hidden rounded-lg bg-white text-sm text-gray-600 dark:bg-gray-900 dark:text-gray-300">
      <form
        class="flex flex-col gap-3 border-b border-gray-200 px-4.5 py-3.5 dark:border-gray-800"
        @submit.prevent="create"
      >
        <div class="flex flex-wrap items-center gap-2">
          <input
            v-model="label"
            class="h-9 min-w-48 flex-1 rounded-md border border-gray-300 bg-white px-3 text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400"
            placeholder="Label, e.g. laptop"
            aria-label="Token label"
            maxlength="32"
          >
          <button
            type="submit"
            class="h-9 whitespace-nowrap rounded-md bg-blue-700 px-3.5 font-medium text-white hover:bg-blue-800 <sm:w-full"
          >
            + Create token
          </button>
        </div>
        <div
          v-if="allowedRoutes.length > 0"
          class="flex flex-wrap gap-x-5 gap-y-1.5"
          role="group"
          aria-label="Paths of the new token"
        >
          <span
            v-for="route in allowedRoutes"
            :key="route.path"
            class="inline-flex items-center gap-2"
          >
            <code class="font-mono text-gray-800 dark:text-gray-100">{{ route.path }}</code>
            <label
              v-if="route.read"
              class="inline-flex items-center gap-1"
            ><input
              v-model="selected[route.path].read"
              type="checkbox"
            > read</label>
            <label
              v-if="route.write"
              class="inline-flex items-center gap-1"
            ><input
              v-model="selected[route.path].write"
              type="checkbox"
            > write</label>
          </span>
        </div>
        <p
          v-else
          class="text-gray-500"
        >
          You have no paths assigned yet, ask a manager to grant you access.
        </p>
      </form>

      <div
        v-if="created"
        class="flex flex-col gap-2 border-b border-gray-200 bg-blue-50 px-4.5 py-3 text-blue-900 dark:border-gray-800 dark:bg-blue-900 dark:text-blue-100"
        role="status"
      >
        <div class="flex flex-wrap items-center gap-2">
          <span>Secret of <strong>{{ created.name }}</strong>:</span>
          <code class="min-w-0 break-all font-mono">{{ created.secret }}</code>
          <button
            type="button"
            class="inline-flex items-center gap-1 rounded-md bg-blue-700 px-2.5 py-1 text-xs font-medium text-white hover:bg-blue-800 dark:bg-blue-600 dark:hover:bg-blue-500"
            :aria-label="copied ? 'Secret copied' : 'Copy secret'"
            @click="copySecret"
          >
            <CopiedIcon
              v-if="copied"
              class="w-4 h-4"
              aria-hidden="true"
            />
            <CopyIcon
              v-else
              class="w-4 h-4"
              aria-hidden="true"
            />
            {{ copied ? 'Copied' : 'Copy' }}
          </button>
          <button
            type="button"
            class="text-xs underline"
            @click="created = null"
          >
            Dismiss
          </button>
        </div>
        <div class="grid grid-cols-2 gap-3 <sm:grid-cols-1">
          <div>
            <div class="text-xs font-semibold">
              Maven: ~/.m2/settings.xml
            </div>
            <pre class="overflow-x-auto rounded-md bg-white p-2 text-xs text-gray-800 dark:bg-gray-900 dark:text-gray-100">{{ mavenSnippet }}</pre>
          </div>
          <div>
            <div class="text-xs font-semibold">
              Gradle: ~/.gradle/gradle.properties
            </div>
            <pre class="overflow-x-auto rounded-md bg-white p-2 text-xs text-gray-800 dark:bg-gray-900 dark:text-gray-100">{{ gradleSnippet }}</pre>
          </div>
        </div>
      </div>

      <p
        v-if="tokens.length === 0"
        class="px-4.5 py-3 text-gray-500"
      >
        You have no tokens yet.
      </p>
      <div
        v-for="token in tokens"
        :key="token.name"
        class="flex min-h-11.5 items-center gap-3 border-b border-gray-200 px-4.5 py-2 hover:bg-gray-50 dark:border-gray-800 dark:hover:bg-gray-800 <sm:flex-wrap"
      >
        <span class="font-semibold text-gray-800 dark:text-gray-100">{{ labelOf(token) }}</span>
        <code
          class="min-w-0 flex-1 truncate font-mono text-xs text-gray-500"
          :title="token.name"
        >{{ token.name }}</code>
        <span class="whitespace-nowrap text-xs text-gray-500 dark:text-gray-400">
          created {{ formatDate(token.createdAt) }} ·
          <span :class="{ 'text-red-500': isExpired(token) }">{{ isExpired(token) ? 'expired' : 'expires' }} {{ formatDate(token.expiresAt) }}</span>
        </span>
        <template v-if="confirming === token.name">
          <button
            type="button"
            class="text-xs font-medium text-red-600 dark:text-red-400"
            @click="revoke(token)"
          >
            Revoke
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
          title="Revoke token"
          :aria-label="`Revoke ${labelOf(token)} token`"
          @click="confirming = token.name"
        >
          <TrashIcon
            class="h-4 w-4"
            aria-hidden="true"
          />
        </button>
      </div>
    </div>
  </div>
</template>
