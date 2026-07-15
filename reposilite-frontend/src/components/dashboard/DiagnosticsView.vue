<script setup>
import { ref, computed } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"
import FailuresList from "./FailuresList.vue"
import StatusSnapshotsChart from "./StatusSnapshotsChart.vue"
import ViewHeader from '../util/ViewHeader.vue'

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

function requestFailures() {
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
        if (instanceStatusData.failuresCount > 0) {
          requestFailures()
        } else {
          failures.value = []
        }
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

const failureText = (failure) =>
  typeof failure === 'string'
    ? failure
    : [
        failure?.path,
        failure?.type,
        failure?.message,
        ...(failure?.messages || []),
        failure?.trace
      ].filter(Boolean).join(' ')

const failureOccurrences = (failure) =>
  typeof failure === 'string' ? 1 : failure?.occurrences || 1

const totalFailures = computed(() =>
  failures.value.reduce((total, failure) => total + failureOccurrences(failure), 0))

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return failures.value
  return failures.value.filter(failure => failureText(failure).toLowerCase().includes(q))
})
</script>

<template>
  <div v-if="instanceStatus" class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Runtime health"
      description="Service status, resource usage, and recorded failures since the last restart."
    />

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
      <div class="section-head">
        <div class="section-copy">
          <div class="section-title">
            <h2>Recorded failures</h2>
            <span class="count"><b class="num">{{ failures.length }}</b> unique</span>
            <span v-if="totalFailures !== failures.length" class="count"><b class="num">{{ totalFailures }}</b> occurrences</span>
          </div>
          <p>Exception traces captured by this instance since the last restart.</p>
        </div>
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

    <div class="chart-block">
      <div class="section-head">
        <div class="section-copy">
          <h2>Resource usage</h2>
          <p>Memory and thread samples collected from runtime status snapshots.</p>
        </div>
      </div>
      <StatusSnapshotsChart :selected-tab="selectedTab" />
    </div>
  </div>
</template>

<style scoped>
.statusbar { @apply flex flex-wrap gap-4 mb-5; }
.statusbar .block { @apply flex-1 min-w-40 bg-white dark:bg-gray-900 rounded-lg px-5 py-4 flex items-center justify-between <sm:min-w-full; }
.statusbar .k { @apply text-sm text-gray-500 dark:text-gray-400; }
.statusbar .v { @apply text-lg font-semibold mt-0.5; }
.statusbar .v.ok { @apply text-green-600 dark:text-green-400; }
.statusbar .v.crit { @apply text-red-600 dark:text-red-400; }
.status { @apply text-sm rounded-full px-2 py-0.5; }
.status.ok { @apply bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-200; }
.status.crit { @apply bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-200; }

.section-head { @apply mb-3 flex items-start justify-between gap-3 min-w-0 <sm:flex-col; }
.section-copy { @apply min-w-0; }
.section-title { @apply flex flex-wrap items-baseline gap-2; }
.section-head h2 { @apply text-base font-semibold leading-6 text-gray-800 dark:text-gray-100; }
.section-head p { @apply mt-0.5 truncate text-sm leading-5 text-gray-500 dark:text-gray-400 <md:whitespace-normal <md:overflow-visible; }
.section-head .count { @apply text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap; }
.section-head .count b { @apply text-gray-800 dark:text-gray-100 font-semibold; }

.chart-block { @apply mt-5 bg-white dark:bg-gray-900 rounded-lg p-5; }

.flat { @apply bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300; }
.flat .section-head { @apply mb-0 px-4.5 py-4 border-b border-gray-200 dark:border-gray-800; }
.flat > :last-child { @apply border-b-0; }
.search { @apply flex items-center gap-2 w-72 h-9 px-3 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 <sm:w-full; }
.search input { @apply flex-1 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400; }

.empty { @apply px-4.5 py-10 text-center text-gray-500 dark:text-gray-400; }
.empty b { @apply text-green-600 dark:text-green-400; }

.num { font-variant-numeric: tabular-nums; }
</style>
