<template>
  <div v-bind:class="{ 'dark': theme.isDark }">
    <router-view class="min-h-screen dark:bg-black dark:text-white"/>
  </div>
</template>

<script>
import { defineComponent, onMounted } from 'vue'
import { useHead } from '@vueuse/head'
import useTheme from "./store/theme"

export default defineComponent({
  setup() {
    useHead({
      title: window.REPOSILITE_TITLE,
      description: window.REPOSILITE_DESCRIPTION
    })

    const { theme, fetchTheme } = useTheme()

    onMounted(() => {
      fetchTheme()
    })

    return {
      theme
    }
  }
})
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');

#app {
  font-family: 'Inter', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.container {
  @apply px-10;
}

.active {
  @apply dark:border-white;
}

.bg-default {
  @apply bg-gray-50 dark:border-gray-900;
}
</style>
