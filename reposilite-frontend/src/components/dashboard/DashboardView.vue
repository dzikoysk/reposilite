<script setup>
import { ref, computed, onUnmounted } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"
import { useTokens } from "../../store/tokens"
import ViewHeader from '../util/ViewHeader.vue'

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})

defineEmits(['goto'])

const { client } = useSession()
const { tokens, fetchTokens, tokenIsManager } = useTokens()

const instanceStatus = ref()
const repositories = ref([])
const resolved = ref()
let statusTimeout

function requestStatus() {
  if (props.selectedTab == 'Dashboard') {
    client.value.status.instance()
      .then(response => response.data)
      .then(instanceStatusData => {
        instanceStatus.value = instanceStatusData
        statusTimeout = setTimeout(requestStatus, 1000)
      })
      .catch((error) => {
        createErrorToast(`Cannot load instance status`)
        console.log(error)
      })
  }
}
requestStatus()
onUnmounted(() => clearTimeout(statusTimeout))

client.value.settings.fetch('maven')
  .then(response => repositories.value = response.data.repositories || [])
  .catch(error => console.log(error))

client.value.statistics.allResolved()
  .then(response => response.data)
  .then(allResolved => resolved.value = allResolved)
  .catch(error => console.log(error))

fetchTokens()

const isUpToDate = computed(() =>
  instanceStatus.value && instanceStatus.value.version === instanceStatus.value.latestVersion)

const prettyUptime = (seconds) => {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor(((seconds % 86400) % 3600) / 60)

  const dDisplay = d > 0 ? d + "d " : ""
  const hDisplay = h > 0 ? h + "h " : ""
  const mDisplay = m + "m"

  return dDisplay + hDisplay + mDisplay
}

const startedAt = computed(() => {
  if (!instanceStatus.value) return ''
  return new Date(Date.now() - instanceStatus.value.uptime).toLocaleString(undefined, {
    month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
})

const heapUsage = computed(() => {
  if (!instanceStatus.value) return 0
  return Math.min(100, Math.round((instanceStatus.value.usedMemory / instanceStatus.value.maxMemory) * 100))
})

const threadsUsage = computed(() => {
  if (!instanceStatus.value) return 0
  return Math.min(100, Math.round((instanceStatus.value.usedThreads / instanceStatus.value.maxThreads) * 100))
})

const visibleRepositories = computed(() =>
  repositories.value.slice(0, 2))

const hiddenRepositories = computed(() =>
  Math.max(0, repositories.value.length - visibleRepositories.value.length))

const tokenTags = computed(() => {
  const persistent = tokens.value.filter(token => token.identifier.type === 'PERSISTENT').length
  const temporary = tokens.value.filter(token => token.identifier.type === 'TEMPORARY').length
  const managers = tokens.value.filter(tokenIsManager).length
  const tags = []
  if (persistent) tags.push(`${persistent} persistent`)
  if (temporary) tags.push(`${temporary} temporary`)
  if (managers) tags.push({ text: `${managers} manager`, mgr: true })
  return tags
})

const traffic = computed(() => {
  if (!resolved.value?.statisticsEnabled) return null
  const repositories = resolved.value.repositories || []
  const total = repositories.reduce((sum, repository) => sum + repository.data.reduce((s, r) => s + r.count, 0), 0)
  const today = repositories.reduce((sum, repository) => sum + (repository.data.at(-1)?.count || 0), 0)
  const busiest = repositories
    .map(repository => ({ name: repository.name, total: repository.data.reduce((s, r) => s + r.count, 0) }))
    .sort((a, b) => b.total - a.total)[0]
  return { total, today, busiest: busiest?.name }
})

const plural = (count, singular, pluralText) =>
  count === 1 ? singular : pluralText
</script>

<template>
  <div v-if="instanceStatus" class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Instance overview"
      description="Live health, resources, repositories, and access state."
    />

    <div class="grid">
      <div class="block c6">
        <div class="hero">
          <div class="cell">
            <div class="k">Status</div>
            <div class="v"><span class="status-ok">Online</span></div>
          </div>
          <div class="cell">
            <div class="k">Version</div>
            <div class="v num">{{ instanceStatus.version }} <small v-if="isUpToDate">· up to date</small></div>
          </div>
          <div class="cell">
            <div class="k">Uptime</div>
            <div class="v num">{{ prettyUptime(instanceStatus.uptime / 1000) }}</div>
          </div>
          <div class="cell">
            <div class="k">Started</div>
            <div class="v sm num">{{ startedAt }}</div>
          </div>
        </div>
      </div>

      <div class="block c3">
        <div class="block-top"><span class="t">Resources</span><button class="link" @click="$emit('goto', 'Diagnostics')">Diagnostics</button></div>
        <div class="meter">
          <div class="lab"><span class="name">Heap memory</span><span class="val num">{{ instanceStatus.usedMemory.toFixed(0) }} MB <small>/ {{ instanceStatus.maxMemory }} MB</small></span></div>
          <div class="track"><div class="fill" :style="{ width: heapUsage + '%' }"></div></div>
        </div>
        <div class="meter">
          <div class="lab"><span class="name">Threads</span><span class="val num">{{ instanceStatus.usedThreads }} <small>/ {{ instanceStatus.maxThreads }}</small></span></div>
          <div class="track"><div class="fill" :style="{ width: threadsUsage + '%' }"></div></div>
        </div>
      </div>

      <div class="block c3">
        <div class="block-top"><span class="t">Traffic</span><button class="link" @click="$emit('goto', 'Statistics')">Statistics</button></div>
        <template v-if="traffic">
          <div class="metric">
            <span class="big num">{{ traffic.total.toLocaleString() }}</span>
            <span class="unit">resolved requests</span>
          </div>
          <div class="details two">
            <div><span>Today</span><b class="num">{{ traffic.today.toLocaleString() }}</b></div>
            <div><span>Busiest</span><b>{{ traffic.busiest || 'No data' }}</b></div>
          </div>
        </template>
        <div v-else class="subline">Statistics disabled</div>
      </div>

      <div class="block c2">
        <div class="block-top"><span class="t">Repositories</span><button class="link" @click="$emit('goto', 'Settings')">Configure</button></div>
        <div class="metric">
          <span class="big num">{{ repositories.length }}</span>
          <span class="unit">{{ plural(repositories.length, 'repository', 'repositories') }}</span>
        </div>
        <div class="details list">
          <div v-for="repository in visibleRepositories" :key="repository.id">
            <span>{{ repository.id }}</span><b>{{ repository.visibility.toLowerCase() }}</b>
          </div>
          <div v-if="hiddenRepositories > 0"><span>More</span><b class="num">+{{ hiddenRepositories }}</b></div>
          <div v-if="repositories.length === 0"><span>No repositories configured</span></div>
        </div>
      </div>

      <div class="block c2">
        <div class="block-top"><span class="t">Access tokens</span><button class="link" @click="$emit('goto', 'Tokens')">Manage</button></div>
        <div class="metric">
          <span class="big num">{{ tokens.length }}</span>
          <span class="unit">{{ plural(tokens.length, 'token', 'tokens') }}</span>
        </div>
        <div class="tags compact">
          <span v-for="tag in tokenTags" :key="tag.text || tag" class="tag" :class="{ mgr: tag.mgr }">{{ tag.text || tag }}</span>
          <span v-if="tokenTags.length === 0" class="muted">No access tokens</span>
        </div>
      </div>

      <div class="block c2">
        <div class="block-top"><span class="t">Diagnostics</span><button class="link" @click="$emit('goto', 'Diagnostics')">Open</button></div>
        <div class="metric">
          <span class="big num" :class="{ crit: instanceStatus.failuresCount > 0 }">{{ instanceStatus.failuresCount }}</span>
          <span class="unit" :class="{ warn: instanceStatus.failuresCount > 0 }">{{ plural(instanceStatus.failuresCount, 'failure', 'failures') }}</span>
        </div>
        <div class="details list">
          <div v-if="instanceStatus.failuresCount === 0"><span>Since restart</span><b>Clean</b></div>
          <div v-else><span>Since restart</span><b class="warn">Needs review</b></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid { @apply grid grid-cols-6 gap-4 <md:grid-cols-1; }
.c2 { @apply col-span-2 <md:col-span-1; }
.c3 { @apply col-span-3 <md:col-span-1; }
.c6 { @apply col-span-6; }

@media (max-width: 767px) {
  .grid { grid-template-columns: minmax(0, 1fr); }
  .c2,
  .c3,
  .c6 { grid-column: 1 / -1; }
}

.block { @apply bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0; }
.block-top { @apply flex items-center justify-between mb-3.5; }
.block-top .t { @apply text-sm font-semibold text-gray-600 dark:text-gray-300; }
.link { @apply text-sm text-blue-600 dark:text-blue-300 cursor-pointer hover:text-blue-700 dark:hover:text-blue-200 bg-transparent border-0 p-0; }

.hero { @apply grid grid-cols-4 gap-6 <sm:grid-cols-2 <sm:gap-5; }
.cell .k { @apply text-sm text-gray-500 dark:text-gray-400 mb-2; }
.cell .v { @apply text-lg font-semibold flex items-center gap-2; }
.cell .v small { @apply text-xs font-normal text-gray-500 dark:text-gray-400; }
.cell .v.sm { @apply text-base; }

.status-ok { @apply text-green-600 dark:text-green-400 font-semibold text-lg inline-flex items-center gap-2; }
.status-ok::before { content: ""; @apply w-2 h-2 rounded-full bg-green-500; }

.tag { @apply text-xs px-1.5 py-0.5 rounded-full bg-gray-150 dark:bg-gray-800 text-gray-600 dark:text-gray-300 whitespace-nowrap; }
.tag.mgr { @apply bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200; }
.tags { @apply flex flex-wrap gap-1.5 mt-3.5; }
.tags.compact { @apply mt-4 min-h-8 content-start; }
.muted { @apply text-sm text-gray-500 dark:text-gray-400; }

.big { @apply text-2xl font-semibold leading-tight; }
.big.crit { @apply text-red-600 dark:text-red-400; }
.subline { @apply text-gray-500 dark:text-gray-400 text-sm mt-2; }
.subline b { @apply text-gray-800 dark:text-gray-100 font-semibold; }
.metric { @apply flex items-baseline gap-2 min-w-0; }
.unit { @apply text-sm text-gray-500 dark:text-gray-400 truncate; }
.unit.warn { @apply text-red-500 dark:text-red-400; }
.details { @apply mt-4 text-sm text-gray-500 dark:text-gray-400; }
.details.two { @apply grid grid-cols-2 gap-3 <sm:grid-cols-1; }
.details.list { @apply space-y-2; }
.details div { @apply min-w-0; }
.details.two div,
.details.list div { @apply flex items-center justify-between gap-3; }
.details span { @apply truncate; }
.details b { @apply text-gray-800 dark:text-gray-100 font-semibold truncate text-right; }
.details b.warn { @apply text-red-600 dark:text-red-400; }

.meter + .meter { @apply mt-3.5; }
.meter .lab { @apply flex justify-between items-baseline gap-3 text-sm mb-2 <sm:flex-wrap; }
.meter .lab .name { @apply text-gray-500 dark:text-gray-400; }
.meter .lab .val { @apply font-semibold; }
.meter .lab .val small { @apply text-gray-600 dark:text-gray-300 font-normal; }
.track { @apply h-2 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden; }
.fill { @apply h-full rounded-full bg-blue-600 dark:bg-blue-500; }

.num { font-variant-numeric: tabular-nums; }
</style>
