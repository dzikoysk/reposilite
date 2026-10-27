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
import { ref, computed, onUnmounted } from "vue"
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
const failuresError = ref(false)
const query = ref('')
let statusTimeout
let stopped = false
let statusFailed = false

function requestFailures() {
  return client.value.status.failures()
    .then(response => {
      failures.value = response.data.failures
      failuresError.value = false
    })
    .catch(error => {
      if (!failuresError.value) createErrorToast(`Cannot load recorded failures`)
      failuresError.value = true
      console.log(error)
    })
}

function requestStatus() {
  if (props.selectedTab == 'Diagnostics' && !stopped) {
    client.value.status.instance()
      .then(response => response.data)
      .then(instanceStatusData => {
        statusFailed = false
        const failuresChanged = instanceStatus.value?.failuresCount !== instanceStatusData.failuresCount
        instanceStatus.value = instanceStatusData
        if (instanceStatusData.failuresCount > 0 && (failuresChanged || failuresError.value)) {
          return requestFailures()
        } else if (instanceStatusData.failuresCount === 0) {
          failuresError.value = false
          failures.value = []
        }
      })
      .catch((error) => {
        if (!statusFailed) createErrorToast(`Cannot load instance status`)
        statusFailed = true
        console.log(error)
      })
      .finally(() => {
        if (!stopped) statusTimeout = setTimeout(requestStatus, 1000)
      })
  }
}
requestStatus()
onUnmounted(() => {
  stopped = true
  clearTimeout(statusTimeout)
})

function requestHealth() {
  if (props.selectedTab == 'Diagnostics') {
    client.value.status.health()
      .then(response => response.data)
      .then(healthData => {
        health.value = healthData
      })
      .catch(error => {
        createErrorToast(`Cannot load instance health`)
        console.log(error)
      })
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

const pools = computed(() => [
  { id: 'web', label: 'Web workers', usage: instanceStatus.value?.resourcePools.web },
  { id: 'io', label: 'Background IO', usage: instanceStatus.value?.resourcePools.io },
  { id: 'database', label: 'Database', usage: instanceStatus.value?.resourcePools.database }
])

const poolUsage = (pool) =>
  Math.min(100, Math.round(pool.active / pool.capacity * 100))

const failureText = (failure) =>
  [failure.path, failure.type, failure.message, ...failure.messages, failure.trace].join(' ')

const totalFailures = computed(() =>
  failures.value.reduce((total, failure) => total + failure.occurrences, 0))

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return failures.value
  return failures.value.filter(failure => failureText(failure).toLowerCase().includes(q))
})
</script>

<template>
  <div
    v-if="instanceStatus"
    class="container mx-auto pt-7 px-15 pb-12 <sm:px-4"
  >
    <ViewHeader
      title="Runtime health"
      description="Service status, resource usage, and recorded failures since the last restart."
    />

    <div class="flex flex-wrap gap-4 mb-5">
      <dl class="flex-1 min-w-40 bg-white dark:bg-gray-900 rounded-lg px-5 py-4 flex items-center justify-between <sm:min-w-full">
        <div>
          <dt class="text-sm text-gray-500 dark:text-gray-400">
            Instance
          </dt>
          <dd
            class="text-lg font-semibold mt-0.5"
            :class="health?.status ? (health.status === 'UP' ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400') : ''"
          >
            {{ health?.status === 'UP' ? 'Online' : (health?.status || '…') }}
          </dd>
        </div>
      </dl>
      <dl class="flex-1 min-w-40 bg-white dark:bg-gray-900 rounded-lg px-5 py-4 flex items-center justify-between <sm:min-w-full">
        <div>
          <dt class="text-sm text-gray-500 dark:text-gray-400">
            Uptime
          </dt><dd class="text-lg font-semibold mt-0.5 tabular-nums">
            {{ prettyUptime(instanceStatus.uptime / 1000) }}
          </dd>
        </div>
      </dl>
      <dl class="flex-1 min-w-40 bg-white dark:bg-gray-900 rounded-lg px-5 py-4 flex items-center justify-between <sm:min-w-full">
        <div>
          <dt class="text-sm text-gray-500 dark:text-gray-400">
            Failures
          </dt><dd class="text-lg font-semibold mt-0.5 tabular-nums">
            {{ instanceStatus.failuresCount }}
          </dd>
        </div>
      </dl>
    </div>

    <section
      class="mb-5 bg-white dark:bg-gray-900 rounded-lg p-5"
      aria-labelledby="pool-usage-heading"
    >
      <h2
        id="pool-usage-heading"
        class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100"
      >
        Worker and connection pools
      </h2>
      <div class="mt-2 divide-y divide-gray-150 dark:divide-gray-800">
        <div
          v-for="pool in pools"
          :key="pool.id"
          class="grid grid-cols-12 items-center gap-x-4 gap-y-2 py-3 text-sm"
        >
          <h3 class="col-span-3 order-1 font-semibold text-gray-800 dark:text-gray-100 <sm:col-span-6">
            {{ pool.label }}
          </h3>
          <template v-if="pool.usage">
            <div class="col-span-5 order-2 <sm:col-span-12 <sm:order-3">
              <div
                v-if="pool.usage.capacity > 0"
                class="h-1.5 rounded-full bg-gray-150 dark:bg-gray-800 overflow-hidden"
                role="progressbar"
                :aria-label="`${pool.label} in use`"
                aria-valuemin="0"
                :aria-valuemax="pool.usage.capacity"
                :aria-valuenow="pool.usage.active"
              >
                <div
                  class="h-full rounded-full"
                  :class="(pool.usage.queued ?? pool.usage.waiting) > 0 ? 'bg-yellow-600 dark:bg-yellow-400' : 'bg-blue-600 dark:bg-blue-500'"
                  :style="{ width: poolUsage(pool.usage) + '%' }"
                />
              </div>
            </div>
            <div class="col-span-2 order-3 text-right tabular-nums whitespace-nowrap <sm:col-span-6 <sm:order-2">
              {{ pool.usage.active }} <span class="text-gray-500 dark:text-gray-400">/ <span
                :class="{ 'underline decoration-dotted underline-offset-3 cursor-help': pool.id === 'web' }"
                :title="pool.id === 'web' ? `${pool.usage.capacity} available workers out of ${pool.usage.capacity + pool.usage.reserved} configured, with ${pool.usage.reserved} reserved by the HTTP server` : undefined"
              >{{ pool.usage.capacity }}</span></span>
            </div>
            <p
              class="col-span-2 order-4 text-right text-xs tabular-nums <sm:col-span-12 <sm:text-left"
              :class="(pool.usage.queued ?? pool.usage.waiting) > 0 ? 'text-yellow-700 dark:text-yellow-400' : 'text-gray-500 dark:text-gray-400'"
            >
              {{ pool.usage.queued ?? pool.usage.waiting }} {{ pool.id === 'database' ? 'waiting' : 'queued' }}
            </p>
          </template>
          <p
            v-else
            class="col-span-9 order-2 text-right text-gray-500 dark:text-gray-400 <sm:col-span-6"
          >
            Unavailable
          </p>
        </div>
      </div>
      <div class="pt-3 border-t border-gray-150 dark:border-gray-800 flex justify-between items-baseline flex-wrap gap-2 text-sm">
        <span class="text-gray-500 dark:text-gray-400">Live JVM platform threads, including idle workers and runtime services</span>
        <span class="font-semibold tabular-nums">{{ instanceStatus.usedThreads }}</span>
      </div>
    </section>

    <section
      class="bg-white dark:bg-gray-900 rounded-lg p-5"
      aria-labelledby="resource-usage-heading"
    >
      <div class="mb-3 flex items-start justify-between gap-3 min-w-0 <sm:flex-col">
        <div class="min-w-0">
          <h2
            id="resource-usage-heading"
            class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100"
          >
            Resource usage
          </h2>
          <p class="mt-0.5 truncate text-sm leading-5 text-gray-500 dark:text-gray-400 <md:whitespace-normal <md:overflow-visible">
            Heap memory and JVM platform threads sampled over time.
          </p>
        </div>
      </div>
      <StatusSnapshotsChart :selected-tab="selectedTab" />
    </section>

    <section
      class="mt-5 bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300"
      aria-labelledby="recorded-failures-heading"
    >
      <div class="mb-0 px-4.5 py-4 border-b border-gray-200 dark:border-gray-800 flex items-start justify-between gap-3 min-w-0 <sm:flex-col">
        <div class="min-w-0">
          <div class="flex flex-wrap items-baseline gap-2">
            <h2
              id="recorded-failures-heading"
              class="text-base font-semibold leading-6 text-gray-800 dark:text-gray-100"
            >
              Recorded failures
            </h2>
            <span class="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap"><b class="text-gray-800 dark:text-gray-100 font-semibold tabular-nums">{{ failures.length }}</b> unique</span>
            <span
              v-if="totalFailures !== failures.length"
              class="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap"
            ><b class="text-gray-800 dark:text-gray-100 font-semibold tabular-nums">{{ totalFailures }}</b> occurrences</span>
          </div>
          <p class="mt-0.5 truncate text-sm leading-5 text-gray-500 dark:text-gray-400 <md:whitespace-normal <md:overflow-visible">
            Exception traces captured by this instance since the last restart.
          </p>
        </div>
        <div class="flex items-center gap-2 w-72 h-9 px-3 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 <sm:w-full">
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
            class="flex-1 bg-transparent outline-none text-gray-700 dark:text-gray-200 placeholder-gray-500 dark:placeholder-gray-400"
            placeholder="Filter by type or path…"
            aria-label="Filter failures by type or path"
          >
        </div>
      </div>
      <FailuresList
        v-if="!failuresError && filtered.length"
        :failures="filtered"
        :version="instanceStatus.version"
      />
      <div
        v-else
        class="px-4.5 py-10 text-center text-gray-500 dark:text-gray-400"
        :role="failuresError ? 'alert' : 'status'"
      >
        <p v-if="failuresError">
          <b class="text-green-600 dark:text-green-400">Cannot load failures.</b> Check the connection and try again.
        </p>
        <p v-else-if="failures.length">
          <b class="text-green-600 dark:text-green-400">No matches.</b> No recorded failures match “{{ query }}”.
        </p>
        <p v-else>
          <b class="text-green-600 dark:text-green-400">All clear.</b> No exceptions recorded since the last restart.
        </p>
      </div>
    </section>
  </div>
</template>
