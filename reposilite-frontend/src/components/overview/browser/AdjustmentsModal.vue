<script>
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'

export default {
  inheritAttrs: false,
  components: { VueFinalModal },
  setup() {
    const showAdjustments = ref(false)

    return {
      showAdjustments
    }
  }
}
</script>

<template>
  <div id="adjustments-modal">
    <vue-final-modal
      v-model="showAdjustments"
      v-bind="$attrs"
      classes="flex justify-center items-center"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <div>#TODO</div>
      </div>
    </vue-final-modal>
    <div @click="showAdjustments = true">
      <slot name="button"></slot>
    </div>
  </div>
</template>
