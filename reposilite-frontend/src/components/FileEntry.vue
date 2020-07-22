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
        a(v-if="file.type === 'file'" v-on:click="download" target="_blank" ).cursor-pointer
            FileEntryContent(:file="file")
        router-link(v-else :to="uri()")
            FileEntryContent(:file="file")
</template>

<script>
import download from 'downloadjs'
import FileEntryContent from './FileEntryContent'

export default {
  props: ['prefix', 'auth', 'file'],
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
    download () {
      const fileUrl = this.normalize(this.url() + this.qualifier.substring(1)) + this.file.name

      this.$http.get(fileUrl, {
        responseType: 'blob',
        auth: {
          username: this.auth.alias,
          password: this.auth.token
        }
      }).then(response => {
        const content = response.headers['content-type']
        download(response.data, this.file.name, content)
      }).catch(err => console.log(err))
    },
    uri () {
      return this.prefix + this.qualifier + this.file.name
    }
  }
}
</script>
