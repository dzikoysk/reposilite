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
        {{ control.label }}
      </label>
    </legend>
    <div>
       <Tabs v-model="selectedIndex">
        <Tab
          v-for="(element, index) in control.data"
          :key="`${control.path}-${index}-tab`"
          :val="index"
          :label="element.id || element?.reference?.replace('https://', 'http://')?.replace('http://', '') || '<new>'"
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
            No data
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
    
    return {
      selectedIndex,
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
