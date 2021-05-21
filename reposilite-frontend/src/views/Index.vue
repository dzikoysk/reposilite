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
  #app
    header
      //Wave(:accentColor="reposilite.accentColor").absolute.w-full
      router-link(to="/").flex.h-56.flex-col.justify-center.px-8.container.mx-auto
        .w-full
          h1.text-5xl.font-bold.pt-1 {{ reposilite.title }}
        .w-full
          p.text-lg.w-96.md_w-full {{ reposilite.description }}
    main.mt-64.lg_mt-24
      .container.mx-auto
        .mx-4.pb-16
           FileBrowser(
             :qualifier='qualifier'
             :auth="{}"
             prefix=""
           )
    notifications(group="index" position="center top")
</template>

<script>
import Vue from 'vue'
import Wave from '../components/Wave'
import FileBrowser from '../components/browser/FileBrowser'

export default {
  data: () => ({
    reposilite: Vue.prototype.$reposilite,
    qualifier: undefined
  }),
  components: {
    Wave,
    FileBrowser
  },
  watch: {
    $route: {
      immediate: true,
      handler: function () {
        this.qualifier = this.getQualifier()
      }
    }
  }
}
</script>

<style lang="stylus">
#app
  -webkit-font-smoothing antialiased
  -moz-osx-font-smoothing grayscale
  height 100%
  width 100%
</style>
