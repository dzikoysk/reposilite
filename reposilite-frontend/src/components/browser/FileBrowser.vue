<template lang="pug">
  .browser
    .flex.justify-between.py-4
      h1.text-xl
        | Index of
        span.ml-2
          span(v-for="(element, idx) in splitQualifier()")
            router-link(:to="pathFragmentUri(idx)") {{ element }}
            span /
        router-link(v-if="!isDashboard()" :to="'/dashboard' + this.qualifier")
          span.ml-3(:style="'color: ' + configuration.accentColor")
            i.fas.fa-feather-alt
      router-link(v-if="!isRoot()" :to='prefix + parentPath()') ← Back
    FileEntry(
      v-if="hasFiles()"
      v-for="file in response.files"
      :key="file.name"
      :prefix="prefix"
      :file="file"
      :auth="auth")
    h1(v-if="isEmpty()") Empty directory
    h1(v-if="!hasFiles()").font-bold {{ response.message }}
    notifications(group="index" position="center top")
</template>

<script>
import Vue from 'vue'
import FileEntry from './FileEntry'

export default {
  props: {
    qualifier: String,
    prefix: String,
    auth: Object
  },
  data () {
    return {
      configuration: Vue.prototype.$reposilite,
      response: []
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

        this.api(this.qualifier, this.auth)
          .then(response => (this.response = response.data))
          .catch(err => {
            this.$notify({
              group: 'index',
              type: 'error',
              title: err.response.data.message
            })
          })
      }
    }
  },
  methods: {
    pathFragmentUri (index) {
      return this.splitQualifier()
        .slice(0, index + 1)
        .join('/')
    },
    hasFiles () {
      return this.response.files !== undefined
    },
    isEmpty () {
      return this.hasFiles() && this.response.files.length === 0
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
