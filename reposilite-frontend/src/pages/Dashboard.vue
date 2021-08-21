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
  <div>
    <Header/>
    <div class="bg-gray-100 dark:bg-black">
      <div class="container mx-auto">
        <tabs v-model="selectedMenuTab">
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
        <tab-panels v-model="selectedMenuTab" :animate="true">
          <tab-panel :val="'Endpoints'">
            <div class="container mx-auto flex flex-row py-10 justify-between">
              <div>Dashboard</div>
            </div>
          </tab-panel>
        </tab-panels>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, toRefs } from 'vue'
import Header from '../components/header/Header.vue'

const menuTabs = [ 'Dashboard' ]

export default {
  components: { Header },
  setup() {
    const state = reactive({
      selectedMenuTab: menuTabs[0]
    })

    return {
      menuTabs,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.item {
  @apply px-1;
  @apply pb-1;
  @apply cursor-pointer;
  @apply text-gray-600 dark:text-gray-300;
  @apply bg-gray-100 dark:bg-black;
}

.selected {
  @apply border-b-2;
  @apply border-black dark:border-white;
  @apply text-black dark:text-white;
}
</style>