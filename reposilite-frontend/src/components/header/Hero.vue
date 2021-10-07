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
  <div class="bg-gray-100 dark:bg-black">
    <div class="container mx-auto flex flex-row <sm:(flex-col items-center)">
      <div class="w-35 <sm:mb-2">
        <img class="border-2 rounded-full dark:border-gray-700" :src="organizationLogo">
      </div>
      <div class="flex flex-col justify-center px-10 <sm:px-0">
        <div class="<sm:text-center">
          {{description}}
        </div>
        <div class="flex flex-row py-2 <sm:justify-center">
          <GlobeIcon />
          <a class="px-3 text-gray-500" :href="organizationWebsite">{{organizationWebsite}}</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import GlobeIcon from '../icons/GlobeIcon.vue'
import usePlaceholders from '../../store/placeholders'

export default {
  components: { GlobeIcon },
  setup() {
    const { description, organizationWebsite, organizationLogo } = usePlaceholders()

    return {
      description,
      organizationWebsite,
      organizationLogo
    }
  }
}
</script>