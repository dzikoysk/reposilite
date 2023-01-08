<!--
  - Copyright (c) 2023 dzikoysk
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

<script setup lang="jsx">
import { computed, ref, watch } from 'vue'
import { useAdjustments } from '../../store/adjustments'
import { useSession } from '../../store/session'
import useQualifier from '../../store/qualifier'
import AdjustmentsIcon from '../icons/AdjustmentsIcon.vue'
import AdjustmentsModal from './AdjustmentsModal.vue'
import Card from '../card/SnippetsCard.vue'
import Breadcrumb from './BreadcrumbNavigation.vue'
import FileList from './FileList.vue'
import BrowserUpload from './FileUpload.vue'
import ViewGrid from '../icons/ViewGrid.vue'
import ViewList from '../icons/ViewList.vue'
import { property } from '../../helpers/vue-extensions'
import { createErrorToast } from '../../helpers/toast'

const props = defineProps({
  qualifier: property(Object, true)
})

const parentPath = ref('')
const files = ref({})
const { client, hasPermissionTo } = useSession()
const { applyAdjustments } = useAdjustments()
const { getParentPath } = useQualifier()

const processedFiles = computed(() => ({
  ...files.value,
  list: applyAdjustments([...files.value.list] || [])
}))

const canUpload = computed(() => {
  return props.qualifier.path.length > 1 && hasPermissionTo(`/${props.qualifier.path}`, 'route:write')
})

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
        createErrorToast(`${error.response.status}: ${error.response.data.message}`)
        files.value = {
          list: [],
          error: true
        }
      })

    parentPath.value = getParentPath()
  },
  { immediate: true }
)

const fileBrowserCompactViewKey = 'file-browser-compact-view'
const fileBrowserCompactMode = ref(localStorage.getItem(fileBrowserCompactViewKey) == "true")
const toggleCompactMode = () => {
  fileBrowserCompactMode.value = !fileBrowserCompactMode.value
  localStorage.setItem(fileBrowserCompactViewKey, fileBrowserCompactMode.value)
}

const MenuButton = (_, context) => {
  return (
    <div class="w-9 mx-2">
      <div class="bg-white dark:bg-gray-900 pl-2 pt-1.3 pb-1 pr-2 cursor-pointer rounded-full default-button">
        {context.slots.default()}
      </div>
    </div>
  )
}
</script>

<template>
  <div class="bg-gray-100">
    <div class="dark:bg-black">
      <div class="container mx-auto relative min-h-320px mb-12">
        <div class="lg:absolute pt-13 -top-5 right-8">
          <Card :qualifier="qualifier" />
        </div>
        <div class="lg:max-w-2/5 xl:max-w-1/2">
          <div class="flex justify-between pt-7 px-2">
            <Breadcrumb :parentPath="parentPath" />
            <div class="flex">
              <MenuButton @click="toggleCompactMode()">
                <ViewGrid v-if="fileBrowserCompactMode" class="pr-0.9"/>
                <ViewList v-else class="pr-0.9"/>
              </MenuButton>
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
          </div>
          <FileList :qualifier="qualifier" :files="processedFiles" :compactMode="fileBrowserCompactMode"/>
          <BrowserUpload v-if="canUpload" :qualifier="qualifier" />
        </div>
      </div>
    </div>
  </div>
</template>