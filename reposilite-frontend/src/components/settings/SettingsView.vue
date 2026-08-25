<!--
  - Copyright (c) 2020-2026 dzikoysk
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->
  
<script setup>
import {ref, toRaw, watch} from 'vue'
import {JsonForms} from '@jsonforms/vue'
import {TabPanels, TabPanel} from 'vue3-tabs'
import { useConfiguration } from '../../store/configuration'
import download from 'downloadjs'
import FactoryResetModal from './FactoryResetModal.vue'
import { property } from '../../helpers/vue-extensions'
import ViewHeader from '../util/ViewHeader.vue'

const props = defineProps({
  selectedTab: property(String, true)
})

const {
  fetchConfiguration,
  updateConfiguration, 
  renderers, 
  configurationValidator,
  domains, 
  configurations,
  schemas,
  selectedDomain,
  isLoading
} = useConfiguration()

const isValid = ref(true)
const hasChanged = ref(false)

const executeIfValid = (callback) => { 
  if (isValid.value) callback() 
}

const updateFormsConfiguration = (domain, event) => {
  if (!hasChanged.value) {
    hasChanged.value = configurations.value[domain] != event.data
  }
  configurations.value[domain] = event.data
  isValid.value = event.errors.length == 0
}

const reload = (action) => action().then(() => hasChanged.value = false)

watch(
  () => props.selectedTab,
  (selectedTab, prev) => {
    /* Fetch configuration only when user opens the configuration tab  */
    if (selectedTab === 'Settings' && prev == undefined && domains.value.length == 0 && !isLoading.value)
      fetchConfiguration()
  },
  { immediate: true }
)

const downloadSettings = () => {
  download(
    JSON.stringify(toRaw(configurations.value)), 
    'shared.configuration.json', 
    'application/json'
  )
}

const factoryReset = () => {
  const emptyConfiguration = {}
  domains.value.forEach(domain => emptyConfiguration[domain] = {})
  configurations.value = emptyConfiguration
  updateConfiguration()
}

/* JsonForms configuration */
const formsConfiguration = {
  showUnfocusedDescription: true
}
</script>

<template>
  <div class="settings-view container mx-auto pt-7 px-15 pb-12 <sm:px-4">
    <ViewHeader
      title="Shared configuration"
      description="Modify configuration shared between all instances. Updates can take up to 10 seconds to propagate."
    >
      <template #actions>
        <div
          v-if="domains.length > 0 && !isLoading"
          id="configuration-state"
          class="flex flex-nowrap items-center justify-end gap-2 <md:flex-wrap <md:justify-start"
        >
          <button
            v-if="hasChanged"
            type="button"
            class="h-8 whitespace-nowrap rounded-md bg-blue-700 px-3 text-sm text-white hover:bg-blue-800 dark:hover:bg-blue-600 cursor-pointer"
            :class="{ '!bg-gray-500 cursor-not-allowed': !isValid }"
            :disabled="!isValid"
            @click.prevent="reload(updateConfiguration)"
          >
            Update and reload
          </button>
          <button
            v-if="hasChanged"
            type="button"
            class="h-8 whitespace-nowrap rounded-md bg-red-600 px-3 text-sm text-white hover:bg-red-700 dark:bg-red-600 dark:hover:bg-red-500 cursor-pointer"
            @click.prevent="reload(fetchConfiguration)"
          >
            Reset changes
          </button>
          <button
            v-if="!hasChanged"
            type="button"
            class="h-8 whitespace-nowrap rounded-md bg-gray-800 px-3 text-sm text-white hover:bg-gray-900 dark:bg-gray-700 dark:hover:bg-gray-600"
            :class="{ '!bg-gray-500 cursor-not-allowed': !isValid }"
            :disabled="!isValid"
            @click.prevent="executeIfValid(downloadSettings)"
          >
            Download as JSON
          </button>
          <FactoryResetModal :callback="factoryReset" />
        </div>
      </template>
    </ViewHeader>
    <div class="overflow-hidden rounded-lg bg-gray-100 dark:bg-black">
      <div
        v-if="isLoading"
        class="flex min-h-40 items-center justify-center gap-2 bg-white text-sm text-gray-600 dark:bg-gray-900 dark:text-gray-300"
        role="status"
      >
        <span
          class="h-5 w-5 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600 dark:border-gray-600 dark:border-t-blue-400"
          aria-hidden="true"
        />
        Loading configuration...
      </div>
      <template v-else>
        <div
          v-if="domains.length > 1"
          class="tabs domain-tabs overflow-x-auto pt-2"
          role="tablist"
          aria-label="Configuration domains"
        >
          <button
            v-for="domain in domains"
            :id="`domain-tab-${domain}`"
            :key="`config:${domain}`"
            type="button"
            role="tab"
            class="item cursor-pointer whitespace-nowrap rounded-t-md bg-transparent px-3 py-1 text-sm leading-5 text-gray-600 dark:text-gray-300"
            :class="{ 'domain-tab-active': selectedDomain === domain }"
            :aria-selected="selectedDomain === domain"
            :aria-controls="`domain-panel-${domain}`"
            @click="selectedDomain = domain"
          >
            <span class="tab block">{{ schemas[domain]?.title }}</span>
          </button>
        </div>
        <TabPanels v-model="selectedDomain">
          <TabPanel
            v-for="domain in domains"
            :id="`domain-panel-${domain}`"
            :key="`config_tab:${domain}`"
            :val="domain"
            class="settings-panel"
            role="tabpanel"
            :aria-labelledby="`domain-tab-${domain}`"
          >
            <JsonForms
              v-if="configurations[domain]"
              :config="formsConfiguration"
              :data="configurations[domain]"
              :schema="schemas[domain]"
              :renderers="renderers"
              :ajv="configurationValidator"
              @change="updateFormsConfiguration(domain, $event)"
            />
          </TabPanel>
        </TabPanels>
      </template>
    </div>
  </div>
</template>

<!--suppress CssInvalidAtRule -->
<style scoped>
.domain-tabs :deep(.tab) {
  @apply whitespace-nowrap;
}
.domain-tabs :deep(.item:hover) {
  @apply bg-white dark:bg-gray-800;
  transition: background-color 0.5s;
}
.domain-tabs :deep(.domain-tab-active) {
  @apply bg-white dark:bg-gray-900 text-gray-900 dark:text-white;
}
</style>

<!--suppress CssInvalidAtRule -->
<style scoped>
.settings-view :deep(.error) {
  @apply text-red-500 px-2 font-bold;
}
.settings-view :deep(.vertical-layout),
.settings-view :deep(.group-layout) {
  @apply container mx-auto;
}
.settings-view :deep(.control .input:not([type=checkbox])),
.settings-view :deep(.control .select) {
  @apply text-sm h-9 px-4 text-black dark:text-white bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 outline-none;
}
.settings-view :deep(.control .input:not([type=checkbox]):focus),
.settings-view :deep(.control .select:focus) {
  @apply border-blue-500;
}
.settings-view :deep(.control .input[type="checkbox"]) {
  @apply h-5 w-5;
}
.settings-view :deep(.control .input),
.settings-view :deep(.control .select) {
  @apply mx-2 rounded <sm:mx-0;
}
.settings-view :deep(.control .select) {
  @apply pr-8;
}
.settings-view :deep(.control) {
  @apply flex flex-col;
}
.settings-view :deep(.control > .description) {
  order: 1;
  white-space: pre-line;
}
.settings-view :deep(.control > .wrapper) {
  order: 2;
}
.settings-view :deep(.control > .error) {
  order: 3;
}
.settings-view :deep(.vertical-layout),
.settings-view :deep(.group) {
  @apply flex flex-col flex-wrap py-4 h-full;
  gap: 1rem;
}
.settings-view :deep(.array-list) {
  @apply flex flex-col flex-wrap h-full;
  gap: 0.75rem;
}
.settings-view :deep(.label),
.settings-view :deep(label) {
  padding-bottom: 0.5em;
  padding-left: 0.45em;
  display: inline-block;
  font-weight: bold;
}
.settings-view :deep(.description) {
  padding-left: 0.45em;
  padding-bottom: 0.7em;
  @apply text-sm italic;
}
.settings-view :deep(.array-list) {
  padding: 0;
}
.settings-view :deep(.array-list-label) {
  font-weight: bold;
}
.settings-view :deep(.array-list-item-label) {
  margin-right: auto;
}
.settings-view :deep(.array-list-item-delete) {
  @apply absolute right-0 top-2 text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300;
}
.settings-view :deep(.array-list-item-toolbar) {
  @apply flex flex-row items-baseline relative;
}
.settings-view :deep(.array-list-item-label) {
  display: none;
}
.settings-view :deep(.array-list-item-toolbar > button) {
  padding: 0.5rem;
}
.settings-view :deep(.one-of-container) {
  @apply h-full flex flex-col rounded-lg bg-gray-100 dark:bg-gray-800 px-3 py-3;
}
.settings-view :deep(.tab-panel) {
  @apply h-full;
}
.settings-view :deep(.settings-panel) {
  @apply bg-white dark:bg-gray-900 px-5 py-5 <sm:px-4;
}
.settings-view :deep(.array-list-shell) {
  @apply rounded-lg bg-gray-100 dark:bg-gray-800 px-4 py-3 <sm:px-3;
}
.settings-view :deep(.array-list-head) {
  @apply flex items-center justify-between gap-3 w-full;
}
.settings-view :deep(.one-of-tabs),
.settings-view :deep(.array-list-tabs) {
  @apply overflow-x-auto;
}
.settings-view :deep(.one-of-tabs .tab),
.settings-view :deep(.array-list-tabs .tab) {
  @apply whitespace-nowrap;
}
.settings-view :deep(.one-of-tabs .item),
.settings-view :deep(.array-list-tabs .item) {
  @apply cursor-pointer whitespace-nowrap rounded-t-md bg-transparent px-3 py-1 text-sm leading-5 text-gray-600 dark:text-gray-300;
}
.settings-view :deep(.one-of-tabs .item:hover),
.settings-view :deep(.array-list-tabs .item:hover) {
  @apply bg-white dark:bg-gray-700;
  transition: background-color 0.2s;
}
.settings-view :deep(.one-of-tabs .active),
.settings-view :deep(.array-list-tabs .active) {
  @apply bg-white dark:bg-gray-900 text-gray-900 dark:text-white;
}
.settings-view :deep(.one-of-panel),
.settings-view :deep(.array-list-panel) {
  @apply rounded-b-lg rounded-tr-lg bg-white dark:bg-gray-900 px-4 py-3 <sm:px-3;
}
.settings-view :deep(.array-list-add) {
  @apply rounded-full h-6 w-6 leading-6 bg-blue-700 text-white z-1 flex-none;
}
.settings-view :deep(.array-list-item-move-up) {
  display: none;
}
.settings-view :deep(.array-list-item-move-down) {
  display: none;
}
.settings-view :deep(.array-list-no-data) {
  @apply p-4 bg-white dark:bg-gray-900 italic rounded-md;
}
.settings-view :deep(.wrapper) {
  @apply flex py-2 <sm:flex-col;
}
.settings-view :deep(.control > .wrapper:has(> input[type="checkbox"])) {
  @apply items-center gap-2 <sm:flex-row;
}
.settings-view :deep(.control > .wrapper:has(> input[type="checkbox"])::before) {
  content: "Enabled";
  @apply px-2 text-sm text-gray-700 dark:text-gray-200;
}
.settings-view :deep(.wrapper p) {
  @apply px-2 text-sm;
}
.settings-view :deep(.wrapper input) {
  @apply w-1/2 <sm:w-full;
}
.settings-view :deep(.wrapper input),
.settings-view :deep(.wrapper select) {
  @apply dark:bg-gray-800 dark:text-white !important;
}
.settings-view :deep(.wrapper input:not([type=checkbox]):read-only) {
  @apply bg-gray-200 dark:bg-gray-800 text-gray-500 !important;
}
.settings-view :deep(.array-list-item-wrapper) {
  @apply bg-transparent rounded-none p-0;
}
.settings-view :deep(.array-list-item-toolbar) {
  @apply min-h-8;
}
.settings-view :deep(.array-list-legend) {
  margin-bottom: 0;
}
.settings-view :deep(.description) {
  padding-bottom: 0;
}
.settings-view :deep(.dialog-root) {
  @apply w-[calc(100%-2rem)] max-w-md rounded-lg border border-gray-200 bg-white px-6 py-5 text-left text-gray-700 shadow-xl dark:border-gray-700 dark:bg-gray-900 dark:text-gray-200;
}
.settings-view :deep(.dialog-root::backdrop) {
  @apply bg-black bg-opacity-50;
}
.settings-view :deep(.dialog-title) {
  @apply text-base font-semibold text-gray-900 dark:text-white;
}
.settings-view :deep(.dialog-body) {
  @apply mt-2 text-sm;
}
.settings-view :deep(.dialog-actions) {
  @apply mt-5 flex justify-end gap-2;
}
.settings-view :deep(.dialog-button-primary),
.settings-view :deep(.dialog-button-secondary) {
  @apply h-8 rounded-md px-3 text-sm;
}
.settings-view :deep(.dialog-button-primary) {
  @apply bg-red-600 text-white hover:bg-red-700 dark:hover:bg-red-500;
}
.settings-view :deep(.dialog-button-secondary) {
  @apply bg-gray-200 text-gray-900 hover:bg-gray-300 dark:bg-gray-700 dark:text-white dark:hover:bg-gray-600;
}
</style>
