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
import prettyBytes from 'pretty-bytes'
import {createURL} from "../../store/client"
import {useSession} from '../../store/session'
import EyeIcon from '../icons/EyeIcon.vue'
import JavaDocsIcon from "../icons/JavaDocsIcon.vue"
import TrashIcon from '../icons/TrashIcon.vue'
import {property} from '../../helpers/vue-extensions'
import { computed } from 'vue'

const props = defineProps({
  file: property(Object, true),
  qualifier: property(Object, true),
  url: property(String, false),
  openDeleteEntryModal: property(Function, true),
  compactMode: property(Boolean, true)
})

const { hasPermissionTo } = useSession()

const humanReadableMimeTypes = ['application/xml', 'text/plain', 'text/xml', 'text/markdown', 'application/json']
const isHumanReadable = humanReadableMimeTypes.some(type => props.file?.contentType == type)

const openUrl = (url) =>
    window.open(url, '_blank')

const isJavaDocsAvailable = () => props.file.name.endsWith('-javadoc.jar') && getJavaDocsUrl() != null
const getJavaDocsUrl = () => {
  const qualifier = props.qualifier.path
  const elements = qualifier.split('/')

  if (elements.length < 2 || elements[1] === '') {
      return null
  }

  return createURL(`/javadoc/${qualifier}`)
}

const defaultMode = computed(() => !props.compactMode)
</script>

<template>
  <div class="browser-entry" :class="{ 'default-entry': defaultMode, 'compact-entry': compactMode }">
    <div class="flex flex-row">
      <div v-if="file.type == 'DIRECTORY'" :class="{ 'default-icon': defaultMode, 'compact-icon': compactMode }">⚫</div>
      <div v-else :class="{ 'default-icon': defaultMode, 'compact-icon': compactMode }">⚪</div>
      <div :class="{ 'default-filename': defaultMode, 'compact-filename': compactMode }">{{file.name}}</div>
    </div>
    <div class="entry-details flex flex-1 justify-end">
      <div class="entry-menu hidden flex flex-row justify-end">
        <EyeIcon
          v-if="file.hasOwnProperty('contentLength') && isHumanReadable" 
          :title="`Click to view ${file.name} file content in a new tab`"
          id="view-button"
          class="px-1 mr-6 pt-0.4 rounded-full text-purple-300 hover:(transition-colors duration-200 bg-gray-100 dark:bg-gray-900)" 
          @click.left.prevent="openUrl(url)"
          v-on:click.stop
        />
        <JavaDocsIcon
          v-if="isJavaDocsAvailable()"
          :title="`Click to view ${file.name} javadocs in a new tab`"
          id="javadoc-button"
          class="px-1 mr-6 pt-0.4 rounded-full text-purple-300 hover:(transition-colors duration-200 bg-gray-100 dark:bg-gray-900)"
          @click.left.prevent="openUrl(getJavaDocsUrl())"
          v-on:click.stop
        />
        <TrashIcon
          v-if="qualifier.path.length > 1 && hasPermissionTo(`/${qualifier.path}`, 'route:write')"
          id="delete-button"
          class="px-1 mr-6 pt-0.4 rounded-full text-purple-300 hover:(transition-colors duration-200 bg-gray-100 dark:bg-gray-900)"
          @click.left.prevent="openDeleteEntryModal(file.name)"
          v-on:click.stop
        />
      </div>
      <div v-if="file.hasOwnProperty('contentLength')" class="pr-6">
        {{ prettyBytes(file.contentLength) }}
      </div>  
    </div>
  </div>
</template>

<style>
.browser-entry:hover > .entry-details .entry-menu {
  display: flex;
}

.default-entry {
  @apply flex flex-row justify-between mb-1.5 py-3 rounded-full default-button;
}
.compact-entry {
  @apply rounded-lg inline-block w-full flex;
  @apply hover:(transition-colors duration-200 bg-purple-400 text-white);
  @apply dark:text-white dark:hover:(transition-colors duration-200 bg-purple-600);
}

.default-icon {
  @apply text-xm px-6 pt-1.75;
}
.compact-icon {
  @apply text-xxs pl-4 pt-2;
}

.default-filename {
  @apply font-semibold;
}
.compact-filename {
  @apply pl-3 pr-2 w-full whitespace-nowrap;
}
</style>