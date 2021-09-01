import { ref, watch, watchEffect, reactive } from 'vue'
import Convert from 'ansi-to-html'

const levelNames = ['Trace', 'Debug', 'Info', 'Warn', 'Error']
const levels  = reactive({})
const filter = ref('')
const id = ref(0)
const rawLog = ref([])
const log = ref([])
const convert = new Convert()

export default function useLog() {
  levelNames.forEach(level => {
    levels[level] = {
      name: level,
      enabled: true,
      count: 0,
      test: (message) => message.includes(`&nbsp;|&nbsp;${level.toUpperCase()}&nbsp;|&nbsp;`)
    }
  })

  const refreshLog = (messages) => {
    levelNames.forEach(level => {
      levels[level].count = 0
    })
      
    log.value = messages
      .filter(entry => entry.message.toLowerCase().includes(filter.value.toLowerCase()))
      .filter(entry => {
        for (let index = 0; index < levelNames.length; index++) {
          const levelName = levelNames[index]
          const level = levels[levelName]
          const matched = level.test(entry.message)

          if (matched) {
            level.count++

            if (!level.enabled) {
              return false
            }
          }
        }

        return true
      })
  }

  watch(
    () => [...rawLog.value],
    (updatedLog) => refreshLog(updatedLog),
    { immediate: true, deep: true }
  )

  watchEffect(() => {
    if (levels)
      refreshLog(rawLog.value)
  })

  const sanitizeMessage = (message) =>
    convert.toHtml(
      message
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll(' ', '&nbsp;')
    )

  const logMessage = (value) => {
    rawLog.value.push({ id: id.value++, message: sanitizeMessage(value) })
  }

  return {
    levels,
    log,
    filter,
    sanitizeMessage,
    logMessage
  }
}