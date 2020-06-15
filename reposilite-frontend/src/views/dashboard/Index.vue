<template lang="pug">
    div(v-if="this.$parent.files")
        .flex.justify-between.pb-6.pt-2.px-2
            h1.font-bold
                | Index of 
                span.ml-1 {{ this.$parent.qualifier }}
            router-link(v-if="this.$parent.qualifier != undefined && this.$parent.qualifier.length > 0" :to="'/dashboard' + parentPath()") ← Back
        FileEntry(
            v-for="file in this.$parent.files" 
            :key="file.name" 
            :file="file"
        )
</template>

<script>
import FileEntry from '../../components/FileEntry'

export default {
    components: {
        FileEntry
    }
}
</script>