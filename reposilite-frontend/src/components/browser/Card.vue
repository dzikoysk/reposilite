<!--
  - Copyright (c) 2021 dzikoysk
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

<template>
  <div class="bg-white dark:bg-gray-900 shadow-lg p-7 rounded-xl border-gray-100 dark:border-black">
    <div class="flex flex-row justify-between">
      <h1 class="font-bold flex items-center w-full">
        {{title}}
        <span v-if="isCopySupported" @click="copy" class="ml-auto cursor-pointer"><copy-icon /></span>
      </h1>
      <!-- <button class="bg-black dark:bg-white text-white dark:text-black px-6 py-1 rounded">Download</button> -->
    </div>
    <div class="flex config-selector-tab">
      <div 
        v-for="entry in configurations" 
        :key="entry.name" 
        @click="selectedTab = entry.name"
        class="py-4 px-7 flex-grow text-center border-b-2 cursor-pointer border-transparent"
        :class="{ '!border-gray-800': entry.name === selectedTab }"
      >
        {{ entry.name }}
      </div>
    </div>
    <div class="config-dropdown">
      <div
          class="selected-config"
          @click="toggleConfigDropdown()">
        {{ selectedTab }}
        <div class="dropdown-icon">
          <down-icon/>
        </div>
      </div>
      <ul class="rounded-lg">
        <li
            v-for="entry in configurations"
            :key="entry.name"
            @click="selectedTab = entry.name; toggleConfigDropdown()"
            class="dropdown mb-1.5"
            :class="{ 'hidden': entry.name === selectedTab }">
          {{ entry.name }}
        </li>
      </ul>
    </div>

    <hr class="dark:border-gray-800">
    <div class="overflow-hidden">
      <transition :name="transitionName" mode="out-in">
        <div :key="selectedTab" class="relative h-33 mt-6 p-4 mr-1 rounded-lg bg-gray-100 dark:bg-gray-800">
          <template v-for="entry in configurations"> 
            <prism-editor 
              v-if="entry.name === selectedTab"
              class="snippet absolute text-sm" 
              v-model="entry.snippet" 
              :highlight="entry.highlighter" 
              readonly
              line-numbers
            />
          </template>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import { ref, watchEffect, watch } from 'vue'
import { PrismEditor } from 'vue-prism-editor'
import 'vue-prism-editor/dist/prismeditor.min.css' // import the styles somewhere
import prism from "prismjs"
import "prismjs/themes/prism-coy.css"
import useArtifacts from '../../store/maven/artifact'
import useRepository from '../../store/maven/repository'
import { createClient } from '../../store/client'
import useMetadata from '../../store/maven/metadata'
import { useClipboard } from '@vueuse/core'
import CopyIcon from '../icons/CopyIcon.vue'
import DownIcon from '../icons/DownIcon.vue'
import { createToast } from 'mosha-vue-toastify'

export default {
  components: { PrismEditor, CopyIcon, DownIcon },
  props: {
    qualifier: {
      type: Object,
      required: true
    },
    token: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const qualifier = props.qualifier
    const token = props.token
    const title = ref('')
    const configurations = ref([])
    const { createRepositories } = useRepository()
    const { createSnippets } = useArtifacts()
    const { parseMetadata, groupId, artifactId, versions } = useMetadata()
    const { client } = createClient(token.name, token.secret)
    const { copy: copyText, isSupported: isCopySupported } = useClipboard()

    let dropdownOpen = false
    const selectedTab = ref(localStorage.getItem('card-tab') || 'Maven')
    watchEffect(() => localStorage.setItem('card-tab', selectedTab.value))
    
    const displayRepository = () => {
      configurations.value = createRepositories(qualifier)
      title.value = 'Repository details'
    }

    const displayArtifact = (metadataSource) => {
      const metadata = parseMetadata(metadataSource)
      configurations.value = createSnippets(groupId(metadata), artifactId(metadata), versions(metadata)[0])
      title.value = 'Artifact details'
    }

    watchEffect(() => {
      // 1. Check if gave enough lngth to be an artifact
      //   1.1 check if gav is an artifact (fetch and process metadata file)
      // 2. If not display repository credentials if at least GAV has one element
      // 3. If GAV is empty display generic snippet to repository like domain.com/{repository}

      const elements = qualifier.path.split('/')

      if (elements.length == 1 && elements[0] == '') {
        displayRepository()
        return
      } 

      client.maven.content(`${qualifier.path}/maven-metadata.xml`)
        .then(response => displayArtifact(response.data))
        .catch(error => {
          if (error.message !== 'Request failed with status code 404') {
            console.log(error)
          }
          displayRepository()
        })
    })

    watchEffect(() => {
      configurations.value.forEach(configuration => {
        configuration.highlighter = (code) => {
          return prism.highlight(code, prism.languages[configuration.lang] ?? prism.languages.js)
        }
      })
    })

    const transitionName = ref('slide-right')
    watch(selectedTab, (to, from) => {
      const toIndex = configurations.value.findIndex(entry => entry.name === to)
      const fromIndex = configurations.value.findIndex(entry => entry.name === from)
      transitionName.value = toIndex - fromIndex < 0 ? 'slide-left' : 'slide-right'
    })
    
    const copy = async () => {
      const { snippet } = configurations.value.find(entry => entry.name === selectedTab.value)

      await copyText(snippet)
      return createToast('Copied snippet', { type: 'info' })
    }

    const toggleConfigDropdown = () => {
      document.querySelector(".config-dropdown ul").style.display = dropdownOpen ? 'none' : 'block';
      dropdownOpen = !dropdownOpen;
    }

    return {
      title,
      configurations,
      selectedTab,
      transitionName,
      copy,
      isCopySupported,
      toggleConfigDropdown
    }
  }
}
</script>

<style>
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

.snippet {
    font-family: 'Consolas', 'monospace';
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
.prism-editor__textarea {
  display: none;
}
.prism-editor-wrapper .prism-editor__editor {
  pointer-events: auto !important;
}
.prism-editor-wrapper .prism-editor__container {
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-track-color: transparent;
  margin-right: 27px;
}
.prism-editor-wrapper .prism-editor__editor, .prism-editor-wrapper .prism-editor__textarea {
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
.config-dropdown {
  display: none;
  flex-direction: column;
  align-items: center;
  margin: 24px 0;
}
.config-dropdown ul {
  display: none;
  width: 100%;
  box-sizing: border-box;
  padding: 10px;
  background-color: #e5e5e56e;
}
.selected-config {
  width: 100%;
  padding: 5px 10px;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
  box-sizing: border-box;
}
.dropdown-icon {
  width: 20px;
  height: 25px;
  float: right;
  margin: auto;
  display: flex;
  align-items: center;
}
@media screen and (max-width: 576px ) {
  .config-selector-tab {
    display: none !important;
  }
  .config-dropdown {
    display: flex;
  }
}

</style>
