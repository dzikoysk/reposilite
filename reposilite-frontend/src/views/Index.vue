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
  #app
      header
          Wave(:accentColor="this.configuration.accentColor").absolute.w-full
          router-link(to="/").flex.text-white.h-56.flex-col.justify-center.px-8.container.mx-auto
              .w-full
                  h1.text-5xl.segoe.text-grey.font-bold.pt-1 {{ this.configuration.title }}
              .w-full
                  p.text-lg.w-96.md_w-full {{ this.configuration.description }}
      main.mt-64.lg_mt-24
          .container.mx-auto
              .mx-4.pb-16
                  .flex.justify-between.py-4
                      h1.text-xl
                          | Index of
                          span.ml-2
                              span(v-for="(element, idx) in splitQualifier()")
                                  router-link(
                                  :to="splitQualifier().slice(0, idx + 1).join('/')"
                                  ) {{ element }}
                                  span /
                              router-link(:to="'/dashboard' + this.qualifier")
                                  span.ml-3(:style="'color: ' + this.configuration.accentColor")
                                      i.fas.fa-feather-alt
                      router-link(
                          v-if="this.qualifier != undefined && this.qualifier.length > 1"
                          :to='parentPath()'
                      ) ← Back
                  FileEntry(
                      v-if="hasFiles()"
                      v-for="file in response.files"
                      :key="file.name"
                      prefix=""
                      :file="file"
                      :auth="{}"
                  )
                  h1(v-if="isEmpty()") Empty directory
                  h1(v-if="!hasFiles()").font-bold {{ response.message }}
      notifications(group="index" position="center top")
</template>

<script>
import Wave from '../components/Wave'
import FileEntry from '../components/FileEntry'

export default {
  data: () => ({
    configuration: {},
    qualifier: undefined,
    message: undefined,
    response: []
  }),
  components: {
    Wave,
    FileEntry
  },
  metaInfo () {
    return {
      meta: [
        // Default
        {
          name: 'description',
          content:
            'Repository holds build artifacts and dependencies of varying types'
        },
        // Twitter Card
        { name: 'twitter:card', content: 'summary' },
        { name: 'twitter:title', content: 'Maven Repository' },
        {
          name: 'twitter:description',
          content:
            'Repository holds build artifacts and dependencies of varying types'
        },
        // Facebook OpenGraph
        { property: 'og:title', content: 'Maven Repository' },
        { property: 'og:site_name', content: 'Maven Repository' },
        { property: 'og:type', content: 'website' },
        {
          property: 'og:description',
          content:
            'Repository holds build artifacts and dependencies of varying types'
        }
      ]
    }
  },
  created () {
    this.message = window.REPOSILITE_MESSAGE

    if (sessionStorage.configuration) {
      this.configuration = JSON.parse(sessionStorage.configuration)
    }

    this.api('/configuration', {})
      .then(response => {
        this.configuration = response.data
        sessionStorage.configuration = JSON.stringify(this.configuration)
      })
      .catch(err => (this.response = err.response.data))
  },
  mounted () {
    this.updateEntities()
  },
  watch: {
    $route () {
      this.updateEntities()
    }
  },
  methods: {
    updateEntities () {
      this.qualifier = this.getQualifier()

      this.api(this.qualifier, {})
        .then(response => (this.response = response.data))
        .catch(err => {
          this.$notify({
            group: 'index',
            type: 'error',
            title: err.response.data.message
          })
        })
    },
    hasFiles () {
      return this.response.files !== undefined
    },
    isEmpty () {
      return this.hasFiles() && this.response.files.length === 0
    }
  }
}
</script>

<style lang="stylus">
html
  background-color #f1f1f1
#app
  font-family 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif
  -webkit-font-smoothing antialiased
  -moz-osx-font-smoothing grayscale
  color #2c3e50
  height 100%
  width 100%
.blue
  background-color #0077dd
svg
  z-index -1
.segoe
  font-family 'Segoe UI', 'Manrope'
</style>
