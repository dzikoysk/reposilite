<script setup>
import { ref, defineAsyncComponent } from "vue"
import { createErrorToast } from '../../helpers/toast'
import DashboardBox from "./DashboardBox.vue"
import { useSession } from "../../store/session"

const VueApexCharts = defineAsyncComponent(() => import('vue3-apexcharts'))

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})

const { client } = useSession()
const instanceStatus = ref()
const statisticsEnabled = ref(false)
const resolvedSeries = ref()

setInterval(function requestStatus() {
  if (props.selectedTab == 'Dashboard') {
    client.value.status.instance()
      .then(response => response.data)
      .then(instanceStatusData => {
        instanceStatus.value = instanceStatusData
      })
      .catch((error) => {
        createErrorToast(`Cannot load instance status`)
        console.log(error)
      })
  }

  return requestStatus
}(), 3000)

client.value.statistics.allResolved()
  .then(response => response.data)
  .then(allResolved => {
    console.log(allResolved)
    resolvedSeries.value = allResolved.repositories.map(repositoryStatistics => {
      return {
        name: repositoryStatistics.name,
        data: repositoryStatistics.data.map(record => [record.date, record.count])
      }
    })
    statisticsEnabled.value = allResolved.statisticsEnabled
  })
  .catch(error => {
    console.log(error)
    createErrorToast(`Cannot load statistics`)
  })

const chartOptions = {
  chart: {
    id: "reposilite-requests-over-time",
  },
  xaxis: {
    type: 'datetime'
  },
  dataLabels: {
    enabled: false
  }
}
</script>

<template>
  <div class="container mx-auto pt-10 px-15 pb-10 text-xs">
    <div class="flex">
      <h1 class="font-bold pb-6 text-lg">Instance status</h1>
      <p class="text-3xl text-green-500 px-3 -mt-1 font-bold">•</p>
    </div>
    <div class="flex" v-if="instanceStatus">
      <DashboardBox 
        title="Version"
        :content="instanceStatus.version"
        link="https://github.com/dzikoysk/reposilite/releases"
      />
      <DashboardBox 
        title="Uptime"
        :content="instanceStatus.uptime"
      />
      <DashboardBox 
        title="Used memory"
        :content="instanceStatus.usedMemory"
      />
      <DashboardBox 
        title="Used threads"
        :content="instanceStatus.usedThreads.toString()"
      />
      <DashboardBox 
        title="Failures"
        :content="instanceStatus.failuresCount.toString()"
      />
    </div>
    <div v-if="statisticsEnabled">
      <h1 class="font-bold py-6 text-lg">Requests over time</h1>
      <VueApexCharts 
        width="100%"
        type="area"
        :stacked="true"
        :options="chartOptions"
        :series="resolvedSeries"
      />
    </div>
  </div>
</template>