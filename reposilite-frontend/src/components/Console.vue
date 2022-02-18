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
import { watch, onUnmounted, nextTick } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import 'mosha-vue-toastify/dist/style.css'
import useSession from '../store/session'
import useLog from '../helpers/console/log'
import useConsole from '../helpers/console/connection'

const props = defineProps({
  selectedTab: {
    type: Object,
    required: true
  }
})

const { levels, log, logMessage, filter, clearLog } = useLog()

const { 
  onOpen, onMessage, onClose, onError, 
  connect,
  close,
  command,
  execute 
} = useConsole()

onUnmounted(() => close())

const scrollToEnd = () => {
  const console = document.getElementById('console')
  console.scrollTop = console.scrollHeight
}

const setupConnection = () => {
  onOpen.value = () => clearLog()
  onMessage.value = message => {
    logMessage(message)
    nextTick(() => scrollToEnd())
  }
  onError.value = error => createToast(`${error || ''}`, { type: 'danger' })
  onClose.value = () => createToast('Connection with console has been lost', { type: 'danger' })
  
  createToast('Connecting to the remote console', { type: 'info', })
  const { token } = useSession()
  connect(token)

  nextTick(() => setTimeout(() => document.getElementById('consoleInput').focus(), 1000))
}

watch(
  () => props.selectedTab.value,
  selectedTab => selectedTab === 'Console' ? setupConnection() : close(),
  { immediate: true }
)
</script>

<template>
  <div class="container mx-auto pt-10 px-15 pb-10 text-xs">
    <div class="flex text-sm flex-col xl:flex-row w-full py-2 justify-between">
      <input placeholder="Filter" v-model="filter" class="w-full xl:w-1/2 mr-5 py-1 px-4 rounded-lg bg-white dark:bg-gray-900" />
      <div class="flex flex-row justify-around w-full xl:w-1/2 <md:flex-wrap">
        <div v-for="level in levels" :key="level.name" class="pt-1.9 xl:pt-0.8 font-sans whitespace-nowrap">
          <input
            type="checkbox" 
            :checked="level.enabled" 
            @change="level.enabled = !level.enabled"
          >
          <span class="pl-2 pr-4">{{ level.name }} ({{ level.count }})</span>
        </div>
      </div>
    </div>
    <div class="bg-white dark:bg-gray-900 rounded-lg">
      <div id="console" class="overflow-scroll h-144 px-4 whitespace-pre-wrap font-mono text-xs">
        <p v-for="entry in log" :key="entry.id" v-html="entry.message" class="whitespace-nowrap"/>
      </div>
      <hr class="dark:border-dark-300">
      <input
        id="consoleInput"
        placeholder="Type command or '?' to get help"
        class="w-full py-2 px-4 rounded-b-lg bg-white dark:bg-gray-900 dark:text-white"
        v-model="command"
        @keyup.enter="execute()"
      />
    </div>
  </div>
</template>