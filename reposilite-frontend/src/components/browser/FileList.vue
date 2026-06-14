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
import { computed, ref } from 'vue'
import { property } from '../../helpers/vue-extensions'

const props = defineProps({
  qualifier: property(Object, true),
  files: property(Object, true),
  compactMode: property(Boolean, true),
  loading: property(Boolean, true)
})

// cycled across skeleton rows so the placeholder name bars vary in width
const skeletonWidths = ['11rem', '7rem', '14rem', '9rem', '12rem', '8rem']
const skeletonCount = computed(() => Math.min(props.files.list?.length || 5, 10))

// one index-keyed loop so rows patch in place (skeleton -> data) instead of remounting
const displayedEntries = computed(() =>
  props.loading
    ? Array.from({ length: skeletonCount.value }, () => ({ __skeleton: true }))
    : (props.files.list ?? [])
)

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
  file.type === 'DIRECTORY'

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
      <div v-for="(entry, index) in displayedEntries" :key="index">
        <div
          v-if="entry.__skeleton"
          :class="{ 'default-entry': !compactMode, 'compact-entry': compactMode }"
        >
          <div class="flex flex-row max-w-full items-center skeleton-bars">
            <div :class="{ 'default-icon': !compactMode, 'compact-icon': compactMode }">
              <div class="w-3 h-3 rounded-full bg-gray-200 dark:bg-gray-700" />
            </div>
            <div class="h-3.5 rounded bg-gray-200 dark:bg-gray-700" :style="{ width: skeletonWidths[index % skeletonWidths.length] }" />
          </div>
          <div class="flex flex-1 justify-end items-center skeleton-bars">
            <div class="pr-6">
              <div class="h-3 w-10 rounded bg-gray-200 dark:bg-gray-700" />
            </div>
          </div>
        </div>
        <template v-else>
          <RouterEntry v-if="isDirectory(entry)" :file="entry">
            <ListEntry
              :file="entry"
              :qualifier="qualifier"
              :openDeleteEntryModal="openDeleteModal"
              :compactMode="compactMode"
            />
          </RouterEntry>
          <LinkEntry v-else :file="entry">
            <ListEntry
              :file="entry"
              :qualifier="qualifier"
              :url="createURL(`${$route.path}/${entry.name}`)"
              :openDeleteEntryModal="openDeleteModal"
              :compactMode="compactMode"
            />
          </LinkEntry>
        </template>
      </div>
    </div>
    <div v-if="!loading && files.isEmpty" class="pl-2 pb-4">
      <p>Directory is empty</p>
    </div>
    <div v-if="!loading && files.error" class="pl-2">
      <p>Directory not found</p>
    </div>
  </div>
</template>

<style>
.compact-background {
  @apply relative w-full bg-white dark:bg-gray-800 py-3 px-1 rounded-xl;
}
</style>
