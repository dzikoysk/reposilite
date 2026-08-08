<script setup>
import { ref } from 'vue'
import { onClickOutside, onKeyStroke } from '@vueuse/core'
import CloseIcon from '../icons/CloseIcon.vue'

defineProps({
  version: {
    type: String,
    required: true
  },
  upToDate: {
    type: Boolean,
    required: true
  },
  sponsors: {
    type: Array,
    required: true
  }
})

const open = ref(false)
const container = ref(null)
const allSponsorsUrl = 'https://github.com/dzikoysk/reposilite#supporters'

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
    class="relative inline-flex"
  >
    <button
      class="flex flex-col items-start bg-transparent border-0 p-0 text-left cursor-pointer"
      aria-haspopup="dialog"
      :aria-expanded="open"
      title="View sponsors"
      @click="open = !open"
    >
      <span class="flex items-center gap-2 text-lg font-semibold">
        {{ version }}
        <small
          v-if="upToDate"
          class="text-xs font-normal text-gray-500 dark:text-gray-400"
        >· up to date</small>
      </span>
      <span class="mt-1 text-xs font-normal text-blue-600 dark:text-blue-300 hover:text-blue-700 dark:hover:text-blue-200">
        Sponsored by {{ sponsors.flat().length }} supporters
      </span>
    </button>
    <div
      v-if="open"
      class="absolute right-0 top-full z-50 mt-3 w-96 max-w-[calc(100vw-2rem)] rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 px-5 py-4 text-left shadow-xl"
      role="dialog"
      aria-label="Sponsors"
    >
      <div class="flex items-start justify-between gap-4">
        <div>
          <h1 class="text-base font-semibold text-gray-900 dark:text-white">
            Sponsors
          </h1>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            This release was sponsored by:
          </p>
        </div>
        <button
          class="shrink-0 text-gray-400 hover:text-gray-700 dark:hover:text-gray-100"
          title="Close"
          @click="open = false"
        >
          <CloseIcon class="h-5 w-5" />
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
