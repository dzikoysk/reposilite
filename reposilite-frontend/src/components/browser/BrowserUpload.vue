<script setup>
import { ref, computed } from 'vue'
import { createSuccessToast, createErrorToast } from '../../helpers/toast'
import { useSession } from '../../store/session'
import useQualifier from '../../helpers/qualifier'
import FileUpload from 'vue-upload-component'
import CloseIcon from '../icons/CloseIcon.vue'

const { client } = useSession()

const { qualifier, refreshQualifier } = useQualifier()
const repository = computed(() => qualifier.path.split("/")[0])
const to = ref(qualifier.path.substring(repository.value.length + 1))
const destination = computed(() => `${repository.value}/${to.value}`)

const files = ref([])
const isEnabled = computed(() => files.value.length > 0)

const removeFile = (file) =>
  files.value = files.value.filter(element => element !== file)

const uploadFiles = () => {
  files.value.forEach(vueFile => 
    client.value.maven.deploy(`${destination.value}/${vueFile.name}`, vueFile.file)
      .then(() => createSuccessToast(`File ${vueFile.name} has been uploaded`))
      .then(() => removeFile(vueFile))
      .then(() => refreshQualifier())
      .catch(error => createErrorToast(`Cannot upload file ${vueFile.name} - ${error.response.status}: ${error.response.data.message}`))
      .catch(error => createErrorToast(error))
  )
}
</script>

<template>
  <div id="browser-upload">
    <div 
      :class="[ isEnabled ? 'rounded' : 'rounded-3xl' ]"
      class="
        border border-dashed mt-1.5 cursor-pointer
        bg-gray-50 border-gray-300 hover:(transition-colors duration-200 bg-white)
        dark:bg-black dark:border-gray-800 dark:hover:(transition-colors duration-400 bg-gray-900)
      "
    >
      <FileUpload
        class="btn btn-primary flex text-left"
        post-action="/upload/post"
        :multiple="true"
        :drop="true"
        :drop-directory="true"
        v-model="files"
        ref="upload"
      >
        <div class="my-3 px-6">
          <div v-if="isEnabled" class="py-1">
            <p class="font-bold">
              Deploy selected files to
              <span class="text-gray-500">{{'/' + destination}}</span>
            </p>
          </div>
          <div v-else class="flex">
            <span class="text-xm pt-1.6">🟣</span>
            <span class="font-bold px-5">Select files</span>
          </div>
        </div>
      </FileUpload>
      <div class="-mt-2 pb-2">
        <div v-for="file in files" :key="file.name" class="pb-1 px-6 flex">
          <span @click="removeFile(file)" class="pt-0.85">
            <CloseIcon class="h-5 pb-1 text-purple-400" />
          </span>
          <span class="px-2">{{file.name}}</span>
        </div>
      </div>
    </div>
    <div v-if="isEnabled" class="flex">
      <input class="flex-1 mt-2 ml-1 mr-2 rounded px-6 border-dashed border" v-model="to" />
      <button 
        @click.prevent="uploadFiles"
        class="
          border text-sm py-1.5 h-9 px-4 mt-2 border-dashed rounded 
          bg-gray-50  border-gray-400 hover:(transition-colors duration-200 bg-purple-500 text-white)
          dark:bg-black dark:border-gray-700 dark:text-white dark:hover:(transition-colors duration-200 bg-purple-700)
        "
      >
        <span>Upload files </span>
        <span class="font-bold text-purple-400">↝</span>
      </button>
    </div>
  </div>
</template>

<style>
#browser-upload label {
  @apply cursor-pointer;
}
.file-uploads {
  display: block !important;
}
</style>