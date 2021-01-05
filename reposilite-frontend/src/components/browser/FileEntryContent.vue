<!--
  - Copyright (c) 2020 Dzikoysk
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

<template lang="pug">
  .flex.py-2.px-6.border-1.border-gray-300
    .pr-5
      i(v-if="file.type === 'directory'").fas.fa-folder
      i(v-if="file.type === 'file'").fas.fa-file
    .flex.justify-between.w-full
      h1(v-if="file.type === 'file'") {{ file.name }}
      h1(v-else).font-bold {{ file.name }}
      p(v-if="file.type === 'file'") {{ fileSize }}
</template>

<script>
import prettyBytes from 'pretty-bytes'

export default {
  props: {
    file: Object
  },
  data () {
    return {
      fileSize: prettyBytes(this.file.contentLength)
    }
  }
}
</script>
