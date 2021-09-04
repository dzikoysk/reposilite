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
  <div class="container mx-auto pt-10 pb-6 px-6">
    <div v-for="entry in configurations" :key="entry.type" class="px-7">
      <h1 class="text-lg font-bold">{{entry.type}}</h1>
      <pre class="my-4 py-4 px-6 rounded-lg shadow-md text-sm bg-gray-50 dark:bg-gray-900 justify-items-center">{{trim(entry.snippet)}}</pre>
    </div>
  </div>
</template>

<script>
export default {
  setup() {

  }
}
</script>
