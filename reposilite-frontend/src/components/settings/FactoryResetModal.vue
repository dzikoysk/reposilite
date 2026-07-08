<script setup>
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import { property } from '../../helpers/vue-extensions'
import CloseIcon from '../icons/CloseIcon.vue'

const props = defineProps({
  callback: property(Function, true)
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
    <VueFinalModal
      v-model="showFactoryReset"
      v-bind="$attrs"
      class="flex justify-center items-center"
    >
      <div class="relative bg-white dark:bg-gray-900 max-w-110 w-[calc(100%-2rem)] py-5 px-8 rounded-lg shadow-xl text-center">
        <div>
          <h1 class="font-bold pb-4">
            Factory reset
          </h1>
          <p>Do you really want to reset whole configuration to the default values?</p>
          <div class="factory-reset-confirm flex mx-auto w-full">
            <button
              class="factory-reset-confirm-yes mx-auto"
              @click="factoryReset"
            >
              Yes
            </button>
            <button
              class="factory-reset-confirm-no mx-auto"
              @click="showFactoryReset = false"
            >
              No
            </button>
          </div>
        </div>
        <button
          class="absolute top-0 right-0 mt-5 mr-9"
          @click="showFactoryReset = false"
        >
          <CloseIcon />
        </button>
      </div>
    </VueFinalModal>
    <div @click="showFactoryReset = true">
      <slot name="button" />
    </div>
  </div>
</template>

<style scoped>
.factory-reset-confirm button {
  @apply mt-6 px-10 py-2 rounded-lg <sm:px-6;
}
.factory-reset-confirm-yes {
  @apply bg-red-600 hover:bg-red-700 text-white;
}
.factory-reset-confirm-no {
  @apply bg-gray-200 dark:bg-gray-700;
}
</style>
