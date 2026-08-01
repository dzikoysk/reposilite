<!--
  - Copyright (c) 2023 dzikoysk
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
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'
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
  selectedDomain
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
  event.errors.forEach(error => {
    console.log(error)
  })
}

const reload = (action) => action().then(() => hasChanged.value = false)

watch(
  () => props.selectedTab,
  (selectedTab, prev) => {
    /* Fetch configuration only when user opens the configuration tab  */
    if (selectedTab === 'Settings' && prev == undefined && domains.value.length == 0)
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
          id="configuration-state"
        >
          <button
            v-if="hasChanged"
            class="action primary"
            :class="{ forbidden: !isValid }"
            :disabled="!isValid"
            @click.prevent="reload(updateConfiguration)"
          >
            Update and reload
          </button>
          <button
            v-if="hasChanged"
            class="action danger"
            @click.prevent="reload(fetchConfiguration)"
          >
            Reset changes
          </button>
          <button
            v-if="!hasChanged"
            class="action utility"
            :class="{ forbidden: !isValid }"
            :disabled="!isValid"
            @click.prevent="executeIfValid(downloadSettings)"
          >
            Download as JSON
          </button>
          <FactoryResetModal :callback="factoryReset">
            <template #button>
              <button class="action danger">
                Factory reset
              </button>
            </template>
          </FactoryResetModal>
        </div>
      </template>
    </ViewHeader>
    <div class="settings-card">
      <Tabs
        v-if="domains.length > 1"
        v-model="selectedDomain"
        class="domain-tabs"
      >
        <Tab
          v-for="domain in domains"
          :key="`config:${domain}`"
          class="item"
          :val="domain"
          :label="schemas[domain]?.title"
          :indicator="true"
        />
      </Tabs>
      <TabPanels v-model="selectedDomain">
        <TabPanel
          v-for="domain in domains"
          :key="`config_tab:${domain}`"
          :val="domain"
          class="settings-panel"
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
    </div>
  </div>
</template>

<!--suppress CssInvalidAtRule -->
<style scoped>
.settings-card {
  @apply bg-gray-100 dark:bg-black rounded-lg overflow-hidden;
}
.settings-view :deep(.view-header > .actions) {
  @apply self-center <md:self-start;
}
#configuration-state {
  @apply flex flex-nowrap items-center justify-end gap-2 <md:flex-wrap <md:justify-start;
}
#configuration-state button {
  @apply rounded-md h-8 px-3 text-sm text-white whitespace-nowrap;
}
#configuration-state .utility {
  @apply bg-gray-800 dark:bg-gray-700 hover:bg-gray-900 dark:hover:bg-gray-600;
}
#configuration-state .primary {
  @apply bg-blue-700 hover:bg-blue-800 dark:hover:bg-blue-600 cursor-pointer;
}
#configuration-state .danger {
  @apply bg-red-600 hover:bg-red-700 dark:bg-red-600 dark:hover:bg-red-500 cursor-pointer;
}
#configuration-state .forbidden {
  @apply bg-gray-500 cursor-not-allowed !important;
}
.item {
  @apply cursor-pointer whitespace-nowrap rounded-t-md px-3 py-1 text-sm leading-5 text-gray-600 dark:text-gray-300 bg-transparent;
}
.domain-tabs {
  @apply overflow-x-auto pt-2;
}
.domain-tabs :deep(.tab) {
  @apply whitespace-nowrap;
}
.domain-tabs :deep(.item:hover) {
  @apply bg-white dark:bg-gray-800;
  transition: background-color 0.5s;
}
.domain-tabs :deep(.active),
.domain-tabs :deep(.item.active) {
  @apply bg-white dark:bg-gray-900 text-gray-900 dark:text-white;
}
</style>

<!--suppress CssInvalidAtRule -->
<style>
.error {
  @apply text-red-500 px-2 font-bold;
}
.vertical-layout, .group-layout {
  @apply container mx-auto;
}
.control .input:not([type=checkbox]), .control .select {
  @apply text-sm h-9 px-4 text-black dark:text-white bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 outline-none;
}
.control .input:not([type=checkbox]):focus, .control .select:focus {
  @apply border-blue-500;
}
.control .input[type="checkbox"] {
  @apply h-5 w-5;
}
.control .input, .control .select {
  @apply mx-2 rounded <sm:mx-0;
}
.control .select {
  @apply pr-8;
}
.control {
  @apply flex flex-col;
}
.control > .description {
  order: 1;
  white-space: pre-line;
}
.control > .wrapper {
  order: 2;
}
.control > .error {
  order: 3;
}
.vertical-layout, .group {
  @apply flex flex-col flex-wrap py-4 h-full;
  gap: 1rem;
}
.array-list {
  @apply flex flex-col flex-wrap h-full;
  gap: 0.75rem;
}
.label, label {
  padding-bottom: 0.5em;
  padding-left: 0.45em;
  display: inline-block;
  font-weight: bold;
}
.description {
  padding-left: 0.45em;
  padding-bottom: 0.7em;
  @apply text-sm italic;
}
.array-list {
  padding: 0;
}
.array-list-label {
  font-weight: bold;
}
.array-list-item-label {
  margin-right: auto;
}
.array-list-item-delete {
  @apply absolute right-0 top-2 text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300;
}
.array-list-item-toolbar {
  @apply flex flex-row items-baseline relative;
}
.array-list-item-label {
  display: none;
}
.array-list-item-toolbar>button {
  padding: 0.5rem;
}
.one-of-container {
  @apply h-full flex flex-col rounded-lg bg-gray-100 dark:bg-gray-800 px-3 py-3;
}
.settings-view .tab-panel {
  @apply h-full;
}
.settings-view .settings-panel {
  @apply bg-white dark:bg-gray-900 px-5 py-5 <sm:px-4;
}
.settings-view .array-list-shell {
  @apply rounded-lg bg-gray-100 dark:bg-gray-800 px-4 py-3 <sm:px-3;
}
.settings-view .array-list-head {
  @apply flex items-center justify-between gap-3 w-full;
}
.settings-view .one-of-tabs,
.settings-view .array-list-tabs {
  @apply overflow-x-auto;
}
.settings-view .one-of-tabs .tab,
.settings-view .array-list-tabs .tab {
  @apply whitespace-nowrap;
}
.settings-view .one-of-tabs .item,
.settings-view .array-list-tabs .item {
  @apply cursor-pointer whitespace-nowrap rounded-t-md bg-transparent px-3 py-1 text-sm leading-5 text-gray-600 dark:text-gray-300;
}
.settings-view .one-of-tabs .item:hover,
.settings-view .array-list-tabs .item:hover {
  @apply bg-white dark:bg-gray-700;
  transition: background-color 0.2s;
}
.settings-view .one-of-tabs .active,
.settings-view .array-list-tabs .active {
  @apply bg-white dark:bg-gray-900 text-gray-900 dark:text-white;
}
.settings-view .one-of-panel,
.settings-view .array-list-panel {
  @apply rounded-b-lg rounded-tr-lg bg-white dark:bg-gray-900 px-4 py-3 <sm:px-3;
}
.array-list-add {
  @apply rounded-full h-6 w-6 leading-6 bg-blue-700 text-white z-1 flex-none;
}
.array-list-item-move-up {
  display: none;
}
.array-list-item-move-down {
  display: none;
}
.array-list-no-data {
  @apply p-4 bg-white dark:bg-gray-900 italic rounded-md;
}
.wrapper {
  @apply flex py-2 <sm:flex-col;
}
.control > .wrapper:has(> input[type="checkbox"]) {
  @apply items-center gap-2 <sm:flex-row;
}
.control > .wrapper:has(> input[type="checkbox"])::before {
  content: "Enabled";
  @apply px-2 text-sm text-gray-700 dark:text-gray-200;
}
.wrapper p {
  @apply px-2 text-sm;
}
.wrapper input {
  @apply w-1/2 <sm:w-full;
}
.wrapper input, .wrapper select {
  @apply dark:bg-gray-800 dark:text-white !important;
}
.wrapper input:not([type=checkbox]):read-only {
  @apply bg-gray-200 dark:bg-gray-800 text-gray-500 !important;
}
.array-list-item-wrapper {
  @apply bg-transparent rounded-none p-0;
}
.array-list-item-toolbar {
  @apply min-h-8;
}
.array-list-legend {
  margin-bottom: 0;
}
.description {
  padding-bottom: 0;
}
</style>
