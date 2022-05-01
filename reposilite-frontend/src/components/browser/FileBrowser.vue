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
import { computed, ref, watch } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import { useAdjustments } from '../../store/adjustments'
import { useSession } from '../../store/session'
import AdjustmentsIcon from '../icons/AdjustmentsIcon.vue'
import AdjustmentsModal from './AdjustmentsModal.vue'
import Card from '../card/SnippetsCard.vue'
import Breadcrumb from './BreadcrumbNavigation.vue'
import BrowserList from './BrowserList.vue'
import BrowserUpload from './BrowserUpload.vue'

const props = defineProps({
  qualifier: {
    type: Object,
    required: true
  }
})

const parentPath = ref('')
const files = ref({})
const { client, isManager } = useSession()
const { applyAdjustments } = useAdjustments()

const processedFiles = computed(() => ({
  ...files.value,
  list: applyAdjustments([...files.value.list] || [])
}))

watch(
  () => props.qualifier.watchable,
  async () => {
    files.value = {
      list: []
    }

    const qualifier = props.qualifier.path

    client.value.maven.details(qualifier)
      .then(response => files.value = {
        list: response.data.files,
      })
      .then(() => files.value.isEmpty = files.value.list.length == 0)
      .catch(error => {
        console.log(error)
        createToast(`${error.response.status}: ${error.response.data.message}`, {
          type: 'danger'
        })
        files.value = {
          list: [],
          error: true
        }
      })
    
    const drop = (path) => 
      (path.endsWith('/') ? path.slice(0, -1) : path)
        .split("/")
        .slice(0, -1)
        .join('/') || '/'

    parentPath.value = drop(`/${qualifier}`)
  },
  { immediate: true }
)
</script>

<template>
  <div class="bg-gray-100">
    <div class="dark:bg-black">
      <div class="container mx-auto relative min-h-320px mb-1.5">
        <div class="lg:absolute pt-13 -top-5 right-8">
          <Card :qualifier="qualifier" />
        </div>
        <div class="lg:max-w-2/5 xl:max-w-1/2">
          <div class="flex justify-between pt-7 px-2">
            <Breadcrumb :parentPath="parentPath" />
            <AdjustmentsModal>
              <template v-slot:button>
                <div class="w-9">
                  <div class="bg-white dark:bg-gray-900 pl-2 pt-1.3 pb-1 pr-2 cursor-pointer rounded-full default-button">
                    <AdjustmentsIcon class="pr-0.9" />
                  </div>
                </div>
              </template>
            </AdjustmentsModal>
          </div>
          <BrowserList :files="processedFiles" />
          <BrowserUpload v-if="isManager"/>
        </div>
      </div>
    </div>
  </div>
</template>