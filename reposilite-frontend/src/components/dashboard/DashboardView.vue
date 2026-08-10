<script setup>
import { ref, computed, onUnmounted } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"
import { useTokens } from "../../store/tokens"
import ViewHeader from '../util/ViewHeader.vue'
import VersionSponsors from './VersionSponsors.vue'
import sponsors from '../../data/version-sponsors.json'

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

const repositoryTags = computed(() => {
  const visibilityCounts = new Map()

  repositories.value.forEach(({ visibility }) =>
    visibilityCounts.set(visibility, (visibilityCounts.get(visibility) || 0) + 1))

  return [...visibilityCounts]
    .map(([visibility, count]) => `${count} ${visibility.toLowerCase()}`)
})

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

const currentIntervalLabels = {
  DAILY: 'Today',
  WEEKLY: 'This week',
  MONTHLY: 'This month',
  YEARLY: 'This year'
}

const traffic = computed(() => {
  if (!resolved.value?.statisticsEnabled) return null
  const repositories = resolved.value.repositories || []
  const total = repositories.reduce((sum, repository) => sum + repository.data.reduce((s, r) => s + r.count, 0), 0)
  const current = repositories.reduce((sum, repository) => sum + (repository.data.at(-1)?.count || 0), 0)
  const busiest = repositories
    .map(repository => ({ name: repository.name, total: repository.data.reduce((s, r) => s + r.count, 0) }))
    .sort((a, b) => b.total - a.total)[0]
  return {
    total,
    current,
    currentLabel: currentIntervalLabels[resolved.value.interval] || currentIntervalLabels.MONTHLY,
    busiest: busiest?.name
  }
})

const plural = (count, singular, pluralText) =>
  count === 1 ? singular : pluralText
</script>

<template>
  <div
    v-if="instanceStatus"
    class="container mx-auto pt-7 px-15 pb-12 <sm:px-4"
  >
    <ViewHeader
      title="Instance overview"
      description="Live health, resources, repositories, and access state."
    />

    <div class="grid grid-cols-6 gap-4 <md:grid-cols-1">
      <section
        class="col-span-6 <md:col-span-1 bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0"
        aria-label="Instance status"
      >
        <dl class="grid grid-cols-3 gap-6 <sm:grid-cols-2 <sm:gap-5">
          <div>
            <dt class="text-sm text-gray-500 dark:text-gray-400 mb-2">
              Status
            </dt>
            <dd class="text-lg font-semibold flex items-center gap-2">
              <span class="text-green-600 dark:text-green-400 font-semibold text-lg inline-flex items-center gap-2"><span
                class="w-2 h-2 rounded-full bg-green-500"
                aria-hidden="true"
              />Online</span>
            </dd>
            <dd
              class="mt-1 text-xs text-gray-500 dark:text-gray-400"
              :class="{ 'text-red-600 dark:text-red-400': instanceStatus.failuresCount > 0 }"
            >
              {{ instanceStatus.failuresCount === 0
                ? 'No failures since restart'
                : `${instanceStatus.failuresCount} ${plural(instanceStatus.failuresCount, 'failure', 'failures')} since restart` }}
            </dd>
          </div>
          <div>
            <dt class="text-sm text-gray-500 dark:text-gray-400 mb-2">
              Runtime
            </dt>
            <dd class="text-lg font-semibold flex items-center gap-2 tabular-nums">
              {{ prettyUptime(instanceStatus.uptime / 1000) }}
            </dd>
            <dd class="mt-1 text-xs text-gray-500 dark:text-gray-400 tabular-nums">
              Started {{ startedAt }}
            </dd>
          </div>
          <div class="<sm:col-span-2">
            <dt class="text-sm text-gray-500 dark:text-gray-400 mb-2">
              Version
            </dt>
            <dd class="text-lg font-semibold flex items-center gap-2 tabular-nums">
              <VersionSponsors
                :version="instanceStatus.version"
                :up-to-date="isUpToDate"
                :sponsors="sponsors"
              />
            </dd>
          </div>
        </dl>
      </section>

      <section class="col-span-3 <md:col-span-1 bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0">
        <div class="flex items-center justify-between mb-3.5">
          <h2 class="text-sm font-semibold text-gray-600 dark:text-gray-300">
            Resources
          </h2><button
            type="button"
            class="text-sm text-blue-600 dark:text-blue-300 cursor-pointer hover:text-blue-700 dark:hover:text-blue-200 bg-transparent border-0 p-0"
            @click="$emit('goto', 'Diagnostics')"
          >
            Diagnostics
          </button>
        </div>
        <div>
          <div class="flex justify-between items-baseline gap-3 text-sm mb-2 <sm:flex-wrap">
            <span class="text-gray-500 dark:text-gray-400">Heap memory</span><span class="font-semibold tabular-nums">{{ instanceStatus.usedMemory.toFixed(0) }} MB <small class="text-gray-600 dark:text-gray-300 font-normal">/ {{ instanceStatus.maxMemory }} MB</small></span>
          </div>
          <div
            class="h-2 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden"
            role="progressbar"
            aria-label="Heap memory usage"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-valuenow="heapUsage"
          >
            <div
              class="h-full rounded-full bg-blue-600 dark:bg-blue-500"
              :style="{ width: heapUsage + '%' }"
            />
          </div>
        </div>
        <div class="mt-3.5">
          <div class="flex justify-between items-baseline gap-3 text-sm mb-2 <sm:flex-wrap">
            <span class="text-gray-500 dark:text-gray-400">Threads</span><span class="font-semibold tabular-nums">{{ instanceStatus.usedThreads }} <small class="text-gray-600 dark:text-gray-300 font-normal">/ {{ instanceStatus.maxThreads }}</small></span>
          </div>
          <div
            class="h-2 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden"
            role="progressbar"
            aria-label="Thread usage"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-valuenow="threadsUsage"
          >
            <div
              class="h-full rounded-full bg-blue-600 dark:bg-blue-500"
              :style="{ width: threadsUsage + '%' }"
            />
          </div>
        </div>
      </section>

      <section class="col-span-3 <md:col-span-1 bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0">
        <div class="flex items-center justify-between mb-3.5">
          <h2 class="text-sm font-semibold text-gray-600 dark:text-gray-300">
            Traffic
          </h2><button
            type="button"
            class="text-sm text-blue-600 dark:text-blue-300 cursor-pointer hover:text-blue-700 dark:hover:text-blue-200 bg-transparent border-0 p-0"
            @click="$emit('goto', 'Statistics')"
          >
            Statistics
          </button>
        </div>
        <template v-if="traffic">
          <div class="flex items-baseline gap-2 min-w-0">
            <span class="text-2xl font-semibold leading-tight tabular-nums">{{ traffic.total.toLocaleString() }}</span>
            <span class="text-sm text-gray-500 dark:text-gray-400 truncate">resolved requests</span>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-3 <sm:grid-cols-1">
            <div class="min-w-0">
              <span class="block text-sm text-gray-500 dark:text-gray-400">{{ traffic.currentLabel }}</span>
              <b class="block mt-1 truncate text-base text-gray-800 dark:text-gray-100 font-semibold tabular-nums">{{ traffic.current.toLocaleString() }}</b>
            </div>
            <div class="min-w-0">
              <span class="block text-sm text-gray-500 dark:text-gray-400">Top repository</span>
              <b class="block mt-1 truncate text-base text-gray-800 dark:text-gray-100 font-semibold">{{ traffic.busiest || 'No data' }}</b>
            </div>
          </div>
        </template>
        <div
          v-else
          class="text-gray-500 dark:text-gray-400 text-sm mt-2"
        >
          Statistics disabled
        </div>
      </section>

      <section class="col-span-2 <md:col-span-1 bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0">
        <div class="flex items-center justify-between mb-3.5">
          <h2 class="text-sm font-semibold text-gray-600 dark:text-gray-300">
            Repositories
          </h2><button
            type="button"
            class="text-sm text-blue-600 dark:text-blue-300 cursor-pointer hover:text-blue-700 dark:hover:text-blue-200 bg-transparent border-0 p-0"
            @click="$emit('goto', 'Settings')"
          >
            Configure
          </button>
        </div>
        <div class="flex items-baseline gap-2 min-w-0">
          <span class="text-2xl font-semibold leading-tight tabular-nums">{{ repositories.length }}</span>
          <span class="text-sm text-gray-500 dark:text-gray-400 truncate">{{ plural(repositories.length, 'repository', 'repositories') }}</span>
        </div>
        <div class="flex flex-wrap gap-1.5 mt-4 min-h-8 content-start">
          <span
            v-for="tag in repositoryTags"
            :key="tag"
            class="text-xs px-1.5 py-0.5 rounded-full bg-gray-150 dark:bg-gray-800 text-gray-600 dark:text-gray-300 whitespace-nowrap"
          >{{ tag }}</span>
          <span
            v-if="repositoryTags.length === 0"
            class="text-sm text-gray-500 dark:text-gray-400"
          >No repositories configured</span>
        </div>
      </section>

      <section class="col-span-2 <md:col-span-1 bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0">
        <div class="flex items-center justify-between mb-3.5">
          <h2 class="text-sm font-semibold text-gray-600 dark:text-gray-300">
            Access tokens
          </h2><button
            type="button"
            class="text-sm text-blue-600 dark:text-blue-300 cursor-pointer hover:text-blue-700 dark:hover:text-blue-200 bg-transparent border-0 p-0"
            @click="$emit('goto', 'Tokens')"
          >
            Manage
          </button>
        </div>
        <div class="flex items-baseline gap-2 min-w-0">
          <span class="text-2xl font-semibold leading-tight tabular-nums">{{ tokens.length }}</span>
          <span class="text-sm text-gray-500 dark:text-gray-400 truncate">{{ plural(tokens.length, 'token', 'tokens') }}</span>
        </div>
        <div class="flex flex-wrap gap-1.5 mt-4 min-h-8 content-start">
          <span
            v-for="tag in tokenTags"
            :key="tag.text || tag"
            class="text-xs px-1.5 py-0.5 rounded-full bg-gray-150 dark:bg-gray-800 text-gray-600 dark:text-gray-300 whitespace-nowrap"
            :class="{ 'bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200': tag.mgr }"
          >{{ tag.text || tag }}</span>
          <span
            v-if="tokenTags.length === 0"
            class="text-sm text-gray-500 dark:text-gray-400"
          >No access tokens</span>
        </div>
      </section>

      <section class="col-span-2 <md:col-span-1 bg-white dark:bg-gray-900 rounded-lg p-5 min-w-0">
        <div class="flex items-center justify-between mb-3.5">
          <h2 class="text-sm font-semibold text-gray-600 dark:text-gray-300">
            Diagnostics
          </h2><button
            type="button"
            class="text-sm text-blue-600 dark:text-blue-300 cursor-pointer hover:text-blue-700 dark:hover:text-blue-200 bg-transparent border-0 p-0"
            @click="$emit('goto', 'Diagnostics')"
          >
            Open
          </button>
        </div>
        <div class="flex items-baseline gap-2 min-w-0">
          <span
            class="text-2xl font-semibold leading-tight tabular-nums"
            :class="{ 'text-red-600 dark:text-red-400': instanceStatus.failuresCount > 0 }"
          >{{ instanceStatus.failuresCount }}</span>
          <span
            class="text-sm text-gray-500 dark:text-gray-400 truncate"
            :class="{ 'text-red-500 dark:text-red-400': instanceStatus.failuresCount > 0 }"
          >{{ plural(instanceStatus.failuresCount, 'failure', 'failures') }}</span>
        </div>
        <div class="mt-4 text-sm text-gray-500 dark:text-gray-400 space-y-2">
          <div
            v-if="instanceStatus.failuresCount === 0"
            class="flex items-center justify-between gap-3 min-w-0"
          >
            <span class="truncate">Since restart</span><b class="text-gray-800 dark:text-gray-100 font-semibold truncate text-right">Clean</b>
          </div>
          <div
            v-else
            class="flex items-center justify-between gap-3 min-w-0"
          >
            <span class="truncate">Since restart</span><b class="text-red-600 dark:text-red-400 font-semibold truncate text-right">Needs review</b>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
