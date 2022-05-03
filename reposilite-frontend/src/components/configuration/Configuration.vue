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
import {watch} from 'vue'
import {JsonForms} from '@jsonforms/vue'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'
import { useConfiguration } from '../../store/configuration'

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
  configurations, 
  configuration,
  schema,
  selectedConfiguration
} = useConfiguration()

watch(
  () => props.selectedTab.value,
  (selectedTab, prev) => {
    if (selectedTab === 'Configuration' && prev == undefined && configurations.value.length == 0) {
      fetchConfiguration().then(() => {
        configurations.value.forEach(value => console.log(value, schema.value[value]))
      })
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="container mx-auto py-10 px-15">
    <div class="flex justify-between pb-5 flex-col xl:flex-row">
      <div>
        <p>Modify configuration shared between all instances.</p>
        <p><strong>Remember</strong>: Configuration propagation can take up to 10 seconds on all your instances.</p>
      </div>
      <div id="configuration-state" class="flex flex-row pt-3 xl:pt-2">
        <button @click="fetchConfiguration">Reset changes</button>
        <button @click="updateConfiguration">Update and reload</button>
      </div>
    </div>
    <Tabs v-model="selectedConfiguration">
      <Tab v-for="cfg in configurations"
        class="item"
        :key="`config:${cfg}`"
        :val="cfg"
        :label="schema[cfg]?.title"
        :indicator="true"
      />
    </Tabs>
    <TabPanels v-model="selectedConfiguration">
      <TabPanel v-for="cfg in configurations" :val="cfg" :key="`config_tab:${cfg}`" class="border-1 rounded dark:border-gray-700 p-4">
        <JsonForms
          v-if="configuration[cfg]"
          :data="configuration[cfg]"
          :schema="schema[cfg]"
          :renderers="renderers"
          :ajv="ajv"
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
.description {
  display: none;
}
.vertical-layout, .group, .array-list {
  @apply flex flex-col flex-wrap py-4;
  height: 100%;
  gap: 1rem;
}
.label {
  padding-bottom: 0.5em;
  padding-left: 0.5em;
  display: inline-block;
}
.array-list-item-label {
  margin-right: auto;
}
.array-list-item-toolbar {
  @apply flex flex-row align-items-baseline;
  display: flex;
  align-items: baseline;
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
  border: solid;
  border-radius: .25rem;
  padding-inline: 1rem;
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
</style>
