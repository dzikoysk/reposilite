<script setup>
import { ref, computed } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"
import FailuresList from "./FailuresList.vue"

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})

const { client } = useSession()

const instanceStatus = ref()
const health = ref()
const failures = ref([])
const query = ref('')
let lastFailuresCount = -1

function requestFailures(count) {
  if (count === lastFailuresCount) return
  lastFailuresCount = count

  if (count === 0) {
    failures.value = []
    return
  }

  client.value.status.failures()
    .then(response => failures.value = response.data)
    .catch(error => console.log(error))
}

function requestStatus() {
  if (props.selectedTab == 'Diagnostics') {
    client.value.status.instance()
      .then(response => response.data)
      .then(instanceStatusData => {
        instanceStatus.value = instanceStatusData
        requestFailures(instanceStatusData.failuresCount)
        setTimeout(requestStatus, 1000)
      })
      .catch((error) => {
        createErrorToast(`Cannot load instance status`)
        console.log(error)
      })
  }
}
requestStatus()

function requestHealth() {
  if (props.selectedTab == 'Diagnostics') {
    client.value.status.health()
      .then(response => response.data)
      .then(healthData => {
        health.value = healthData
        setTimeout(requestHealth, 30 * 1000)
      })
      .catch(error => console.log(error))
  }
}
requestHealth()

const prettyUptime = (seconds) => {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor(((seconds % 86400) % 3600) / 60)

  const dDisplay = d > 0 ? d + "d " : ""
  const hDisplay = h > 0 ? h + "h " : ""
  const mDisplay = m + "m"

  return dDisplay + hDisplay + mDisplay
}

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return failures.value
  return failures.value.filter(failure => failure.toLowerCase().includes(q))
})
</script>

<template>
  <div v-if="instanceStatus" class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <div class="head">
      <h1 class="font-semibold text-lg">Diagnostics</h1>
    </div>

    <div class="statusbar">
      <div class="block">
        <div>
          <div class="k">Instance</div>
          <div class="v" :class="health?.status ? (health.status === 'UP' ? 'ok' : 'crit') : ''">{{ health?.status === 'UP' ? 'Online' : (health?.status || '…') }}</div>
        </div>
      </div>
      <div class="block">
        <div><div class="k">Failures</div><div class="v num">{{ instanceStatus.failuresCount }}</div></div>
        <span class="status" :class="instanceStatus.failuresCount > 0 ? 'crit' : 'ok'">{{ instanceStatus.failuresCount > 0 ? 'review' : 'clean' }}</span>
      </div>
      <div class="block">
        <div><div class="k">Uptime</div><div class="v num">{{ prettyUptime(instanceStatus.uptime / 1000) }}</div></div>
      </div>
    </div>

    <div class="flat">
      <div class="bar">
        <div class="count"><b class="num">{{ failures.length }}</b> recorded</div>
        <div class="search">
          <svg viewBox="0 0 24 24" class="w-4 h-4 flex-shrink-0 text-gray-400"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M21 21l-4.3-4.3m1.3-5.2a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          <input v-model="query" placeholder="Filter by type or path…" />
        </div>
      </div>
      <FailuresList v-if="filtered.length" :failures="filtered" :version="instanceStatus.version" />
      <div v-else class="empty">
        <p v-if="failures.length"><b>No matches.</b> No recorded failures match “{{ query }}”.</p>
        <p v-else><b>All clear.</b> No exceptions recorded since the last restart.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { @apply mb-5; }

.statusbar { @apply flex flex-wrap gap-4 mb-5; }
.statusbar .block { @apply flex-1 min-w-40 bg-white dark:bg-gray-900 rounded-lg px-5 py-4 flex items-center justify-between <sm:min-w-full; }
.statusbar .k { @apply text-sm text-gray-500 dark:text-gray-400; }
.statusbar .v { @apply text-lg font-semibold mt-0.5; }
.statusbar .v.ok { @apply text-green-600 dark:text-green-400; }
.statusbar .v.crit { @apply text-red-600 dark:text-red-400; }
.status { @apply text-sm rounded-full px-2 py-0.5; }
.status.ok { @apply bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-200; }
.status.crit { @apply bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-200; }

.flat { @apply bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300; }
.flat > :last-child { @apply border-b-0; }
.bar { @apply flex items-center gap-3 px-3.5 py-3.5 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex-wrap; }
.bar .count { @apply text-sm text-gray-500 dark:text-gray-400; }
.bar .count b { @apply text-gray-800 dark:text-gray-100 font-semibold; }
.search { @apply flex items-center gap-2 flex-1 max-w-72 h-9 px-3 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 <sm:max-w-none <sm:w-full; }
.search input { @apply flex-1 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400; }

.empty { @apply px-4.5 py-10 text-center text-gray-500 dark:text-gray-400; }
.empty b { @apply text-green-600 dark:text-green-400; }

.num { font-variant-numeric: tabular-nums; }
</style>
