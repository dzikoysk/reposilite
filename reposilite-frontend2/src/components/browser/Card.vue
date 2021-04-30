<template>
  <div class="bg-white dark:bg-black shadow-lg p-7 border rounded-lg border-gray-100 dark:border-gray-900">
    <div class="flex flex-row justify-between">
      <h1 class="font-medium pt-2">Artifact details</h1>
      <button class="bg-black dark:bg-white text-white dark:text-black px-6 py-1 rounded">Download</button>
    </div>
    <tabs v-model="selectedTab" class="pt-6">
      <tab
        class="buildtool py-1 px-2 cursor-pointer"
        v-for="(entry, i) in tabs"
        :key="`t${i}`"
        :val="entry.name"
        :label="entry.name"
        :indicator="true"
      />
    </tabs>
    <hr class="dark:border-gray-800">
    <tab-panels v-model="selectedTab" :animate="true">
      <tab-panel 
        v-for="(entry, i) in tabs"
        :key="`tp${i}`"
        :val="entry.name"
      >
        <div class="mt-6 p-4 mr-1 rounded-lg bg-gray-100 dark:bg-hex-090909">
          <pre class="text-sm max-w-22">{{entry.value.trim()}}</pre>
        </div>
      </tab-panel>
    </tab-panels>
  </div>
</template>

<script>
import { reactive, toRefs } from 'vue'

const tabs = [
  { 
    name: 'Maven', 
    value: `
<dependency>
    <groupId>{groupId}</groupId>
    <artifactId>{artifactId}</artifactId>
    <version>{version}</version>
</dependency>`
  },
  { name: 'Gradle Groovy', value: `implementation "{groupId}:{artifactId}:{version}"` }, 
  { name: 'Gradle Kotlin', value: `implementation("{groupId:{artifactId}:{version}")` },
  { name: 'Panda', value: `maven:{groupId}/{artifactId}@{version}` },
  { name: 'SBT', value: `libraryDependencies  += "{groupId}" %% "{artifactId}" %% "{version}"` }
]

export default {
  setup() {
    const state = reactive({
      selectedTab: tabs[0].name
    })

    return {
      tabs,
      ...toRefs(state)
    }
  }
}
</script>