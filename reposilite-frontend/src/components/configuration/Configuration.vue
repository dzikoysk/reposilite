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
import {computed, ref, shallowRef} from 'vue'
import {useSession} from '../../store/session'
import {createToast} from 'mosha-vue-toastify'
import {createAjv} from '@jsonforms/core'
import {JsonForms} from '@jsonforms/vue'
import {vanillaRenderers} from '@jsonforms/vue-vanilla'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'
import { default as ObjectRenderer, tester as objectTester } from './renderers/ObjectRenderer.vue'
import { default as AllOfRenderer, tester as allOfTester } from './renderers/AllOfRenderer.vue'
import { default as OneOfRenderer, tester as oneOfTester } from './renderers/OneOfRenderer.vue'
import { default as ConstantRenderer, tester as constantTester } from './renderers/ConstantRenderer.vue'
import { default as OptionalRenderer, tester as optionalTester } from './renderers/OptionalRenderer.vue'

const { client } = useSession()
const configuration = ref({})
const configurations = shallowRef([])
const configurationSchema = shallowRef({})
const selectedConfig = ref('')

const getSchema = async (name) => {
  try {
    return (await client.value.schema.get(name)).data
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}
const listConfigs = async () => {
  try {
    return (await client.value.config.list()).data
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}

const getConfig = async (name) => {
  try {
    return (await client.value.config.get(name)).data
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}

const updateConfig = async (name, value) => {
  try {
    return (await client.value.config.put(name, value)).data
  } catch (e) {
    createToast(`${e || ''}`, { type: 'danger' })
  }
}

const fetchConfiguration = async () => {
  const confs = {}
  const schemas = {}
  configurations.value = await listConfigs()
  for (const conf of configurations.value) {
    confs[conf] = await getConfig(conf)
    schemas[conf] = await getSchema(conf)
  }
  configuration.value = confs
  configurationSchema.value = schemas
  selectedConfig.value = configurations.value[0]
  createToast('Configuration loaded', { type: 'success' })
}

const updateConfiguration = async () => {
  const confs = {}
  const errored = []
  for (const conf of configurations.value) {
    const newValue = await updateConfig(conf, configuration.value[conf])
    if (newValue) {
      confs[conf] = newValue
    } else {
      errored.push(conf)
    }
  }
  configuration.value = confs
  if (errored.length > 0) {
    createToast(`Failed to update ${errored.join(', ')}`, { type: 'danger' })
  } else {
    createToast('Configuration updated', { type: 'success' })
  }
}

fetchConfiguration().then(() => {
  configurations.value.forEach(value => console.log(value, configurationSchema.value[value]))
})

const renderers = [
  ...vanillaRenderers,
  {
    tester: (uischema, schema) => {
      let x = objectTester(uischema, schema)
      return x === -1 || schema.title === 'Proxied Maven Repository' ? -1 : x  // needed because without it hangs TODO find out why
    },
    renderer: ObjectRenderer
  },
  {tester: allOfTester, renderer: AllOfRenderer},
  {tester: oneOfTester, renderer: OneOfRenderer},
  {tester: constantTester, renderer: ConstantRenderer},
  {tester: optionalTester, renderer: OptionalRenderer}
]

const ajv = computed(() => createAjv({
  'formats': {
    'storage-quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'maven-artifact-group': /^(\w+\.)*\w+$/,
    'repository-name': {
      type: 'string',
      validate: (name) => name in configuration.value['maven_repositories'].repositories
    }
  }
}))
</script>

<template>
  <div class="container mx-auto py-10 px-15">
    <div class="flex justify-between pb-5 flex-col xl:flex-row">
      <div>
        <p>Modify configuration shared between all instances.</p>
        <p><strong>Remember</strong>: Configuration propagation can take up to 30 seconds on all instances</p>
      </div>
      <div id="configuration-state" class="flex flex-row pt-3 xl:pt-2">
        <button @click="fetchConfiguration">
          Reset changes
        </button>
        <button @click="updateConfiguration">
          Update and reload
        </button>
      </div>
    </div>
    <Tabs v-model="selectedConfig">
      <Tab v-for="cfg in configurations"
           class="item"
           :key="`config:${cfg}`"
           :val="cfg"
           :label="configurationSchema[cfg]?.title"
           :indicator="true"/>
    </Tabs>
    <TabPanels v-model="selectedConfig">
      <TabPanel v-for="cfg in configurations" :val="cfg" :key="`config_tab:${cfg}`" class="border-1 rounded dark:border-gray-700 p-4">
        <JsonForms
            v-if="configuration[cfg]"
            :data="configuration[cfg]"
            :schema="configurationSchema[cfg]"
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
  @apply rounded-full h-6 w-6 line-height-6 bg-blue-700 ml-auto;
}
.array-list-item-move-up {
  display: none;
}
.array-list-item-move-down {
  display: none;
}
</style>
