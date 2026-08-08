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
          <div class="flex mx-auto w-full">
            <button
              class="mx-auto mt-6 rounded-lg bg-red-600 px-10 py-2 text-white hover:bg-red-700 <sm:px-6"
              @click="factoryReset"
            >
              Yes
            </button>
            <button
              class="mx-auto mt-6 rounded-lg bg-gray-200 px-10 py-2 dark:bg-gray-700 <sm:px-6"
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
          <CloseIcon class="w-6 h-6" />
        </button>
      </div>
    </VueFinalModal>
    <div @click="showFactoryReset = true">
      <slot name="button" />
    </div>
  </div>
</template>
