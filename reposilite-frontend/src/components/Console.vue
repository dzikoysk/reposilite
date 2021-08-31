<template>
  <div class="container mx-auto pt-10 px-15 text-xs">
    <div class="bg-white dark:bg-gray-900 rounded-lg">
      <div id="console" class="overflow-scroll h-144 px-4">
        <p v-for="entry in log" :key="entry.id" v-html="entry.message" class="whitespace-nowrap"/>
      </div>
      <hr class="dark:border-dark-300">
      <input
        placeholder="Type command or '?' to get help"
        class="w-full pb-3 pt-2 px-4 rounded-b-lg bg-white dark:bg-gray-900 dark:text-white"
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
import Convert from 'ansi-to-html'
import useSession from '../store/session'
import { createURL } from '../store/client'

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
      newTab => {
        console.log('Watch: ' + newTab)
        active.value = (newTab == 'Console')
      },
      { immediate: true }
    )

    const connection = ref({})
    const log = ref([])
    const command = ref('')

    const execute = () => {
      connection.value.send(command.value)
      command.value = ''
    }

    watch(
      () => active.value,
      isActive => {
        if (!isActive) {
          connection?.value?.close()
          return
        }

        createToast('Connecting to the remote console', {
          type: 'info',
          transition: 'zoom'
        })

        try {
          const scrollToEnd = () => {
            const console = document.getElementById('console')
            console.scrollTop = console.scrollHeight
          }

          const consoleAddress = createURL('/api/console/sock')
            .replace('https', 'wss')
            .replace('http', 'ws')

          connection.value = new WebSocket(consoleAddress)
          const { token } = useSession()

          connection.value.onopen = () => {
            log.value = []
            connection.value.send(`Authorization:${token.name}:${token.secret}`)
          }

          const id = ref(0)
          const convert = new Convert()

          connection.value.onmessage = event => {
            const message = event.data
              .replaceAll('<', '&lt;')
              .replaceAll('>', '&gt;')
              .replaceAll(' ', '&nbsp;')

            if (message == 'keep-alive') {
              return
            }

            log.value.push({ id: id.value++, message: convert.toHtml(message) })
            nextTick(() => scrollToEnd())
          }

          connection.value.onerror = error =>
            createToast(`Cli error ${error.data}`, { type: 'danger' })

          connection.value.onclose = () =>
            createToast('Connection with console has been lost', {
              type: 'danger'
            })

          setInterval(() => {
            connection?.value?.send('keep-alive')
          }, 1000 * 10)
        } catch (error) {
          console.log(error)
          createToast(`${error.response.status}: ${error.response.data}`, {
            type: 'danger'
          })
        }
      }
    )

    onUnmounted(() => connection?.value?.close())

    return {
      log,
      command,
      execute
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