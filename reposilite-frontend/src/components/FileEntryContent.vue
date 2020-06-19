<template lang="pug">
    .py-2.px-6.border.flex
        .pr-5
            i(v-if="file.type === 'directory'").fas.fa-folder
            i(v-if="file.type === 'file'").fas.fa-file
        .flex.justify-between.w-full
            h1(v-if="file.type === 'file'") {{ file.name }}
            h1(v-else).font-bold {{ file.name }}
            p(v-if="file.type === 'file'") {{ (file.contentLength) }} bytes
</template>

<script>
export default {
  props: ['file']
}
</script>
