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
  <figure>
    <figcaption class="sr-only">
      Resolved requests over time by repository
    </figcaption>
    <VueApexCharts 
      class="dark:text-black"
      width="100%"
      height="320px"
      type="area"
      :options="chartOptions"
      :series="resolvedSeries"
    />
  </figure>
</template>
