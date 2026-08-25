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

<script setup lang="jsx">
import { ref, computed, watchEffect } from 'vue'
import { createSuccessToast, createErrorToast } from '../../helpers/toast'
import { useSession } from '../../store/session'
import useQualifier from '../../store/qualifier'
import FileUpload from 'vue-upload-component'
import CloseIcon from '../icons/CloseIcon.vue'

const { client } = useSession()

const { qualifier, refreshQualifier } = useQualifier()
const repository = computed(() => qualifier.path.split("/")[0])
const defaultTo = qualifier.path.substring(repository.value.length + 1)
const to = ref(defaultTo)
const destination = computed(() => `${repository.value}/${to.value.replace(/(^\/+)|(\/+$)/g, '')}`)
const customDestination = ref(false)

const checksumsEnabled = ref(false)
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
    client.value.maven.deploy(`${destination.value}/${vueFile.name}`, vueFile.file, checksumsEnabled.value)
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
  <section id="browser-upload" aria-label="File upload">
    <div 
      :class="[ isEnabled ? 'rounded-t-3xl rounded-b' : 'rounded-3xl' ]"
      class="
        border border-dashed mt-1.5 cursor-pointer
        bg-gray-50 border-gray-300 hover:(transition-colors duration-200 bg-white)
        dark:bg-black dark:border-gray-800 dark:hover:(transition-colors duration-400 bg-gray-900)
      "
    >
      <label
        for="file-upload-input"
        class="sr-only"
      >Select files to upload</label>
      <FileUpload
        input-id="file-upload-input"
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
        <ul class="-mt-2 pb-2">
          <li v-for="file in files" :key="file.name" class="pt-1 px-6 flex">
            <button
              type="button"
              class="pt-0.85"
              :aria-label="`Remove ${file.name}`"
              :title="`Remove ${file.name}`"
              @click="removeFile(file)"
            >
              <CloseIcon class="w-5 h-5 pb-1 text-purple-400" aria-hidden="true" />
            </button>
            <span class="px-2">{{file.name}}</span>
          </li>
        </ul>
        <div class="px-6 pb-4">
          <label class="block">
            <input type="checkbox" v-model="checksumsEnabled" class="mb-1 ml-1 dark:bg-gray-900" />
            <span class="pl-3">Generate default checksums</span>
          </label>
          <label class="block">
            <input type="checkbox" v-model="stubPomEnabled" class="mb-1 ml-1 dark:bg-gray-900" />
            <span class="pl-3">Generate stub POM file</span>
          </label>
          <div v-if="stubPomEnabled" class="pom-form mt-4 border px-3 pb-3 pt-1 bg-gray-100 dark:bg-black dark:border-gray-800 rounded">
            <div>
              <label for="pom-group">Group</label>
              <input id="pom-group" v-model="groupId" placeholder="com.dzikoysk" required/>
            </div>
            <div>
              <label for="pom-artifact">Artifact</label>
              <input id="pom-artifact" v-model="artifactId" placeholder="reposilite" required/>
            </div>
            <div>
              <label for="pom-version">Version</label>
              <input id="pom-version" v-model="version" placeholder="3.0.0" required/>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="isEnabled" class="flex flex-col">
      <div class="flex">
        <label for="upload-destination" class="sr-only">Upload destination</label>
        <input
          id="upload-destination"
          class="flex-1 mt-2 mr-2 rounded px-6 border-dashed dark:bg-gray-900 dark:border-gray-800 border"
          v-model="to"
          placeholder="E.g. path/to/deploy"
          @change="customDestination = true"
        />
        <button
          type="button"
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
      <span v-if="!pathMatchesPom" class="px-6 text-yellow-500" role="alert">⚠ Warning: Path does not match artifact coordinates</span>
    </div>
  </section>
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
  @apply flex-1 py-1 px-2 rounded dark:bg-gray-900;
}
</style>
