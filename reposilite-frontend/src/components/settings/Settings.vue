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
import {toRaw, watch} from 'vue'
import {JsonForms} from '@jsonforms/vue'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'
import { useConfiguration } from '../../store/configuration'
import download from 'downloadjs'

const props = defineProps({
  selectedTab: {
    type: Object,
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

const updateFormsConfiguration = (domain, event) =>
  configurations.value[domain] = event.data

/* Fetch configuration only when user opens the configuration tab  */
watch(
  () => props.selectedTab.value,
  (selectedTab, prev) => {
    if (selectedTab === 'Settings' && prev == undefined && domains.value.length == 0) {
      fetchConfiguration().then(() => {
        // debug println
        domains.value.forEach(value => console.log(value, schemas.value[value]))
      })
    }
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

/* JsonForms configuration */
const formsConfiguration = {
  showUnfocusedDescription: true
}
</script>

<template>
  <div class="container mx-auto py-10 px-15">
    <div class="flex justify-between pb-5 flex-col xl:flex-row">
      <div>
        <p>Modify configuration shared between all instances.</p>
        <p><strong>Remember</strong>: Configuration propagation can take up to 10 seconds on all your instances.</p>
      </div>
      <div id="configuration-state" class="flex flex-row pt-3 xl:pt-2">
        <button @click="updateConfiguration">Update and reload</button>
        <button @click="fetchConfiguration">Reset changes</button>
        <button @click="downloadSettings">Download as JSON</button>
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
#configuration-state button {
  @apply bg-blue-700 mx-2 rounded text-sm h-9 px-4 text-white;
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
  @apply flex flex-col flex-wrap py-4;
  height: 100%;
  gap: 1rem;
}
.label {
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
.array-list-item-toolbar {
  @apply flex flex-row align-items-baseline;
  display: flex;
  align-items: baseline;
  display: none;
}
.array-list-item-toolbar>button {
  padding: 0.5rem;
}
.array-list-legend {
  @apply flex flex-row-reverse gap-2;
  width: 100%;
  margin-bottom: 1rem;
}
.array-list-item-wrapper {
  @apply border rounded-md px-6 py-2 dark:border-gray-600;
}
.one-of-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.tab-panel {
  height: 100%;
}
.array-list-add {
  @apply rounded-full h-6 w-6 line-height-6 bg-blue-700 ml-auto text-white;
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
  @apply px-2;
}
</style>
