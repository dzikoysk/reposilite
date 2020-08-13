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
  form(method="put").flex.flex-col.items-center.pb-3
    h1.pb-3.font-bold Upload artifact
    select#repositories(name="repository" v-model="repository" placeholder="repository" required).w-96.text-center.p-1.m-1
      option(v-for="repo in this.$parent.auth.repositories" :value="repo") {{ repo }}
    input(name="groupId" v-model="groupId" placeholder="groupId" required).p-1.m-1.w-96.text-center
    input(name="artifactId" v-model="artifactId" placeholder="artifactId" required).p-1.m-1.w-96.text-center
    input(name="version" v-model="version" placeholder="version" required).p-1.m-1.w-96.text-center
    div(v-for="(file, index) in files" :key="file.id").p-1.m-1
      i.fas.fa-file.mr-2
      span {{ file.name }}
      span(v-if="file.error") {{ file.error }}
      span(v-else-if="file.success") {{ file.success }}
    FileUpload.my-2.bg-gray-200.border-dashed.border-gray-500.rounded.border.w-96.h-9.pt-1(
      v-model="files"
      ref="upload"
      :drop="true"
      :multiple="true"
    ) Select or drop files
    button(name="submit" type="submit" v-on:click="upload").w-96.p-1.m-1.bg-white.cursor-pointer.border Upload
    notifications(group="upload" position="center top")
</template>

<script>
import FileUpload from 'vue-upload-component'

export default {
  data () {
    return {
      files: [],
      repository: this.$parent.auth.repositories[0],
      groupId: '',
      artifactId: '',
      version: ''
    }
  },
  components: {
    FileUpload
  },
  methods: {
    upload (event) {
      const artifact = `${this.repository}/${this.groupId.replace(
        /\./g,
        '/'
      )}/${this.artifactId}/${this.version}/`

      for (const vueFile of this.files) {
        const auth = this.$parent.auth

        this.$http
          .put(this.url() + artifact + vueFile.name, vueFile.file, {
            auth: {
              username: auth.alias,
              password: auth.token
            }
          })
          .then(() =>
            this.$notify({
              group: 'upload',
              type: 'success',
              title: 'File ' + vueFile.name + ' has been uploaded successfully'
            })
          )
          .catch(err => {
            this.error = err
            this.$notify({
              group: 'upload',
              type: 'error',
              title: 'Cannot upload file ' + vueFile.name,
              text: err.status + ': ' + err.message
            })
          })

        event.preventDefault()
      }
    }
  }
}
</script>

<style lang="stylus">
#file
    cursor pointer
</style>
