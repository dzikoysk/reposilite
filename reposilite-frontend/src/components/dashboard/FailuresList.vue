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
  <div>
    <template
      v-for="entry in failures"
      :key="failureKey(entry)"
    >
      <div
        class="frow group flex items-center gap-3 pr-4 py-3.5 border-b border-gray-200 dark:border-gray-800 cursor-pointer transition-colors last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-800 <sm:gap-2 <sm:pr-3"
        :class="openFailureKey === failureKey(entry) ? 'open bg-gray-100 dark:bg-gray-800 border-b-0' : ''"
        @click="toggle(entry)"
      >
        <div class="stripe w-1 self-stretch bg-red-500 flex-none opacity-80" />
        <svg
          class="caret text-gray-400 dark:text-gray-600 flex-none transition-transform ml-3 transform"
          :class="{ 'rotate-90': openFailureKey === failureKey(entry) }"
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
        ><path d="M9 6l6 6-6 6" /></svg>
        <div class="fmeta flex flex-none flex-wrap items-center gap-2 min-w-0">
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
        <div class="fmid min-w-0 flex-1">
          <div class="font-mono text-sm truncate <sm:text-xs">
            {{ entry.path }}
          </div>
          <div class="text-sm text-gray-500 dark:text-gray-400 truncate mt-0.5 <sm:text-xs">
            {{ entry.message }}
          </div>
        </div>
        <div
          class="fact flex-none flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100"
          :class="{ 'opacity-100': openFailureKey === failureKey(entry) }"
        >
          <a
            :href="reportUrl(entry)"
            target="_blank"
            class="inline-flex items-center gap-1.5 text-sm text-blue-600 dark:text-blue-300 whitespace-nowrap hover:text-blue-700 dark:hover:text-blue-200 <sm:text-xs"
            title="Report on GitHub"
            @click.stop
          >
            Report on GitHub
            <LinkIcon class="w-3.5 h-3.5" />
          </a>
        </div>
      </div>
      <div
        v-if="openFailureKey === failureKey(entry)"
        class="px-4 pb-4 pl-10 bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-800 last:border-b-0 <sm:px-3 <sm:pl-3"
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
            class="absolute top-2 right-2 z-10 flex items-center cursor-pointer select-none rounded-md p-1 bg-gray-100 dark:bg-gray-800 text-gray-400 hover:(text-gray-600 bg-gray-200) dark:hover:(text-gray-200 bg-gray-700) transition-colors duration-200"
            title="Copy trace"
            @click="copyTrace(entry)"
          >
            <span
              v-if="copiedFailureKey === failureKey(entry)"
              class="text-ssm font-normal text-green-500 mr-1.5"
            >Copied</span>
            <CopiedIcon
              v-if="copiedFailureKey === failureKey(entry)"
              class="w-5 h-5"
            />
            <CopyIcon
              v-else
              class="w-5 h-5"
            />
          </button>
          <pre class="m-0 font-mono text-sm leading-6 text-gray-600 dark:text-gray-300 bg-white dark:bg-gray-900 rounded-lg py-3.5 pl-4 pr-13 overflow-x-auto <sm:text-xs <sm:leading-5">{{ entry.trace }}</pre>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
@media (max-width: 640px) {
  .frow {
    display: grid;
    grid-template-columns: 4px 12px minmax(0, 1fr);
    align-items: start;
    column-gap: 0.5rem;
    row-gap: 0.25rem;
    padding-right: 0.75rem;
  }

  .stripe {
    grid-column: 1;
    grid-row: 1 / 4;
    width: 4px;
  }

  .caret {
    grid-column: 2;
    grid-row: 1 / 3;
    margin-left: 0;
    margin-top: 0.2rem;
  }

  .fmeta {
    grid-column: 3;
    grid-row: 1;
  }

  .fmid {
    grid-column: 3;
    grid-row: 2;
  }

  .fact {
    display: none;
    grid-column: 3;
    grid-row: 3;
    opacity: 1;
    margin-top: 0.35rem;
  }

  .frow.open .fact { display: flex; }
}
</style>
