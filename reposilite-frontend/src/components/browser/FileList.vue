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

<script setup lang="jsx">
import download from 'downloadjs'
import { useRoute } from 'vue-router'
import { createToast } from 'mosha-vue-toastify'
import { createURL } from '../../store/client'
import { useSession } from '../../store/session'
import ListEntry from './DetailedListEntry.vue'
import DeleteEntryModal from './DeleteEntryModal.vue'
import { ref } from 'vue'
import { property } from '../../helpers/vue-extensions'

const props = defineProps({
  qualifier: property(Object, true),
  files: property(Object, true),
  compactMode: property(Boolean, true)
})

const route = useRoute()
const { client } = useSession()

const downloadHandler = (path, name) => {
  client.value.maven.download(path.substring(1) + '/' + name)
    .then(response => download(response.data, name, response.headers['content-type']))
    .catch(error => createToast(`Cannot download file - ${error.response.status}: ${error.response.data.message}`, {
      type: 'danger'
    }))
}

const deleteModalValue = ref()
const openDeleteModal = (file) => {
  deleteModalValue.value = {
    path: props.qualifier.path,
    file
  }
}
const closeDeleteModal = () =>
  (deleteModalValue.value = undefined)

const isDirectory = (file) =>
  file.type == 'DIRECTORY'

const LinkEntry = ({ file }, context) => {
  return (
    <a
      onClick={(event) => {
        event.preventDefault()
        downloadHandler(route.path, file.name, event)
      }}
      href={createURL(route.path + '/' + file.name)} 
      target="_blank"
    >
      {context.slots.default()}
    </a>
  )
}

const append = (path, pathToAppend) =>
  path + (path.endsWith('/') ? '' : '/') + pathToAppend

const RouterEntry = ({ file }, context) => {
  return (
    <router-link to={append(route.path, file.name)}>
      {context.slots.default()}
    </router-link>
  )
}
</script>

<template>
  <div id="browser-list" class="pt-3">
    <DeleteEntryModal
      :qualifier="qualifier"
      :value="deleteModalValue"
      :close="closeDeleteModal"
    />
    <div :class="{'compact-background': compactMode}">
      <div v-for="file in files.list" v-bind:key="file">
        <RouterEntry v-if="isDirectory(file)" :file="file">
          <ListEntry
            :file="file"
            :qualifier="qualifier"
            :openDeleteEntryModal="openDeleteModal"
            :compactMode="compactMode"
          />
        </RouterEntry>
        <LinkEntry v-else :file="file">
          <ListEntry 
            :file="file" 
            :qualifier="qualifier"
            :url="createURL(`${$route.path}/${file.name}`)"
            :openDeleteEntryModal="openDeleteModal"
            :compactMode="compactMode"
          />
        </LinkEntry>
      </div>
    </div>
    <div v-if="files.isEmpty" class="pl-2 pb-4">
      <p>Directory is empty</p>
    </div>
    <div v-if="files.error" class="pl-2">
      <p>Directory not found</p>
    </div>
  </div>
</template>

<style>
.compact-background {
  @apply relative w-full bg-white dark:bg-gray-800 py-3 px-1 rounded-xl;
}
</style>
