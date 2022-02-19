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
import usePlaceholders from '../../store/placeholders'
import Hero from './Hero.vue'
import Menu from './Menu.vue'

const { title } = usePlaceholders()
</script>

<template>
  <header class="bg-gray-100 dark:bg-black dark:text-white">
    <div class="container mx-auto flex flex-row py-10 justify-between <sm:(pb-2 pt-5 px-5 justify-start)">
      <h1 class="text-xl font-medium py-1 <sm:w-1/2">
        <router-link :to="'/'">{{ title }}</router-link>
      </h1>
      <Menu class="mt-0.5"/>
    </div>
    <Hero class="pt-2 pb-10 <sm:pb-4" />
  </header>
</template>