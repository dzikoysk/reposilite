<template>
  <div v-if="control.visible">
    <template v-if="delegateUISchema">
      <DispatchRenderer
          :schema="subSchema"
          :uischema="delegateUISchema"
          :path="control.path"
          :enabled="control.enabled"
          :renderers="control.renderers"
          :cells="control.cells"
      />
    </template>
    <template v-else-if="allOfRenderInfos">
      <DispatchRenderer
          v-for="(allOfRenderInfo, allOfIndex) in allOfRenderInfos"
          :key="`${control.path}-${allOfIndex}`"
          :schema="allOfRenderInfo.schema"
          :uischema="allOfRenderInfo.uischema"
          :path="control.path"
          :enabled="control.enabled"
          :renderers="control.renderers"
          :cells="control.cells"
      />
    </template>
  </div>
</template>

<script>
import {createCombinatorRenderInfos, findMatchingUISchema, resolveSubSchemas, isAllOfControl, rankWith} from '@jsonforms/core'
import {DispatchRenderer, rendererProps, useJsonFormsAllOfControl} from '@jsonforms/vue'
import {useVanillaControl} from '@jsonforms/vue-vanilla'

export const tester = rankWith(3, isAllOfControl)

export default {
  name: 'AllOfRenderer',
  components: {
    DispatchRenderer
  },
  props: rendererProps(),
  setup: props => useVanillaControl(useJsonFormsAllOfControl(props)),
  computed: {
    subSchema() {
      return resolveSubSchemas(
          this.control.schema,
          this.control.rootSchema,
          'allOf'
      )
    },
    delegateUISchema() {
      return findMatchingUISchema(this.control.uischemas)(
          this.subSchema,
          this.control.uischema.scope,
          this.control.path
      )
    },
    allOfRenderInfos() {
      return createCombinatorRenderInfos(
          this.subSchema.allOf,
          this.control.rootSchema,
          'allOf',
          this.control.uischema,
          this.control.path,
          this.control.uischemas
      )
    }
  }
}
</script>

<style scoped>

</style>
