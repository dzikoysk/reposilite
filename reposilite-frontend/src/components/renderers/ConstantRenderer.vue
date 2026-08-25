<!--
  ~ Copyright (c) 2020-2026 dzikoysk
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

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
