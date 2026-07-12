<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <header class="view-header">
    <div class="copy">
      <h1>{{ title }}</h1>
      <p
        v-if="description || $slots.note"
        class="summary"
      >
        <span v-if="description">{{ description }}</span>
        {{ description && $slots.note ? ' ' : '' }}
        <slot name="note" />
      </p>
    </div>
    <div
      v-if="$slots.actions"
      class="actions"
    >
      <slot name="actions" />
    </div>
  </header>
</template>

<!--suppress CssInvalidAtRule -->
<style scoped>
.view-header {
  @apply mb-5 flex items-start justify-between gap-4 text-gray-800 dark:text-gray-100 <md:flex-col;
}
.copy {
  @apply min-w-0;
}
.copy h1 {
  @apply text-lg font-semibold leading-7;
}
.summary {
  @apply mt-1 max-w-full truncate text-sm leading-5 text-gray-500 dark:text-gray-400 <md:whitespace-normal <md:overflow-visible;
}
.summary :deep(strong) {
  @apply text-gray-700 dark:text-gray-200;
}
.actions {
  @apply flex flex-wrap items-center justify-end gap-2 <md:w-full <md:justify-start;
}
</style>
