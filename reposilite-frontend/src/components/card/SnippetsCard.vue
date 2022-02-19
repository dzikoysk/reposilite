<!--
  - Copyright (c) 2022 dzikoysk
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup>
import { ref, watchEffect, watch } from 'vue'
import { useClipboard } from '@vueuse/core'
import { createToast } from 'mosha-vue-toastify'
import { useSession } from '../../store/session'
import useArtifacts from '../../helpers/maven/artifact'
import useRepository from '../../helpers/maven/repository'
import useMetadata from '../../helpers/maven/metadata'
import CopyIcon from '../icons/CopyIcon.vue'
import CodeSnippet from './CodeSnippet.vue'
import CardMenu from './CardMenu.vue'

const props = defineProps({
  qualifier: {
    type: Object,
    required: true
  }
})

const title = ref('')
const configurations = ref([])
const { createRepositories } = useRepository()
const { createSnippets } = useArtifacts()
const { parseMetadata } = useMetadata()
const { client } = useSession()
const { copy: copyText, isSupported: isCopySupported } = useClipboard()

const displayRepository = () => {
  title.value = 'Repository details'
  configurations.value = createRepositories(props.qualifier)
}

const displayArtifact = (metadataSource, version) => {
  title.value = 'Artifact details'
  const { groupId, artifactId, versions } = parseMetadata(metadataSource)
  const latestVersion = versions[version ? versions.indexOf(version) : versions.length - 1]
  configurations.value = createSnippets(groupId, artifactId, latestVersion)
}

watchEffect(() => {
  // 1. Check if gave enough length to be an artifact
  //   1.1 check if gav is an artifact (fetch and process metadata file)
  // 2. If not display repository credentials if at least GAV has one element
  // 3. If GAV is empty display generic snippet to repository like domain.com/{repository}
  const qualifier = props.qualifier.path
  const elements = qualifier.split('/')

  if (elements.length == 1 && elements[0] == '') {
    displayRepository()
    return
  } 

  client.value.maven.content(`${qualifier}/maven-metadata.xml`)
    .then(response => displayArtifact(response.data))
    .catch(() => {
      client.value.maven.content(`${qualifier.substring(0, qualifier.indexOf(elements[elements.length-1])-1)}/maven-metadata.xml`)
        .then(response => displayArtifact(response.data, elements[elements.length-1]))
        .catch(error => {
          if (error.message !== 'Request failed with status code 404') {
            console.log(error)
          }
          displayRepository()
        })
    })
})

const selectedTab = ref()
const transitionName = ref('slide-right')

watch(selectedTab, (to, from) => {
  const toIndex = configurations.value.findIndex(entry => entry.name === to)
  const fromIndex = configurations.value.findIndex(entry => entry.name === from)
  transitionName.value = toIndex - fromIndex < 0 ? 'slide-left' : 'slide-right'
})

const copy = async () => {
  const { snippet } = configurations.value.find(entry => entry.name === selectedTab.value)
  await copyText(snippet)
  return createToast('Snippet copied', { type: 'info', timeout: '2000' })
}

const selectTab = (tab) =>
  selectedTab.value = tab
</script>

<template>
  <div class="bg-white dark:bg-gray-900 shadow-lg p-7 rounded-xl border-gray-100 dark:border-black">
    <div class="flex flex-row justify-between">
      <h1 class="font-bold flex items-center w-full">
        {{title}}
        <span v-if="isCopySupported" @click="copy" class="ml-auto cursor-pointer">
          <CopyIcon />
        </span>
      </h1>
      <!-- <button class="bg-black dark:bg-white text-white dark:text-black px-6 py-1 rounded">Download</button> -->
    </div>
    
    <CardMenu 
      :configurations="configurations" 
      @selectTab="selectTab" 
    />

    <hr class="dark:border-gray-800 <sm:(hidden)">
    
    <div class="overflow-hidden">
      <transition :name="transitionName" mode="out-in">
        <div :key="selectedTab" class="h-29 relative mt-6 py-3 pl-1 mr-1 rounded-lg bg-gray-100 dark:bg-gray-800">
          <template v-for="entry in configurations" :key="entry.name"> 
            <CodeSnippet 
              v-if="entry.name === selectedTab" 
              :configuration="entry"
            />
          </template>
        </div>
      </transition>
    </div>
  </div>
</template>

<style>
#card-menu div {
  border-top-left-radius: 10%;
  border-top-right-radius: 10%;
}
#card-menu div:hover {
  @apply bg-gray-100 dark:bg-gray-800;
  transition: background-color 0.5s;
}

.slide-right-enter-active,
.slide-right-leave-active,
.slide-left-enter-active,
.slide-left-leave-active {
  transition: opacity .1s ease, transform .1s ease;
}

.slide-right-leave-to,
.slide-left-enter-from {
  opacity: 0;
  transform: translateX(60px);
}

.slide-right-enter-from,
.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-60px);
}

::-webkit-scrollbar {
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background-color: rgba(155, 155, 155, 0.4);
  border-radius: 20px;
  border: transparent;
  margin-top: 10px;
}
.card-editor .card-editor.prism-editor__textarea {
  display: none;
}
.card-editor .prism-editor__line-numbers {
  user-select: none;
}
.card-editor .prism-editor-wrapper .prism-editor__editor {
  pointer-events: auto !important;
}
.card-editor .prism-editor-wrapper .prism-editor__container {
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-track-color: transparent;
  margin-right: 27px;
}
.card-editor .prism-editor-wrapper .prism-editor__editor, 
.card-editor .prism-editor-wrapper .prism-editor__textarea {
  white-space: pre !important;
  min-height: 100px;
}
.token.tag {
  color: mediumpurple;
}
.token.operator {
  background: none;
}
.token.function {
  @apply text-black dark:text-white;
}
.token.string {
    color: mediumpurple;
}
</style>
