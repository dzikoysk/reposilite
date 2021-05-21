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

<template lang="pug">
  .flex.py-2.px-6.border-1.border-gray-300
    .pr-5
      FolderIcon(v-if="file.type === 'directory'")
      FileIcon(v-if="file.type === 'file'")
    .flex.justify-between.w-full
      h1(v-if="file.type === 'file'") {{ file.name }}
      h1(v-else).font-bold {{ file.name }}
      p(v-if="file.type === 'file'") {{ fileSize }}
</template>

<script>
import prettyBytes from 'pretty-bytes'
import FolderIcon from 'vue-material-design-icons/Folder.vue'
import FileIcon from 'vue-material-design-icons/File.vue'

export default {
  props: {
    file: Object
  },
  components: {
    FileIcon,
    FolderIcon
  },
  data () {
    return {
      fileSize: prettyBytes(this.file.contentLength)
    }
  }
}
</script>

<style lang="stylus">
.folder-icon svg
.file-icon svg
  width: 1.2em
  padding-top: 0.2em
</style>
