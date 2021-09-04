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
  <div class="bg-gray-100">
    <div class="bg-gray-100 dark:bg-black">
      <div class="container mx-auto">
        <p class="pt-7 pb-3 pl-2 font-semibold">
          <span class="select-none">Index of </span>
          <span class="select-text">{{ $route.path }}</span>
          <router-link :to="parentPath">
            <span class="font-normal text-xl text-gray-500 select-none"> ⤴ </span>
          </router-link>
        </p>
      </div>
    </div>
    <div class="dark:bg-black">
      <div class="container mx-auto relative min-h-320px">
        <div class="lg:absolute pt-5 -top-5 right-8">
          <Card :qualifier="qualifier" :session="session"/>
        </div>
        <div class="pt-4">
          <div v-for="file in files" v-bind:key="file">
            <router-link v-if="isDirectory(file)" :to="append($route.path, file.name)">
              <Entry :file="file"/>
            </router-link>
            <a v-else :href="createURL($route.path + '/' + file.name)" target="_blank">
              <Entry :file="file"/>
            </a>
          </div>
          <div v-if="isEmpty">
            <p>Directory is empty</p>
          </div>
          <div v-if="isErrored">
            <p>Directory not found</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import 'mosha-vue-toastify/dist/style.css'
import useSession from '../../store/session'
import { createURL, createClient } from '../../store/client'
import Card from './Card.vue'
import Entry from './Entry.vue'

export default {
  components: { Card, Entry },
  props: {
    qualifier: {
      type: Object,
      required: true
    },
    session: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const qualifier = props.qualifier
    const session = props.session
    const parentPath = ref('')

    const drop = (path) => (path.endsWith('/') ? path.slice(0, -1) : path).split("/")
      .slice(0, -1)
      .join('/') || '/'

    const files = ref([])
    const isEmpty = ref(false)
    const isErrored = ref(undefined)
    const isDirectory = (file) => file.type == 'DIRECTORY'

    watch(
      () => qualifier.watchable,
      async (_) => {            
        const { name, secret } = session.tokenInfo
        const { client } = createClient(name, secret)

        client.maven.details(qualifier.path)
          .then(response => {
            files.value = response.data.files
            isEmpty.value = files.value.length == 0
            isErrored.value = undefined
          })
          .catch(error => {
            console.log(error)
            createToast(`${error.response.status}: ${error.response.data.message}`, {
              type: 'danger'
            })
            isErrored.value = error
          })

        parentPath.value = drop(`/${qualifier.path}`)
      },
      { immediate: true }
    )

    return {
      qualifier,
      session,
      parentPath,
      files,
      isEmpty,
      isErrored,
      isDirectory,
      createURL
    }
  }
}
</script>