<!--
  - Copyright (c) 2022 dzikoysk
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

const props = defineProps({
  selectedTab: {
    type: String,
    required: true
  }
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

const executeIfValid = (callback) => { 
  if (isValid.value) callback() 
}

const updateFormsConfiguration = (domain, event) => {
  configurations.value[domain] = event.data
  isValid.value = event.errors.length == 0
  event.errors.forEach(error => {
    console.log(error)
  })
}

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
  <div class="container mx-auto py-10 px-15">
    <div class="flex justify-between pb-3 flex-col">
      <div>
        <p>Modify configuration shared between all instances.</p>
        <p><strong>Remember</strong>: Configuration propagation can take up to 10 seconds on all your instances.</p>
      </div>
      <div id="configuration-state" class="flex flex-row pt-8">
        <button @click.prevent="fetchConfiguration">Reset changes</button>
        <button @click.prevent="executeIfValid(updateConfiguration)" :class="{ forbidden: !isValid }">Update and reload</button>
        <button @click.prevent="executeIfValid(downloadSettings)" :class="{ forbidden: !isValid }">Download as JSON</button>
        <FactoryResetModal :callback="factoryReset">
            <template v-slot:button>
                <button>Factory reset</button>
            </template>
        </FactoryResetModal>
      </div>
    </div>
    <Tabs v-model="selectedDomain">
      <Tab v-for="domain in domains"
        class="item"
        :key="`config:${domain}`"
        :val="domain"
        :label="schemas[domain]?.title"
        :indicator="true"
      />
    </Tabs>
    <TabPanels v-model="selectedDomain">
      <TabPanel 
        v-for="domain in domains" 
        :val="domain" 
        :key="`config_tab:${domain}`" 
        class="border-1 rounded dark:border-gray-700 p-4"
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
</template>

<!--suppress CssInvalidAtRule -->
<style scoped>
#configuration-state button {
  @apply bg-blue-700 mx-2 rounded text-sm px-4 text-white py-2;
}
#configuration-state .forbidden {
  @apply bg-gray-500 cursor-not-allowed !important;
}
.item {
  @apply pb-1;
  @apply pt-1.5;
  @apply cursor-pointer;
  @apply text-gray-600 dark:text-gray-300;
  @apply bg-gray-100 dark:bg-black;
}
.tabs .item:hover {
  @apply bg-gray-150 dark:bg-gray-900;
  transition: background-color 0.5s;
}
</style>

<!--suppress CssInvalidAtRule -->
<style>
.error {
  @apply text-red-500 px-2 font-bold;
}
input, select {
  @apply dark:bg-gray-900 dark:text-white !important;
}
.vertical-layout, .group-layout {
  @apply container mx-auto;
}
.control .input:not([type=checkbox]), .control .select {
  @apply text-sm h-9 px-4 text-black;
}
.control .input[type="checkbox"] {
  @apply h-5 w-5;
}
.control .input, .control .select {
  @apply mx-2 rounded;
}
.control .select {
  @apply pr-8;
}
.vertical-layout, .group, .array-list {
  @apply flex flex-col flex-wrap py-4 h-full;
  gap: 1rem;
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
  @apply absolute right-0 top-2;
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
.array-list-legend {
  @apply flex flex-row-reverse gap-2 w-full;
  margin-bottom: 1rem;
}
.array-list-item-wrapper {
  @apply border rounded-md px-6 py-2 dark:border-gray-600;
}
.one-of-container {
  @apply h-full flex flex-col; 
}
.tab-panel {
  @apply h-full;
}
.array-list-add {
  @apply rounded-full h-6 w-6 leading-6 bg-blue-700 ml-auto text-white;
}
.array-list-item-move-up {
  display: none;
}
.array-list-item-move-down {
  display: none;
}
.array-list-no-data {
  @apply p-4 bg-gray-200 dark:bg-gray-900 italic rounded-md;
}
.wrapper {
  @apply flex py-2;
}
.wrapper p {
  @apply px-2 text-sm;
}
.wrapper input {
  @apply w-1/2;
}
</style>
