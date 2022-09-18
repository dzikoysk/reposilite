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
import CodeEditor from 'simple-code-editor'
// fix for https://github.com/justcaliturner/simple-code-editor/issues/20
// eslint-disable-next-line no-unused-vars
import hljs from 'highlight.js'

defineProps({
  configuration: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <code-editor
      class="card-editor font-mono text-ssm !absolute !w-auto overflow-x-scroll inset-0 !rounded-none"
      :value="configuration.snippet"
      :languages="[[configuration.lang]]"
      font_size=".8rem"
      read_only
      hide_header
  />
</template>

<style>
.card-editor.hljs,
.card-editor .hljs {
  background: none;
}

.card-editor > .code_area,
.card-editor > .code_area > pre,
.card-editor > .code_area > pre > code {
  width: max-content;
}

.card-editor > .code_area > pre {
  top: -1rem;
}

.card-editor > .code_area > pre > code {
  padding-block: .5rem !important;
  padding-inline: 1rem !important;
}
</style>