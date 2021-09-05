import { watch, reactive } from 'vue'
import { useRoute } from 'vue-router'

const qualifier = reactive({
  watchable: 0,
  path: ''
})

export default function useQualifier(token) {
  const route = useRoute()

  watch(
    () => route.params.qualifier,
    newQualifier => {
      qualifier.path = newQualifier
      qualifier.watchable++
    },
    { immediate: true }
  )

  watch(
    () => token.name,
    _ => qualifier.watchable++
  )

  return {
    qualifier
  }
}