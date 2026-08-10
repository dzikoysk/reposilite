<template>
  <div>
    <label
      :for="`${control.id}-present`"
      class="label"
    >
      {{ control.label }}
      <input
        :id="`${control.id}-present`"
        type="checkbox"
        :checked="present"
        class="mx-4 mb-1"
        :aria-describedby="control.description ? `${control.id}-description` : undefined"
        @change="toggle"
      >
    </label>
    <div
      :id="`${control.id}-description`"
      class="description"
    >
      {{ control.description }}
    </div>
    <div
      v-if="present && control.visible"
      class="optional-panel"
    >
      <DispatchRenderer
        :visible="control.visible"
        :enabled="control.enabled"
        :schema="filteredSchema"
        :uischema="matchedUISchema"
        :path="control.path"
        :renderers="control.renderers"
        :cells="control.cells"
      />
    </div>
  </div>
</template>

<script>
import {DispatchRenderer, rendererProps, useJsonFormsControlWithDetail} from '@jsonforms/vue'
import {useVanillaControl} from '@jsonforms/vue-vanilla'
import {computed} from 'vue'
import {and, findUISchema, rankWith, schemaMatches, uiTypeIs} from '@jsonforms/core'
import includes from 'lodash/includes'

export const tester = rankWith(2, and(uiTypeIs('Control'), schemaMatches(schema => Array.isArray(schema.type) && includes(schema.type, 'null'))))

export default {
  name: 'OptionalRenderer',
  components: {DispatchRenderer},
  props: rendererProps(),
  setup(props) {
    const control = useVanillaControl(useJsonFormsControlWithDetail(props))
    const present = computed(() => control.control.value.data != null)
    const toggle = event =>
      control.handleChange(
        control.control.value.path,
        event.target.checked ? {} : null
      )

    return {
      ...control,
      present,
      toggle
    }
  },
  computed: {
    filteredSchema() {
      const schema = this.control.schema
      return Array.isArray(schema.type) 
        ? {
            ...schema,
            type: schema.type.filter(t => t !== 'null')
          } 
        : schema
    },
    matchedUISchema() {
      return findUISchema(
          this.control.uischemas,
          this.control.schema,
          this.control.scope,
          this.control.path,
          undefined,
          this.control,
          this.control.rootSchema
      )
    }
  }
}
</script>

<style scoped>
.optional-panel {
  @apply mt-1 rounded-lg bg-gray-100 dark:bg-gray-800 px-4 py-3;
}
</style>
