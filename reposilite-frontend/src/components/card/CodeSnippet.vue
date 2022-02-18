<script setup>
import {  computed } from 'vue'
import { PrismEditor } from 'vue-prism-editor'
import 'vue-prism-editor/dist/prismeditor.min.css'
import prism from "prismjs"
import "prismjs/themes/prism-coy.css"

const props = defineProps({
  configuration: {
    type: Object,
    required: true
  }
})

const highlightedSnippet = computed(() => ({
  highlighter: (code) => prism.highlight(code, prism.languages[props.configuration.lang] ?? prism.languages.js),
  ...props.configuration
}))
</script>

<template>
  <prism-editor
    class="card-editor font-mono text-ssm absolute"
    v-model="highlightedSnippet.snippet" 
    :highlight="highlightedSnippet.highlighter" 
    readonly
    line-numbers
  />
</template>