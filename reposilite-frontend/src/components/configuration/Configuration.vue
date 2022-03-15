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
import { ref } from 'vue'
import { useSession } from '../../store/session'
import { createToast } from 'mosha-vue-toastify'
import {JsonForms} from '@jsonforms/vue';
import {createAjv} from '@jsonforms/core';

const { client } = useSession()

const configurationName = "all"
const configuration = ref({})
const configurationSchema = ref({})
const configurationInitialized = ref(false)

const fetchSchema = () =>
    client.value.schema.get(configurationName)
        .then(response => configurationSchema.value = response.data)
        .catch(error => createToast(error, { type: 'danger' }))

const fetchConfiguration = () =>
    client.value.config.get(configurationName)
        .then(response => configuration.value = response.data)
        .catch(error => createToast(error, { type: 'danger' }))

const updateConfiguration = () =>
    client.value.config.put(configurationName, configuration.value)
        .then(response => {
          if (response.status >= 200 && response.status < 300) {
            createToast('Configuration has been deployed', {type: 'success'});
            configuration.value = response.data
          }
        })
        .catch(error => createToast(error, { type: 'danger' }))

fetchSchema().then(fetchConfiguration)
    .then(() => configurationInitialized.value = true)

const ajvOptions = {
  'formats': {
    'storage-quota': /^([1-9]\d*)([KkMmGg][Bb]|%)$/,
    'maven-artifact-group': /^(\w+\.)*\w+$/,
    'repository-name': {
      type: 'string',
      validate: (name) => name in configuration.value.repositories
    }
  }
}
const ajv = createAjv(ajvOptions)
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
    <div class="border-1 rounded p-4 dark:border-gray-700">
      <json-forms :schema="configurationSchema" :data="configuration" :ajv="ajv"></json-forms><!-- TODO: add renderer -->
    </div>
  </div>
</template>

<!--suppress CssInvalidAtRule -->
<style>
#configuration-state button {
  @apply bg-blue-700 mx-2 rounded text-sm h-9 px-4 text-white;
}
.vertical-layout, .group-layout {
  @apply container mx-auto;
}
.vertical-layout .vertical-layout-item .control input:not([type=checkbox]) {
  @apply mx-2 rounded text-sm h-9 px-4 text-black;
}
.vertical-layout .vertical-layout-item .control>.description {
  display: none;
}
</style>
