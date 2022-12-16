<script setup>
import { ref, defineAsyncComponent } from "vue"
import { createErrorToast } from '../../helpers/toast'
import { useSession } from "../../store/session"

const VueApexCharts = defineAsyncComponent(() => import('vue3-apexcharts'))

const { client } = useSession()
const statisticsEnabled = ref(false)
const resolvedSeries = ref()

client.value.statistics.allResolved()
  .then(response => response.data)
  .then(allResolved => {
    resolvedSeries.value = allResolved.repositories.map(repositoryStatistics => {
      return {
        name: repositoryStatistics.name,
        data: repositoryStatistics.data.map(record => ({
          x: record.date,
          y: record.count
        }))
      }
    })
    console.log(resolvedSeries.value)
    statisticsEnabled.value = allResolved.statisticsEnabled
  })
  .catch(error => {
    console.log(error)
    createErrorToast(`Cannot load statistics`)
  })

const chartOptions = {
  chart: {
    id: "reposilite-requests-over-time",
    stacked: true
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
  <div v-if="statisticsEnabled">
    <h1 class="font-bold text-lg">Resolved requests</h1>
    <VueApexCharts 
      class="dark:text-black pt-2"
      width="100%"
      height="320px"
      type="area"
      :options="chartOptions"
      :series="resolvedSeries"
    />
  </div>
</template>