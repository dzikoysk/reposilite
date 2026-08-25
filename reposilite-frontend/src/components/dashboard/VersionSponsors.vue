<!--
  ~ Copyright (c) 2020-2026 dzikoysk
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<script setup>
import { ref } from 'vue'
import { onClickOutside, onKeyStroke } from '@vueuse/core'
import CloseIcon from '../icons/CloseIcon.vue'

defineProps({
  version: {
    type: String,
    required: true
  },
  latestVersion: {
    type: String,
    default: null
  },
  sponsors: {
    type: Array,
    required: true
  }
})

const open = ref(false)
const container = ref(null)
const allSponsorsUrl = 'https://github.com/dzikoysk/reposilite#supporters'
const releasesUrl = 'https://github.com/dzikoysk/reposilite/releases'

onClickOutside(container, () => {
  open.value = false
})
onKeyStroke('Escape', () => {
  open.value = false
})
</script>

<template>
  <div
    ref="container"
    class="relative inline-flex flex-col items-start"
  >
    <span class="flex items-center gap-2 text-lg font-semibold">
      {{ version }}
      <small
        v-if="latestVersion"
        class="inline-flex items-center gap-2 text-xs font-normal"
        :class="version === latestVersion
          ? 'text-green-700 dark:text-green-400'
          : 'text-orange-700 dark:text-orange-400'"
      >
        <span aria-hidden="true">·</span>
        <span v-if="version === latestVersion">up to date</span>
        <a
          v-else
          :href="releasesUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="text-current underline hover:no-underline"
        >update available: {{ latestVersion }}</a>
      </small>
      <small
        v-else
        class="inline-flex items-center gap-2 text-xs font-normal text-red-600 dark:text-red-400"
      >
        <span aria-hidden="true">·</span>
        <span>unavailable</span>
      </small>
    </span>
    <button
      id="version-sponsors-trigger"
      type="button"
      class="mt-1 bg-transparent border-0 p-0 text-left cursor-pointer text-xs font-normal text-blue-600 dark:text-blue-300 hover:text-blue-700 dark:hover:text-blue-200"
      aria-haspopup="dialog"
      :aria-expanded="open"
      aria-controls="version-sponsors-dialog"
      @click="open = !open"
    >
      Sponsored by {{ sponsors.flat().length }} supporters
    </button>
    <div
      v-if="open"
      id="version-sponsors-dialog"
      class="absolute right-0 top-full z-50 mt-3 w-96 max-w-[calc(100vw-2rem)] rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 px-5 py-4 text-left shadow-xl"
      role="dialog"
      aria-labelledby="version-sponsors-heading"
    >
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2
            id="version-sponsors-heading"
            class="text-base font-semibold text-gray-900 dark:text-white"
          >
            Sponsors
          </h2>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            This release was sponsored by:
          </p>
        </div>
        <button
          type="button"
          class="shrink-0 text-gray-400 hover:text-gray-700 dark:hover:text-gray-100"
          title="Close"
          aria-label="Close sponsors"
          @click="open = false"
        >
          <CloseIcon
            class="h-5 w-5"
            aria-hidden="true"
          />
        </button>
      </div>
      <div class="my-4 space-y-2">
        <p
          v-for="(group, index) in sponsors"
          :key="index"
          class="border-l-2 border-gray-200 dark:border-gray-700 pl-3 text-sm font-medium leading-5 text-gray-700 dark:text-gray-200"
        >
          {{ group.join(', ') }}
        </p>
      </div>
      <a
        :href="allSponsorsUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="text-sm font-medium text-blue-600 hover:text-blue-700 dark:text-blue-300 dark:hover:text-blue-200"
      >
        View all sponsors
      </a>
    </div>
  </div>
</template>
