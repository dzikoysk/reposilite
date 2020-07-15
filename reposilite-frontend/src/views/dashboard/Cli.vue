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
    div.text-white.text-xs.bg-black
        #console.pt-3.px-4.overflow-y-scroll.h-144
            p(v-for="message in log" :key="message" v-html="message")
        input#in(placeholder="Type command or '?' to get help" v-on:keyup.enter="execute").w-full.pb-3.pt-2.px-4.bg-black.text-white
        notifications(group="cli" position="center top")
</template>

<script>
import Convert from 'ansi-to-html'

export default {
  data () {
    return {
      connection: undefined,
      log: []
    }
  },
  created () {
    let origin =
      process.env.NODE_ENV === 'production'
        ? window.location.origin + '{{REPOSILITE.VUE_BASE_PATH}}'
        : 'http://localhost:80'

    if (origin.startsWith('https')) {
      origin = origin.replace('https', 'wss')
    }

    if (origin.startsWith('http')) {
      origin = origin.replace('http', 'ws')
    }

    if (origin.endsWith('/')) {
      origin.substring(1)
    }

    const convert = new Convert()
    this.connection = new WebSocket(origin + '/api/cli', [
      this.$parent.auth.alias + ':' + this.$parent.auth.token
    ])

    this.connection.onmessage = event => {
      this.log.push(convert.toHtml(event.data))
      this.$nextTick(() => this.scrollToEnd())
    }
    this.connection.onerror = error =>
      this.$notify({
        group: 'cli',
        type: 'error',
        title: 'CLI Error',
        text: error
      })

    this.connection.onclose = () =>
      this.$notify({
        group: 'cli',
        type: 'warn',
        title: 'Connection closed'
      })
  },
  mounted () {
    this.$nextTick(() => document.getElementById('in').focus())
  },
  beforeDestroy () {
    this.connection.close()
  },
  methods: {
    execute () {
      const input = document.getElementById('in')
      const value = input.value
      input.value = ''
      this.connection.send(value)
    },
    scrollToEnd () {
      const console = document.getElementById('console')
      console.scrollTop = console.scrollHeight
    }
  }
}
</script>

<style lang="stylus">
#console
    white-space pre-wrap
    font-family 'Consolas', 'monospace'
    font-size 12px
</style>
