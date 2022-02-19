<!--
  - Copyright (c) 2022 dzikoysk
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
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import Toggle from '@vueform/toggle'
import '@vueform/toggle/themes/default.css'
import { useAdjustments } from '../../store/adjustments'

const { reversedFileOrder, displayHashFiles } = useAdjustments()
const showAdjustments = ref(false)
</script>

<script>
export default {
  inheritAttrs: false
}
</script>

<template>
  <div id="adjustments-modal">
    <vue-final-modal
      v-model="showAdjustments"
      v-bind="$attrs"
      classes="flex justify-center iems-center"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <div>
          <h1 class="font-bold pb-4">File browser adjustments</h1>
          <hr class>
          <div class="flex justify-between pt-6">
            <p class="pr-7">Sort files from newest to oldest</p>
            <Toggle 
              v-model="reversedFileOrder"
              class="ml-10"
            />
          </div>
          <div class="flex justify-between pt-6">
            <p class="pr-7">
              Display hash files such as 
              <span class="font-italic font-mono bg-gray-200 dark:bg-black px-2 py-0.5 m-2 rounded-lg">.md5/.sha1/.sha256/.sha512</span>
            </p>
            <Toggle 
              v-model="displayHashFiles"
              class="ml-10"
            />
          </div>
        </div>
        <button class="absolute top-0 right-0 mt-5 mr-9" @click="showAdjustments = false">🗙</button>
      </div>
    </vue-final-modal>
    <div @click="showAdjustments = true">
      <slot name="button"></slot>
    </div>
  </div>
</template>
