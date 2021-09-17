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
  <nav class="flex flex-row">
    <div v-if="isLogged()" class="pt-1.1 px-2">
      Welcome 
      <span class="font-bold underline">{{ token.name }}</span>
    </div>
    <LoginModal>
      <template v-slot:button>
      <MenuButton v-if="!isLogged()">
        Sign in
      </MenuButton>
      </template>
    </LoginModal>
    <MenuButton v-if="isLogged()" @click="signout()">
      Logout
    </MenuButton>
    <div class="pl-2 pt-1.3 cursor-pointer rounded-full bg-white dark:bg-gray-900" @click="toggleTheme()">
      <SunIcon class="mr-1.9" v-if="theme.isDark"/>
      <MoonIcon class="mr-1.5" v-else/>
    </div>
  </nav>
</template>

<script>
import { ref } from 'vue'
import MenuButton from './MenuButton.vue'
import LoginModal from './LoginModal.vue'
import MoonIcon from '../icons/MoonIcon.vue'
import SunIcon from '../icons/SunIcon.vue'
import useTheme from "../../store/theme"
import useSession from '../../store/session'
import usePlaceholders from '../../store/placeholders'

export default {
  components: { MenuButton, LoginModal, MoonIcon, SunIcon },
  setup() {
    const { token, isLogged, logout } = useSession()
    const signout = () => logout()

    const { theme, toggleTheme } = useTheme()
    const { title } = usePlaceholders()

    return {
      isLogged,
      signout,
      token,
      title,
      theme,
      toggleTheme,
    }
  },
}
</script>