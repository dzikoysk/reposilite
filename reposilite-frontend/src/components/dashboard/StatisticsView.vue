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
const isLoading = ref(false)
const dateFormatter = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  year: 'numeric'
})

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
      range: dates.length
        ? `${dateFormatter.format(new Date(Number(dates[0])))} - ${dateFormatter.format(new Date(Number(dates.at(-1))))}`
        : 'No recorded period',
      labels
    }
    search()
  })
  .catch(error => {
    console.log(error)
    createErrorToast('Cannot load statistics')
  })

let searchGeneration = 0
function search(offset = 0) {
  if (!statisticsEnabled.value) return
  const generation = ++searchGeneration
  isLoading.value = true
  client.value.statistics.resolvedEntries(limit.value, repository.value, phrase.value, offset)
    .then(response => {
      if (generation === searchGeneration) {
        results.value = response.data
      }
    })
    .catch(error => {
      if (generation !== searchGeneration) return
      results.value = null
      console.log(error)
      createErrorToast('Cannot load resolved paths')
    })
    .finally(() => {
      if (generation === searchGeneration) isLoading.value = false
    })
}

watch([repository, limit], () => search())
watch(phrase, (_, __, onCleanup) => {
  searchGeneration++
  isLoading.value = true
  const debounce = setTimeout(search, 1000)
  onCleanup(() => clearTimeout(debounce))
})

const entries = computed(() => results.value?.entries || [])
const page = computed(() => results.value?.page)
const firstEntry = computed(() => (page.value?.offset || 0) + 1)
const lastEntry = computed(() => (page.value?.offset || 0) + entries.value.length)
const maxCount = computed(() => entries.value[0]?.count || 1)
const barWidth = (count) => `${Math.max(4, Math.round((count / maxCount.value) * 100))}%`
const previousPage = () => search(Math.max(0, (page.value?.offset || 0) - limit.value))
const nextPage = () => page.value?.nextOffset != null && search(page.value.nextOffset)
</script>

<template>
  <div class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Traffic analytics"
      description="Resolved request volume and path demand."
    />

    <template v-if="statisticsEnabled">
      <div class="bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300">
        <div class="px-4.5 py-4 border-b border-gray-200 dark:border-gray-800">
          <h2 class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100">
            Most resolved paths
          </h2>
          <p class="mt-0.5 text-sm leading-5 text-gray-500 dark:text-gray-400">
            Highest requested artifact paths in the selected repository.
          </p>
        </div>
        <div class="flex items-center gap-3 px-3.5 py-3.5 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex-wrap">
          <div class="flex items-center gap-2 flex-1 min-w-48 px-3 h-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 <sm:w-full <sm:min-w-0">
            <svg
              viewBox="0 0 24 24"
              class="w-4 h-4 flex-shrink-0 text-gray-400"
            ><path
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              d="M21 21l-4.3-4.3m1.3-5.2a7 7 0 11-14 0 7 7 0 0114 0z"
            /></svg>
            <input
              v-model="phrase"
              class="flex-1 min-w-0 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400"
              placeholder="Filter by path…"
            >
            <span
              v-if="isLoading"
              class="h-4 w-4 flex-none animate-spin rounded-full border-2 border-gray-300 border-t-blue-600 dark:border-gray-600 dark:border-t-blue-400"
              role="status"
              aria-label="Loading resolved paths"
            />
          </div>
          <select
            v-model="repository"
            class="h-9 min-w-36 pl-3 pr-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 text-sm outline-none <sm:flex-1"
          >
            <option value="">
              All repositories
            </option>
            <option
              v-for="repo in repositories"
              :key="repo.id"
              :value="repo.id"
            >
              {{ repo.id }}
            </option>
          </select>
          <select
            v-model.number="limit"
            class="h-9 min-w-36 pl-3 pr-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 text-sm outline-none <sm:flex-1"
          >
            <option :value="10">
              Top 10
            </option>
            <option :value="20">
              Top 20
            </option>
            <option :value="50">
              Top 50
            </option>
            <option :value="100">
              Top 100
            </option>
          </select>
        </div>

        <div v-if="results">
          <div
            v-for="(entry, index) in entries"
            :key="`${entry.repository}:${entry.path}`"
            class="flex items-center gap-3.5 px-4.5 h-12 border-b border-gray-200 dark:border-gray-800 last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-800"
          >
            <span class="w-5 text-right text-gray-400 dark:text-gray-600 text-sm flex-none tabular-nums">{{ firstEntry + index }}</span>
            <span class="flex-1 min-w-0 truncate text-sm text-gray-800 dark:text-gray-100 font-mono">/{{ repository ? entry.path : `${entry.repository}/${entry.path}` }}</span>
            <span class="w-32 h-2 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden flex-none <sm:hidden"><i
              class="block h-full rounded-full bg-blue-600 dark:bg-blue-500 opacity-85"
              :style="{ width: barWidth(entry.count) }"
            /></span>
            <span class="w-16 text-right text-base font-semibold flex-none tabular-nums">{{ entry.count.toLocaleString() }}</span>
          </div>
          <div
            v-if="!entries.length"
            class="px-4.5 py-10 text-center text-gray-500 dark:text-gray-400"
          >
            {{ phrase ? `No paths match “${phrase}”.` : 'No resolved requests recorded yet.' }}
          </div>
          <div
            v-if="entries.length || page?.offset"
            class="flex items-center justify-between gap-3 border-t border-gray-200 px-4.5 py-2.5 text-xs text-gray-500 dark:border-gray-800 dark:text-gray-400 <sm:flex-wrap"
          >
            <span v-if="entries.length">Showing <b class="font-semibold tabular-nums text-gray-800 dark:text-gray-100">{{ firstEntry.toLocaleString() }}-{{ lastEntry.toLocaleString() }}</b></span>
            <span v-else>No results</span>
            <div class="ml-auto flex items-center gap-2 <sm:ml-0">
              <button
                v-if="page?.offset"
                class="h-8 rounded-md border border-gray-300 px-3 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                @click="previousPage"
              >
                Previous
              </button>
              <button
                v-if="page?.hasMore"
                class="h-8 rounded-md border border-gray-300 px-3 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                @click="nextPage"
              >
                Next
              </button>
            </div>
          </div>
        </div>
        <div
          v-else-if="isLoading"
          class="flex min-h-36 items-center justify-center gap-3 px-4.5 py-10 text-gray-500 dark:text-gray-400"
        >
          <span class="h-5 w-5 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600 dark:border-gray-600 dark:border-t-blue-400" />
          Loading resolved paths…
        </div>
      </div>

      <div
        v-if="kpis"
        class="mt-6 bg-white dark:bg-gray-900 rounded-lg p-5"
      >
        <div class="mb-4">
          <h2 class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100">
            Resolved requests
          </h2>
          <p class="mt-0.5 text-sm leading-5 text-gray-500 dark:text-gray-400">
            {{ kpis.range }}
          </p>
        </div>
        <div class="flex gap-8 flex-wrap mb-4 pb-4 border-b border-gray-200 dark:border-gray-800">
          <div>
            <div class="text-sm text-gray-500 dark:text-gray-400">
              Total
            </div><div class="text-xl font-semibold mt-0.5 tabular-nums">
              {{ kpis.total.toLocaleString() }}
            </div>
          </div>
          <div>
            <div class="text-sm text-gray-500 dark:text-gray-400">
              {{ kpis.labels.average }}
            </div><div class="text-xl font-semibold mt-0.5 tabular-nums">
              {{ kpis.average.toLocaleString() }}
            </div>
          </div>
          <div>
            <div class="text-sm text-gray-500 dark:text-gray-400">
              {{ kpis.labels.current }}
            </div><div class="text-xl font-semibold mt-0.5 tabular-nums">
              {{ kpis.current.toLocaleString() }}
            </div>
          </div>
        </div>
        <ResolvedRequestsChart :statistics="resolvedStatistics" />
      </div>
    </template>
    <div
      v-else-if="statisticsEnabled === false"
      class="bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300 px-4.5 py-10 text-center text-gray-500 dark:text-gray-400"
    >
      <b>Statistics disabled.</b> Enable resolved request statistics to view traffic analytics.
    </div>
  </div>
</template>
