<template lang="pug">
  #app.flex.justify-center.items-center
    form#login(v-if="!auth.verified").p-8.text-center(method="post").border-dashed.border-black.border-1.rounded
        h1.font-bold.py-4.text-xl Login
        div.py-1
            input(placeholder="Alias" name="alias" v-model="auth.alias").w-96.p-1
        div.py-1
            input(placeholder="Token" name="token" v-model="auth.token" autocomple="on").w-96.p-1
        div.py-3
            button.bg-gray-300.px-6.py-1.mt-1(v-on:click="login") Login
        div.py-1
            p(v-if="error") {{ this.error }}
    #panel(v-else).p-6.container
        header.pb-4
            router-link(to="/dashboard").px-4 Index
            router-link(to="/dashboard/upload").px-4 Upload
            router-link(to="/dashboard/settings" v-if="auth.manager").px-4 Settings
            button(v-on:click="logout").px-4 Logout
        hr.py-2
        router-view
</template>

<script>
import FileEntry from '../components/FileEntry'

const defaultAuth = {
    alias: '',
    token: '',
    path: '',
    manager: false,
    verified: false
}

export default {
    data: () => ({
        error: undefined,
        auth: Object.assign({}, defaultAuth),
        qualifier: '',
        files: [],
    }),
    components: {
        FileEntry
    },
    mounted() {
        if (sessionStorage.auth) {
            this.auth = JSON.parse(sessionStorage.auth)
            this.list()
        }
    },
    watch: {
        $route() {
            this.list()
        }
    },
    methods: {
        login(event) {
            event.preventDefault()

            this.api('/auth', {
                username: this.auth.alias,
                password: this.auth.token 
            }).then(response => {
                this.auth.verified = true
                this.auth.path = response.data.path
                this.auth.manager = response.data.manager
                sessionStorage.auth = JSON.stringify(this.auth)
                this.list()
            }).catch(err => {
                this.error = err.response.status + ': ' + err.response.data.message
            })
        },
        list() {  
            const qualifier = this.getQualifier()
            console.log(this.qualifier)

            this.api(qualifier, {
                username: this.auth.alias,
                password: this.auth.token 
            }).then(response => {
                this.files = response.data.files
                this.qualifier = qualifier
            }).catch(err => {
                console.log(err)
                this.error = err.status + ': ' + err.response.data.message
            })
        },
        logout() {
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
</style>

