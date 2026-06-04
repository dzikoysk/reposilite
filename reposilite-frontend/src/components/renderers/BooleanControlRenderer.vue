<template>
  <ControlWrapper
    v-bind="controlWrapper"
    :styles="styles"
    :isFocused="isFocused"
    :appliedOptions="appliedOptions"
  >
    <p>{{ t('settings.schema.enabled') }}</p>
    <input
      :id="control.id + '-input'"
      type="checkbox"
      :class="styles.control.input"
      :checked="!!control.data"
      :disabled="!control.enabled"
      :autofocus="appliedOptions.focus"
      :placeholder="appliedOptions.placeholder"
      @change="onChange"
      @focus="isFocused = true"
      @blur="isFocused = false"
    />
  </ControlWrapper>
</template>

<script>
import { isBooleanControl, rankWith } from '@jsonforms/core'
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue'
import { ControlWrapper, useVanillaControl } from '@dzikoysk/vue-vanilla'
import { useI18n } from 'vue-i18n'

export const tester = rankWith(3, isBooleanControl)

export default {
  name: 'BooleanControlRenderer',
  components: {
    ControlWrapper
  },
  props: rendererProps(),
  setup(props) {
    const { t } = useI18n()

    return {
      t,
      ...useVanillaControl(
        useJsonFormsControl(props),
        target => target.checked
      )
    }
  }
}
</script>

<style scoped>

</style>
