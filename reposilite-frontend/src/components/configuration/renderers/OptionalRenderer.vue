<template>
  <div>
    <label>{{ control.label }}<input type="checkbox" v-model="present" /></label><!-- TODO styling -->
    <div v-if="present && control.visible">
      <DispatchRenderer
          :visible="control.visible"
          :enabled="control.enabled"
          :schema="schema"
          :uischema="uischema"
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
import {ref} from 'vue'
import {and, rankWith, schemaMatches, uiTypeIs} from '@jsonforms/core'
import includes from 'lodash/includes'
import {findUISchema} from '@jsonforms/core/src/reducers'

export const tester = rankWith(2, and(uiTypeIs('Control'), schemaMatches(schema => Array.isArray(schema.type) && includes(schema.type, 'null'))))

export default {
  name: 'OptionalRenderer',
  components: {DispatchRenderer},
  props: rendererProps(),
  setup(props) {
    const control = useVanillaControl(useJsonFormsControlWithDetail(props))
    const present = ref(control.control.data !== undefined)
    return {
      ...control,
      present
    }
  },
  computed: {
    schema() {
      const schema = this.control.schema
      return Array.isArray(schema.type) ? {
        ...schema,
        type: schema.type.filter(t => t !== 'null')
      } : schema
    },
    uischema() {
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

</style>
