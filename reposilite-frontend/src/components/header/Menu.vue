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
  <nav class="flex flex-row <sm:(max-w-100px flex-wrap flex-1 justify-end min-w-1/2)">
    <div v-if="logged" class="pt-1.1 px-2 <sm:(py-2 max-w-full truncate)">
      Welcome 
      <span class="font-bold underline">{{ token.name }}</span>
    </div>
    <LoginModal>
      <template v-slot:button>
        <MenuButton v-if="!logged">
          Sign in
        </MenuButton>
      </template>
    </LoginModal>
    <MenuButton v-if="logged" @click="signout()" class="<sm:hidden">
      Logout
    </MenuButton>
    <div v-if="logged"
         class="hidden px-2.7 pt-0.8 mr-1.5 cursor-pointer rounded-full bg-white dark:bg-gray-900 max-h-35px <sm:(block)">
      <LogoutIcon @click="signout()"/>
    </div>
    <div class="pl-2 pt-1.3 rounded-full max-h-35px default-button" @click="toggleTheme()">
      <SunIcon class="mr-1.9" v-if="theme.isDark"/>
      <MoonIcon class="mr-1.5" v-else/>
    </div>
  </nav>
</template>

<script>
import { computed } from 'vue'
import MenuButton from './MenuButton.vue'
import LoginModal from './LoginModal.vue'
import MoonIcon from '../icons/MoonIcon.vue'
import SunIcon from '../icons/SunIcon.vue'
import LogoutIcon from '../icons/LogoutIcon.vue'
import useTheme from "../../store/theme"
import useSession from '../../store/session'
import usePlaceholders from '../../store/placeholders'

export default {
  components: {LogoutIcon, MenuButton, LoginModal, MoonIcon, SunIcon },
  props: {
    token: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const { theme, toggleTheme } = useTheme()
    const { title } = usePlaceholders()

    const { isLogged, logout } = useSession()
    const token = props.token
    const logged = computed(() => isLogged(token))
    const signout = () => logout()

    return {
      token,
      title,
      logged,
      signout,
      theme,
      toggleTheme,
    }
  },
}
</script>