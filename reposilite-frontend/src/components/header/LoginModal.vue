<!--
  - Copyright (c) 2023 dzikoysk
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
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import { createToast } from 'mosha-vue-toastify'
import { useSession } from '../../store/session'
import CloseIcon from '../icons/CloseIcon.vue'

const { login } = useSession()
const showLogin = ref(false)
const name = ref('')
const secret = ref('')

const close = () => 
  (showLogin.value = false)

const signin = (name, secret) =>
  login(name, secret)
    .then(() => createToast(`Dashboard accessed as ${name}`, { position: 'bottom-right' }))
    .then(() => close())
    .catch(error => createToast(`${error.response.status}: ${error.response.data.message}`, { type: 'danger' }))
</script>

<script>
export default {
  inheritAttrs: false,
}
</script>

<template>
  <div id="login-modal">
    <VueFinalModal
      v-model="showLogin"
      v-bind="$attrs"
      class="flex justify-center items-center"
      aria-labelledby="login-dialog-title"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <h2 id="login-dialog-title" class="font-bold text-xl pb-4">Login with access token</h2>
        <form class="flex flex-col w-96 <sm:w-65" @submit.prevent="signin(name, secret)">
          <label class="sr-only" for="login-name">Name</label>
          <input id="login-name" placeholder="Name" v-model="name" type="text" autocomplete="username" class="input"/>
          <label class="sr-only" for="login-secret">Secret</label>
          <input id="login-secret" placeholder="Secret" v-model="secret" type="password" autocomplete="current-password" class="input"/>
          <div class="text-right mt-1">
            <button type="button" @click="close()" class="text-blue-400 text-xs">← Back to index</button>
          </div>
          <button type="submit" class="bg-gray-100 dark:bg-gray-800 py-2 my-3 rounded-md cursor-pointer">Sign in</button>
        </form>
        <button
          type="button"
          class="absolute top-0 right-0 mt-5 mr-5"
          aria-label="Close login dialog"
          title="Close"
          @click="close()"
        >
          <CloseIcon class="w-6 h-6" aria-hidden="true" />
        </button>
      </div>
    </VueFinalModal>
    <button
      type="button"
      class="mx-2 py-1.5 rounded-full font-bold px-6 text-sm max-h-35px min-w-93px default-button"
      @click="showLogin = true"
    >
      <slot name="button"></slot>
    </button>
  </div>
</template>

<style scoped>
.input {
  @apply p-2;
  @apply my-1;
  @apply bg-gray-50 dark:bg-gray-800;
  @apply rounded-md;
}
#login-modal button:hover {
  @apply bg-gray-200 dark:bg-gray-700;
  transition: background-color 0.5s;
}
</style>
