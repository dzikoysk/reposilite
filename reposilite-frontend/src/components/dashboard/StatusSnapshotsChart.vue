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
  <div v-if="statusSnapshots">
    <VueApexCharts 
      class="dark:text-black"
      width="100%"
      height="320px"
      type="line"
      :options="chartOptions"
      :series="statusSnapshotsSeries"
    />
  </div>
</template>
