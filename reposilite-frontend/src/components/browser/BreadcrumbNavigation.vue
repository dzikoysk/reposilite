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
import { computed } from 'vue'

import { useRoute } from 'vue-router'
const route = useRoute()

const breadcrumbs = computed(() => {
  const crumbs = route.path.split('/')

  return crumbs.map((name, index) => ({
    link: crumbs.slice(0, index + 1).join('/') || '/',
    name: index === crumbs.length - 1 ? name : name + '/'
  }))
})

defineProps({
  parentPath: {
    type: String,
    required: true
  }
})
</script>

<template>
  <div class="">
    <p class="pb-3 font-semibold">
      <span class="select-none">
        <router-link to="/">
          Index of 
        </router-link>
      </span>
      <span class="select-text">
        <router-link v-for="crumb of breadcrumbs" :key="crumb.link" :to="crumb.link">
          <span class="hover:(transition-colors duration-200 text-purple-500)">{{ crumb.name }}</span>
        </router-link>
      </span>
      <router-link :to="parentPath">
        <span class="font-normal text-xl text-gray-500 select-none"> ⤴ </span>
      </router-link>
    </p>
  </div>
</template>