<!--
  ~ Copyright (c) 2020-2026 dzikoysk
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<script setup>
import { ref, computed, watch } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"
import ResolvedRequestsChart from "./ResolvedRequestsChart.vue"
import ViewHeader from '../util/ViewHeader.vue'

defineEmits(['goto'])

const { client } = useSession()

const repositories = ref([])
const kpis = ref(null)
const resolvedStatistics = ref(null)
const statisticsEnabled = ref(null)
const statisticsInterval = ref('MONTHLY')
const selectedRange = ref('default')

const phrase = ref('')
const repository = ref('')
const limit = ref(20)
const results = ref(null)
const isLoading = ref(false)
const dateFormatter = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  timeZone: 'UTC'
})

const intervalLabels = {
  DAILY: { average: 'Average / day', current: 'Today' },
  WEEKLY: { average: 'Average / week', current: 'This week' },
  MONTHLY: { average: 'Average / month', current: 'This month' },
  YEARLY: { average: 'Average / year', current: 'This year' }
}

const intervalConfigurations = {
  DAILY: {
    adjective: 'daily',
    ranges: [
      { id: 'short', periods: 30, label: 'Last 30 days', buttonLabel: '30 days' },
      { id: 'default', periods: 90, label: 'Last 90 days', buttonLabel: '90 days' },
      { id: 'long', periods: 365, label: 'Last 365 days', buttonLabel: '1 year' }
    ]
  },
  WEEKLY: {
    adjective: 'weekly',
    ranges: [
      { id: 'short', periods: 4, label: 'Last 4 weeks', buttonLabel: '4 weeks' },
      { id: 'default', periods: 13, label: 'Last 13 weeks', buttonLabel: '13 weeks' },
      { id: 'long', periods: 52, label: 'Last 52 weeks', buttonLabel: '1 year' }
    ]
  },
  MONTHLY: {
    adjective: 'monthly',
    ranges: [
      { id: 'short', periods: 1, label: 'Current month', buttonLabel: 'This month' },
      { id: 'default', periods: 3, label: 'Last 3 months', buttonLabel: '3 months' },
      { id: 'long', periods: 12, label: 'Last 12 months', buttonLabel: '1 year' }
    ]
  },
  YEARLY: {
    adjective: 'yearly',
    ranges: [
      { id: 'default', periods: 1, label: 'Current year', buttonLabel: 'This year' },
      { id: 'short', periods: 3, label: 'Last 3 years', buttonLabel: '3 years' },
      { id: 'long', periods: 10, label: 'Last 10 years', buttonLabel: '10 years' }
    ]
  }
}

const intervalConfiguration = computed(() =>
  intervalConfigurations[statisticsInterval.value] || intervalConfigurations.MONTHLY)
const rangeOptions = computed(() => [
  ...intervalConfiguration.value.ranges,
  { id: 'all', label: 'All time', buttonLabel: 'All time' }
])
const statisticsDescription = computed(() => resolvedStatistics.value
  ? `Resolved request volume and path demand. Requests are recorded in ${intervalConfiguration.value.adjective} periods.`
  : 'Resolved request volume and path demand.')

const formatDate = date => dateFormatter.format(new Date(date))

function createDateRange(interval = statisticsInterval.value) {
  const option = rangeOptions.value.find(range => range.id === selectedRange.value)
  const to = new Date()
  if (!option || option.id === 'all') return { to: to.toISOString() }

  const from = new Date(to)
  const periods = option.periods - 1
  switch (interval) {
    case 'DAILY':
      from.setUTCDate(from.getUTCDate() - periods)
      break
    case 'WEEKLY': {
      const days = option.periods * 7
      from.setUTCDate(from.getUTCDate() - days + 1)
      break
    }
    case 'YEARLY':
      from.setUTCMonth(0, 1)
      from.setUTCFullYear(from.getUTCFullYear() - periods)
      break
    default:
      from.setUTCDate(1)
      from.setUTCMonth(from.getUTCMonth() - periods)
  }
  from.setUTCHours(0, 0, 0, 0)

  return { from: from.toISOString(), to: to.toISOString() }
}

client.value.settings.fetch('maven')
  .then(response => repositories.value = response.data.repositories || [])
  .catch(error => {
    console.log(error)
    createErrorToast('Cannot load repositories')
  })

let statisticsGeneration = 0
function loadStatistics(loadEntries = false) {
  const generation = ++statisticsGeneration
  const requestedInterval = statisticsInterval.value
  const requestedRange = createDateRange(requestedInterval)
  client.value.statistics.allResolved(requestedRange)
    .then(response => response.data)
    .then(allResolved => {
      if (generation !== statisticsGeneration) return
      statisticsEnabled.value = allResolved.statisticsEnabled
      if (!allResolved.statisticsEnabled) return
      statisticsInterval.value = allResolved.interval
      if (allResolved.interval !== requestedInterval && selectedRange.value !== 'all') {
        loadStatistics(loadEntries)
        return
      }
      resolvedStatistics.value = allResolved
      const points = allResolved.repositories.flatMap(repo => repo.data)
      const byDate = {}
      points.forEach(point => byDate[point.date] = (byDate[point.date] || 0) + point.count)
      const dates = Object.keys(byDate).sort()
      const rangeStart = dates.length ? Number(dates[0]) : null
      const total = dates.reduce((sum, date) => sum + byDate[date], 0)
      const labels = intervalLabels[allResolved.interval] || intervalLabels.MONTHLY
      kpis.value = {
        total,
        average: dates.length ? Math.round(total / dates.length) : 0,
        current: dates.length ? byDate[dates.at(-1)] : 0,
        range: rangeStart
          ? `${formatDate(rangeStart)} – ${formatDate(requestedRange.to)}`
          : 'No recorded period',
        labels
      }
      if (loadEntries) search()
    })
    .catch(error => {
      if (generation !== statisticsGeneration) return
      console.log(error)
      createErrorToast('Cannot load statistics')
    })
}

let searchGeneration = 0
function search(offset = 0) {
  if (!statisticsEnabled.value) return
  const generation = ++searchGeneration
  isLoading.value = true
  client.value.statistics.resolvedEntries(limit.value, repository.value, phrase.value, offset, createDateRange())
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
watch(selectedRange, () => {
  loadStatistics()
  search()
})
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

loadStatistics(true)
</script>

<template>
  <div class="container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Traffic analytics"
      :description="statisticsDescription"
    >
      <template #note>
        <button
          type="button"
          class="relative -top-0.5 -my-1 ml-0.5 inline-flex h-7 w-7 items-center justify-center rounded-md align-middle leading-none text-gray-400 hover:text-gray-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-gray-500 dark:hover:text-gray-200"
          aria-label="Open settings"
          title="Open settings"
          @click="$emit('goto', 'Settings')"
        >
          <svg
            viewBox="0 0 24 24"
            class="h-4 w-4"
            fill="none"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
              d="M9.6 3.9l.2 1.3c.1.4-.1.7-.4.9l-.4.2c-.3.2-.7.3-1.1.1L6.7 6a1.1 1.1 0 00-1.4.5L4 8.7a1.1 1.1 0 00.2 1.4l1 .9c.3.3.4.6.4 1v.5c0 .4-.1.8-.4 1l-1 .9A1.1 1.1 0 004 15.8L5.3 18c.3.5.9.7 1.4.5l1.2-.5c.4-.1.8-.1 1.1.1l.4.2c.3.2.5.5.4.9l-.2 1.3c-.1.5.3 1 .9 1h2.6c.6 0 1-.4 1.1-1l.2-1.3c.1-.4.3-.7.6-.9l.4-.2c.3-.2.7-.3 1.1-.1l1.2.5c.5.2 1.1 0 1.4-.5l1.3-2.2c.3-.5.2-1.1-.2-1.4l-1-.9c-.3-.3-.4-.6-.4-1V12c0-.4.1-.8.4-1l1-.9c.4-.4.5-1 .2-1.4L19 6.5a1.1 1.1 0 00-1.4-.5l-1.2.5c-.4.1-.8.1-1.1-.1l-.4-.2c-.3-.2-.5-.5-.6-.9l-.2-1.3c-.1-.5-.5-1-1.1-1h-2.6c-.5 0-1 .4-1 1z"
            />
            <circle
              cx="12"
              cy="12.3"
              r="3"
              stroke-width="1.8"
            />
          </svg>
        </button>
      </template>
      <template #actions>
        <div
          v-if="statisticsEnabled"
          class="inline-flex max-w-full items-center gap-1 overflow-x-auto"
          role="group"
          aria-label="Statistics time range"
        >
          <button
            v-for="option in rangeOptions"
            :key="option.id"
            type="button"
            class="h-8 whitespace-nowrap rounded-md px-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1 dark:focus-visible:ring-offset-black"
            :class="selectedRange === option.id
              ? 'bg-blue-600 text-white shadow-sm dark:bg-blue-500'
              : 'text-gray-600 hover:bg-gray-200 dark:text-gray-300 dark:hover:bg-gray-800'"
            :aria-label="option.label"
            :aria-pressed="selectedRange === option.id"
            @click="selectedRange = option.id"
          >
            {{ option.buttonLabel }}
          </button>
        </div>
      </template>
    </ViewHeader>

    <template v-if="statisticsEnabled">
      <section
        class="bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300"
        aria-labelledby="resolved-paths-heading"
      >
        <div class="flex items-start justify-between gap-4 border-b border-gray-200 px-4.5 py-4 dark:border-gray-800 <sm:flex-col <sm:gap-1.5">
          <div class="min-w-0">
            <h2
              id="resolved-paths-heading"
              class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100"
            >
              Most resolved paths
            </h2>
            <p class="mt-0.5 text-sm leading-5 text-gray-500 dark:text-gray-400">
              Highest requested artifact paths in the selected repository and time range.
            </p>
          </div>
          <p
            v-if="kpis"
            class="flex flex-none items-center gap-1.5 whitespace-nowrap text-xs font-medium leading-5 text-gray-500 dark:text-gray-400"
          >
            <svg
              viewBox="0 0 24 24"
              class="h-3.5 w-3.5 flex-none"
              aria-hidden="true"
            ><path
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M7 3v3m10-3v3M4 9h16M5 5h14a1 1 0 011 1v14H4V6a1 1 0 011-1z"
            /></svg>
            <span>{{ kpis.range }}</span>
          </p>
        </div>
        <div class="flex items-center gap-3 px-3.5 py-3.5 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex-wrap">
          <div class="flex items-center gap-2 flex-1 min-w-48 px-3 h-9 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 <sm:w-full <sm:min-w-0">
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
              v-model="phrase"
              class="flex-1 min-w-0 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400"
              placeholder="Filter by path…"
              aria-label="Filter resolved paths"
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
            aria-label="Repository"
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
            aria-label="Number of paths"
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
          <ol :start="firstEntry">
            <li
              v-for="(entry, index) in entries"
              :key="`${entry.repository}:${entry.path}`"
              class="flex items-center gap-3.5 px-4.5 h-12 border-b border-gray-200 dark:border-gray-800 last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-800"
            >
              <span class="w-5 text-right text-gray-400 dark:text-gray-600 text-sm flex-none tabular-nums">{{ firstEntry + index }}</span>
              <span class="flex-1 min-w-0 truncate text-sm text-gray-800 dark:text-gray-100 font-mono">/{{ repository ? entry.path : `${entry.repository}/${entry.path}` }}</span>
              <span
                class="w-32 h-2 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden flex-none <sm:hidden"
                role="progressbar"
                aria-valuemin="0"
                :aria-valuemax="maxCount"
                :aria-valuenow="entry.count"
                :aria-label="`${entry.count.toLocaleString()} requests`"
              ><i
                class="block h-full rounded-full bg-blue-600 dark:bg-blue-500 opacity-85"
                :style="{ width: barWidth(entry.count) }"
              /></span>
              <span class="w-16 text-right text-base font-semibold flex-none tabular-nums">{{ entry.count.toLocaleString() }}</span>
            </li>
          </ol>
          <div
            v-if="!entries.length"
            class="px-4.5 py-10 text-center text-gray-500 dark:text-gray-400"
            role="status"
          >
            {{ phrase ? `No paths match “${phrase}”.` : 'No resolved requests recorded yet.' }}
          </div>
          <nav
            v-if="entries.length || page?.offset"
            class="flex items-center justify-between gap-3 border-t border-gray-200 px-4.5 py-2.5 text-xs text-gray-500 dark:border-gray-800 dark:text-gray-400 <sm:flex-wrap"
            aria-label="Resolved paths pages"
          >
            <span v-if="entries.length">Showing <b class="font-semibold tabular-nums text-gray-800 dark:text-gray-100">{{ firstEntry.toLocaleString() }}-{{ lastEntry.toLocaleString() }}</b></span>
            <span v-else>No results</span>
            <div class="ml-auto flex items-center gap-2 <sm:ml-0">
              <button
                v-if="page?.offset"
                type="button"
                class="h-8 rounded-md border border-gray-300 px-3 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                @click="previousPage"
              >
                Previous
              </button>
              <button
                v-if="page?.hasMore"
                type="button"
                class="h-8 rounded-md border border-gray-300 px-3 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                @click="nextPage"
              >
                Next
              </button>
            </div>
          </nav>
        </div>
        <div
          v-else-if="isLoading"
          class="flex min-h-36 items-center justify-center gap-3 px-4.5 py-10 text-gray-500 dark:text-gray-400"
          role="status"
        >
          <span
            class="h-5 w-5 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600 dark:border-gray-600 dark:border-t-blue-400"
            aria-hidden="true"
          />
          Loading resolved paths…
        </div>
      </section>

      <section
        v-if="kpis"
        class="mt-6 bg-white dark:bg-gray-900 rounded-lg p-5"
        aria-labelledby="resolved-requests-heading"
      >
        <div class="mb-4 flex items-start justify-between gap-4 <sm:flex-col <sm:gap-1.5">
          <h2
            id="resolved-requests-heading"
            class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100"
          >
            Resolved requests
          </h2>
          <p
            class="flex flex-none items-center gap-1.5 whitespace-nowrap text-xs font-medium leading-5 text-gray-500 dark:text-gray-400"
            aria-live="polite"
          >
            <svg
              viewBox="0 0 24 24"
              class="h-3.5 w-3.5 flex-none"
              aria-hidden="true"
            ><path
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M 7 3 V 6 M 17 3 V 6 M 4 9 H 20 M 5 5 H 19 A 1 1 0 0 1 20 6 V 20 H 4 V 6 A 1 1 0 0 1 5 5 Z"
            /></svg>
            <span>{{ kpis.range }}</span>
          </p>
        </div>
        <dl class="flex gap-8 flex-wrap mb-4 pb-4 border-b border-gray-200 dark:border-gray-800">
          <div>
            <dt class="text-sm text-gray-500 dark:text-gray-400">
              Total
            </dt><dd class="text-xl font-semibold mt-0.5 tabular-nums">
              {{ kpis.total.toLocaleString() }}
            </dd>
          </div>
          <div>
            <dt class="text-sm text-gray-500 dark:text-gray-400">
              {{ kpis.labels.average }}
            </dt><dd class="text-xl font-semibold mt-0.5 tabular-nums">
              {{ kpis.average.toLocaleString() }}
            </dd>
          </div>
          <div>
            <dt class="text-sm text-gray-500 dark:text-gray-400">
              {{ kpis.labels.current }}
            </dt><dd class="text-xl font-semibold mt-0.5 tabular-nums">
              {{ kpis.current.toLocaleString() }}
            </dd>
          </div>
        </dl>
        <ResolvedRequestsChart :statistics="resolvedStatistics" />
      </section>
    </template>
    <div
      v-else-if="statisticsEnabled === false"
      class="bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300 px-4.5 py-10 text-center text-gray-500 dark:text-gray-400"
      role="status"
    >
      <b>Statistics disabled.</b> Enable resolved request statistics to view traffic analytics.
    </div>
  </div>
</template>
