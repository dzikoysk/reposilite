<template>
  <div class="container mx-auto pt-10 px-15 text-xs">
    <div class="flex text-sm flex-col xl:flex-row w-full py-2 justify-between">
      <input placeholder="Filter" v-model="filter" class="w-full xl:w-1/2 mr-5 py-1 px-4 rounded-lg bg-white dark:bg-gray-900" />
      <div class="flex flex-row justify-around w-full xl:w-1/2">
        <div v-for="level in levels" :key="level.name" class="pt-1.9 xl:pt-0.8 font-sans">
          <input
            type="checkbox" 
            :checked="level.enabled" 
            @change="level.enabled = !level.enabled"
          >
          <span class="pl-2 pr-4">{{ level.name }} ({{ level.count }})</span>
        </div>
      </div>
    </div>
    <div class="bg-white dark:bg-gray-900 rounded-lg">
      <div id="console" class="overflow-scroll h-144 px-4">
        <p v-for="entry in log" :key="entry.id" v-html="entry.message" class="whitespace-nowrap"/>
      </div>
      <hr class="dark:border-dark-300">
      <input
        placeholder="Type command or '?' to get help"
        class="w-full py-2 px-4 rounded-b-lg bg-white dark:bg-gray-900 dark:text-white"
        v-model="command"
        @keyup.enter="execute()"
      />
    </div>
  </div>
</template>

<script>
import { ref, watch, onUnmounted, nextTick } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import 'mosha-vue-toastify/dist/style.css'
import useSession from '../store/session'
import useLog from '../store/log'
import useConsole from '../store/console'

export default {
  props: {
    selectedTab: {
      type: Object,
      required: true
    }
  },
  setup(props) {    
    const selectedTab = props.selectedTab
    const active = ref(false)

    watch(
      () => selectedTab.value,
      newTab => (active.value = (newTab == 'Console')),
      { immediate: true }
    )

    const { levels, log, logMessage, filter } = useLog()

    const { 
      onOpen, onMessage, onClose, onError, 
      connect,
      close,
      command,
      execute 
    } = useConsole()

    onUnmounted(() => close())

    const scrollToEnd = () => {
      const console = document.getElementById('console')
      console.scrollTop = console.scrollHeight
    }

    const setupConnection = () => {
      createToast('Connecting to the remote console', { type: 'info', })
      const { token } = useSession()

      onOpen.value = () => 
        (log.value = [])

      onMessage.value = message => {
        logMessage(message)
        nextTick(() => scrollToEnd())
      }

      onError.value = error =>
        createToast(`${error} || ''}`, {
          type: 'danger'
        })

      onClose.value = () =>
        createToast('Connection with console has been lost', {
          type: 'danger'
        })

      connect(token)
    }

    watch(
      () => active.value,
      isActive => isActive ? setupConnection() : close(),
      { immediate: true }
    )

    return {
      log,
      command,
      execute,
      levels,
      filter
    }
  }
}
</script>

<style>
#console {
  white-space: pre-wrap;
  font-family: 'Consolas', 'monospace';
  font-size: 12px;
}
</style>