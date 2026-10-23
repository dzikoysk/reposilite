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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Chart, LineController, LineElement, PointElement, LinearScale, Tooltip, Legend, Filler } from 'chart.js'
import download from 'downloadjs'
import useTheme from '../../store/theme'

Chart.register(LineController, LineElement, PointElement, LinearScale, Tooltip, Legend, Filler)

const props = defineProps({
  series: { type: Array, required: true },
  label: { type: String, required: true },
  area: { type: Boolean, default: false },
  dateOnly: { type: Boolean, default: false }
})

const { theme } = useTheme()
const canvas = ref()
const colors = ['#a855f7', '#14b8a6', '#f59e0b', '#3b82f6', '#f43f5e', '#84cc16']
const datasets = computed(() => props.series.map((series, index) => ({
  label: series.name,
  data: series.data.map(point => ({ x: new Date(point.x).getTime(), y: point.y })),
  borderColor: colors[index % colors.length],
  backgroundColor: `${colors[index % colors.length]}33`,
  borderWidth: 2,
  pointRadius: 2,
  pointHoverRadius: 5,
  fill: props.area
})))
const bounds = computed(() => {
  const times = datasets.value.flatMap(dataset => dataset.data.map(point => point.x))
  if (!times.length) return undefined
  const min = Math.min(...times)
  const max = Math.max(...times)
  const padding = min === max ? (props.dateOnly ? 86400000 : 60000) : 0
  return { min: min - padding, max: max + padding }
})
let chart
const range = ref()

function options() {
  const color = theme.isDark ? '#d1d5db' : '#374151'
  const gridColor = theme.isDark ? '#374151' : '#e5e7eb'
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    interaction: { mode: 'index', axis: 'x', intersect: false },
    plugins: {
      legend: { position: 'bottom', labels: { color, padding: 20 } },
      tooltip: {
        callbacks: {
          title: items => items.length ? new Date(items[0].parsed.x).toLocaleString() : ''
        }
      }
    },
    scales: {
      x: {
        type: 'linear',
        min: range.value?.min ?? bounds.value?.min,
        max: range.value?.max ?? bounds.value?.max,
        grid: { display: false },
        ticks: {
          color,
          maxTicksLimit: 6,
          callback: value => new Date(value).toLocaleString(undefined, {
            month: 'short', day: 'numeric',
            ...(!props.dateOnly && { hour: '2-digit', minute: '2-digit', second: '2-digit' })
          })
        }
      },
      y: { beginAtZero: true, ticks: { color, precision: 0 }, grid: { color: gridColor } }
    }
  }
}

function zoom(factor) {
  const { min, max } = chart.scales.x
  const middle = (min + max) / 2
  const half = (max - min) * factor / 2
  range.value = { min: middle - half, max: middle + half }
}

function pan(direction) {
  const { min, max } = chart.scales.x
  const offset = (max - min) * direction / 2
  range.value = { min: min + offset, max: max + offset }
}

function exportCsv() {
  const quote = value => `"${String(value).replaceAll('"', '""')}"`
  const rows = [['Series', 'Timestamp', 'Value'], ...datasets.value.flatMap(dataset =>
    dataset.data.map(point => [dataset.label, new Date(point.x).toISOString(), point.y]))]
  download(rows.map(row => row.map(quote).join(',')).join('\r\n'), `${props.label}.csv`, 'text/csv;charset=utf-8')
}

const controls = [
  { label: 'Zoom in', path: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0M6 10h8M10 6v8', action: () => zoom(0.5) },
  { label: 'Zoom out', path: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0M6 10h8', action: () => zoom(2) },
  { label: 'Pan earlier', path: 'M15 5l-7 7 7 7', action: () => pan(-1) },
  { label: 'Pan later', path: 'M9 5l7 7-7 7', action: () => pan(1) },
  { label: 'Reset zoom', path: 'M3 10a9 9 0 1 1 2 8M3 4v6h6', action: () => { range.value = undefined } },
  { label: 'Download CSV', path: 'M14 3H4v18h16V9zM14 3v6h6M4 13h16M4 17h16M10 13v8', action: exportCsv },
  { label: 'Download PNG', path: 'M3 3h18v18H3zM3 17l6-6 4 4 3-3 5 5M17 7h.01', action: () => download(chart.toBase64Image(), `${props.label}.png`, 'image/png') }
]

onMounted(() => {
  chart = new Chart(canvas.value, { type: 'line', data: { datasets: datasets.value }, options: options() })
})
watch([datasets, range, () => theme.isDark], () => {
  if (chart) {
    chart.data.datasets = datasets.value
    chart.options = options()
    chart.update()
  }
})
onUnmounted(() => chart?.destroy())
</script>

<template>
  <div>
    <div
      class="flex flex-wrap justify-end gap-1 mb-2"
      role="group"
      :aria-label="`${label} controls`"
    >
      <button
        v-for="control in controls"
        :key="control.label"
        type="button"
        class="p-2 rounded disabled:opacity-40 disabled:cursor-not-allowed text-gray-500 hover:text-gray-900 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-100 dark:hover:bg-gray-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-purple-500"
        :disabled="!bounds"
        :aria-label="control.label"
        :title="control.label"
        @click="control.action"
      >
        <svg
          class="w-4 h-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.75"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path :d="control.path" />
        </svg>
      </button>
    </div>
    <div class="relative h-80 w-full">
      <p
        v-if="!bounds"
        class="absolute inset-0 flex items-center justify-center text-sm text-gray-500 dark:text-gray-400"
        role="status"
      >
        No data available for this period.
      </p>
      <canvas
        v-show="bounds"
        ref="canvas"
        role="img"
        :aria-label="label"
      >{{ label }}</canvas>
    </div>
  </div>
</template>
