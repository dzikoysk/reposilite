<script setup>
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import CloseIcon from '../icons/CloseIcon.vue'

const props = defineProps({
  callback: {
    type: Function,
    required: true
  }
})

const showFactoryReset = ref(false)

const factoryReset = () => {
  props.callback()
  showFactoryReset.value = false
}
</script>

<script>
export default {
  inheritAttrs: false
}
</script>

<template>
  <div id="adjustments-modal">
    <vue-final-modal
      v-model="showFactoryReset"
      v-bind="$attrs"
      classes="flex justify-center iems-center"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <div>
          <h1 class="font-bold pb-4">Factory reset</h1>
          <p>Do you really want to reset whole configuration to the default values?</p>
          <div class="factory-reset-confirm flex mx-auto w-full">
            <button class="mx-auto" @click="factoryReset">Yes</button>
            <button class="mx-auto" @click="showFactoryReset = false">No</button>
          </div>
        </div>
        <button class="absolute top-0 right-0 mt-5 mr-9" @click="showFactoryReset = false">
          <CloseIcon />
        </button>
      </div>
    </vue-final-modal>
    <div @click="showFactoryReset = true">
      <slot name="button"></slot>
    </div>
  </div>
</template>

<style scoped>
.factory-reset-confirm button {
  @apply mt-6 bg-gray-200 dark:bg-gray-700 px-20 py-2 rounded-lg;
}
</style>