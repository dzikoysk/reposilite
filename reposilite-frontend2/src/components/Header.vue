<template>
  <header class="bg-white dark:bg-black dark:text-white">
    <div class="container mx-auto flex flex-row py-10 justify-between">
      <h1 class="text-xl font-medium py-1">
        <router-link to="/">{{ title }}</router-link>
      </h1>
      <nav class="flex flex-row mt-0.5">
        <div class="border rounded-full pt-1.5 px-6 text-sm dark:border-gray-900">
          <router-link to="/dashboard">Dashboard</router-link>
        </div>
        <div class="pl-6 py-1 cursor-pointer" v-on:click="toggleTheme()">
          <SunIcon v-if="theme.isDark"/>
          <MoonIcon v-else/>
        </div>
      </nav>
    </div>
  </header>
</template>

<script>
import MoonIcon from './icons/MoonIcon.vue'
import SunIcon from './icons/SunIcon.vue'
import useTheme from "../store/theme"

export default {
  name: 'Index',
  components: {
    MoonIcon,
    SunIcon
  },
  setup() {
    const { theme, toggleTheme } = useTheme()

    return {
      theme,
      toggleTheme
    }
  },
  data() {
    return {
      title: window.REPOSILITE_TITLE
    }
  }
}
</script>