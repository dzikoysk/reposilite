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
    <div class="bg-white dark:bg-black">
      <div class="container mx-auto">
        <p class="py-4 font-semibold">Index of /</p>
      </div>
    </div>
    <hr class="border-gray-100 dark:border-gray-900">
    <div>
      <div class="container mx-auto relative">
        <div class="lg:absolute pt-5 -top-24.5 right-8">
          <Card/>
        </div>
        <div class="pt-2 min-h-90">
          <div v-for="file in files" v-bind:key="file" class="flex flex-row py-3">
            <div class="text-xm pt-1.6">⚫</div>
            <div class="px-6">{{file}}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Card from './Card.vue'

export default {
  components: {
    Card
  },
  data() {
    return {
      files: [ 'releases', 'snapshot' ],
    }
  }
}
</script>