<template>
  <div class="flex flex-row mb-1.5 py-3 rounded-full bg-white dark:bg-gray-900 xl:max-w-1/2 cursor-pointer">
    <div v-if="file.type == 'DIRECTORY'" class="text-xm px-6 pt-1.75">⚫</div>
    <div v-else class="text-xm px-6 pt-1.75">⚪</div>
    <div class="font-semibold">{{file.name}}</div>
  </div>
</template>

<script>
export default {
  props: {
    file: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const file = props.file

    return {
      file
    }
  }
}
</script>