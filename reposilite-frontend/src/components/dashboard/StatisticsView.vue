<script setup>
import { ref, computed, watch } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"
import ResolvedRequestsChart from "./ResolvedRequestsChart.vue"
import ViewHeader from '../util/ViewHeader.vue'

const { client } = useSession()

const repositories = ref([])
const kpis = ref(null)
const resolvedStatistics = ref(null)
const statisticsEnabled = ref(null)

const phrase = ref('')
const repository = ref('')
const limit = ref(20)
const results = ref(null)

const intervalLabels = {
  DAILY: { average: 'Average / day', current: 'Today' },
  WEEKLY: { average: 'Average / week', current: 'This week' },
  MONTHLY: { average: 'Average / month', current: 'This month' },
  YEARLY: { average: 'Average / year', current: 'This year' }
}

client.value.settings.fetch('maven')
  .then(response => repositories.value = response.data.repositories || [])
  .catch(error => {
    console.log(error)
    createErrorToast('Cannot load repositories')
  })

client.value.statistics.allResolved()
  .then(response => response.data)
  .then(allResolved => {
    statisticsEnabled.value = allResolved.statisticsEnabled
    if (!allResolved.statisticsEnabled) return
    resolvedStatistics.value = allResolved
    const points = allResolved.repositories.flatMap(repo => repo.data)
    const byDate = {}
    points.forEach(point => byDate[point.date] = (byDate[point.date] || 0) + point.count)
    const dates = Object.keys(byDate).sort()
    const total = dates.reduce((sum, date) => sum + byDate[date], 0)
    const labels = intervalLabels[allResolved.interval] || intervalLabels.MONTHLY
    kpis.value = {
      total,
      average: dates.length ? Math.round(total / dates.length) : 0,
      current: dates.length ? byDate[dates.at(-1)] : 0,
      labels
    }
    search()
  })
  .catch(error => {
    console.log(error)
    createErrorToast('Cannot load statistics')
  })

let searchGeneration = 0
function search() {
  if (!statisticsEnabled.value) return
  const generation = ++searchGeneration
  client.value.statistics.resolvedEntries(limit.value, repository.value, phrase.value)
    .then(response => {
      if (generation === searchGeneration) results.value = response.data
    })
    .catch(error => {
      if (generation !== searchGeneration) return
      results.value = null
      console.log(error)
      createErrorToast('Cannot load resolved paths')
    })
}

watch([repository, limit], () => search())
watch(phrase, (_, __, onCleanup) => {
  searchGeneration++
  const debounce = setTimeout(search, 250)
  onCleanup(() => clearTimeout(debounce))
})

const entries = computed(() => results.value?.entries || [])
const maxCount = computed(() => entries.value[0]?.count || 1)
const barWidth = (count) => `${Math.max(4, Math.round((count / maxCount.value) * 100))}%`
</script>

<template>
  <div class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Traffic analytics"
      description="Resolved request volume and path demand."
    />

    <template v-if="statisticsEnabled">
      <div class="section-head">
        <h1 class="font-semibold text-lg">Most resolved paths</h1>
      </div>

      <div class="flat">
        <div class="bar">
          <div class="search">
            <svg viewBox="0 0 24 24" class="w-4 h-4 flex-shrink-0 text-gray-400"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M21 21l-4.3-4.3m1.3-5.2a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
            <input v-model="phrase" placeholder="Filter by path…" />
          </div>
          <select class="sel" v-model="repository">
            <option value="">All repositories</option>
            <option v-for="repo in repositories" :key="repo.id" :value="repo.id">{{ repo.id }}</option>
          </select>
          <select class="sel" v-model.number="limit">
            <option :value="10">Top 10</option>
            <option :value="20">Top 20</option>
            <option :value="50">Top 50</option>
            <option :value="100">Top 100</option>
          </select>
          <span v-if="results" class="sum">
            Showing <b class="num">{{ entries.length.toLocaleString() }}</b><template v-if="results.page?.hasMore">+</template>
          </span>
        </div>

        <div v-if="results" class="rows">
          <div v-for="(entry, index) in entries" :key="`${entry.repository}:${entry.path}`" class="srow">
            <span class="rank num">{{ index + 1 }}</span>
            <span class="path font-mono">/{{ repository ? entry.path : `${entry.repository}/${entry.path}` }}</span>
            <span class="rbar"><i :style="{ width: barWidth(entry.count) }"></i></span>
            <span class="cnt num">{{ entry.count.toLocaleString() }}</span>
          </div>
          <div v-if="!entries.length" class="empty">
            {{ phrase ? `No paths match “${phrase}”.` : 'No resolved requests recorded yet.' }}
          </div>
        </div>
      </div>

      <div v-if="kpis" class="chart-block">
        <div class="kpis">
          <div class="kpi"><div class="k">Visible period</div><div class="v num">{{ kpis.total.toLocaleString() }}</div></div>
          <div class="kpi"><div class="k">{{ kpis.labels.average }}</div><div class="v num">{{ kpis.average.toLocaleString() }}</div></div>
          <div class="kpi"><div class="k">{{ kpis.labels.current }}</div><div class="v num">{{ kpis.current.toLocaleString() }}</div></div>
        </div>
        <ResolvedRequestsChart :statistics="resolvedStatistics" />
      </div>
    </template>
    <div v-else-if="statisticsEnabled === false" class="flat empty">
      <b>Statistics disabled.</b> Enable resolved request statistics to view traffic analytics.
    </div>
  </div>
</template>

<style scoped>
.section-head { @apply mb-3 flex items-baseline justify-between gap-3; }

.chart-block { @apply bg-white dark:bg-gray-900 rounded-lg p-5; }
.flat + .chart-block { @apply mt-6; }

.kpis { @apply flex gap-8 flex-wrap mb-4 pb-4 border-b border-gray-200 dark:border-gray-800; }
.kpi .k { @apply text-sm text-gray-500 dark:text-gray-400; }
.kpi .v { @apply text-xl font-semibold mt-0.5; }

.flat { @apply bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300; }
.flat > :last-child { @apply border-b-0; }
.bar { @apply flex items-center gap-3 px-3.5 py-3.5 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex-wrap; }
.search { @apply flex items-center gap-2 flex-1 min-w-48 px-3 h-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 <sm:w-full <sm:min-w-0; }
.search input { @apply flex-1 min-w-0 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400; }
.sel { @apply h-9 min-w-36 pl-3 pr-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 text-sm outline-none <sm:flex-1; }
.sum { @apply ml-auto text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap <sm:ml-0 <sm:w-full; }
.sum b { @apply text-gray-800 dark:text-gray-100 font-semibold; }

.srow { @apply flex items-center gap-3.5 px-4.5 h-12 border-b border-gray-200 dark:border-gray-800; }
.srow:last-child { @apply border-b-0; }
.srow:hover { @apply bg-gray-50 dark:bg-gray-800; }
.rank { @apply w-5 text-right text-gray-400 dark:text-gray-600 text-sm flex-none; }
.path { @apply flex-1 min-w-0 truncate text-sm text-gray-800 dark:text-gray-100; }
.rbar { @apply w-32 h-2 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden flex-none <sm:hidden; }
.rbar i { @apply block h-full rounded-full bg-blue-600 dark:bg-blue-500 opacity-85; }
.cnt { @apply w-16 text-right text-base font-semibold flex-none; }

.empty { @apply px-4.5 py-10 text-center text-gray-500 dark:text-gray-400; }
.num { font-variant-numeric: tabular-nums; }
</style>
