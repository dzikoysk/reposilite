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
  router-view
</template>

<script>
import Vue from 'vue'

export default {
  name: 'App',
  data () {
    return {
      reposilite: Vue.prototype.$reposilite
    }
  },
  metaInfo () {
    return {
      title: this.reposilite.title
    }
  },
  mounted () {
    console.log('REPOSILITE_MESSAGE: ' + this.reposilite.message)
    console.log('REPOSILITE_BASE_PATH: ' + this.reposilite.basePath)
    console.log('REPOSILITE_VUE_BASE_PATH: ' + this.reposilite.vueBasePath)
    console.log('REPOSILITE_TITLE: ' + this.reposilite.title)
    console.log('REPOSILITE_DESCRIPTION: ' + this.reposilite.description)
    console.log('REPOSILITE_ACCENT_COLOR: ' + this.reposilite.accentColor)
  }
}
</script>
