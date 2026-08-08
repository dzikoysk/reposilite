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
  <header class="mb-5 flex items-start justify-between gap-4 text-gray-800 dark:text-gray-100 <md:flex-col">
    <div class="min-w-0">
      <h1 class="text-lg font-semibold leading-7">
        {{ title }}
      </h1>
      <p
        v-if="description || $slots.note"
        class="mt-1 max-w-full truncate text-sm leading-5 text-gray-500 dark:text-gray-400 <md:whitespace-normal <md:overflow-visible"
      >
        <span v-if="description">{{ description }}</span>
        {{ description && $slots.note ? ' ' : '' }}
        <slot name="note" />
      </p>
    </div>
    <div
      v-if="$slots.actions"
      class="flex flex-wrap self-center items-center justify-end gap-2 <md:w-full <md:self-start <md:justify-start"
    >
      <slot name="actions" />
    </div>
  </header>
</template>
