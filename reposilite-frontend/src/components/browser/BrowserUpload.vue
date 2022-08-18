<script setup>
import { ref, computed, watchEffect } from 'vue'
import { createSuccessToast, createErrorToast } from '../../helpers/toast'
import { useSession } from '../../store/session'
import useQualifier from '../../helpers/qualifier'
import FileUpload from 'vue-upload-component'
import CloseIcon from '../icons/CloseIcon.vue'

const { client } = useSession()

const { qualifier, refreshQualifier } = useQualifier()
const repository = computed(() => qualifier.path.split("/")[0])
const defaultTo = qualifier.path.substring(repository.value.length + 1)
const to = ref(defaultTo)
const destination = computed(() => `${repository.value}/${to.value.replace(/(^\/+)|(\/+$)/g, '')}`)
const customDestination = ref(false)

const stubPomEnabled = ref(false)
const artifactId = ref('')
const groupId = ref('')
const version = ref('')

const stubPomGeneratedPath = computed(() => {
  let path = ''

  if (groupId.value) {
    path = groupId.value.replaceAll('.', '/')
    
    if (!path.endsWith('/')) {
      path = path + '/'
    }
  }
  
  if (artifactId.value) {
    path = path + artifactId.value + '/'
  }

  if (version.value) {
    path = path + version.value
  }

  return path
})

watchEffect(() => {
  if (stubPomEnabled.value && !customDestination.value) {
    to.value = defaultTo
      ? defaultTo + '/' + version.value
      : stubPomGeneratedPath.value
    }
  }
)

const pathMatchesPom = computed(() => {
  if (!stubPomEnabled.value) {
    return true
  }

  const generatedPath = stubPomGeneratedPath.value.endsWith('/')
    ? stubPomGeneratedPath.value.slice(0, -1)
    : stubPomGeneratedPath.value

  const currentPath = to.value.endsWith('/')
    ? to.value.slice(0, -1)
    : to.value

  return generatedPath === currentPath
})

const files = ref([])
const isEnabled = computed(() => files.value.length > 0)

const removeFile = (file) =>
  files.value = files.value.filter(element => element !== file)

const uploadFiles = () => {
  if (stubPomEnabled.value && (artifactId.value == "" || groupId.value == "" || version.value == "")) {
    createErrorToast(`Cannot upload files, one of artifactId/groupId/version is empty`)
    return
  }

  files.value.forEach(vueFile => 
    client.value.maven.deploy(`${destination.value}/${vueFile.name}`, vueFile.file)
      .then(() => createSuccessToast(`File ${vueFile.name} has been uploaded`))
      .then(() => removeFile(vueFile))
      .then(() => refreshQualifier())
      .catch(error => createErrorToast(`Cannot upload file ${vueFile.name} - ${error.response.status}: ${error.response.data.message}`))
      .catch(error => createErrorToast(error))
  )

  if (stubPomEnabled.value) {
    client.value.maven.generatePom(destination.value, groupId.value, artifactId.value, version.value)
      .then(() => createSuccessToast(`Stub POM.xml file has been generated`))
      .catch(error => createErrorToast(`Cannot generate stub POM file - ${error.response.status}: ${error.response.data.message}`))
      .catch(error => createErrorToast(error))
  }
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
      <div v-if="isEnabled">
        <div class="-mt-2 pb-2">
          <div v-for="file in files" :key="file.name" class="pt-1 px-6 flex">
            <span @click="removeFile(file)" class="pt-0.85">
              <CloseIcon class="h-5 pb-1 text-purple-400" />
            </span>
            <span class="px-2">{{file.name}}</span>
          </div>
        </div>
        <div class="px-6 pb-4">
          <div>
            <input type="checkbox" v-model="stubPomEnabled" class="mb-1 ml-1" />
            <span class="pl-3" @click="stubPomEnabled = !stubPomEnabled" >Generate stub POM file</span>
          </div>
          <div v-if="stubPomEnabled" class="pom-form mt-2 border px-2 pb-2 bg-gray-100 dark:bg-black rounded">
            <div>
              <label>Group</label>
              <input v-model="groupId" placeholder="com.dzikoysk" required/>
            </div>
            <div>
              <label>Artifact</label>
              <input v-model="artifactId" placeholder="reposilite" required/>
            </div>
            <div>
              <label>Version</label>
              <input v-model="version" placeholder="3.0.0" required/>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="isEnabled" class="flex flex-col">
      <div class="flex">
        <input
          class="flex-1 mt-2 mr-2 rounded px-6 border-dashed border"
          v-model="to"
          placeholder="E.g. path/to/deploy"
          @change="customDestination = true"
        />
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
      <span v-if="!pathMatchesPom" class="px-6 text-yellow-500">⚠ Warning: Path does not match artifact coordinates</span>
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
.pom-form div {
  @apply flex flex-row items-center mt-2;
}
.pom-form label {
  @apply w-1/6 p-1;
}
.pom-form input {
  @apply flex-1 py-1 px-2 rounded;
}
</style>