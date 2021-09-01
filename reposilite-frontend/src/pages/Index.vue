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
            <Browser :qualifier="qualifier" ref=""/>
          </tab-panel>
          <tab-panel :val="'Usage'">
            <Usage/>
          </tab-panel>
          <tab-panel :val="'Endpoints'">
            <Endpoints/>
          </tab-panel>
          <tab-panel :val="'Console'" v-if="consoleEnabled">
            <Console :selectedTab="selectedTab" />
          </tab-panel>
        </tab-panels>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, ref, toRefs, watch } from 'vue'
import Header from '../components/header/Header.vue'
import Browser from '../components/browser/Browser.vue'
import Usage from '../components/Usage.vue'
import Endpoints from '../components/Endpoints.vue'
import Console from '../components/Console.vue'
import useSession from '../store/session'

export default {
  components: { Header, Browser, Usage, Endpoints, Console },
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
  setup(props) {
    const qualifier = props.qualifier
    const session = props.session
    const { isManager } = useSession()

    const menuTabs = ref([])
    const consoleEnabled = ref(false)
    const selectedTab = reactive({
      value: localStorage.getItem('selectedTab') || 'Overview'
    })

    watch(
      () => selectedTab.value,
      newTab => localStorage.setItem('selectedTab', newTab),
      { immediate: true }
    )

    watch(
      () => session.tokenInfo, 
      async newTokenInfo => {
        menuTabs.value = [ 
          { name: 'Overview' },
          { name: 'Usage' }, 
          { name: 'Endpoints' },
          { name: 'Console', manager: true }
        ]
        .filter(entry => !entry?.manager || isManager(newTokenInfo))
        .map(entry => entry.name)

        consoleEnabled.value = menuTabs.value.find(element => element === 'Console')
      },
      { immediate: true }
    )
      
    return {
      qualifier,
      isManager,
      menuTabs,
      consoleEnabled,
      selectedTab
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