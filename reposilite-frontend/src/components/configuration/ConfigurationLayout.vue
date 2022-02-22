<template>
  <div class="test">
    <div
        v-for="(element, index) in layout.uischema.elements"
        v-bind:key="`${layout.path}-${index}`"
    >
      <dispatch-renderer
          v-bind:schema="layout.schema"
          v-bind:uischema="element"
          v-bind:path="layout.path"
          v-bind:enabled="layout.enabled"
          v-bind:renderers="layout.renderers"
          v-bind:cells="layout.cells"
      />
    </div>
  </div>
</template>

<script>
import {
  uiTypeIs,
  isLayout,
  and,
  rankWith
} from '@jsonforms/core'
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsLayout
} from '@jsonforms/vue'
import { defineComponent } from 'vue'

const layoutRenderer = defineComponent({
  name: 'ConfigurationLayout',
  components: {
    DispatchRenderer
  },
  props: {
    ...rendererProps()
  },
  setup(props) {
    return useJsonFormsLayout(props)
  }
});

export default layoutRenderer;

export const entry = {
  renderer: layoutRenderer,
  tester: rankWith(1, and(isLayout, uiTypeIs('Configuration')))
}
</script>

<style scoped>

</style>
