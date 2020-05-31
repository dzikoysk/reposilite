<template lang="pug">
  #app
    header
        Wave(:accentColor="this.configuration.accentColor").absolute.w-full
        a(href="/").flex.text-white.h-56.flex-col.justify-center.px-8.container.mx-auto
            .w-full
                h1.text-5xl.segoe.text-grey.font-bold.pt-1 {{ this.configuration.title }}
            .w-full
                p.text-lg.w-96.md_w-full {{ this.configuration.description }}
    main.mt-64.lg_mt-24
        .container.mx-auto
            .mx-4.pb-16
                .flex.justify-between.py-4
                    h1.text-xl Index of {{ this.qualifier }}
                    router-link(
                        v-if="this.qualifier != undefined && this.qualifier.length > 1" 
                        :to='getParentPath()'
                    ) ← Back
                FileEntry(
                    v-if="response.files != undefined && response.files.length > 0" 
                    v-for="file in response.files" 
                    :key="file.name" 
                    :file="file"
                )
                h1(v-else) {{window.REPOSILITE_MESSAGE}}

</template>

<script>
import Wave from '../components/Wave'
import FileEntry from '../components/FileEntry'

export default {
  data: () => ({
    configuration: {},
    qualifier: undefined,
    response: []
  }),
  components: {
    Wave,
    FileEntry
  },
  created() {
    this.$http
      .get(this.getApi('configuration'))
      .then(response => (this.configuration = response.data))
  },
  mounted() {
    this.updateEntities()
  },
  watch: {
    $route() {
       this.updateEntities()
    }
  },
  methods: {
    updateEntities() {
      this.qualifier = this.$route.params['qualifier']

      this.$http
        .get(this.getApi(this.qualifier))
        .then(response => (this.response = response.data))
    },
    getApi(path) {
      return ((process.env.NODE_ENV == 'production') ? '/' : 'http://localhost:80/') + 'api/' + path
    },
    getParentPath() {
        const elements = ('/' + this.qualifier).split('/')
        elements.pop()
        const path = elements.join('/')
        return path.length == 0 ? '/' : path
    }
  }
}
</script>

<style lang="stylus">  
html
  background-color: #f8f8f8
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
  font-family 'Segoe UI'
</style>