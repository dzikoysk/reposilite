<!--
  - Copyright (c) 2023 dzikoysk
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

const tid = (token) => token.identifier.value
const isOpen = (key) => editing.value === key
const close = () => { editing.value = null }

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return tokens.value
  return tokens.value.filter(token =>
    (token.name + ' ' + (token.description || '')).toLowerCase().includes(q) ||
    (token.routes || []).some(route => route.path.toLowerCase().includes(q)))
})

const formatDate = (value) => value == null ? null : new Date(toMs(value)).toLocaleDateString()
const toDateInput = (value) => {
  if (!value) return ''
  const date = new Date(toMs(value))
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
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

/* Placeholder usage metric (UI commented out below) — Reposilite tracks no per-token usage yet. */
const spark = (token) => {
  const seed = tid(token) * 7 + token.name.length
  return Array.from({ length: 10 }, (_, i) => 5 + ((seed * (i + 3)) % 18))
}
const requests = (token) => spark(token).reduce((a, b) => a + b, 0) * 11

const editToken = (token) => {
  if (editing.value === `token:${tid(token)}`) { close(); return }
  draft.value = { description: token.description || '', manager: tokenIsManager(token), expiresAt: toDateInput(token.expiresAt) }
  editing.value = `token:${tid(token)}`
}
const saveToken = (token) =>
  saveTokenMeta(token, draft.value).then(close)

const editRoute = (token, route) => { editing.value = `route:${tid(token)}:${route.path}`; draft.value = { path: route.path, read: route.read, write: route.write, original: route.path } }
const addRoute = (token) => { editing.value = `newroute:${tid(token)}`; draft.value = { path: '', read: true, write: false } }
const persistRoute = (token) => {
  let path = (draft.value.path || '').trim()
  if (!path) return
  if (!path.startsWith('/')) path = '/' + path
  if (!draft.value.read && !draft.value.write) { createWarningToast('Select read and/or write'); return }
  saveRoute(token, { ...draft.value, path }, draft.value.original).then(close)
}

const startCreate = () => { editing.value = 'newtoken'; draft.value = { name: '', type: 'PERSISTENT' } }
const create = () => {
  const name = (draft.value.name || '').trim()
  if (name === '') { createWarningToast('Token name is required'); return }
  if (/[:/]/.test(name)) { createWarningToast("Token name cannot contain ':' or '/'"); return }
  if (tokens.value.some(token => token.name === name)) { createWarningToast(`A token named '${name}' already exists`); return }
  createToken(name, { type: draft.value.type })
    .then(response => { secret.value = { name, value: response.secret }; close() })
    .catch(error => createErrorToast(errorMessage(error)))
}

const ask = (token, action) => { confirming.value = { id: tid(token), action } }
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
    <div class="pb-7">
      <p>Generate and revoke access tokens used to authenticate with this Reposilite instance.</p>
      <p class="text-sm text-gray-500">A token's secret is shown only once, at the moment it is generated.</p>
    </div>

    <div class="flat">
      <div class="bar">
        <div class="search">
          <svg viewBox="0 0 24 24" class="w-4 h-4 flex-shrink-0 text-gray-400"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M21 21l-4.3-4.3m1.3-5.2a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          <input v-model="query" placeholder="Search tokens, routes…" />
        </div>
        <button class="primary" @click="startCreate">+ Generate token</button>
      </div>

      <div v-if="isOpen('newtoken')" class="tokenform">
        <input class="field" v-model="draft.name" placeholder="Token name (e.g. ci-bot)" @keyup.enter="create" />
        <div class="seg">
          <button :class="{ on: draft.type === 'PERSISTENT' }" @click="draft.type = 'PERSISTENT'">persistent</button>
          <button :class="{ on: draft.type === 'TEMPORARY' }" @click="draft.type = 'TEMPORARY'">temporary</button>
        </div>
        <button class="primary sm" @click="create">Generate</button>
        <button class="sm" @click="close">Cancel</button>
      </div>

      <div v-if="secret" class="banner">New secret for <strong>{{ secret.name }}</strong>: <code>{{ secret.value }}</code> — copy it now. <button class="link" @click="secret = null">Dismiss</button></div>

      <template v-for="token in filtered" :key="tid(token)">
        <div class="row" :class="{ 'is-open': isOpen(`token:${tid(token)}`), confirming: isConfirming(token) }">
          <span class="lead font-semibold text-gray-800 dark:text-gray-100">{{ token.name }}</span>
          <span class="tag">{{ token.identifier.type.toLowerCase() }}</span>
          <span v-if="tokenIsManager(token)" class="tag mgr">manager</span>
          <span class="desc">{{ token.description }}</span>
          <span class="dates" :title="datesTitle(token)"><span>{{ ageOf(token) }}</span><span class="exsep">·</span><span class="ex" :class="{ expired: isExpired(token), forever: !token.expiresAt }">{{ expiryOf(token) }}</span><!-- usage count (placeholder, disabled): <span class="exsep">·</span><span class="reqs" title="Sample data — Reposilite doesn't track per-token usage yet">{{ requests(token).toLocaleString() }} requests</span> --></span>
          <!-- usage chart (placeholder, disabled — no per-token stats in the backend yet)
          <span class="usage" title="Sample data — Reposilite doesn't track per-token usage yet">
            <svg class="spark" viewBox="0 0 60 18" preserveAspectRatio="none">
              <rect v-for="(h, i) in spark(token)" :key="i" :x="i * 6 + 0.5" :y="18 - h" width="5" :height="h" rx="1" />
            </svg>
          </span>
          -->
          <span class="ctl">
            <template v-if="isConfirming(token)">
              <button class="confirm-yes" @click="runConfirm(token)">{{ confirming.action === 'revoke' ? 'Revoke' : 'Regenerate' }}</button>
              <button class="confirm-cancel" @click="confirming = null">Cancel</button>
            </template>
            <template v-else>
              <button class="ic" title="Edit token" @click="editToken(token)"><PencilIcon /></button>
              <button class="ic" title="Regenerate secret" @click="ask(token, 'regenerate')"><RefreshIcon /></button>
              <button class="ic danger" title="Revoke token" @click="ask(token, 'revoke')"><TrashIcon /></button>
            </template>
          </span>
        </div>

        <div v-if="isOpen(`token:${tid(token)}`)" class="tokenform">
          <input class="field" v-model="draft.description" placeholder="Description" />
          <div class="seg">
            <button :class="{ on: draft.manager }" @click="draft.manager = !draft.manager">manager</button>
          </div>
          <span class="when-label">expires</span>
          <input class="when" type="date" v-model="draft.expiresAt" />
          <button v-if="draft.expiresAt" class="sm" @click="draft.expiresAt = ''">clear</button>
          <button class="primary sm" @click="saveToken(token)">Save</button>
          <button class="sm" @click="close">Cancel</button>
        </div>

        <template v-if="!tokenIsManager(token)">
          <template v-for="route in groupRoutes(token)" :key="route.path">
            <div class="row indent" :class="{ 'is-open': isOpen(`route:${tid(token)}:${route.path}`) }">
              <span class="lead font-mono text-gray-700 dark:text-gray-200">{{ route.path }}</span>
              <span class="meta">{{ routeLabel(route) }}</span>
              <span class="ctl">
                <button class="ic" title="Edit" @click="editRoute(token, route)"><PencilIcon /></button>
                <button class="ic danger" title="Remove" @click="removeRoute(token, route.path)"><TrashIcon /></button>
              </span>
            </div>
            <div v-if="isOpen(`route:${tid(token)}:${route.path}`)" class="routeform indent">
              <input class="path" v-model="draft.path" />
              <div class="seg">
                <button :class="{ on: draft.read }" @click="draft.read = !draft.read">read</button>
                <button :class="{ on: draft.write }" @click="draft.write = !draft.write">write</button>
              </div>
              <button class="primary sm" @click="persistRoute(token)">Save</button>
              <button class="sm" @click="close">Cancel</button>
            </div>
          </template>

          <div v-if="!isOpen(`newroute:${tid(token)}`)" class="row indent add" @click="addRoute(token)">
            <span class="lead">+ Add route</span>
          </div>
          <div v-else class="routeform indent new">
            <input class="path" v-model="draft.path" placeholder="/releases/com/example/artifact" />
            <div class="seg">
              <button :class="{ on: draft.read }" @click="draft.read = !draft.read">read</button>
              <button :class="{ on: draft.write }" @click="draft.write = !draft.write">write</button>
            </div>
            <button class="primary sm" @click="persistRoute(token)">Add</button>
            <button class="sm" @click="close">Cancel</button>
          </div>
        </template>
        <div v-else class="row indent full">Full access to all repositories</div>
      </template>

      <div v-if="!filtered.length" class="empty">{{ query ? `No tokens match “${query}”.` : 'No access tokens yet. Generate one to get started.' }}</div>
    </div>
  </div>
</template>

<style scoped>
.flat { @apply bg-white dark:bg-transparent dark:border dark:border-gray-800 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300; }
.flat > :last-child { @apply border-b-0; }

.bar { @apply flex items-center gap-3 px-3.5 py-3.5 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-transparent; }
.search { @apply flex items-center gap-2 flex-1 px-3 h-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800; }
.search input { @apply flex-1 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400; }

.row { @apply flex items-center gap-2 px-4.5 h-11.5 border-b border-gray-200 dark:border-gray-800 transition-colors; }
.row:hover { @apply bg-gray-50 dark:bg-gray-900; }
.row.is-open { @apply bg-gray-100 dark:bg-gray-900; }
.indent { @apply pl-9.5; }
.lead { @apply whitespace-nowrap; }
.meta { @apply text-gray-500 dark:text-gray-500 truncate; }
.desc { @apply flex-1 min-w-0 truncate text-gray-500 dark:text-gray-500; }
.dates { @apply inline-flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap cursor-default <sm:hidden; }
.exsep { @apply text-gray-400 dark:text-gray-500; }
.ex { @apply text-gray-600 dark:text-gray-300; }
.ex.expired { @apply text-red-500; }
.ex.forever { @apply text-lg leading-none align-middle; }

.tag { @apply text-[0.7rem] px-1.5 py-0.5 rounded-full bg-gray-150 dark:bg-gray-800 text-gray-600 dark:text-gray-400 whitespace-nowrap; }
.tag.mgr { @apply bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200; }

.reqs { @apply text-xs tabular-nums text-gray-600 dark:text-gray-400; }
.usage { @apply flex items-center mr-1; }
.spark { width: 60px; height: 18px; }
.spark rect { @apply fill-blue-300 dark:fill-blue-900; }

.ctl { @apply flex items-center gap-1; }
.row .ctl { @apply opacity-0 transition-opacity; }
.row:hover .ctl, .row.confirming .ctl { @apply opacity-100; }
.row:not(.indent) .ctl { @apply opacity-100; }
.ic { @apply text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-100; }
.ic :deep(svg) { @apply w-4 h-4; }
.ic.danger:hover { @apply text-red-500; }
.confirm-yes { @apply text-xs font-medium text-red-600 dark:text-red-400; }
.confirm-cancel { @apply text-xs text-gray-500 dark:text-gray-400; }
.add { @apply text-blue-600 dark:text-blue-300 cursor-pointer; }
.add:hover { @apply text-blue-700 dark:text-blue-200; }
.full { @apply text-gray-500 dark:text-gray-500; }
.full:hover { @apply bg-transparent; }

.primary { @apply px-3.5 h-9 rounded-md bg-blue-700 text-white font-medium whitespace-nowrap hover:bg-blue-800; }
.primary.sm { @apply h-8 px-3; }
button.sm { @apply h-8 px-3 rounded-md border border-gray-300 dark:border-gray-700; }

.routeform, .tokenform { @apply flex flex-wrap items-center gap-2 px-4.5 py-3 bg-gray-100 dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800; }
.routeform.indent { @apply pl-9.5; }
.routeform .path, .tokenform .field, .tokenform .when {
  @apply h-8 px-3 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 outline-none focus:border-blue-500;
}
.routeform .path { @apply flex-1 min-w-48 font-mono; }
.tokenform .field { @apply flex-1 min-w-48; }
.tokenform .when { @apply text-sm; }
.when-label { @apply text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400; }
.seg { @apply inline-flex rounded-md overflow-hidden border border-gray-300 dark:border-gray-700; }
.seg button { @apply px-3 h-8 text-xs bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 border-r border-gray-300 dark:border-gray-700; }
.seg button:last-child { @apply border-r-0; }
.seg button.on { @apply bg-blue-600 text-white; }

.banner { @apply px-4.5 py-3 text-blue-900 bg-blue-50 dark:bg-blue-900 dark:text-blue-100 border-b border-gray-200 dark:border-gray-800; }
.banner code { @apply font-mono px-1; }
.link { @apply underline ml-2; }
.empty { @apply px-4.5 py-10 text-center text-gray-500 dark:text-gray-400; }
</style>
