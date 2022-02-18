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

<script setup>
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import { createToast } from 'mosha-vue-toastify'
import useSession from '../../store/session'

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
    <vue-final-modal
      v-model="showLogin"
      v-bind="$attrs"
      classes="flex justify-center items-center"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <p class="font-bold text-xl pb-4">Login with access token</p>
        <form class="flex flex-col w-96 <sm:w-65" @submit.prevent="signin(name, secret)">
          <input placeholder="Name" v-model="name" type="text" class="input"/>
          <input placeholder="Secret" v-model="secret" type="password" class="input"/>
          <div class="text-right mt-1">
            <button @click="close()" class="text-blue-400 text-xs">← Back to index</button>
          </div>
          <button class="bg-gray-100 dark:bg-gray-800 py-2 my-3 rounded-md cursor-pointer">Sign in</button>
        </form>
        <button class="absolute top-0 right-0 mt-5 mr-5" @click="close()">🗙</button>
      </div>
    </vue-final-modal>
    <div @click="showLogin = true">
      <slot name="button"></slot>
    </div>
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