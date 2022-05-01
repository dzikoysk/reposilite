import {inject, provide} from 'vue'

export const useNested = (element) => {
  const nestedInfo = inject('jsonforms.nestedInfo', { level: 0 })
  if (element) {
    provide('jsonforms.nestedInfo', {
      level: nestedInfo.level + 1,
      parentElement: element,
    })
  }
  return nestedInfo
}
