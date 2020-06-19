<template lang="pug">
    .file-preview.w-full
        a(v-if="file.type === 'file'" :href="url()" target="_blank" )
            FileEntryContent(:file="file")
        router-link(v-else :to="uri()")
            FileEntryContent(:file="file")
</template>

<script>
import FileEntryContent from './FileEntryContent'

export default {
  props: ['file'],
  components: {
    FileEntryContent
  },
  data: () => ({
    qualifier: ''
  }),
  mounted () {
    this.qualifier = this.getQualifier()
  },
  methods: {
    uri () {
      return this.normalize(this.$route.fullPath) + '/' + this.file.name
    },
    url () {
      return this.qualifier + '/' + this.file.name
    }
  }
}
</script>
