<script setup>
import { ref, watchEffect } from 'vue'
import DownIcon from '../../icons/DownIcon.vue'

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
    <div id="card-menu" class="flex mt-2 <sm:(hidden)">
      <div 
        v-for="configuration in configurations" 
        :key="configuration.name" 
        @click="selectConfiguration(configuration)"
        class="py-4 px-7 flex-grow text-center border-b-2 cursor-pointer border-transparent"
        :class="{ '!border-gray-800': configuration.name === selectedTab }"
      >
        {{ configuration.name }}
      </div>
    </div>
    <div class="hidden flex-col items-center mt-24px <sm:(flex)">
      <div
        class="w-full box-border py-5px p-2 rounded-lg border-1 border-true-gray-200 dark:border-dark-300"
        @click="dropdownOpen = !dropdownOpen"
      >
        {{ selectedTab }}
        <div class="w-20px h-25px float-right m-auto flex items-center">
          <DownIcon />
        </div>
      </div>
      <ul v-if="!dropdownOpen" class="rounded-lg w-full box-border p-2 bg-true-gray-100 dark:bg-dark-600">
        <li
            v-for="configuration in configurations"
            :key="configuration.name"
            @click="selectTab(configuration); dropdownOpen = !dropdownOpen"
            class="dropdown py-1"
            :class="{ 'hidden': configuration.name === selectedTab }">
          {{ configuration.name }}
        </li>
      </ul>
    </div>
  </div>
</template>