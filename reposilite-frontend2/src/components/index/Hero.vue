<template>
  <div class="bg-white dark:bg-black">
    <div class="container mx-auto flex flex-row pt-17 pb-12">
      <div class="w-35">
        <img class="border-2 rounded-full dark:border-gray-700" src="https://avatars.githubusercontent.com/u/75123628?s=200&v=4">
      </div>
      <div class="flex flex-col justify-center px-10">
        <div>
          <p>Public Maven repository for Bookkity organization</p>
        </div>
        <div class="flex flex-row py-2">
          <GlobeIcon />
          <a class="px-3 text-gray-500" href="https://github.com/bookkity">https://github.com/bookkity</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import GlobeIcon from '../icons/GlobeIcon.vue'

export default {
  components: {
    GlobeIcon
  }
}
</script>