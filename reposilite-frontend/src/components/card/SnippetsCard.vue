<!--
  - Copyright (c) 2023 dzikoysk
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
import { ref, watch, watchEffect } from 'vue'
import { useClipboard } from '@vueuse/core'
import { useSession } from '../../store/session'
import useRepository from '../../store/maven/repository'
import useMetadata from '../../store/maven/metadata'
import CopyIcon from '../icons/CopyIcon.vue'
import CopiedIcon from '../icons/CopiedIcon.vue'
import CardMenu from './CardMenu.vue'
import RepositorySnippet from "./RepositorySnippet.vue"
import ArtifactSnippet from "./ArtifactSnippet.vue"

const props = defineProps({
  qualifier: {
    type: Object,
    required: true
  }
})

const title = ref('')
const configurations = [
  { name: 'Maven', lang: 'xml' },
  { name: 'Gradle Kotlin', lang: 'kotlin' },
  { name: 'Gradle Groovy', lang: 'groovy' },
  { name: 'SBT', lang: 'scala' }
]
const data = ref({})
const loading = ref(false)
const { createRepositories } = useRepository()
const { parseMetadata } = useMetadata()
const { client } = useSession()
const { copy: copyText, isSupported: isCopySupported } = useClipboard()

const displayRepository = () => {
  title.value = 'Repository details'
  data.value = createRepositories(props.qualifier)
}

const displayArtifact = (metadataSource, version) => {
  title.value = 'Artifact details'
  const { groupId, artifactId, versions } = parseMetadata(metadataSource)
  const latestVersion = versions[version ? versions.indexOf(version) : versions.length - 1]
  data.value = { type: 'artifact', groupId, artifactId, version: latestVersion }
}

watchEffect(() => {
  // 1. Check if gave enough length to be an artifact
  //   1.1 check if gav is an artifact (fetch and process metadata file)
  // 2. If not display repository credentials if at least GAV has one element
  // 3. If GAV is empty display generic snippet to repository like domain.com/{repository}
  const qualifier = props.qualifier.path
  const elements = qualifier.split('/')

  const isStale = () => qualifier !== props.qualifier.path

  if (elements.length === 1 && elements[0] == '') {
    loading.value = false
    displayRepository()
    return
  }

  loading.value = true

  client.value.maven.content(`${qualifier}/maven-metadata.xml`)
    .then(response => {
      if (isStale()) return
      displayArtifact(response.data)
      loading.value = false
    })
    .catch(() => {
      client.value.maven.content(`${qualifier.substring(0, qualifier.indexOf(elements[elements.length-1])-1)}/maven-metadata.xml`)
        .then(response => {
          if (isStale()) return
          displayArtifact(response.data, elements[elements.length-1])
          loading.value = false
        })
        .catch(error => {
          if (isStale()) return
          if (error.response?.status !== 404 && error.response?.status !== 403) {
            console.log(error)
          }
          displayRepository()
          loading.value = false
        })
    })
})

const selectedTab = ref()
const transitionName = ref('slide-right')

watch(selectedTab, (to, from) => {
  const toIndex = configurations.findIndex(entry => entry.name === to)
  const fromIndex = configurations.findIndex(entry => entry.name === from)
  transitionName.value = toIndex - fromIndex < 0 ? 'slide-left' : 'slide-right'
})

const snippetRef = ref()
const copied = ref(false)

const copy = async () => {
  if (copied.value) return
  let snippet = snippetRef.value[0].content.trim()
  await copyText(snippet)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}

const selectTab = (tab) =>
  selectedTab.value = tab
</script>

<template>
  <div class="bg-white dark:bg-gray-900 shadow-lg p-7 rounded-xl border-gray-100 dark:border-black">
    <div class="flex flex-row justify-between">
      <h1 class="font-bold flex items-center w-full">
        <span v-if="loading" class="h-4 w-36 rounded bg-gray-200 dark:bg-gray-700 skeleton-bars" />
        <template v-else>{{title}}</template>
      </h1>
      <!-- <button class="bg-black dark:bg-white text-white dark:text-black px-6 py-1 rounded">Download</button> -->
    </div>

    <CardMenu
      :configurations="configurations"
      @selectTab="selectTab"
    />

    <hr class="dark:border-gray-800 <sm:(hidden)">

    <div class="mt-6">
      <transition :name="transitionName" mode="out-in">
        <div :key="selectedTab" class="relative">
          <span
            v-if="isCopySupported && !loading"
            @click="copy"
            class="absolute top-2 right-2 z-10 flex items-center cursor-pointer select-none rounded-md p-1 bg-gray-100 dark:bg-gray-800 text-gray-400 hover:(text-gray-600 bg-gray-200) dark:hover:(text-gray-200 bg-gray-700) transition-colors duration-200"
          >
            <span v-if="copied" class="text-ssm font-normal text-green-500 mr-1.5">Copied</span>
            <CopiedIcon v-if="copied" class="text-green-500" />
            <CopyIcon v-else />
          </span>
          <div class="card-editor overflow-auto font-mono text-ssm h-29 relative py-3 px-4 rounded-lg bg-gray-100 dark:bg-gray-800">
            <div v-if="loading" class="skeleton-bars space-y-2.5 pt-1">
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 80%" />
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 55%" />
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 68%" />
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 40%" />
            </div>
            <template v-else>
              <template v-for="entry in configurations" :key="entry.name">
                <template v-if="entry.name === selectedTab">
                  <RepositorySnippet
                      v-if="data.type === 'repository'"
                      ref="snippetRef"
                      :configuration="entry"
                      :data="data"
                  />
                  <ArtifactSnippet
                      v-else-if="data.type === 'artifact'"
                      ref="snippetRef"
                      :configuration="entry"
                      :data="data"
                  />
                </template>
              </template>
            </template>
          </div>
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

.card-editor > pre {
  position: absolute;
}
</style>
