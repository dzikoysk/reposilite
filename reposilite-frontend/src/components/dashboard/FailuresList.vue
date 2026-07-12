<script setup>
import { ref, computed } from 'vue'
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

const openIndex = ref(null)
const copiedIndex = ref(null)
const toggle = (index) => openIndex.value = openIndex.value === index ? null : index

const parseTrace = (trace) => {
  const lines = trace.split(/\r?\n/)
  const path = lines[0].replace(/^failure\s+/, '')
  const cause = (lines[1] || '').match(/^\s*by\s+([^:]+):\s*(.*)$/)
  return {
    path,
    type: cause ? cause[1].trim() : 'Exception',
    message: cause ? cause[2].trim() : (lines[1] || '').trim()
  }
}

const normalizeFailure = (failure) => {
  const trace = typeof failure === 'string' ? failure : failure?.trace || failure?.failure || ''
  const parsed = parseTrace(trace)

  if (typeof failure === 'string') {
    return {
      ...parsed,
      trace,
      occurrences: 1
    }
  }

  return {
    path: failure?.path || parsed.path,
    type: failure?.type || parsed.type,
    message: failure?.message ?? parsed.message,
    trace,
    occurrences: failure?.occurrences || 1
  }
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

const parsedFailures = computed(() =>
  props.failures.map((failure, index) => {
    const normalized = normalizeFailure(failure)

    return {
      ...normalized,
      key: `${index}:${normalized.trace}:${normalized.occurrences}`
    }
  }))

const copyTrace = (failure, index) =>
  navigator.clipboard.writeText(failure)
    .then(() => {
      copiedIndex.value = index
      setTimeout(() => {
        if (copiedIndex.value === index) copiedIndex.value = null
      }, 1200)
    })
</script>

<template>
  <div>
    <template v-for="(entry, index) in parsedFailures" :key="entry.key">
      <div
        class="frow"
        :class="{ open: openIndex === index }"
        @click="toggle(index)"
      >
        <div class="stripe"></div>
        <svg class="caret" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 6l6 6-6 6"/></svg>
        <div class="fmeta">
          <span class="ftype">{{ entry.type }}</span>
          <span v-if="entry.occurrences > 1" class="occurrences"><b class="num">{{ entry.occurrences }}</b> occurrences</span>
        </div>
        <div class="fmid">
          <div class="fpath">{{ entry.path }}</div>
          <div class="fmsg">{{ entry.message }}</div>
        </div>
        <div class="fact">
          <a :href="reportUrl(entry)" target="_blank" class="report-link" title="Report on GitHub" @click.stop>
            Report on GitHub
            <LinkIcon />
          </a>
        </div>
      </div>
      <div v-if="openIndex === index" class="row-detail open">
        <div class="trace">
          <button class="copy" title="Copy trace" @click="copyTrace(entry.trace, index)">
            <span v-if="copiedIndex === index">Copied</span>
            <CopiedIcon v-if="copiedIndex === index" />
            <CopyIcon v-else />
          </button>
          <pre>{{ entry.trace }}</pre>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.frow { @apply flex items-center gap-3 pr-4 py-3.5 border-b border-gray-200 dark:border-gray-800 cursor-pointer transition-colors <sm:gap-2 <sm:pr-3; }
.frow:hover { @apply bg-gray-50 dark:bg-gray-800; }
.frow.open { @apply bg-gray-100 dark:bg-gray-800 border-b-0; }
.frow:last-child { @apply border-b-0; }
.stripe { @apply w-1 self-stretch bg-red-500 flex-none opacity-80; }
.caret { @apply text-gray-400 dark:text-gray-600 flex-none transition-transform ml-3; }
.frow.open .caret { transform: rotate(90deg); }
.fmeta { @apply flex flex-none flex-wrap items-center gap-2 min-w-0; }
.ftype { @apply font-mono text-sm font-semibold text-red-600 dark:text-red-400 <sm:text-xs; }
.occurrences { @apply inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300 whitespace-nowrap; }
.occurrences b { @apply font-semibold text-gray-800 dark:text-gray-100; }
.fmid { @apply min-w-0 flex-1; }
.fpath { @apply font-mono text-sm truncate <sm:text-xs; }
.fmsg { @apply text-sm text-gray-500 dark:text-gray-400 truncate mt-0.5 <sm:text-xs; }
.fact { @apply flex-none flex items-center gap-1 opacity-0 transition-opacity; }
.frow:hover .fact, .frow.open .fact { @apply opacity-100; }
.report-link { @apply inline-flex items-center gap-1.5 text-sm text-blue-600 dark:text-blue-300 whitespace-nowrap hover:text-blue-700 dark:hover:text-blue-200; }
.report-link :deep(svg) { @apply w-3.5 h-3.5; }

.row-detail { @apply px-4 pb-4 pl-10 bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-800 <sm:px-3 <sm:pl-3; }
.row-detail:last-child { @apply border-b-0; }
.trace { @apply relative; }
.trace pre { @apply m-0 font-mono text-sm leading-6 text-gray-600 dark:text-gray-300 bg-white dark:bg-gray-900 rounded-lg py-3.5 pl-4 pr-13 overflow-x-auto <sm:text-xs <sm:leading-5; }
.copy { @apply absolute top-2 right-2 z-10 flex items-center cursor-pointer select-none rounded-md p-1 bg-gray-100 dark:bg-gray-800 text-gray-400 hover:(text-gray-600 bg-gray-200) dark:hover:(text-gray-200 bg-gray-700) transition-colors duration-200; }
.copy span { @apply text-ssm font-normal text-green-500 mr-1.5; }
.copy :deep(svg) { @apply w-5 h-5; }
.num { font-variant-numeric: tabular-nums; }

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

  .frow.open .fact {
    display: flex;
  }

  .report-link {
    font-size: 0.75rem;
  }
}
</style>
