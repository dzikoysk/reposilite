<script>
/* eslint-disable vue/no-unused-components */

import {
  rankWith,
  uiTypeIs
} from '@jsonforms/core'

import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsLayout,
} from '@jsonforms/vue'

import {useVanillaLayout} from '@dzikoysk/vue-vanilla'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'label-renderer',
  components: {
    DispatchRenderer
  },
  props: {
    ...rendererProps()
  },
  setup(props) {
    // reuse layout bindings for label
    console.log('TEST')
    return useVanillaLayout(useJsonFormsLayout(props))
  }
})

export const tester = rankWith(1, uiTypeIs('Label'))
</script>

<template>
  <label v-if="layout.visible" :class="styles.label.root">
      Custom? {{ this.layout.uischema.text }}
  </label>
</template>