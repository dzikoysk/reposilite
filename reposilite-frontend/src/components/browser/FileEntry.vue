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
  .file-preview.w-full
    a(v-if="file.type === 'file'" v-on:click="handleDownload" target="_blank" ).cursor-pointer
      FileEntryContent(:file="file")
    router-link(v-else :to="fileUri()")
      FileEntryContent(:file="file")
</template>

<script>
import FileEntryContent from './FileEntryContent'
import download from 'downloadjs'
import mime from 'mime-types'

export default {
  props: {
    prefix: String,
    auth: Object,
    file: Object
  },
  components: {
    FileEntryContent
  },
  data: () => ({
    qualifier: ''
  }),
  mounted () {
    this.qualifier = this.getQualifier()
  },
  methods: {
    handleDownload () {
      this.$http
        .get(this.fileUrl(), {
          responseType: 'blob',
          headers: {
            'Content-Type': mime.lookup(this.file.name)
          },
          auth: {
            username: this.auth.alias,
            password: this.auth.token
          }
        })
        .then(response => {
          const content = response.headers['content-type']
          download(response.data, this.file.name, content)
        })
        .catch(err => console.log(err))
    },
    fileUrl () {
      return this.baseUrl() + this.qualifier.substring(1) + this.file.name
    },
    fileUri () {
      return this.prefix + this.qualifier + this.file.name
    }
  }
}
</script>
