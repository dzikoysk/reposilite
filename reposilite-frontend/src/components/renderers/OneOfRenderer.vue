<template>
  <div v-if="control.visible" class="one-of-container">
    <Tabs v-model="tabIndex">
      <Tab 
        v-for="(oneOfRenderInfo, oneOfIndex) in oneOfRenderInfos"
        :key="`${control.path}-${oneOfIndex}`"
        :val="oneOfIndex"
        :label="oneOfRenderInfo.label"
        @click="tabChanged" 
      />
    </Tabs>
    <TabPanels v-model="selectedIndex">
      <TabPanel 
        v-for="(oneOfRenderInfo, oneOfIndex) in oneOfRenderInfos"
        :key="`${control.path}-${oneOfIndex}`"
        :val="oneOfIndex"
        :name="`${control.path}-${oneOfIndex}`"
      >
        <DispatchRenderer
          v-if="selectedIndex === oneOfIndex"
          :schema="oneOfRenderInfo.schema"
          :uischema="oneOfRenderInfo.uischema"
          :path="control.path"
          :renderers="control.renderers"
          :cells="control.cells" 
        />
      </TabPanel>
    </TabPanels>
  </div>
  <DialogWrapper :open="dialog">
    <p>
      Your data will be cleared if you navigate away from this tab. Do you
      want to proceed?
    </p>
    <div class="dialog-actions">
      <button @click="cancel"> No </button>
      <button ref="confirm" @click="confirm"> Yes </button>
    </div>
  </DialogWrapper>
</template>

<script>
import {inject, ref, computed} from 'vue'
import {
  createCombinatorRenderInfos, createDefaultValue,
  getConfig,
  getData, getSchema, isInherentlyEnabled,
  isOneOfControl,
  rankWith,
  resolveSubSchemas
} from '@jsonforms/core'
import {DispatchRenderer, rendererProps, useJsonFormsOneOfControl} from '@jsonforms/vue'
import {useVanillaControl} from '@dzikoysk/vue-vanilla'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'
import isEmpty from 'lodash/isEmpty'
import DialogWrapper from '../util/DialogWrapper.vue'

export const tester = rankWith(2, isOneOfControl)

const isControlEnabled = (ownProps, jsonforms) => {
  const state = { jsonforms }
  const config = getConfig(state)
  const rootData = getData(state)
  const { uischema } = ownProps
  const rootSchema = getSchema(state)

  return isInherentlyEnabled(
      state,
      ownProps,
      uischema,
      ownProps.schema || rootSchema,
      rootData,
      config
  )
}

export default {
  name: 'OneOfRenderer',
  components: {
    DialogWrapper,
    DispatchRenderer,
    Tabs,
    Tab,
    TabPanels,
    TabPanel
  },
  props: rendererProps(),
  setup(props) {
    const input = useJsonFormsOneOfControl(props)
    const selectedIndex = ref(input.control.indexOfFittingSchema || 0)
    const tabIndex = ref(selectedIndex.value)
    const newSelectedIndex = ref(0)
    const dialog = ref(false)
    const jsonforms = inject('jsonforms')

    if (!jsonforms) {
      throw new Error(
          "'jsonforms' couldn't be injected. Are you within JSON Forms?"
      )
    }
    
    const controlEnabled = computed(() => isControlEnabled(props, jsonforms))

    return {
      ...useVanillaControl(input),
      selectedIndex,
      tabIndex,
      newSelectedIndex,
      dialog,
      controlEnabled
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
  },
  methods: {
    tabChanged() {
      if (this.controlEnabled && !isEmpty(this.control.data)) {
        this.dialog = true
        this.$nextTick(() => {
          this.newSelectedIndex = this.tabIndex
          this.tabIndex = this.selectedIndex
        })
        setTimeout(() => this.$refs.confirm.focus())
      } else {
        this.$nextTick(() => {
          this.selectedIndex = this.tabIndex
        })
      }
    },
    confirm() {
      this.openNewTab()
      this.dialog = false
    },
    cancel() {
      this.newSelectedIndex = this.selectedIndex
      this.dialog = false
    },
    openNewTab() {
      this.handleChange(
          this.path,
          createDefaultValue(this.control.schema.oneOf[this.newSelectedIndex])
      )
      this.tabIndex = this.newSelectedIndex
      this.selectedIndex = this.newSelectedIndex
    }
  }
}
</script>

<style scoped>
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.5rem;
}
dialog {
  @apply text-gray-600 dark:text-gray-300;
  @apply bg-gray-100 dark:bg-black;
  @apply border-gray-200 dark:border-gray-700;
  @apply rounded border-2;
}
dialog::backdrop {
  background-color: rgba(0, 0, 0, 0.5);
}
button {
  @apply bg-blue-700 mx-2 rounded text-sm h-9 px-4 text-white;
}
</style>
