<!--
  - Copyright (c) 2020 Dzikoysk
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

<template lang="pug">
  svg.w-full.h-264.lg_h-168(viewBox="0 0 1080 640" preserveAspectRatio="none")
    path(
      :fill="this.accentColor"
      fill-opacity="1"
      d=" \
        M 0,260 \
        C 640,330 650,0 1080,170 \
        L 1440 0 \
        L 0,0 \
        Z \
    ")
    path(
      fill="#212121"
      fill-opacity="1"
      d=" \
        M 0,230 \
        C 620,310 650,50 1080,150 \
        L 1440 0 \
        L 0,0 \
        Z \
    ")
</template>

<script>
export default {
  props: {
    accentColor: String
  }
}
</script>
