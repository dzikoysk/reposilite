<template>
  <div v-if="control.visible" class="one-of-container">
    <Tabs v-model="selectedTab">
      <Tab v-for="(oneOfRenderInfo, oneOfIndex) in oneOfRenderInfos"
           :key="`${control.path}-${oneOfIndex}`"
           :val="`${control.path}-${oneOfIndex}`"
           :label="oneOfRenderInfo.label" />
    </Tabs>
    <TabPanels v-model="selectedTab">
      <TabPanel v-for="(oneOfRenderInfo, oneOfIndex) in oneOfRenderInfos"
                :key="`${control.path}-${oneOfIndex}`"
                :val="`${control.path}-${oneOfIndex}`"
                :name="`${control.path}-${oneOfIndex}`">
        <DispatchRenderer :schema="oneOfRenderInfo.schema"
                          :uischema="oneOfRenderInfo.uischema"
                          :path="control.path"
                          :renderers="control.renderers"
                          :cells="control.cells" />
      </TabPanel>
    </TabPanels>
  </div>
</template>

<script>
import {ref} from 'vue'
import {createCombinatorRenderInfos, isOneOfControl, rankWith, resolveSubSchemas} from '@jsonforms/core'
import {DispatchRenderer, rendererProps, useJsonFormsOneOfControl} from '@jsonforms/vue'
import {useVanillaControl} from '@jsonforms/vue-vanilla'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'

export const tester = rankWith(2, isOneOfControl)
export default {
  name: 'OneOfRenderer',
  components: {
    DispatchRenderer,
    Tabs,
    Tab,
    TabPanels,
    TabPanel
  },
  props: rendererProps(),
  setup(props) {
    const input = useJsonFormsOneOfControl(props)
    const control = input.control.value

    const selectedTab = ref(control.indexOfFittingSchema || 0)
    return {
      ...useVanillaControl(input),
      selectedTab
    }
  },
  computed: {
    subSchema() {
      return resolveSubSchemas(
          this.control.schema,
          this.control.rootSchema,
          'oneOf'
      )
    },
    oneOfRenderInfos() {
      return createCombinatorRenderInfos(
          this.subSchema.oneOf,
          this.control.rootSchema,
          'oneOf',
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
