<template>
  <div>
    <Header/>
    <div class="container mx-auto pt-10 px-6 flex justify-center">
      <div class="border border-gray-100 dark:border-gray-700 m-w-20 p-10 rounded-2xl shadow-md text-center">
        <h1 class="font-bold text-xl pb-4">Login</h1>
        <form class="flex flex-col w-96">
          <input placeholder="Alias" type="text" class="input"/>
          <input placeholder="Token" type="password" class="input"/>
          <div class="text-right mt-1">
            <router-link to="/" class="text-blue-400 text-xs">← Back to index</router-link>
          </div>
          <div class="bg-gray-100 dark:bg-gray-900 py-2 my-3 cursor-pointer" v-bind:click="login">Sign in</div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import Header from '../components/Header.vue'

export default {
  name: 'Index',
  components: {
    Header
  },
  methods: {
    login() {

    }
  }
}
</script>

<style scoped>
.input {
  @apply p-2;
  @apply my-1;
  @apply bg-gray-50 dark:bg-gray-900;
}
</style>