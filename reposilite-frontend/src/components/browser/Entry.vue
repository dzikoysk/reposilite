<template>
  <div class="flex flex-row justify-between mb-1.5 py-3 rounded-full bg-white dark:bg-gray-900 lg:max-w-2/5 xl:max-w-1/2 cursor-pointer">
    <div class="flex flex-row">
      <div v-if="file.type == 'DIRECTORY'" class="text-xm px-6 pt-1.75">⚫</div>
      <div v-else class="text-xm px-6 pt-1.75">⚪</div>
      <div class="font-semibold">{{file.name}}</div>
    </div>
    <div v-if="file.contentLength" class="px-6">
      {{prettyBytes(file.contentLength)}}
    </div>
  </div>
</template>

<script>
import prettyBytes from 'pretty-bytes'

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
      file,
      prettyBytes
    }
  }
}
</script>