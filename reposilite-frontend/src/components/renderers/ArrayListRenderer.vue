<template>
  <fieldset v-if="control.visible" :class="styles.arrayList.root">
    <legend :class="styles.arrayList.legend">
      <button
        :class="styles.arrayList.addButton"
        @click="addButtonClick"
        type="button"
      >
        +
      </button>
      <label :class="styles.arrayList.label">
        {{ translateSettingsText(control.label) }}
      </label>
    </legend>
    <div v-if="control?.description" class="description">
      {{ translateSettingsText(control.description) }}
      <!-- It is a list of items. You can add new entries by clicking the '+' button on the right. -->
    </div>
    <div class="tabs-component">
       <Tabs v-model="selectedIndex">
        <Tab
          v-for="(element, index) in control.data"
          :key="`${control.path}-${index}-tab`"
          :val="index"
          :label="createLabel(element)"
          :indicator="true"
          class="item"
        />
      </Tabs>
      <TabPanels v-model="selectedIndex">
        <TabPanel
            v-for="(element, index) in control.data"
            :key="`${control.path}-${index}-panel`"
            :val="index"
        >
          <div :class="styles.arrayList.itemWrapper">
            <array-list-element
              :moveUp="moveUp(control.path, index)"
              :moveUpEnabled="index > 0"
              :moveDown="moveDown(control.path, index)"
              :moveDownEnabled="index < control.data.length - 1"
              :delete="removeItems(control.path, [index])"
              :label="childLabelForIndex(index)"
              :styles="styles"
            >
              <dispatch-renderer
                :schema="control.schema"
                :uischema="childUiSchema"
                :path="composePaths(control.path, `${index}`)"
                :enabled="control.enabled"
                :renderers="control.renderers"
                :cells="control.cells"
              />
            </array-list-element>
          </div>
          <div v-if="noData" :class="styles.arrayList.noData">
            {{ t('settings.noData') }}
          </div>
        </TabPanel>
      </TabPanels>
    </div>
  </fieldset>
</template>

<script>
import { ref } from 'vue'
import { composePaths, createDefaultValue, rankWith, schemaTypeIs } from '@jsonforms/core'
import { DispatchRenderer, rendererProps, useJsonFormsArrayControl } from '@jsonforms/vue'
import { useVanillaArrayControl } from '@dzikoysk/vue-vanilla'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'
import ArrayListElement from './ArrayListElement.vue'
import { useI18n } from 'vue-i18n'
import { translateSettingsSchemaText } from '../../i18n/settings-schema'

export const tester = rankWith(2, schemaTypeIs('array'))

export default {
  name: 'array-list-renderer',
  components: {
    ArrayListElement,
    DispatchRenderer,
    Tabs,
    Tab, 
    TabPanels,
    TabPanel
  },
  props: {
    ...rendererProps()
  },
  setup(props) {
    const vanillaArrayControl = useVanillaArrayControl(useJsonFormsArrayControl(props))
    const selectedIndex = ref(0)
    const { t } = useI18n()
    const translateSettingsText = (text) => translateSettingsSchemaText(t, text)

    const createLabel = (element) => {
      if (element.id?.length > 0) {
        const capitalized = element.id.charAt(0).toUpperCase() + element.id.slice(1)
        return capitalized
      }

      if (element.reference) {
        let adjustedUrl = element
          ?.reference
          ?.replace('https://', 'http://')
          ?.replace('http://', '')

        if (adjustedUrl.indexOf("/") > -1) {
          adjustedUrl = adjustedUrl.substring(0, adjustedUrl.indexOf("/"))
        }

        return adjustedUrl
      }

      if (typeof element == 'string' && element) {
        return element
      }

      return t('settings.newEntry')
    }
    
    return {
      createLabel,
      selectedIndex,
      t,
      translateSettingsText,
      ...vanillaArrayControl
    }
  },
  computed: {
    noData() {
      return !this.control.data || this.control.data.length === 0
    }
  },
  methods: {
    composePaths,
    createDefaultValue,
    addButtonClick() {
      this.addItem(
        this.control.path,
        createDefaultValue(this.control.schema)
      )()
    }
  },
  watch: {
    'control.data'(newValue, oldValue) {
      newValue = newValue || []
      if (newValue.length != oldValue?.length) {
        this.selectedIndex = newValue.length - 1
      }
    }
  }
}
</script>

<style>
.array-list .tabs .tab {
  overflow-wrap: anywhere;
  text-transform: none !important;
}
.tabs-component {
  width: 100%;
}
@media (min-width: 640px) {
  .tabs-component {
    max-width: 520px;
  }
}
@media (min-width: 768px) {
  .tabs-component {
    max-width: 648px;
  }
}
@media (min-width: 1024px) {
  .tabs-component {
    max-width: 900px;
  }
}
@media (min-width: 1280px) {
  .tabs-component {
    max-width: 1110px;
  }
}
</style>
