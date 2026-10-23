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
import { ref, computed, defineAsyncComponent, onUnmounted } from "vue"
import { useSession } from "../../store/session"
import { createErrorToast } from '../../helpers/toast'

const TimeSeriesChart = defineAsyncComponent(() => import('./TimeSeriesChart.vue'))

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})

const { client } = useSession()
const statusSnapshots = ref()
let statusTimeout
const statusSnapshotsSeries = computed(() => {
  return [
    {
      name: 'Used memory (MB)',
      data: statusSnapshots.value.map(record => ({ x: record.at, y: record.memory }))
    },
    {
      name: 'Used threads',
      data: statusSnapshots.value.map(record => ({ x: record.at, y: record.threads }))
    }
  ]
})

function requestStatus() {
  if (props.selectedTab == 'Diagnostics') {
    client.value.status.snapshots()
      .then(response => response.data)
      .then(snapshotsData => {
        statusSnapshots.value = snapshotsData
        statusTimeout = setTimeout(requestStatus, 1000 * 30)
      })
      .catch(error => {
        console.log(error)
        createErrorToast(`Cannot load status snapshots statistics`)
      })
  }
}
requestStatus()
onUnmounted(() => clearTimeout(statusTimeout))
</script>

<template>
  <figure v-if="statusSnapshots">
    <figcaption class="sr-only">
      Runtime memory and thread usage over time
    </figcaption>
    <TimeSeriesChart
      label="Runtime memory and thread usage over time"
      :series="statusSnapshotsSeries"
    />
  </figure>
</template>
