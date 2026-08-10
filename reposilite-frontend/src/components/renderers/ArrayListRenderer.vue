<template>
  <div v-if="control.visible" :class="[styles.arrayList.root, 'array-list-shell']">
    <div :class="[styles.arrayList.legend, 'array-list-head']">
      <h2 :class="styles.arrayList.label">
        {{ control.label }}
      </h2>
      <button
        :class="styles.arrayList.addButton"
        @click="addButtonClick"
        type="button"
        :aria-label="`Add ${control.label} item`"
      >
        +
      </button>
    </div>
    <div v-if="control?.description" class="description">
      {{ control.description }}
      <!-- It is a list of items. You can add new entries by clicking the '+' button on the right. -->
    </div>
    <div class="tabs-component">
       <div class="tabs array-list-tabs" role="tablist" :aria-label="control.label">
        <button
          v-for="(element, index) in control.data"
          :id="`${control.id}-tab-${index}`"
          :key="`${control.path}-${index}-tab`"
          type="button"
          role="tab"
          class="item"
          :class="{ 'active': selectedIndex === index }"
          :aria-selected="selectedIndex === index"
          :aria-controls="`${control.id}-panel-${index}`"
          @click="selectedIndex = index"
        >
          <span class="tab block">{{ createLabel(element) }}</span>
        </button>
      </div>
      <TabPanels v-model="selectedIndex">
        <TabPanel
            v-for="(element, index) in control.data"
            :id="`${control.id}-panel-${index}`"
            :key="`${control.path}-${index}-panel`"
            :val="index"
            class="array-list-panel"
            role="tabpanel"
            :aria-labelledby="`${control.id}-tab-${index}`"
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
          <div
            v-if="noData"
            :class="styles.arrayList.noData"
            role="status"
          >
            No data
          </div>
        </TabPanel>
      </TabPanels>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { composePaths, createDefaultValue, rankWith, schemaTypeIs } from '@jsonforms/core'
import { DispatchRenderer, rendererProps, useJsonFormsArrayControl } from '@jsonforms/vue'
import { useVanillaArrayControl } from '@jsonforms/vue-vanilla'
import {TabPanels, TabPanel} from 'vue3-tabs'
import ArrayListElement from './ArrayListElement.vue'

export const tester = rankWith(2, schemaTypeIs('array'))

export default {
  name: 'array-list-renderer',
  components: {
    ArrayListElement,
    DispatchRenderer,
    TabPanels,
    TabPanel
  },
  props: {
    ...rendererProps()
  },
  setup(props) {
    const vanillaArrayControl = useVanillaArrayControl(useJsonFormsArrayControl(props))
    const selectedIndex = ref(0)

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

      return '<New>'
    }
    
    return {
      createLabel,
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
        createDefaultValue(this.control.schema, this.control.rootSchema)
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
