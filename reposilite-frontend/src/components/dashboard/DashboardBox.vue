<script setup>
import LinkIcon from '../icons/LinkIcon.vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  content: {
    type: String,
    required: true
  },
  link: {
    type: String,
    required: false
  }
})
</script>

<template>
  <div class="py-3 w-1/6 mx-4 text-center border-2 border-xl rounded-xl border-gray-700">
    <a v-if="link" :href="link" target="_blank">
      <div class="flex text-blue-500 w-full justify-center">
        <h1 class="text-md pt-1">{{title}}</h1>
        <LinkIcon class="w-3 py-1"/>
      </div>
    </a>
    <h1 v-else class="text-md pt-1">{{title}}</h1>
    <p class="text-md font-bold">{{content}}</p>
  </div>
</template>