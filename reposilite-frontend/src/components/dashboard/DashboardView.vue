<script setup>
import InstanceStatusCharts from "./InstanceStatusCharts.vue"
import StatusSnapshotsChart from "./StatusSnapshotsChart.vue"
import ResolvedRequestsChart from "./ResolvedRequestsChart.vue"

defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})
</script>

<template>
  <div class="container mx-auto pt-6 px-15 pb-10 text-xs">
    <div class="flex">
      <h1 class="font-bold pb-6 text-lg">Instance status</h1>
      <p class="text-3xl text-green-500 px-3 -mt-1 font-bold">•</p>
    </div>
    <InstanceStatusCharts :selectedTab="selectedTab" />
    <StatusSnapshotsChart :selectedTab="selectedTab" />
    <ResolvedRequestsChart />
  </div>
</template>