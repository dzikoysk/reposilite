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
    class="sponsors"
  >
    <button
      class="version-trigger"
      aria-haspopup="dialog"
      :aria-expanded="open"
      title="View sponsors"
      @click="open = !open"
    >
      <span class="version">
        {{ version }}
        <small v-if="upToDate">· up to date</small>
      </span>
      <span class="sponsor-hint">
        Sponsored by {{ sponsors.flat().length }} supporters
      </span>
    </button>
    <div
      v-if="open"
      class="sponsors-popover"
      role="dialog"
      aria-label="Sponsors"
    >
      <div class="sponsors-header">
        <div>
          <h1>Sponsors</h1>
          <p>This release was sponsored by:</p>
        </div>
        <button
          class="close"
          title="Close"
          @click="open = false"
        >
          <CloseIcon />
        </button>
      </div>
      <div class="sponsor-list">
        <p
          v-for="(group, index) in sponsors"
          :key="index"
        >
          {{ group.join(', ') }}
        </p>
      </div>
      <a
        :href="allSponsorsUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        View all sponsors
      </a>
    </div>
  </div>
</template>

<style scoped>
.sponsors { @apply relative inline-flex; }
.sponsors-popover {
  @apply absolute right-0 top-full z-50 mt-3 w-96 max-w-[calc(100vw-2rem)] rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 px-5 py-4 text-left shadow-xl;
}
.sponsors-header { @apply flex items-start justify-between gap-4; }
.sponsors-header h1 { @apply text-base font-semibold text-gray-900 dark:text-white; }
.sponsors-header p { @apply mt-1 text-sm text-gray-500 dark:text-gray-400; }
.sponsor-list { @apply my-4 space-y-2; }
.sponsor-list p { @apply border-l-2 border-gray-200 dark:border-gray-700 pl-3 text-sm font-medium leading-5 text-gray-700 dark:text-gray-200; }
.sponsors-popover > a { @apply text-sm font-medium text-blue-600 hover:text-blue-700 dark:text-blue-300 dark:hover:text-blue-200; }
.close { @apply shrink-0 text-gray-400 hover:text-gray-700 dark:hover:text-gray-100; }
.close :deep(svg) { @apply h-5 w-5; }
.version-trigger { @apply flex flex-col items-start bg-transparent border-0 p-0 text-left cursor-pointer; }
.version { @apply flex items-center gap-2 text-lg font-semibold; }
.version small { @apply text-xs font-normal text-gray-500 dark:text-gray-400; }
.sponsor-hint { @apply mt-1 text-xs font-normal text-blue-600 dark:text-blue-300 hover:text-blue-700 dark:hover:text-blue-200; }
</style>
