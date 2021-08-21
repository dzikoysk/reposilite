<!--
  - Copyright (c) 2021 dzikoysk
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <header class="bg-gray-100 dark:bg-black dark:text-white">
    <div class="container mx-auto flex flex-row py-10 justify-between">
      <h1 class="text-xl font-medium py-1">
        <router-link to="/">{{ title }}</router-link>
      </h1>
      <nav class="flex flex-row mt-0.5">
       <router-link :to="isLogged() ? '/dashboard' : '/login'">
          <div class="mx-2 py-1.5 rounded-full bg-white dark:bg-gray-900 font-bold px-6 text-sm">
            Dashboard
          </div>
        </router-link>
        <div v-if="isLogged()" @click="signout()" class="ml-2 mr-4 py-1.5 rounded-full bg-white dark:bg-gray-900 font-bold px-6 text-sm cursor-pointer">
            Logout
          </div>
        <div class="pl-2 pr-1.5 py-0.9 cursor-pointer rounded-full bg-white dark:bg-gray-900" @click="toggleTheme()">
          <SunIcon v-if="theme.isDark"/>
          <MoonIcon v-else/>
        </div>
      </nav>
    </div>
    <Hero class="pt-2 pb-11" />
  </header>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import Hero from './Hero.vue'
import MoonIcon from '../icons/MoonIcon.vue'
import SunIcon from '../icons/SunIcon.vue'
import useTheme from "../../store/theme"
import useSession from '../../store/session'

export default {
  components: { Hero, MoonIcon, SunIcon },
  setup() {
    const router = useRouter()
    const { session, isLogged, logout } = useSession()
    const { theme, toggleTheme } = useTheme()
    const title = ref(window.REPOSILITE_TITLE)

    const signout = () => {
      logout()
      router.push('/')
    }

    return {
      title,
      theme,
      toggleTheme,
      isLogged,
      signout
    }
  }
}
</script>