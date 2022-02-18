import { ref, watchEffect } from 'vue'

const reversedFileOrder = ref(localStorage.getItem('reversedFileOrder') === 'true')
watchEffect(() => localStorage.setItem('reversedFileOrder', reversedFileOrder.value))

const displayHashFiles = ref(localStorage.getItem('displayHashFiles') === 'true')
watchEffect(() => localStorage.setItem('displayHashFiles', displayHashFiles.value))

export function useAdjustments() {
  return {
    reversedFileOrder,
    displayHashFiles
  }
}