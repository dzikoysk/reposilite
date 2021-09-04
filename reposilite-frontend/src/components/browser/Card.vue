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
  <div class="bg-white dark:bg-gray-900 shadow-lg p-7 dark:border-4 rounded-xl border-gray-100 dark:border-black">
    <div class="flex flex-row justify-between">
      <h1 class="font-bold">Artifact details</h1>
      <!-- <button class="bg-black dark:bg-white text-white dark:text-black px-6 py-1 rounded">Download</button> -->
    </div>
    <tabs v-model="selectedTab" class="pt-3">
      <tab
        class="buildtool py-1 px-2 cursor-pointer"
        v-for="(entry, i) in configurations"
        :key="`t${i}`"
        :val="entry.name"
        :label="entry.name"
        :indicator="true"
      />
    </tabs>
    <hr class="dark:border-gray-800">
    <tab-panels v-model="selectedTab" :animate="true">
      <tab-panel 
        v-for="(entry, i) in configurations"
        :key="`tp${i}`"
        :val="entry.name"
      >
        <div class="relative h-33 mt-6 p-4 mr-1 rounded-lg bg-gray-100 dark:bg-gray-800">
          <prism-editor 
            class="snippet absolute text-sm" 
            v-model="entry.value" 
            :highlight="highlighter" 
            readonly
            line-numbers
          />
        </div>
      </tab-panel>
    </tab-panels>
  </div>
</template>

<script>
import { reactive, toRefs } from 'vue'
import { PrismEditor } from 'vue-prism-editor'
import 'vue-prism-editor/dist/prismeditor.min.css' // import the styles somewhere
import prism from "prismjs"
import "prismjs/themes/prism-coy.css"
import useArtifacts from '../../store/maven/artifact'

export default {
  components: { PrismEditor },
  props: {
    qualifier: {
      type: Object,
      required: true
    },
    session: {
      type: Object,
      required: true
    }
  },
  setup() {
    const { configurations } = useArtifacts()

    const state = reactive({
      selectedTab: configurations[0].name
    })

    // TODO:
    // 1. Check if gave enough lngth to be an artifact
    //   1.1 check if gav is an artifact (fetch and process metadata file)
    // 2. If not display repository credentials if at least GAV has one element
    // 3. If GAV is empty display ??? (maybe generic snippet to repository like domain.com/{repository})

    const highlighter = (code) =>
      code.includes('<dependency>') 
        ? prism.highlight(code, prism.languages.xml) 
        : prism.highlight(code, prism.languages.js)

    return {
      configurations,
      highlighter,
      ...toRefs(state)
    }
  }
}
</script>

<style>
.snippet {
    font-family: 'Consolas', 'monospace';
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