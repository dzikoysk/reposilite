<!--
  - Copyright (c) 2022 dzikoysk
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
import download from 'downloadjs'
import { createToast } from 'mosha-vue-toastify'
import { createURL } from '../../helpers/client'
import { useSession } from '../../store/session'
import ListEntry from './ListEntry.vue'

defineProps({
  files: {
    type: Object,
    required: true
  }
})

const { client } = useSession()

const downloadHandler = (path, name) => {
  client.value.maven.download(path.substring(1) + '/' + name)
    .then(response => download(response.data, name, response.headers['content-type']))
    .catch(error => createToast(`Cannot download file - ${error.response.status}: ${error.response.data.message}`, {
      type: 'danger'
    }))
}
</script>

<template>
  <div id="browser-list" class="pt-3">
    <div v-for="file in files.list" v-bind:key="file">
      <router-link v-if="file.type === 'DIRECTORY'" :to="append($route.path, file.name)">
        <ListEntry :file="file"/>
      </router-link>
      <a v-else 
        @click.left.prevent="downloadHandler($route.path, file.name)" 
        :href="createURL($route.path + '/' + file.name)" 
        target="_blank"
      >
        <ListEntry 
          :file="file" 
          :url="createURL($route.path + '/' + file.name)"
        />
      </a>
    </div>
    <div v-if="files.isEmpty" class="pl-2">
      <p>Directory is empty</p>
    </div>
    <div v-if="files.error" class="pl-2">
      <p>Directory not found</p>
    </div>
  </div>
</template>
