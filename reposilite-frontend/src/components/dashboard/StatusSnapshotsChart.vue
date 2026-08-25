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

const VueApexCharts = defineAsyncComponent(() => import('vue3-apexcharts'))

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
      data: statusSnapshots.value.map(record => [record.at, record.memory])
    },
    {
      name: 'Used threads',
      data: statusSnapshots.value.map(record => [record.at, record.threads])
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

const chartOptions = {
  chart: {
    id: "reposilite-instance-status",
  },
  tooltip: {
    shared: true,
  },
  xaxis: {
    type: 'datetime',
    axisBorder: {
      show: false
    },
    axisTicks: {
      show: false
    }
  },
  dataLabels: {
    enabled: false
  },
  dropShadow: {
    enabled: true,
  },
  onDatasetHover: {
    highlightDataSeries: true
  },
  theme: {
    palette: 'palette10'
  },
  legend: {
    itemMargin: {
      vertical: 15
    }
  }
}
</script>

<template>
  <figure v-if="statusSnapshots">
    <figcaption class="sr-only">
      Runtime memory and thread usage over time
    </figcaption>
    <VueApexCharts 
      class="dark:text-black"
      width="100%"
      height="320px"
      type="line"
      :options="chartOptions"
      :series="statusSnapshotsSeries"
    />
  </figure>
</template>
