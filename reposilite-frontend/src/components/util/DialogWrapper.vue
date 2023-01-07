<script setup>
import { onMounted, ref, watchEffect, useAttrs } from 'vue'
import { property } from '../../helpers/vue-extensions'

const props = defineProps({
  open: property(Boolean, true),
  inline: property(Boolean, false),
})

const dialog = ref()
const internalOpen = ref(false)
const attrs = useAttrs()

const openCloseDialog = () => {
  if (!dialog?.value) return
  if (props.open) dialog.value.show()
  else dialog.value.close()
}

const showHideDialog = () => {
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

<template>
  <dialog ref="dialog" class="v-native-dialog" v-bind="attrs">
    <slot />
  </dialog>
</template>
