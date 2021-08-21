<template>
  <nav class="flex flex-row">
    <div v-if="isLogged()" class="pt-1.1 px-2">
      Welcome 
      <span class="font-bold underline">{{ session.alias }}</span>
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
import { useRouter } from 'vue-router'
import MenuButton from './MenuButton.vue'
import LoginModal from './LoginModal.vue'
import MoonIcon from '../icons/MoonIcon.vue'
import SunIcon from '../icons/SunIcon.vue'
import useTheme from "../../store/theme"
import useSession from '../../store/session'

export default {
  components: { MenuButton, LoginModal, MoonIcon, SunIcon },
  setup() {
    const router = useRouter()
    const { session, isLogged, logout } = useSession()
    const { theme, toggleTheme } = useTheme()
    const title = ref(window.REPOSILITE_TITLE)

    const signout = () => {
      logout()
      router.go('/')
    }

    return {
      title,
      theme,
      toggleTheme,
      isLogged,
      signout,
      session
    }
  },
}
</script>