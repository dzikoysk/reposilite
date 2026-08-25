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
import { ref } from 'vue'
import Toggle from '@vueform/toggle'
import { VueFinalModal } from 'vue-final-modal'
import '@vueform/toggle/themes/default.css'
import { useAdjustments } from '../../store/adjustments'
import CloseIcon from '../icons/CloseIcon.vue'

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
    <VueFinalModal
      v-model="showAdjustments"
      v-bind="$attrs"
      class="flex justify-center iems-center"
      aria-labelledby="adjustments-dialog-title"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <div>
          <h2 id="adjustments-dialog-title" class="font-bold pb-4">File browser adjustments</h2>
          <hr class>
          <div class="flex justify-between pt-6">
            <p id="reverse-file-order-label" class="pr-7">Sort files from newest to oldest</p>
            <Toggle
              id="reverse-file-order"
              v-model="reversedFileOrder"
              class="ml-10"
              labelledby="reverse-file-order-label"
            />
          </div>
          <div class="flex justify-between pt-6">
            <p id="display-hash-files-label" class="pr-7">
              Display utility files such as 
              <span class="font-italic font-mono bg-gray-200 dark:bg-black px-2 py-0.5 m-2 rounded-lg">.asc/.md5/.sha1/.sha256/.sha512</span>
            </p>
            <Toggle
              id="display-hash-files"
              v-model="displayHashFiles"
              class="ml-10"
              labelledby="display-hash-files-label"
            />
          </div>
        </div>
        <button
          type="button"
          class="absolute top-0 right-0 mt-5 mr-9"
          aria-label="Close file browser adjustments"
          title="Close"
          @click="showAdjustments = false"
        >
          <CloseIcon class="w-6 h-6" aria-hidden="true" />
        </button>
      </div>
    </VueFinalModal>
    <button
      type="button"
      class="w-9 bg-white dark:bg-gray-900 pl-2 pt-1.3 pb-1 pr-2 cursor-pointer rounded-full default-button"
      aria-label="Open file browser adjustments"
      title="File browser adjustments"
      @click="showAdjustments = true"
    >
      <slot name="button"></slot>
    </button>
  </div>
</template>
