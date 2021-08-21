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
  <div>
    <Header/>
    <!--<hr class="border-gray-700">-->
    <div class="container mx-auto pt-10 px-6 flex justify-center">
      <div class="border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 p-10 rounded-2xl shadow-xl text-center">
        <h1 class="font-bold text-xl pb-4">Login</h1>
        <form class="flex flex-col w-96">
          <input placeholder="Alias" v-model="alias" type="text" class="input"/>
          <input placeholder="Token" v-model="token" type="password" class="input"/>
          <div class="text-right mt-1">
            <router-link to="/" class="text-blue-400 text-xs">← Back to index</router-link>
          </div>
          <div class="bg-gray-100 dark:bg-gray-800 py-2 my-3 rounded-md cursor-pointer" @click="signin(alias, token)">Sign in</div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import 'mosha-vue-toastify/dist/style.css'
import client from '../client'
import useSession from '../store/session'
import Header from '../components/header/Header.vue'

export default {
  components: { Header },
  setup() {
    const router = useRouter()
    const { login } = useSession()
    
    const alias = ref('')
    const token = ref('')

    const signin = (alias, token) => {
      client.auth.me(alias, token)
        .then(response => {
          login(alias, token)
          router.push('/dashboard')
        })
        .catch(error => createToast(`${error.response.status}: ${error.response.data}`, {
          type: 'danger'
        }))
    }

    return {
      alias,
      token,
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