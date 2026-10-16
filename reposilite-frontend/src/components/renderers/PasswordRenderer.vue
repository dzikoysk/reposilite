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
    :is-focused="isFocused"
    :applied-options="appliedOptions"
  >
    <div class="flex items-center gap-2">
      <input
        :id="control.id + '-input'"
        :class="styles.control.input"
        :type="revealed ? 'text' : 'password'"
        :value="control.data"
        :disabled="!control.enabled"
        autocomplete="new-password"
        @input="onChange"
        @focus="isFocused = true"
        @blur="isFocused = false"
      >
      <button
        type="button"
        class="text-xs underline whitespace-nowrap"
        :aria-label="revealed ? 'Hide value' : 'Show value'"
        :aria-pressed="revealed"
        @click="revealed = !revealed"
      >
        {{ revealed ? 'Hide' : 'Show' }}
      </button>
    </div>
  </ControlWrapper>
</template>

<script>
import { and, isStringControl, rankWith } from '@jsonforms/core'
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue'
import { ControlWrapper, useVanillaControl } from '@jsonforms/vue-vanilla'
import { ref } from 'vue'

// secrets like OIDC client secret, LDAP search user password or S3 secret key are masked by name
export const tester = rankWith(3, and(isStringControl, (uischema) => /(password|secret|secretKey)$/i.test(uischema.scope || '')))

export default {
  name: 'PasswordRenderer',
  components: {
    ControlWrapper
  },
  props: rendererProps(),
  setup(props) {
    const control = useVanillaControl(useJsonFormsControl(props), target => target.value)
    return { ...control, revealed: ref(false) }
  }
}
</script>
