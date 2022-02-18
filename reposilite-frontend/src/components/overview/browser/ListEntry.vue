<!--
  - Copyright (c) 2021 dzikoysk
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

defineProps({
  file: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="
    browser-entry flex flex-row justify-between mb-1.5 py-3 rounded-full default-button">
    <div class="flex flex-row">
      <div v-if="file.type == 'DIRECTORY'" class="text-xm px-6 pt-1.75">⚫</div>
      <div v-else class="text-xm px-6 pt-1.75">⚪</div>
      <div class="font-semibold">{{file.name}}</div>
    </div>
    <div v-if="file.contentLength" class="px-6">
      {{prettyBytes(file.contentLength)}}
    </div>
  </div>
</template>
