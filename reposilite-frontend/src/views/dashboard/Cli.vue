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
    data() {
        return {
            connection: undefined,
            log: []
        }
    },
    created() {
        let origin = (process.env.NODE_ENV === 'production') ? window.location.origin : 'http://localhost:80'

        if (origin.startsWith('https')) {
            origin = origin.replace('https', 'wss')
        }

        if (origin.startsWith('http')) {
            origin = origin.replace('http', 'ws')
        }

        const convert = new Convert()
        this.connection = new WebSocket(origin + '/api/cli', [ this.$parent.auth.alias + ':' + this.$parent.auth.token ])

        this.connection.onmessage = (event) => {
            this.log.push(convert.toHtml(event.data))
            this.$nextTick(() => this.scrollToEnd())
        }
        this.connection.onerror = (error) => this.$notify({
            group: 'cli',
            type: 'error',
            title: 'CLI Error',
            text: error
        })

        this.connection.onclose = () => this.$notify({
            group: 'cli',
            type: 'warn',
            title: 'Connection closed'
        })
    },
    mounted() {
        this.$nextTick(() => document.getElementById('in').focus())
    },
    beforeDestroy() {
        this.connection.close()
    },
    methods: {
        execute() {
            const input = document.getElementById('in')
            const value = input.value
            input.value = ''
            this.connection.send(value)
        },
        scrollToEnd() {
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