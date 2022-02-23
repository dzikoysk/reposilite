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
import prettyBytes from 'pretty-bytes'
import EyeIcon from '../icons/EyeIcon.vue'

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const isHumanReadable =
  ['application/xml', 'text/plain', 'text/xml', 'text/markdown', 'application/json'].some(type => props.file?.contentType == type)

const openUrl = (url) =>
  window.open(url)
</script>

<template>
  <div class="
    browser-entry flex flex-row justify-between mb-1.5 py-3 rounded-full default-button">
    <div class="flex flex-row">
      <div v-if="file.type == 'DIRECTORY'" class="text-xm px-6 pt-1.75">⚫</div>
      <div v-else class="text-xm px-6 pt-1.75">⚪</div>
      <div class="font-semibold">{{file.name}}</div>
    </div>
    <div v-if="file.hasOwnProperty('contentLength')" class="px-6 flex">
      <div v-if="isHumanReadable" :title="`Click to view ${file.name} file content in a new tab`">
        <EyeIcon
          id="view-button"
          class="px-1 mr-7 pt-0.4 rounded-full text-purple-300 hover:(transition-colors duration-200 bg-gray-100 dark:bg-gray-900)" 
          @click.left.prevent="openUrl(`${$route.path}/${file.name}`)"
          v-on:click.stop
        />
      </div>
      <div>
        {{ prettyBytes(file.contentLength) }}
      </div>
    </div>
  </div>
</template>