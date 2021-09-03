import { computed, ref, watch, watchEffect, reactive } from 'vue'
import Convert from 'ansi-to-html'

const levelNames = ['Trace', 'Debug', 'Info', 'Warn', 'Error']
const levels  = reactive({})
const filter = ref('')
const id = ref(0)
const rawLog = reactive([])
const convert = new Convert()

const getLevel = message => {
  for (const level of levelNames) {
    // NOTE: If the message starts with following prefix, then use startsWith
    if (message.includes(`&nbsp;|&nbsp;${level.toUpperCase()}&nbsp;|&nbsp;`)) {
      return level
    }
  }
}

const sanitizeMessage = (message) => convert.toHtml(
  message
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll(' ', '&nbsp;')
)

export default function useLog() {
  for (const level of levelNames) {
    levels[level] = {
      name: level,
      enabled: true,
      count: computed(() => rawLog.reduce((accumulator, entry) => accumulator + (entry.level === level), 0))
    }
  }

  const log = computed(() => {
    return rawLog
      .filter(entry => entry.message.toLowerCase().includes(filter.value.toLowerCase()))
      .filter(entry => levels[entry.level].enabled)
  })

  const logMessage = value => {
    rawLog.push({ id: id.value++, message: sanitizeMessage(value), level: getLevel(value) })
  }

  const clearLog = () => {
    rawLog.length = 0
  }

  return {
    levels,
    log,
    filter,
    sanitizeMessage,
    logMessage,
    clearLog
  }
}
