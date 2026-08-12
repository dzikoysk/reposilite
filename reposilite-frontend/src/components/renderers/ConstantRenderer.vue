<template>
  <ControlWrapper 
    v-bind="controlWrapper"
    :styles="styles"
    :isFocused="isFocused"
    :appliedOptions="appliedOptions"
  >
    <input 
      :id="control.id + '-input'"
      :class="styles.control.input"
      :value="control.schema.const"
      readonly
      @focus="isFocused = true"
      @blur="isFocused = false"
    />
  </ControlWrapper>
</template>

<script>
import {and, isControl, rankWith, schemaMatches} from '@jsonforms/core'
import {rendererProps, useJsonFormsControl} from '@jsonforms/vue'
import {ControlWrapper, useVanillaControl} from '@jsonforms/vue-vanilla'
import {watchEffect} from 'vue'

export const tester = rankWith(4, and(isControl, schemaMatches(schema => 'const' in schema)))

export default {
  name: 'ConstantRenderer',
  components: {
    ControlWrapper
  },
  props: rendererProps(),
  setup(props) {
    const control = useVanillaControl(useJsonFormsControl(props))
    watchEffect(() => {
      const {data, path, schema} = control.control.value
      if (data !== schema.const) {
        control.handleChange(path, schema.const)
      }
    })
    return control
  }
}
</script>

<style scoped>

</style>
