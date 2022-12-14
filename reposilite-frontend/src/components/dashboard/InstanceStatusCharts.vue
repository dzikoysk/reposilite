<script setup>
import { ref } from "vue"
import { createErrorToast } from '../../helpers/toast'
import DashboardBox from "./DashboardBox.vue"
import { useSession } from "../../store/session"

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
})

const { client } = useSession()
const instanceStatus = ref()

function requestStatus() {
  if (props.selectedTab == 'Dashboard') {
    client.value.status.instance()
      .then(response => response.data)
      .then(instanceStatusData => {
        instanceStatus.value = instanceStatusData
        setTimeout(requestStatus, 1000)
      })
      .catch((error) => {
        createErrorToast(`Cannot load instance status`)
        console.log(error)
      })
  }
}
requestStatus()

const secondsToHms = (seconds) => {
    const h = Math.floor(seconds / 3600)
    const m = Math.floor(seconds % 3600 / 60)
    const s = Math.floor(seconds % 3600 % 60)

    const hDisplay = h > 0 ? h + "h " : ""
    const mDisplay = m > 0 ? m + "min " : ""
    const sDisplay = s > 0 ? s + "s" : ""
    
    return hDisplay + mDisplay + sDisplay
}
</script>

<template>
  <div v-if="instanceStatus">
    <div class="flex w-full">
      <DashboardBox 
        title="Version"
        :content="instanceStatus.version"
        link="https://github.com/dzikoysk/reposilite/releases"
      />
      <DashboardBox 
        title="Uptime"
        :content="secondsToHms(instanceStatus.uptime / 1000)"
      />
      <DashboardBox 
        title="Used memory"
        :content="instanceStatus.usedMemory.toFixed(1) + ' of ' + instanceStatus.maxMemory"
      />
      <DashboardBox 
        title="Used threads"
        :content="instanceStatus.usedThreads + ' of ' + instanceStatus.maxThreads"
      />
      <DashboardBox 
        title="Failures"
        :content="instanceStatus.failuresCount.toString()"
      />
    </div>
  </div>
</template>