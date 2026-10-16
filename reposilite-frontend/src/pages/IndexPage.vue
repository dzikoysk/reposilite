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
const MyTokensView = defineAsyncComponent(() => import('../components/tokens/MyTokensView.vue'))
const UsersView = defineAsyncComponent(() => import('../components/users/UsersView.vue'))
const SettingsView = defineAsyncComponent(() => import('../components/settings/SettingsView.vue'))

defineProps({
  qualifier: property(Object, true)
})

// name is the internal key (ids, localStorage), label is displayed
const listOfTabs = [
  { name: 'Overview', manager: false },
  { name: 'MyTokens', label: 'Tokens', user: true },
  { name: 'Dashboard', manager: true },
  { name: 'Statistics', manager: true },
  { name: 'Diagnostics', manager: true },
  { name: 'Users', manager: true },
  { name: 'Tokens', label: 'Access Tokens', manager: true },
  { name: 'Console', manager: true },
  { name: 'Settings', manager: true },
]

const { isManager, isLogged, token } = useSession()
// tokens created by users (user#label) can't create further tokens
const isUser = computed(() => isLogged.value && !token.value.name.includes('#'))
const { redirectTo } = useQualifier()

const menuTabs = computed(() => {
  return listOfTabs
    .filter(entry => !entry?.manager || isManager.value)
    .filter(entry => !entry?.user || isUser.value)
})

const selectedTab = ref(localStorage.getItem('selectedTab') || 'Overview')

watchEffect(() => {
  localStorage.setItem('selectedTab', selectedTab.value)
})

// the remembered tab may be unavailable (e.g. manager tab after logout), views must not load without access
const activeTab = computed(() =>
  menuTabs.value.some(tab => tab.name === selectedTab.value) ? selectedTab.value : 'Overview'
)

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
            :id="`main-tab-${tab.name.toLowerCase()}`"
            :key="`menu${i}`"
            type="button"
            role="tab"
            class="item main-menu-tab font-normal <sm:w-1/3"
            :class="{
              'main-menu-tab-active': activeTab === tab.name,
              'dashboard': tab.name === 'Dashboard'
            }"
            :aria-selected="activeTab === tab.name"
            :aria-controls="`main-panel-${tab.name.toLowerCase()}`"
            @click="selectedTab = tab.name; createTabClick(tab.name)"
          >
            <span class="tab block">{{ tab.label || tab.name }}</span>
          </button>
        </div>
      </div>
      <hr class="dark:border-gray-700">
      <div class="overflow-auto">
        <TabPanels :model-value="activeTab" @update:model-value="selectedTab = $event">
          <TabPanel
            id="main-panel-overview"
            role="tabpanel"
            aria-labelledby="main-tab-overview"
            :val="'Overview'"
          >
            <FileBrowserView v-if="activeTab == 'Overview'" :qualifier="qualifier" ref=""/>
          </TabPanel>
          <TabPanel
            v-show="isUser"
            id="main-panel-mytokens"
            role="tabpanel"
            aria-labelledby="main-tab-mytokens"
            :val="'MyTokens'"
          >
            <MyTokensView v-if="activeTab == 'MyTokens'" />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-users"
            role="tabpanel"
            aria-labelledby="main-tab-users"
            :val="'Users'"
          >
            <UsersView v-if="activeTab == 'Users'" />
          </TabPanel>
          <TabPanel
            v-show="isManager"
            id="main-panel-dashboard"
            role="tabpanel"
            aria-labelledby="main-tab-dashboard"
            :val="'Dashboard'"
          >
            <DashboardView
              v-if="activeTab == 'Dashboard'"
              :selected-tab="activeTab"
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
              v-if="activeTab == 'Statistics'"
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
              v-if="activeTab == 'Diagnostics'"
              :selected-tab="activeTab"
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
              v-if="activeTab == 'Console'"
              :selected-tab="activeTab"
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
              v-if="activeTab == 'Tokens'"
              :selected-tab="activeTab"
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
              v-if="activeTab == 'Settings'"
              :selected-tab="activeTab"
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
