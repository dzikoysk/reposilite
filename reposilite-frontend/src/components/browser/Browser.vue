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
          Index of {{ $route.path }}
          <router-link :to="parentPath">
            <span class="font-normal text-xl text-gray-500"> ⤴ </span>
          </router-link>
        </p>
      </div>
    </div>
    <div class="dark:bg-black">
      <div class="container mx-auto relative">
        <div class="lg:absolute pt-5 -top-5 right-8">
          <Card/>
        </div>
        <div class="pt-4">
          <router-link v-for="file in files" v-bind:key="file" :to="append($route.path, file.name)">
            <div class="flex flex-row mb-1.5 py-3 rounded-full bg-white dark:bg-gray-900 xl:max-w-1/2 cursor-pointer">
              <div v-if="file.type == 'DIRECTORY'" class="text-xm px-6 pt-1.75">⚫</div>
              <div v-else class="text-xm px-6 pt-1.75">⚪</div>
              <div class="font-semibold">{{file.name}}</div>
            </div>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useRoute } from 'vue-router'
import { ref, watch } from 'vue'
import useSession from '../../store/session'
import useClient from '../../store/client'
import Card from './Card.vue'

export default {
  components: { Card },
  setup() {
    const route = useRoute()
    const { session } = useSession()
    const { client } = useClient(session.alias, session.token)

    const parentPath = ref('xyz')
    const files = ref([])

    const drop = (path) => (path.endsWith('/') ? path.slice(0, -1) : path).split("/")
      .slice(0, -1)
      .join('/') || '/'

    watch(
      () => route.params.qualifier,
      async newGav => {
        parentPath.value = drop(`/${newGav}`)

        client.maven.details(newGav)
          .then(response => files.value = response.data.files)
          .catch(error => console.log(error))
      },
      { immediate: true }
    )

    return {
      parentPath,
      files
    }
  }
}
</script>