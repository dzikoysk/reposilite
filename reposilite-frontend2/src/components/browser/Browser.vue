<template>
  <div>
    <div class="bg-white dark:bg-black">
      <div class="container mx-auto">
        <p class="py-4 font-semibold">Index of /</p>
      </div>
    </div>
    <hr class="border-gray-100 dark:border-gray-900">
    <div>
      <div class="container mx-auto relative">
        <div class="lg:absolute pt-5 -top-24.5 right-8">
          <Card/>
        </div>
        <div class="pt-2 min-h-90">
          <div v-for="file in files" v-bind:key="file" class="flex flex-row py-3">
            <div class="text-xm pt-1.6">⚫</div>
            <div class="px-6">{{file}}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Card from './Card.vue'

export default {
  components: {
    Card
  },
  data() {
    return {
      files: [ 'releases', 'snapshot' ],
    }
  }
}
</script>