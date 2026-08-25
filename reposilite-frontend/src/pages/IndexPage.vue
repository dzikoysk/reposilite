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
import { computed, ref, watchEffect, defineAsyncComponent } from 'vue'
import { useSession } from '../store/session'
import useQualifier from '../store/qualifier'
import DefaultHeader from '../components/header/DefaultHeader.vue'
import FileBrowserView from '../components/browser/FileBrowserView.vue'
import {TabPanels, TabPanel} from 'vue3-tabs'
import { property } from '../helpers/vue-extensions'

const ConsoleView = defineAsyncComponent(() => import('../components/console/ConsoleView.vue'))
const DashboardView = defineAsyncComponent(() => import('../components/dashboard/DashboardView.vue'))
const StatisticsView = defineAsyncComponent(() => import('../components/dashboard/StatisticsView.vue'))
const DiagnosticsView = defineAsyncComponent(() => import('../components/dashboard/DiagnosticsView.vue'))
const TokensView = defineAsyncComponent(() => import('../components/tokens/TokensView.vue'))
const SettingsView = defineAsyncComponent(() => import('../components/settings/SettingsView.vue'))

defineProps({
  qualifier: property(Object, true)
})

const listOfTabs = [
  { name: 'Overview', manager: false },
  { name: 'Dashboard', manager: true },
  { name: 'Statistics', manager: true },
  { name: 'Diagnostics', manager: true },
  { name: 'Tokens', manager: true },
  { name: 'Console', manager: true },
  { name: 'Settings', manager: true },
]

const { isManager } = useSession()
const { redirectTo } = useQualifier()

const menuTabs = computed(() => {
  return listOfTabs
    .filter(entry => !entry?.manager || isManager.value)
    .map(entry => entry.name)
})

const selectedTab = ref(localStorage.getItem('selectedTab') || 'Overview')

watchEffect(() => {
  localStorage.setItem('selectedTab', selectedTab.value)
})

const createTabClick = (newTab) => {
  if (newTab == 'Overview') {
    redirectTo('/')
  }
}

const selectHomepage = () => 
  selectedTab.value = 'Overview'
</script>

<template>
  <div>
    <DefaultHeader :logoClickCallback="selectHomepage" />
    <main
      id="main-content"
      class="bg-gray-100 dark:bg-black overflow-y-visible"
    >
      <div class="container mx-auto <sm:px-0">
        <div
          class="tabs"
          role="tablist"
          aria-label="Main views"
        >
          <button
            v-for="(tab, i) in menuTabs"
            :id="`main-tab-${tab.toLowerCase()}`"
            :key="`menu${i}`"
            type="button"
            role="tab"
            class="item main-menu-tab font-normal <sm:w-1/3"
            :class="{
              'main-menu-tab-active': selectedTab === tab,
              'dashboard': tab === 'Dashboard'
            }"
            :aria-selected="selectedTab === tab"
            :aria-controls="`main-panel-${tab.toLowerCase()}`"
            @click="selectedTab = tab; createTabClick(tab)"
          >
            <span class="tab block">{{ tab }}</span>
          </button>
        </div>
      </div>
      <hr class="dark:border-gray-700">
      <div class="overflow-auto">
        <TabPanels v-model="selectedTab">
          <TabPanel
            id="main-panel-overview"
            role="tabpanel"
            aria-labelledby="main-tab-overview"
            :val="'Overview'"
          >
            <FileBrowserView v-if="selectedTab == 'Overview'" :qualifier="qualifier" ref=""/>
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-dashboard"
            role="tabpanel"
            aria-labelledby="main-tab-dashboard"
            :val="'Dashboard'"
          >
            <DashboardView
              v-if="selectedTab == 'Dashboard'"
              :selected-tab="selectedTab"
              @goto="selectedTab = $event"
            />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-statistics"
            role="tabpanel"
            aria-labelledby="main-tab-statistics"
            :val="'Statistics'"
          >
            <StatisticsView
              v-if="selectedTab == 'Statistics'"
              @goto="selectedTab = $event"
            />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-diagnostics"
            role="tabpanel"
            aria-labelledby="main-tab-diagnostics"
            :val="'Diagnostics'"
          >
            <DiagnosticsView
              v-if="selectedTab == 'Diagnostics'"
              :selected-tab="selectedTab"
            />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-console"
            role="tabpanel"
            aria-labelledby="main-tab-console"
            :val="'Console'"
          >
            <ConsoleView
              v-if="selectedTab == 'Console'"
              :selected-tab="selectedTab"
            />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-tokens"
            role="tabpanel"
            aria-labelledby="main-tab-tokens"
            :val="'Tokens'"
          >
            <TokensView
              v-if="selectedTab == 'Tokens'"
              :selected-tab="selectedTab"
            />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-settings"
            role="tabpanel"
            aria-labelledby="main-tab-settings"
            :val="'Settings'"
          >
            <SettingsView
              v-if="selectedTab == 'Settings'"
              :selected-tab="selectedTab"
            />
          </TabPanel>
        </TabPanels>
      </div>
    </main>
  </div>
</template>

<style>
.tabs {
  display: flex;
  flex-wrap: wrap;
}
.tabs .tab {
  cursor: pointer;
  padding: 10px 20px;
  text-transform: capitalize;
}
.tabs .active {
  border-width: 0;
  border-bottom-width: 2px;
  border-style: solid;
  border-color: #000;
}
.tabs .item:hover {
  @apply bg-gray-150 dark:bg-gray-900;
  transition: background-color 0.5s;
}
  .dashboard {
    @media (max-width: 640px) {
      padding-left: 0px !important;
    }
}
.dashboard .tab {
  @media (max-width: 640px){
    padding-left: 15px !important;
  }
}
</style>

<style scoped>
.item {
  @apply px-1;
  @apply pb-1;
  @apply pt-1.5;
  @apply cursor-pointer;
  @apply text-gray-600 dark:text-gray-300;
  @apply bg-gray-100 dark:bg-black;
}
:deep(.main-menu-tab-active) {
  @apply border-b-2;
  @apply border-black dark:border-white;
  @apply text-black dark:text-white;
}
.tabs .item {
  border-top-left-radius: 10%;
  border-top-right-radius: 10%;
}
</style>
