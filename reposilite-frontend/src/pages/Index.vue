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

<script setup>
import { computed, reactive, watchEffect } from 'vue'
import Header from '../components/header/Header.vue'
import Browser from '../components/browser/FileBrowser.vue'
import Configuration from '../components/configuration/Configuration.vue'
import Console from '../components/Console.vue'
import useSession from '../store/session'

const props = defineProps({
  qualifier: {
    type: Object,
    required: true
  },
  token: {
    type: Object,
    required: true
  },
  session: {
    type: Object,
    required: true
  }
})

const { hasManagerPermission } = useSession()
const isManager = computed(() => hasManagerPermission(props.session.details))

const listOfTabs = [ 
  { name: 'Overview' },
  { name: 'Console', manager: true },
  { name: 'Configuration', manager: true },
]

const selectedTab = reactive({
  value: localStorage.getItem('selectedTab') || 'Overview'
})

watchEffect(() => localStorage.setItem('selectedTab', selectedTab.value))

const menuTabs = computed(() =>
  listOfTabs
    .filter(entry => !entry?.manager || hasManagerPermission(props.session.details))
    .map(entry => entry.name)
    )
</script>

<template>
  <div>
    <Header :token="token" />
    <div class="bg-gray-100 dark:bg-black">
      <div class="container mx-auto <sm:px-0">
        <tabs v-model="selectedTab.value">
          <tab
            v-for="(tab, i) in menuTabs"
            class="item font-normal"
            :key="`menu${i}`"
            :val="tab"
            :label="tab"
            :indicator="true"
          />
        </tabs>
      </div>
      <hr class="dark:border-gray-700">
      <div class="overflow-auto">
        <tab-panels v-model="selectedTab.value" :animate="true">
          <tab-panel :val="'Overview'">
            <Browser :qualifier="qualifier" :token="token" ref=""/>
          </tab-panel>
          <tab-panel :val="'Console'" v-if="isManager">
            <Console :selectedTab="selectedTab" />
          </tab-panel>
           <tab-panel :val="'Configuration'" v-if="isManager">
            <Configuration :selectedTab="selectedTab" :token="token" />
          </tab-panel>
        </tab-panels>
      </div>
    </div>
  </div>
</template>

<style scoped>
.item {
  @apply px-1;
  @apply pb-1;
  @apply pt-1.5;
  @apply cursor-pointer;
  @apply text-gray-600 dark:text-gray-300;
  @apply bg-gray-100 dark:bg-black;
}
.selected {
  @apply border-b-2;
  @apply border-black dark:border-white;
  @apply text-black dark:text-white;
}
.tabs .item {
  border-top-left-radius: 10%;
  border-top-right-radius: 10%;
}
.tabs .item:hover {
  @apply bg-gray-150 dark:bg-gray-900;
  transition: background-color 0.5s;
}
</style>
