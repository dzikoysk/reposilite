<template lang="pug">
    .file-preview.w-full    
        a(v-if="file.type === 'file'" :href="getUri()" target="_blank" )
            FileEntryContent(:file="file")
        router-link(v-else :to="getUri()")
            FileEntryContent(:file="file")
</template>

<script>
import FileEntryContent from './FileEntryContent'

export default {
    props: [ 'file' ],
    components: {
        FileEntryContent
    },
    data: () => ({
        uri: undefined
    }),
    mounted() {
        this.uri = this.$route.params['qualifier']

        if (this.uri.length > 0) {
            this.uri += '/'
        }
    },
    methods: {
        getUri() {
            return '/' + this.uri + this.file.name
        }
    }
}
</script>

<style lang="stylus">

</style>