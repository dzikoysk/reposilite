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
import {ref} from 'vue'
import {useSession} from '../../store/session'
import {createToast} from 'mosha-vue-toastify'
import {vanillaRenderers} from '@jsonforms/vue-vanilla'
import {createAjv} from '@jsonforms/core'
import {JsonForms} from '@jsonforms/vue'
import {Tabs, Tab, TabPanels, TabPanel} from 'vue3-tabs'

const { client } = useSession()
const configuration = ref({})
const configurations = ref({})
const configurationSchema = ref({})
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
  for (const conf of Object.keys(configurations.value)) {
    confs[conf] = await getConfig(conf)
    schemas[conf] = await getSchema(conf)
  }
  configuration.value = confs
  configurationSchema.value = schemas
  selectedConfig.value = Object.keys(confs)[0]
}

const updateConfiguration = async () => {
  const confs = {}
  for (const conf of Object.keys(configuration.value)) {
    confs[conf] = await updateConfig(conf, configuration.value[conf])
  }
  configuration.value = confs
}

fetchConfiguration().then()

const configs = () => Object.keys(configuration.value)
const config = (name) => configuration.value[name]
const configSchema = (name) => configurationSchema.value[name]
const configName = (name) => configurationSchema.value[name].title

const renderers = [
  ...vanillaRenderers
]

const ajv = createAjv({
  'formats': {
    'storage-quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'maven-artifact-group': /^(\w+\.)*\w+$/,
    'repository-name': {
      type: 'string',
      validate: (name) => name in configuration.value['maven_repositories']
    }
  }
})
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
      <Tab v-for="cfg in configs()"
           class="item"
           :key="`config:${cfg}`"
           :val="cfg"
           :label="configName(cfg)"
           :indicator="true"/>
    </Tabs>
    <TabPanels v-model="selectedConfig">
      <TabPanel v-for="cfg in configs()" :val="cfg" :key="`config_tab:${cfg}`" class="border-1 rounded dark:border-gray-700 p-4">
        <JsonForms
          :data="config(cfg)"
          :schema="configSchema(cfg)"
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
.control .input:not([type=checkbox]) {
  @apply text-sm h-9 px-4 text-black;
}
.control .input[type="checkbox"] {
  @apply h-5 w-5;
}
.control .input {
  @apply mx-2 rounded;
}
.description {
  display: none;
}
.vertical-layout {
  display: flex;
  gap: 1rem;
  flex-direction: column;
}
.label {
  padding-bottom: 0.5em;
  padding-left: 0.5em;
  display: inline-block;
}
</style>
