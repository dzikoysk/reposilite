<template>
  <ControlWrapper v-bind="controlWrapper"
                  :styles="styles"
                  :isFocused="isFocused"
                  :appliedOptions="appliedOptions">
    <input :id="control.id + '-input'"
           :class="styles.control.input"
           :value="control.schema.const"
           readonly
           @focus="isFocused = true"
           @blur="isFocused = false" />
  </ControlWrapper>
</template>

<script>
import {and, isControl, rankWith, schemaMatches} from '@jsonforms/core'
import {rendererProps, useJsonFormsControl} from '@jsonforms/vue'
import {ControlWrapper, useVanillaControl} from '@jsonforms/vue-vanilla'

export const tester = rankWith(4, and(isControl, schemaMatches(schema => 'const' in schema)))
export default {
  name: 'ConstantRenderer',
  components: {
    ControlWrapper
  },
  props: rendererProps(),
  setup(props) {
    return useVanillaControl(useJsonFormsControl(props))
  }
}
</script>

<style scoped>

</style>
