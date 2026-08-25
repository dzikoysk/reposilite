<!--
  - Copyright (c) 2020-2026 dzikoysk
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
import { ref, watchEffect } from 'vue'
import DownIcon from '../icons/DownIcon.vue'

defineProps({
  configurations: {
    type: Object,
    required: true
  }
})

const emit = defineEmits([
  'selectTab'
])

const selectedTab = ref(localStorage.getItem('card-tab') || 'Maven')
watchEffect(() => {
  localStorage.setItem('card-tab', selectedTab.value)
  emit('selectTab', selectedTab.value)
})

const selectConfiguration = (configuration) =>
  selectedTab.value = configuration.name

const dropdownOpen = ref(localStorage.getItem('dropdown-open') || false)
watchEffect(() => localStorage.setItem('dropdown-open', dropdownOpen.value))
</script>

<template>
  <div>
    <div id="card-menu" class="flex mt-2 <sm:(hidden)" role="tablist" aria-label="Snippet format">
      <button
        v-for="configuration in configurations" 
        :key="configuration.name" 
        type="button"
        role="tab"
        class="py-4 px-7 flex-grow text-center border-b-2 cursor-pointer border-transparent"
        :class="{ '!border-gray-800': configuration.name === selectedTab }"
        :aria-selected="configuration.name === selectedTab"
        @click="selectConfiguration(configuration)"
      >
        {{ configuration.name }}
      </button>
    </div>
    <div class="hidden flex-col items-center mt-24px <sm:(flex)">
      <button
        type="button"
        class="w-full box-border py-5px p-2 rounded-lg border-1 border-true-gray-200 dark:border-dark-300"
        aria-controls="card-menu-options"
        :aria-expanded="!dropdownOpen"
        @click="dropdownOpen = !dropdownOpen"
      >
        {{ selectedTab }}
        <span class="w-20px h-25px float-right m-auto flex items-center">
          <DownIcon aria-hidden="true" />
        </span>
      </button>
      <ul id="card-menu-options" v-if="!dropdownOpen" class="rounded-lg w-full box-border p-2 bg-true-gray-100 dark:bg-dark-600">
        <li
            v-for="configuration in configurations"
            :key="configuration.name"
            class="dropdown py-1"
            :class="{ 'hidden': configuration.name === selectedTab }">
          <button
            type="button"
            class="w-full text-left"
            @click="selectConfiguration(configuration); dropdownOpen = !dropdownOpen"
          >
            {{ configuration.name }}
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>
