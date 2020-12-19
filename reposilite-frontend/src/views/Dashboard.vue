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
  #app.flex.justify-center.items-center
    form#login(v-if="!auth.verified" method="post").p-8.text-center.border-dashed.border-black.rounded.bg-white
      h1.font-bold.pb-4.text-xl Login
      div.py-1
        input(placeholder="Alias" name="alias" v-model="auth.alias").w-96.bg-gray-100.p-2.rounded
      div.py-1
        input(
          name="token"
          v-model="auth.token"
          type="password"
          placeholder="Token"
          autocomplete="on"
        ).w-96.bg-gray-100.p-2.rounded
      div.py-1.text-right.px-2.mt-1
        router-link(:to="this.qualifier").text-blue-400.text-xs ← Back to index
      div.py-3
        button(v-on:click="login").bg-gray-200.px-6.py-1.mt-1.w-96 Login
      notifications(group="login" position="center top")
    #panel(v-else).p-6.container
      header.pb-4
        router-link(to="/dashboard").px-4 Index
        router-link(to="/dashboard/upload").px-4 Upload
        router-link(to="/dashboard/cli" v-if="auth.manager").px-4 Cli
        router-link(to="/dashboard/settings" v-if="auth.manager").px-4 Settings
        button(v-on:click="logout").px-4 Logout
      hr.py-2
      router-view
</template>

<script>
const defaultAuth = {
  alias: '',
  token: '',
  path: '',
  repositories: [],
  manager: false,
  verified: false,
  qualifier: ''
}

export default {
  data: () => ({
    auth: Object.assign({}, defaultAuth),
    qualifier: ''
  }),
  mounted () {
    this.qualifier = this.getQualifier()

    if (sessionStorage.auth) {
      this.auth = JSON.parse(sessionStorage.auth)
    }
  },
  methods: {
    login (event) {
      event.preventDefault()

      this.api('/auth', this.auth)
        .then(response => {
          this.auth.verified = true
          this.auth.path = response.data.path
          this.auth.manager = response.data.permissions.contains('m')
          this.auth.repositories = response.data.repositories
          sessionStorage.auth = JSON.stringify(this.auth)
        })
        .catch(err => {
          console.log(err)
          this.$notify({
            group: 'login',
            type: 'error',
            title: err.response.data.message
          })
        })
    },
    logout () {
      sessionStorage.removeItem('auth')
      this.auth = Object.assign({}, defaultAuth)
      this.error = undefined
    }
  }
}
</script>

<style lang="stylus">
html, body
    height 100%
    width 100%
#app
  height 100%
  width 100%
#panel
    background-color #f8f8f8
    max-height: 90vh
</style>
