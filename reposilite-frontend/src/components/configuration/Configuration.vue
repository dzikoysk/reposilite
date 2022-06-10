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
import { PrismEditor } from 'vue-prism-editor'
import 'vue-prism-editor/dist/prismeditor.min.css'
import prism from "prismjs"
import "prismjs/themes/prism-coy.css"

const { client } = useSession()

const highlighter = (code) => 
  prism.highlight(code, prism.languages.js)

const configurationName = "configuration.shared.cdn"
const configuration = ref('')
const configurationInitialized = ref(false)

const fetchConfiguration = () => 
  client.value.settings.content(configurationName)
    .then(response => configuration.value = response.data.content)
    .catch(error => createToast(error, { type: 'error' }))

const updateConfiguration = () =>
  client.value.settings.updateContent(configurationName, configuration.value)
    .then(() => createToast('Configuration has been deployed, fetching...', { type: 'info' }))
    .then(() => fetchConfiguration())
    .then(() => createToast('Configuration reloaded, refresh page to see changes', { type: 'success' }))
    .catch(error => createToast(error, { type: 'error' }))

fetchConfiguration()
  .then(() => configurationInitialized.value = true)
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
          <p>Reset changes</p>
        </button>
        <button @click="updateConfiguration">
          <p>Update and reload</p>
        </button>
      </div>
    </div>
    <div class="border-1 rounded p-4 dark:border-gray-700">
      <prism-editor
        v-if="configurationInitialized"
        class="configuration-editor font-mono text-xs"
        v-model="configuration" 
        :highlight="highlighter" 
        line-numbers
      />
    </div>
  </div>
</template>

<style>
#configuration-state button {
  @apply bg-blue-700 mx-2 rounded text-sm h-9 px-4 text-white;
}
.configuration-editor .prism-editor__textarea {
  width: 4096px !important;
}
.configuration-editor .prism-editor__editor {
  white-space: pre !important;
}
.configuration-editor .prism-editor__container {
  overflow-x: auto !important;
}
</style>
