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
  .browser
    .flex.justify-between.py-4
      h1.text-xl
        router-link(to="/")
          | Index of
        span.ml-2
          span(v-for="(element, idx) in splitQualifier()")
            router-link(:to="pathFragmentUri(idx)") {{ element }}
            span /
        router-link(v-if="!isRoot()" :to='prefix + parentPath()')
          span.ml-1.text-2xl(:style="'color: ' + configuration.accentColor")
            | &#x2934;
      router-link(v-if="!isDashboard()" :to="'/dashboard' + this.qualifier")
        span.ml-3.text-xl(:style="'color: ' + configuration.accentColor")
          i.fas.fa-feather-alt
    .list.overflow-y-auto
      FileEntry(
        v-if="!error"
        v-for="file in files"
        :key="file.name"
        :prefix="prefix"
        :file="file"
        :auth="auth"
      )
      h1(v-if="!error && filesFetched && files.length == 0") Empty directory
      h1(v-if="error").font-bold {{ response.message }}
    notifications(group="index" position="center top")
</template>

<script>
import Vue from 'vue'
import smoothReflow from 'vue-smooth-reflow'
import FileEntry from './FileEntry'

const CHUNK_SIZE = 10

export default {
  mixins: [smoothReflow],
  props: {
    initQualifier: String,
    prefix: String,
    auth: Object
  },
  data () {
    return {
      configuration: Vue.prototype.$reposilite,
      files: [],
      filesFetched: false,
      error: undefined,
      taskId: 0,
      qualifier: this.initQualifier
    }
  },
  components: {
    FileEntry
  },
  watch: {
    $route: {
      immediate: true,
      handler: function () {
        this.qualifier = this.getQualifier()
        this.taskId += 1
        this.files = []

        this.api(this.qualifier, this.auth)
          .then(response => this.loadFiles(this.taskId, response.data.files))
          .then(() => console.log(this.files))
          .catch(err => this.$notify({
            group: 'index',
            type: 'error',
            title: (this.error = err.response.data.message)
          }))
      }
    }
  },
  mounted () {
    this.$smoothReflow({
      transition: '.25s'
    })
  },
  methods: {
    // replace with virtual scroller in the future
    loadFiles (taskId, files) {
      if (this.taskId !== taskId) {
        return
      }

      let delegatedCount = CHUNK_SIZE

      for (const file of files) {
        Object.freeze(file)
        this.files.push(file)
        delegatedCount--

        if (delegatedCount === 0) {
          setTimeout(() => this.loadFiles(taskId, files.slice(CHUNK_SIZE)), 250)
          break
        }
      }

      this.filesFetched = true
    },
    pathFragmentUri (index) {
      return this.splitQualifier()
        .slice(0, index + 1)
        .join('/')
    },
    isDashboard () {
      return this.prefix === '/dashboard'
    },
    isRoot () {
      return this.qualifier === undefined || this.qualifier.length < 2
    }
  }
}
</script>

<style lang="stylus">
.list
  max-height: 70vh
</style>
