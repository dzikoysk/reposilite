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
const failuresError = ref(false)
const query = ref('')

function requestFailures() {
  failuresError.value = false
  client.value.status.failures()
    .then(response => failures.value = response.data.failures)
    .catch(error => {
      failuresError.value = true
      createErrorToast(`Cannot load recorded failures`)
      console.log(error)
    })
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
          failuresError.value = false
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
            Failures
          </dt><dd class="text-lg font-semibold mt-0.5 tabular-nums">
            {{ instanceStatus.failuresCount }}
          </dd>
        </div>
        <div
          class="text-sm rounded-full px-2 py-0.5"
          :class="instanceStatus.failuresCount > 0 ? 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-200' : 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-200'"
        >
          <dt class="sr-only">Review status</dt>
          <dd>{{ instanceStatus.failuresCount > 0 ? 'review' : 'clean' }}</dd>
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
    </div>

    <section
      class="bg-white dark:bg-gray-900 rounded-lg overflow-hidden text-sm text-gray-600 dark:text-gray-300"
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

    <section
      class="mt-5 bg-white dark:bg-gray-900 rounded-lg p-5"
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
            Memory and thread samples collected from runtime status snapshots.
          </p>
        </div>
      </div>
      <StatusSnapshotsChart :selected-tab="selectedTab" />
    </section>
  </div>
</template>
