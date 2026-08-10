<template>
  <div v-if="control.visible" class="one-of-container">
    <div class="tabs one-of-tabs" role="tablist" :aria-label="control.label">
      <button
        v-for="(oneOfRenderInfo, oneOfIndex) in oneOfRenderInfos"
        :id="`${control.id}-option-tab-${oneOfIndex}`"
        :key="`${control.path}-${oneOfIndex}`"
        type="button"
        role="tab"
        class="item"
        :class="{ 'active': selectedIndex === oneOfIndex }"
        :aria-selected="selectedIndex === oneOfIndex"
        :aria-controls="`${control.id}-option-panel-${oneOfIndex}`"
        @click="tabIndex = oneOfIndex; tabChanged()"
      >
        <span class="tab block">{{ oneOfRenderInfo.label }}</span>
      </button>
    </div>
    <TabPanels v-model="selectedIndex">
      <TabPanel 
        v-for="(oneOfRenderInfo, oneOfIndex) in oneOfRenderInfos"
        :id="`${control.id}-option-panel-${oneOfIndex}`"
        :key="`${control.path}-${oneOfIndex}`"
        :val="oneOfIndex"
        :name="`${control.path}-${oneOfIndex}`"
        class="one-of-panel"
        role="tabpanel"
        :aria-labelledby="`${control.id}-option-tab-${oneOfIndex}`"
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
  <DialogWrapper
    :open="dialog"
    :aria-labelledby="`${control.id}-confirmation-title`"
  >
    <p :id="`${control.id}-confirmation-title`">
      Your data will be cleared if you navigate away from this tab. 
      Do you want to proceed?
    </p>
    <div class="dialog-actions">
      <button
        type="button"
        @click="cancel"
      > No </button>
      <button
        ref="confirm"
        type="button"
        @click="confirm"
      > Yes </button>
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
} from '@jsonforms/core'
import {DispatchRenderer, rendererProps, useJsonFormsOneOfControl} from '@jsonforms/vue'
import {useVanillaControl} from '@jsonforms/vue-vanilla'
import {TabPanels, TabPanel} from 'vue3-tabs'
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
    TabPanels,
    TabPanel
  },
  props: rendererProps(),
  setup(props) {
    const input = useJsonFormsOneOfControl(props)
    const selectedIndex = ref(input.control.value.indexOfFittingSchema ?? 0)
    const tabIndex = ref(selectedIndex.value)
    const newSelectedIndex = ref(0)
    const dialog = ref(false)
    const jsonforms = inject('jsonforms')

    if (!jsonforms) {
      throw new Error("'jsonforms' couldn't be injected. Are you within JSON Forms?")
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
      return this.control.schema
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
          createDefaultValue(this.control.schema.oneOf[this.newSelectedIndex], this.control.rootSchema)
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
  @apply bg-white dark:bg-gray-900;
  @apply border-gray-200 dark:border-gray-700;
  @apply rounded border-2;
}
dialog::backdrop {
  background-color: rgba(0, 0, 0, 0.5);
}
.dialog-actions button {
  @apply bg-blue-700 mx-2 rounded text-sm h-9 px-4 text-white;
}
.tabs {
  @apply cursor-pointer !important;
}
</style>
