<template>
  <dialog
      ref="dialog"
      class="v-native-dialog"
      v-bind="attrs"
  >
    <slot />
  </dialog>
</template>

<script setup>
import {
  onMounted, ref, watchEffect, useAttrs,
} from 'vue'
const dialog = ref(null)
const internalOpen = ref(false)
const props = defineProps({
  open: Boolean,
  inline: Boolean,
})
const attrs = useAttrs()
function openCloseDialog() {
  if (!dialog?.value) return
  if (props.open) dialog.value.show()
  else dialog.value.close()
}
function showHideDialog() {
  if (!dialog?.value) return
  if (props.open) dialog.value.showModal()
  else dialog.value.close()
}
onMounted(() => {
  watchEffect(() => {
    if (props.open !== internalOpen.value) {
      if (props.inline) openCloseDialog()
      else showHideDialog()
      internalOpen.value = props.open
    }
  })
})
</script>

<style>
</style>
