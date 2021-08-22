<template>
  <div>
    <vue-final-modal
      v-model="showLogin"
      v-bind="$attrs"
      classes="flex justify-center items-center"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <p class="font-bold text-xl pb-4">Login</p>
        <form class="flex flex-col w-96">
          <input placeholder="Alias" v-model="alias" type="text" class="input"/>
          <input placeholder="Token" v-model="token" type="password" class="input"/>
          <div class="text-right mt-1">
            <button @click="close()" class="text-blue-400 text-xs">← Back to index</button>
          </div>
          <div class="bg-gray-100 dark:bg-gray-800 py-2 my-3 rounded-md cursor-pointer" @click="signin(alias, token)">Sign in</div>
        </form>
        <button class="absolute top-0 right-0 mt-5 mr-5" @click="close()">🗙</button>
      </div>
    </vue-final-modal>
    <div @click="showLogin = true">
      <slot name="button"></slot>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { VueFinalModal, ModalsContainer } from 'vue-final-modal'
import { createToast } from 'mosha-vue-toastify'
import 'mosha-vue-toastify/dist/style.css'
import useSession from '../../store/session'
import useClient from '../../store/client'

export default {
  inheritAttrs: false,
  components: { VueFinalModal, ModalsContainer },
  setup() {
    const { login } = useSession()
    const { client } = useClient()
    const showLogin = ref(false)
    const alias = ref('')
    const token = ref('')

    const close = () => 
      (showLogin.value = false)

    const signin = (alias, token) => {
      client.auth.me(alias, token)
        .then(_ => {
          try {
            createToast(`Dashboard accessed as ${alias}`, { position: 'bottom-right' })
          } catch(ignored) { /* bug */ }
        })
        .then(_ => login(alias, token))
        .then(_ => close())
        .catch(error => {
          console.log(error)
          createToast(`${error.response.status}: ${error.response.data}`, {
           type: 'danger'
          })
        })
    }

    return {
      alias,
      token,
      close,
      showLogin,
      signin
    }
  }
}
</script>

<style scoped>
.input {
  @apply p-2;
  @apply my-1;
  @apply bg-gray-50 dark:bg-gray-800;
  @apply rounded-md;
}
</style>