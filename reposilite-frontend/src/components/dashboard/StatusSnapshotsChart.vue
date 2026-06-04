<script setup>
import { ref, computed, defineAsyncComponent } from "vue"
import { useSession } from "../../store/session"
import { createErrorToast } from '../../helpers/toast'
import { useI18n } from 'vue-i18n'

const VueApexCharts = defineAsyncComponent(() => import('vue3-apexcharts'))

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})

const { client } = useSession()
const { t } = useI18n()
const statusSnapshots = ref()
const statusSnapshotsSeries = computed(() => {
  return [
    {
      name: t('dashboard.usedMemoryMb'),
      data: statusSnapshots.value.map(record => [record.at, record.memory])
    },
    {
      name: t('dashboard.usedThreads'),
      data: statusSnapshots.value.map(record => [record.at, record.threads])
    }
  ]
})

function requestStatus() {
  if (props.selectedTab == 'Dashboard') {
    client.value.status.snapshots()
      .then(response => response.data)
      .then(snapshotsData => {
        statusSnapshots.value = snapshotsData
        console.log(statusSnapshots.value)
        setTimeout(requestStatus, 1000 * 30)
      })
      .catch(error => {
        console.log(error)
        createErrorToast(t('dashboard.cannotLoadStatusSnapshots'))
      })
  }
}
requestStatus()

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
    <h1 class="font-bold pt-6 text-lg">{{ t('dashboard.resources') }}</h1>
    <VueApexCharts 
      class="dark:text-black pt-1"
      width="100%"
      height="320px"
      type="line"
      :options="chartOptions"
      :series="statusSnapshotsSeries"
    />
  </div>
</template>
