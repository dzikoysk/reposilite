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
import { VueFinalModal } from 'vue-final-modal'
import '@vueform/toggle/themes/default.css'
import CloseIcon from '../icons/CloseIcon.vue'
import { useRoute } from 'vue-router'
import { useSession } from '../../store/session'
import { createToast } from 'mosha-vue-toastify'
import { computed } from 'vue'
import useQualifier from '../../helpers/qualifier'

const props = defineProps({
  value: {
    type: Object,
    required: false
  },
  close: {
    type: Function,
    required: true
  }
})

const route = useRoute()
const qualifier = route.params.qualifier

const { client } = useSession()
const { refreshQualifier } = useQualifier()

const deleteEntry = ({ path, file }) => {
  client.value.maven.delete(path + '/' + file)
    .then(() => refreshQualifier())
    .catch(error => createToast(`Cannot delete file - ${error.response.status}: ${error.response.data.message}`, {
      type: 'danger'
    }))
}

const deleteAndClose = () => {
  deleteEntry(props.value)
  props.close()
}

const isOpen = computed(() => props.value != undefined)
console.log(qualifier)
</script>

<script>
export default {
  inheritAttrs: false
}
</script>

<template>
  <div id="adjustments-modal">
    <vue-final-modal
      v-if="isOpen"
      v-model="isOpen"
      v-bind="$attrs"
      classes="flex justify-center iems-center"
    >
      <div class="relative border bg-white dark:bg-gray-900 border-gray-100 dark:border-black m-w-20 py-5 px-10 rounded-2xl shadow-xl text-center">
        <div>
          <h1 class="font-bold px-16">
            Do you want to delete 
            <span class="text-red-700">
              {{ '/' + value.path + '/' + value.file }}
            </span>
          </h1>
          <div class="flex flex-row justify-evenly pt-4">
            <button @click="deleteAndClose()" class="px-12 py-1 rounded-full bg-red-500">Confirm</button>
            <button @click="close()" class="px-12 py-1 rounded-full bg-gray-200 dark:bg-gray-600">Cancel</button>
          </div>
        </div>
        <button class="absolute top-0 right-0 mt-5 mr-9" @click.left.prevent="close()" v-on:click.stop>
          <CloseIcon />
        </button>
      </div>
    </vue-final-modal>
  </div>
</template>
