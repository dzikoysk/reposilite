<script setup>
import { computed, defineAsyncComponent } from "vue"

const VueApexCharts = defineAsyncComponent(() => import('vue3-apexcharts'))

const props = defineProps({
  statistics: {
    type: Object,
    required: true
  }
})

const resolvedSeries = computed(() => props.statistics.repositories.map(repositoryStatistics => ({
  name: repositoryStatistics.name,
  data: repositoryStatistics.data.map(record => ({
    x: record.date,
    y: record.count
  }))
})))

const chartOptions = {
  chart: {
    id: "reposilite-requests-over-time",
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
  <div>
    <h1 class="font-semibold text-lg">Resolved requests</h1>
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
