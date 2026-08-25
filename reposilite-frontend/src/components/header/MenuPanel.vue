<!--
  - Copyright (c) 2020-2026 dzikoysk
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

<script setup>
import { useSession } from '../../store/session'
import MenuButton from './MenuButton.vue'
import LoginModal from './LoginModal.vue'
import MoonIcon from '../icons/MoonIcon.vue'
import SunIcon from '../icons/SunIcon.vue'
import LogoutIcon from '../icons/LogoutIcon.vue'
import useTheme from "../../store/theme"

const { theme, changeTheme } = useTheme()
const { token, isLogged, logout } = useSession()

const toggleTheme = () => {
  switch (theme.mode) {
    case 'light':
      changeTheme('dark')
      break
    case 'dark':
      changeTheme('auto')
      break
    case 'auto':
      changeTheme('light')
      break
  }
}
</script>

<template>
  <nav
    class="flex flex-row <sm:(max-w-100px flex-wrap flex-1 justify-end min-w-1/2)"
    aria-label="Account and display settings"
  >
    <div v-if="isLogged" class="pt-1.1 px-2 <sm:hidden">
      Welcome 
      <span class="font-bold underline">{{ token.name }}</span>
    </div>
    <LoginModal v-if="!isLogged">
      <template v-slot:button>
        Sign in
      </template>
    </LoginModal>
    <MenuButton v-if="isLogged" @click="logout()" class="<sm:hidden">
      Logout
    </MenuButton>
    <button
      v-if="isLogged"
      type="button"
      class="hidden px-2.7 pt-0.8 mr-1.5 cursor-pointer rounded-full bg-white dark:bg-gray-900 max-h-35px <sm:(block pt-1.5)"
      aria-label="Log out"
      title="Log out"
      @click="logout()"
    >
      <LogoutIcon aria-hidden="true" />
    </button>
    <button
      type="button"
      class="flex justify-center items-center rounded-full w-40px h-35px default-button"
      :aria-label="`Current theme: ${theme.mode}. Change theme`"
      :title="`Current theme: ${theme.mode}. Change theme`"
      @click="toggleTheme()"
    >
      <SunIcon v-if="theme.mode === 'light'" aria-hidden="true" />
      <MoonIcon v-if="theme.mode === 'dark'" class="pl-0.5" aria-hidden="true" />
      <span class="font-bold w-full text-center text-lg" v-if="theme.mode === 'auto'">
        A
      </span>
    </button>
  </nav>
</template>
