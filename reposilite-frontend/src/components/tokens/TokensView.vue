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
import { ref, computed, watch } from 'vue'
import { createErrorToast, createWarningToast } from '../../helpers/toast'
import { useTokens } from '../../store/tokens'
import { property } from '../../helpers/vue-extensions'
import PencilIcon from '../icons/PencilIcon.vue'
import TrashIcon from '../icons/TrashIcon.vue'
import RefreshIcon from '../icons/RefreshIcon.vue'
import ViewHeader from '../util/ViewHeader.vue'

const props = defineProps({
  selectedTab: property(String, true)
})

const { tokens, fetchTokens, createToken, saveTokenMeta, saveRoute, removeRoute, deleteToken, regenerateSecret, groupRoutes, tokenIsManager, toMs, errorMessage } = useTokens()

watch(
  () => props.selectedTab,
  (selectedTab, prev) => {
    if (selectedTab === 'Tokens' && prev === undefined)
      fetchTokens()
  },
  { immediate: true }
)

const query = ref('')
const editing = ref(null)
const draft = ref({})
const secret = ref(null)
const confirming = ref(null)

const tid = (token) => token.name
const isOpen = (key) => editing.value === key
const close = () => { editing.value = null }

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return tokens.value
  return tokens.value.filter(token =>
    (token.name + ' ' + (token.description || '')).toLowerCase().includes(q) ||
    (token.routes || []).some(route => route.path.toLowerCase().includes(q)))
})

const formatDate = (value) => value == null ? null : new Date(toMs(value)).toLocaleDateString(undefined, { timeZone: 'UTC' })
const toDateInput = (value) => {
  if (!value) return ''
  const date = new Date(toMs(value))
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`
}
const minExpiry = () => toDateInput(Date.now())
const routeLabel = (route) => [route.read && 'read', route.write && 'write'].filter(Boolean).join(' · ')

const DAY = 86400000
const rel = (ms) => {
  const days = Math.floor(Math.abs(Date.now() - ms) / DAY)
  if (days < 1) return 'today'
  if (days < 30) return days + 'd'
  if (days < 365) { const m = Math.floor(days / 30); return m + (m === 1 ? ' month' : ' months') }
  return Math.floor(days / 365) + 'y'
}
const ageOf = (token) => { const r = rel(toMs(token.createdAt)); return r === 'today' ? 'new' : r + ' old' }
const isExpired = (token) => !!token.expiresAt && Date.now() > toMs(token.expiresAt)
const expiryOf = (token) => !token.expiresAt ? '∞' : (isExpired(token) ? 'expired' : rel(toMs(token.expiresAt)) + ' left')
const datesTitle = (token) => `Created ${formatDate(token.createdAt)}` + (token.expiresAt ? `  •  Expires ${formatDate(token.expiresAt)}` : '  •  Never expires')

const editToken = (token) => {
  if (editing.value === `token:${tid(token)}`) { close(); return }
  confirming.value = null
  draft.value = { description: token.description || '', manager: tokenIsManager(token), expiresAt: toDateInput(token.expiresAt) }
  editing.value = `token:${tid(token)}`
}
const saveToken = (token) =>
  saveTokenMeta(token, draft.value).then(ok => { if (ok) close() })

const editRoute = (token, route) => { confirming.value = null; editing.value = `route:${tid(token)}:${route.path}`; draft.value = { path: route.path, read: route.read, write: route.write, original: route.path } }
const addRoute = (token) => { confirming.value = null; editing.value = `newroute:${tid(token)}`; draft.value = { path: '', read: true, write: false } }
const persistRoute = (token) => {
  let path = (draft.value.path || '').trim()
  if (!path) return
  if (!path.startsWith('/')) path = '/' + path
  if (!draft.value.read && !draft.value.write) { createWarningToast('Select read and/or write'); return }
  saveRoute(token, { ...draft.value, path }, draft.value.original).then(ok => { if (ok) close() })
}

const startCreate = () => { confirming.value = null; editing.value = 'newtoken'; draft.value = { name: '', type: 'PERSISTENT' } }
const create = () => {
  const name = (draft.value.name || '').trim()
  if (name === '') { createWarningToast('Token name is required'); return }
  if (/[:/]/.test(name)) { createWarningToast("Token name cannot contain ':' or '/'"); return }
  if (tokens.value.some(token => token.name === name)) { createWarningToast(`A token named '${name}' already exists`); return }
  createToken(name, { type: draft.value.type })
    .then(response => { secret.value = { name, value: response.secret }; close() })
    .catch(error => createErrorToast(errorMessage(error)))
}

const ask = (token, action) => { editing.value = null; confirming.value = { id: tid(token), action } }
const isConfirming = (token) => confirming.value?.id === tid(token)
const runConfirm = (token) => {
  const { action } = confirming.value
  confirming.value = null
  if (action === 'revoke') deleteToken(token.name)
  else regenerateSecret(token.name)
    .then(value => { secret.value = { name: token.name, value } })
    .catch(error => createErrorToast(errorMessage(error)))
}
</script>

<template>
  <div class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Access credentials"
      description="Generate and revoke tokens used to authenticate with this instance."
    >
      <template #note>
        A token's secret is shown only once, when it is generated.
      </template>
    </ViewHeader>

    <div class="overflow-hidden rounded-lg bg-white text-sm text-gray-600 dark:bg-gray-900 dark:text-gray-300">
      <div class="flex flex-wrap items-center gap-3 border-b border-gray-200 bg-white px-3.5 py-3.5 dark:border-gray-800 dark:bg-gray-900">
        <div class="flex h-9 min-w-56 flex-1 items-center gap-2 rounded-md border border-gray-300 bg-white px-3 dark:border-gray-700 dark:bg-gray-800 <sm:w-full <sm:min-w-0">
          <svg
            viewBox="0 0 24 24"
            class="w-4 h-4 flex-shrink-0 text-gray-400"
            aria-hidden="true"
          ><path
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            d="M21 21l-4.3-4.3m1.3-5.2a7 7 0 11-14 0 7 7 0 0114 0z"
          /></svg>
          <input
            v-model="query"
            class="flex-1 bg-transparent text-gray-700 outline-none placeholder-gray-500 dark:text-gray-200 dark:placeholder-gray-400"
            placeholder="Search tokens, routes…"
            aria-label="Search tokens and routes"
          >
        </div>
        <button
          type="button"
          class="h-9 whitespace-nowrap rounded-md bg-blue-700 px-3.5 font-medium text-white hover:bg-blue-800 <sm:w-full"
          @click="startCreate"
        >
          + Generate token
        </button>
      </div>

      <div
        v-if="isOpen('newtoken')"
        class="flex flex-wrap items-center gap-2 border-b border-gray-200 bg-gray-100 px-4.5 py-3 dark:border-gray-800 dark:bg-gray-800"
      >
        <input
          v-model="draft.name"
          class="h-8 min-w-48 flex-1 rounded-md border border-gray-300 bg-white px-3 text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-400"
          placeholder="Token name (e.g. ci-bot)"
          aria-label="Token name"
          @keyup.enter="create"
        >
        <div
          class="inline-flex overflow-hidden rounded-md border border-gray-300 dark:border-gray-700"
          role="group"
          aria-label="Token type"
        >
          <button
            type="button"
            class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
            :class="{ 'bg-blue-600 text-white': draft.type === 'PERSISTENT' }"
            :aria-pressed="draft.type === 'PERSISTENT'"
            @click="draft.type = 'PERSISTENT'"
          >
            persistent
          </button>
          <button
            type="button"
            class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
            :class="{ 'bg-blue-600 text-white': draft.type === 'TEMPORARY' }"
            :aria-pressed="draft.type === 'TEMPORARY'"
            @click="draft.type = 'TEMPORARY'"
          >
            temporary
          </button>
        </div>
        <button
          type="button"
          class="h-8 rounded-md bg-blue-700 px-3 font-medium text-white hover:bg-blue-800"
          @click="create"
        >
          Generate
        </button>
        <button
          type="button"
          class="h-8 rounded-md border border-gray-300 px-3 dark:border-gray-700"
          @click="close"
        >
          Cancel
        </button>
      </div>

      <div
        v-if="secret"
        class="border-b border-gray-200 bg-blue-50 px-4.5 py-3 text-blue-900 dark:border-gray-800 dark:bg-blue-900 dark:text-blue-100"
        role="status"
      >
        New secret for <strong>{{ secret.name }}</strong>: <code class="px-1 font-mono">{{ secret.value }}</code> — copy it now. <button
          type="button"
          class="ml-2 underline"
          @click="secret = null"
        >
          Dismiss
        </button>
      </div>

      <template
        v-for="(token, tokenIndex) in filtered"
        :key="tid(token)"
      >
        <div
          class="group flex min-h-11.5 items-center gap-2 border-b border-gray-200 px-4.5 py-2 transition-colors dark:border-gray-800 <sm:flex-wrap <sm:items-start <sm:py-3"
          :class="{ 'bg-gray-100 dark:bg-gray-800': isOpen(`token:${tid(token)}`), 'hover:bg-gray-50 dark:hover:bg-gray-800': !isOpen(`token:${tid(token)}`) }"
        >
          <span class="whitespace-nowrap font-semibold text-gray-800 dark:text-gray-100">{{ token.name }}</span>
          <span class="whitespace-nowrap rounded-full bg-gray-150 px-1.5 py-0.5 text-[0.7rem] text-gray-600 dark:bg-gray-800 dark:text-gray-400">{{ token.identifier.type.toLowerCase() }}</span>
          <span
            v-if="tokenIsManager(token)"
            class="whitespace-nowrap rounded-full bg-blue-100 px-1.5 py-0.5 text-[0.7rem] text-blue-700 dark:bg-blue-900 dark:text-blue-200"
          >manager</span>
          <span class="min-w-0 flex-1 truncate text-gray-500 dark:text-gray-500 <sm:basis-full <sm:order-5">{{ token.description }}</span>
          <span
            class="inline-flex cursor-default items-center gap-2 whitespace-nowrap text-xs text-gray-500 dark:text-gray-400 <sm:hidden"
            :title="datesTitle(token)"
          ><span>{{ ageOf(token) }}</span><span class="text-gray-400 dark:text-gray-500">·</span><span
            class="text-gray-600 dark:text-gray-300"
            :class="{ 'text-red-500': isExpired(token), 'align-middle text-lg leading-none': !token.expiresAt }"
          >{{ expiryOf(token) }}</span></span>
          <span class="flex items-center gap-1">
            <template v-if="isConfirming(token)">
              <button
                type="button"
                class="text-xs font-medium text-red-600 dark:text-red-400"
                @click="runConfirm(token)"
              >{{ confirming.action === 'revoke' ? 'Revoke' : 'Regenerate' }}</button>
              <button
                type="button"
                class="text-xs text-gray-500 dark:text-gray-400"
                @click="confirming = null"
              >Cancel</button>
            </template>
            <template v-else>
              <button
                type="button"
                class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-100"
                title="Edit token"
                :aria-label="`Edit ${token.name} token`"
                @click="editToken(token)"
              ><PencilIcon
                class="h-4 w-4"
                aria-hidden="true"
              /></button>
              <button
                type="button"
                class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-100"
                title="Regenerate secret"
                :aria-label="`Regenerate ${token.name} secret`"
                @click="ask(token, 'regenerate')"
              ><RefreshIcon
                class="h-4 w-4"
                aria-hidden="true"
              /></button>
              <button
                type="button"
                class="text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-500"
                title="Revoke token"
                :aria-label="`Revoke ${token.name} token`"
                @click="ask(token, 'revoke')"
              ><TrashIcon
                class="h-4 w-4"
                aria-hidden="true"
              /></button>
            </template>
          </span>
        </div>

        <div
          v-if="isOpen(`token:${tid(token)}`)"
          class="flex flex-wrap items-center gap-2 border-b border-gray-200 bg-gray-100 px-4.5 py-3 dark:border-gray-800 dark:bg-gray-800"
        >
          <input
            v-model="draft.description"
            class="h-8 min-w-48 flex-1 rounded-md border border-gray-300 bg-white px-3 text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-400"
            placeholder="Description"
            :aria-label="`Description for ${token.name}`"
          >
          <div class="inline-flex overflow-hidden rounded-md border border-gray-300 dark:border-gray-700">
            <button
              type="button"
              class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
              :class="{ 'bg-blue-600 text-white': draft.manager }"
              :aria-pressed="draft.manager"
              @click="draft.manager = !draft.manager"
            >
              manager
            </button>
          </div>
          <label
            :for="`token-expiry-${tokenIndex}`"
            class="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400"
          >expires</label>
          <input
            :id="`token-expiry-${tokenIndex}`"
            v-model="draft.expiresAt"
            class="h-8 rounded-md border border-gray-300 bg-white px-3 text-sm text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-400"
            type="date"
            :min="minExpiry()"
          >
          <button
            v-if="draft.expiresAt"
            type="button"
            class="h-8 rounded-md border border-gray-300 px-3 dark:border-gray-700"
            @click="draft.expiresAt = ''"
          >
            clear
          </button>
          <button
            type="button"
            class="h-8 rounded-md bg-blue-700 px-3 font-medium text-white hover:bg-blue-800"
            @click="saveToken(token)"
          >
            Save
          </button>
          <button
            type="button"
            class="h-8 rounded-md border border-gray-300 px-3 dark:border-gray-700"
            @click="close"
          >
            Cancel
          </button>
        </div>

        <template v-if="!tokenIsManager(token)">
          <template
            v-for="route in groupRoutes(token)"
            :key="route.path"
          >
            <div
              class="group flex min-h-11.5 items-center gap-2 border-b border-gray-200 py-2 pl-9.5 pr-4.5 transition-colors hover:bg-gray-50 dark:border-gray-800 dark:hover:bg-gray-800 <sm:flex-wrap <sm:items-start <sm:py-3 <sm:pl-4.5"
              :class="{ 'bg-gray-100 dark:bg-gray-800': isOpen(`route:${tid(token)}:${route.path}`) }"
            >
              <span class="whitespace-nowrap font-mono text-gray-700 dark:text-gray-200">{{ route.path }}</span>
              <span class="truncate text-gray-500 dark:text-gray-500">{{ routeLabel(route) }}</span>
              <span class="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 <sm:opacity-100">
                <button
                  type="button"
                  class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-100"
                  title="Edit"
                  :aria-label="`Edit ${route.path} route`"
                  @click="editRoute(token, route)"
                ><PencilIcon
                  class="h-4 w-4"
                  aria-hidden="true"
                /></button>
                <button
                  type="button"
                  class="text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-500"
                  title="Remove"
                  :aria-label="`Remove ${route.path} route`"
                  @click="removeRoute(token, route.path)"
                ><TrashIcon
                  class="h-4 w-4"
                  aria-hidden="true"
                /></button>
              </span>
            </div>
            <div
              v-if="isOpen(`route:${tid(token)}:${route.path}`)"
              class="flex flex-wrap items-center gap-2 border-b border-gray-200 bg-gray-100 py-3 pl-9.5 pr-4.5 dark:border-gray-800 dark:bg-gray-800 <sm:pl-4.5"
            >
              <input
                v-model="draft.path"
                class="h-8 min-w-48 flex-1 rounded-md border border-gray-300 bg-white px-3 font-mono text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-400"
                :aria-label="`Route path for ${token.name}`"
              >
              <div
                class="inline-flex overflow-hidden rounded-md border border-gray-300 dark:border-gray-700"
                role="group"
                :aria-label="`Permissions for ${draft.path}`"
              >
                <button
                  type="button"
                  class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
                  :class="{ 'bg-blue-600 text-white': draft.read }"
                  :aria-pressed="draft.read"
                  @click="draft.read = !draft.read"
                >
                  read
                </button>
                <button
                  type="button"
                  class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
                  :class="{ 'bg-blue-600 text-white': draft.write }"
                  :aria-pressed="draft.write"
                  @click="draft.write = !draft.write"
                >
                  write
                </button>
              </div>
              <button
                type="button"
                class="h-8 rounded-md bg-blue-700 px-3 font-medium text-white hover:bg-blue-800"
                @click="persistRoute(token)"
              >
                Save
              </button>
              <button
                type="button"
                class="h-8 rounded-md border border-gray-300 px-3 dark:border-gray-700"
                @click="close"
              >
                Cancel
              </button>
            </div>
          </template>

          <button
            v-if="!isOpen(`newroute:${tid(token)}`)"
            type="button"
            class="flex min-h-11.5 cursor-pointer items-center gap-2 border-b border-gray-200 py-2 pl-9.5 pr-4.5 text-blue-600 hover:bg-gray-50 hover:text-blue-700 dark:border-gray-800 dark:text-blue-300 dark:hover:bg-gray-800 dark:hover:text-blue-200 <sm:flex-wrap <sm:items-start <sm:py-3 <sm:pl-4.5"
            @click="addRoute(token)"
          >
            <span class="whitespace-nowrap">+ Add route</span>
          </button>
          <div
            v-else
            class="flex flex-wrap items-center gap-2 border-b border-gray-200 bg-gray-100 py-3 pl-9.5 pr-4.5 dark:border-gray-800 dark:bg-gray-800 <sm:pl-4.5"
          >
            <input
              v-model="draft.path"
              class="h-8 min-w-48 flex-1 rounded-md border border-gray-300 bg-white px-3 font-mono text-gray-800 outline-none placeholder-gray-500 focus:border-blue-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-400"
              placeholder="/releases/com/example/artifact"
              :aria-label="`New route path for ${token.name}`"
            >
            <div
              class="inline-flex overflow-hidden rounded-md border border-gray-300 dark:border-gray-700"
              role="group"
              aria-label="New route permissions"
            >
              <button
                type="button"
                class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
                :class="{ 'bg-blue-600 text-white': draft.read }"
                :aria-pressed="draft.read"
                @click="draft.read = !draft.read"
              >
                read
              </button>
              <button
                type="button"
                class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
                :class="{ 'bg-blue-600 text-white': draft.write }"
                :aria-pressed="draft.write"
                @click="draft.write = !draft.write"
              >
                write
              </button>
            </div>
            <button
              type="button"
              class="h-8 rounded-md bg-blue-700 px-3 font-medium text-white hover:bg-blue-800"
              @click="persistRoute(token)"
            >
              Add
            </button>
            <button
              type="button"
              class="h-8 rounded-md border border-gray-300 px-3 dark:border-gray-700"
              @click="close"
            >
              Cancel
            </button>
          </div>
        </template>
        <div
          v-else
          class="flex min-h-11.5 items-center gap-2 border-b border-gray-200 py-2 pl-9.5 pr-4.5 text-gray-500 dark:border-gray-800 dark:text-gray-500 <sm:flex-wrap <sm:items-start <sm:py-3 <sm:pl-4.5"
        >
          Full access to all repositories
        </div>
      </template>

      <div
        v-if="!filtered.length"
        class="px-4.5 py-10 text-center text-gray-500 dark:text-gray-400"
        role="status"
      >
        {{ query ? `No tokens match “${query}”.` : 'No access tokens yet. Generate one to get started.' }}
      </div>
    </div>
  </div>
</template>
