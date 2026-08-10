<script setup>
import { ref } from 'vue'
import CopiedIcon from '../icons/CopiedIcon.vue'
import CopyIcon from '../icons/CopyIcon.vue'
import LinkIcon from '../icons/LinkIcon.vue'

const props = defineProps({
  failures: {
    type: Array,
    required: true
  },
  version: {
    type: String,
    required: true
  }
})

const REPOSITORY_URL = 'https://github.com/dzikoysk/reposilite'
const MAX_LOG_LENGTH = 6000

const openFailureKey = ref(null)
const copiedFailureKey = ref(null)
const failureKey = (failure) => `${failure.path}:${failure.type}:${failure.trace}`
const toggle = (failure) => {
  const key = failureKey(failure)
  openFailureKey.value = openFailureKey.value === key ? null : key
}

const reportUrl = (entry) => {
  const occurrenceNote = entry.occurrences > 1
    ? `\nOccurrences since restart: ${entry.occurrences}`
    : ''
  const trace = entry.trace + occurrenceNote
  const logs = trace.length > MAX_LOG_LENGTH
    ? trace.slice(0, MAX_LOG_LENGTH) + '\n… (truncated)'
    : trace

  const params = new URLSearchParams({
    template: 'bug-report.yml',
    version: '3.x',
    'what-happened': `Failure automatically captured by the Reposilite dashboard (v${props.version}). Please describe what you were doing when it occurred.${occurrenceNote}`,
    logs
  })

  return `${REPOSITORY_URL}/issues/new?${params.toString()}`
}

const copyTrace = (failure) => {
  const key = failureKey(failure)
  return navigator.clipboard.writeText(failure.trace)
    .then(() => {
      copiedFailureKey.value = key
      setTimeout(() => {
        if (copiedFailureKey.value === key) copiedFailureKey.value = null
      }, 1200)
    })
}
</script>

<template>
  <div role="list">
    <template
      v-for="(entry, index) in failures"
      :key="failureKey(entry)"
    >
      <div
        class="group relative flex items-center gap-3 pr-4 py-3.5 border-b border-gray-200 dark:border-gray-800 cursor-pointer transition-colors last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-800 <sm:grid <sm:grid-cols-[4px_12px_minmax(0,1fr)] <sm:items-start <sm:gap-x-2 <sm:gap-y-1 <sm:pr-3"
        :class="openFailureKey === failureKey(entry) ? 'bg-gray-100 dark:bg-gray-800 border-b-0' : ''"
        role="listitem"
      >
        <button
          type="button"
          class="absolute inset-0 z-0 w-full"
          :aria-expanded="openFailureKey === failureKey(entry)"
          :aria-controls="`failure-details-${index}`"
          :aria-label="`${openFailureKey === failureKey(entry) ? 'Collapse' : 'Expand'} ${entry.type} failure at ${entry.path}`"
          @click="toggle(entry)"
        />
        <div class="w-1 self-stretch bg-red-500 flex-none opacity-80 <sm:col-start-1 <sm:row-start-1 <sm:row-end-4" />
        <svg
          class="text-gray-400 dark:text-gray-600 flex-none transition-transform ml-3 transform <sm:col-start-2 <sm:row-start-1 <sm:row-end-3 <sm:ml-0 <sm:mt-0.5"
          :class="{ 'rotate-90': openFailureKey === failureKey(entry) }"
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          aria-hidden="true"
        ><path d="M9 6l6 6-6 6" /></svg>
        <div class="flex flex-none flex-wrap items-center gap-2 min-w-0 <sm:col-start-3 <sm:row-start-1">
          <span class="font-mono text-sm font-semibold text-red-600 dark:text-red-400 <sm:text-xs">{{ entry.type }}</span>
          <span
            v-if="entry.occurrences > 1"
            class="inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300 whitespace-nowrap"
          ><b class="font-semibold text-gray-800 dark:text-gray-100 tabular-nums">{{ entry.occurrences }}</b> occurrences</span>
          <span
            v-if="entry.messages.length > 1"
            class="inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300 whitespace-nowrap"
          ><b class="font-semibold text-gray-800 dark:text-gray-100 tabular-nums">{{ entry.messages.length }}</b> messages</span>
        </div>
        <div class="min-w-0 flex-1 <sm:col-start-3 <sm:row-start-2">
          <div class="font-mono text-sm truncate <sm:text-xs">
            {{ entry.path }}
          </div>
          <div class="text-sm text-gray-500 dark:text-gray-400 truncate mt-0.5 <sm:text-xs">
            {{ entry.message }}
          </div>
        </div>
        <div
          class="flex-none flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 <sm:col-start-3 <sm:row-start-3 <sm:mt-1.5 <sm:opacity-100"
          :class="openFailureKey === failureKey(entry) ? 'opacity-100 <sm:flex' : '<sm:hidden'"
        >
          <a
            :href="reportUrl(entry)"
            target="_blank"
            rel="noopener noreferrer"
            class="relative z-10 inline-flex items-center gap-1.5 text-sm text-blue-600 dark:text-blue-300 whitespace-nowrap hover:text-blue-700 dark:hover:text-blue-200 <sm:text-xs"
            title="Report on GitHub"
          >
            Report on GitHub
            <LinkIcon
              class="w-3.5 h-3.5"
              aria-hidden="true"
            />
          </a>
        </div>
      </div>
      <div
        v-if="openFailureKey === failureKey(entry)"
        :id="`failure-details-${index}`"
        class="px-4 pb-4 pl-10 bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-800 last:border-b-0 <sm:px-3 <sm:pl-3"
        role="region"
        :aria-label="`${entry.type} failure details`"
      >
        <div
          v-if="entry.messages.length > 1"
          class="mb-3 space-y-1 rounded-lg bg-white dark:bg-gray-900 px-4 py-3 font-mono text-xs text-gray-600 dark:text-gray-300"
        >
          <div
            v-for="message in entry.messages"
            :key="message"
          >
            {{ message }}
          </div>
        </div>
        <div class="relative">
          <button
            type="button"
            class="absolute top-2 right-2 z-10 flex items-center cursor-pointer select-none rounded-md p-1 bg-gray-100 dark:bg-gray-800 text-gray-400 hover:(text-gray-600 bg-gray-200) dark:hover:(text-gray-200 bg-gray-700) transition-colors duration-200"
            title="Copy trace"
            aria-label="Copy failure trace"
            @click="copyTrace(entry)"
          >
            <span
              v-if="copiedFailureKey === failureKey(entry)"
              class="text-ssm font-normal text-green-500 mr-1.5"
              role="status"
            >Copied</span>
            <CopiedIcon
              v-if="copiedFailureKey === failureKey(entry)"
              class="w-5 h-5"
              aria-hidden="true"
            />
            <CopyIcon
              v-else
              class="w-5 h-5"
              aria-hidden="true"
            />
          </button>
          <pre class="m-0 font-mono text-sm leading-6 text-gray-600 dark:text-gray-300 bg-white dark:bg-gray-900 rounded-lg py-3.5 pl-4 pr-13 overflow-x-auto <sm:text-xs <sm:leading-5">{{ entry.trace }}</pre>
        </div>
      </div>
    </template>
  </div>
</template>
