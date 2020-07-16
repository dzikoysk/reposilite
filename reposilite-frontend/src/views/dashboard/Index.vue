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
    div(v-if="this.files")
        .flex.justify-between.pb-6.pt-2.px-2
            h1.font-bold
                | Index of
                span.ml-2 {{ this.qualifier }}
            router-link(v-if="this.qualifier != undefined && this.qualifier.length > 1" :to="'/dashboard' + parentPath()") ← Back
        FileEntry(
            v-for="file in files"
            :key="file.name"
            :file="file"
        )
        h1(v-if="files && files.length === 0").px-2 Directory is empty
        notifications(group="index" position="center top")
</template>

<script>
import FileEntry from '../../components/FileEntry'

export default {
  data () {
    return {
      qualifier: '',
      files: [],
      auth: this.$parent.auth
    }
  },
  components: {
    FileEntry
  },
  watch: {
    $route: {
      immediate: true,
      handler () {
        if (!this.redirect()) {
          this.list()
        }
      }
    }
  },
  methods: {
    redirect () {
      let path = this.auth.path.replaceAll('\\', '/')

      if (path.startsWith('*')) {
        path = path.replace('*', '/' + this.auth.repositories[0])
      }

      if (!this.$route.fullPath.includes(path)) {
        this.$router.push({ path: `/dashboard${path}` })
        return true
      }

      return false
    },
    list () {
      const qualifier = this.getQualifier()

      this.api(qualifier, this.$parent.auth)
        .then(response => {
          this.files = response.data.files
          this.qualifier = qualifier
        })
        .catch(err => {
          this.$notify({
            group: 'index',
            type: 'warn',
            title: 'Indexing is not available',
            text: err.response.status + ': ' + err.response.data.message
          })
        })

      this.qualifier = qualifier
    }
  }
}
</script>
