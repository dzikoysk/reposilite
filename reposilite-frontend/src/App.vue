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

<script setup>
import { useHead } from '@unhead/vue'
import { useSession } from "./store/session"
import useTheme from "./store/theme"
import useQualifier from "./store/qualifier"
import usePlaceholders from './store/placeholders'
import HeartIcon from './components/icons/HeartIcon.vue'

const { title, description, icpLicense, privacyPolicy } = usePlaceholders()
const { theme, fetchColorMode } = useTheme()
const { initializeSession } = useSession()
const { qualifier } = useQualifier()

useHead({
  title, 
  description
})
fetchColorMode()
initializeSession().catch(() => {})
</script>

<template>
  <div :class="{ 'dark': theme.isDark }">
    <div class="min-h-screen flex flex-col dark:bg-black dark:text-white">
      <router-view
        class="flex-1"
        :qualifier="qualifier"
      />
      <footer class="page-footer">
        <div class="container mx-auto flex items-center justify-center gap-2 <sm:flex-wrap">
          <span class="footer-attribution">
            <a
              href="https://reposilite.com"
              target="_blank"
              rel="noopener noreferrer"
            >Reposilite</a>
            <HeartIcon
              class="footer-heart"
              aria-hidden="true"
            />
            <span class="sr-only">loves</span>
            <a
              href="https://github.com/dzikoysk/reposilite"
              target="_blank"
              rel="noopener noreferrer"
            >Open Source</a>
          </span>
          <template v-if="icpLicense">
            <span aria-hidden="true">·</span>
            <a
              href="https://beian.miit.gov.cn"
              target="_blank"
              rel="noopener noreferrer"
            >{{ icpLicense }}</a>
          </template>
          <template v-if="privacyPolicy">
            <span aria-hidden="true">·</span>
            <a
              :href="privacyPolicy"
              target="_blank"
              rel="noopener noreferrer"
            >Privacy Policy</a>
          </template>
        </div>
      </footer>
    </div>
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@300;400;500;600&display=swap');

html, body {
  @apply bg-gray-100 dark:bg-black;
}
#app {
  font-family: 'Open Sans', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
.page-footer {
  @apply w-full bg-gray-100 dark:bg-black py-3 text-center text-xs text-gray-500 dark:text-gray-400;
}
.page-footer a {
  @apply hover:text-gray-800 dark:hover:text-gray-100;
}
.footer-attribution {
  @apply inline-flex items-center gap-1.5;
}
.footer-heart {
  @apply h-2.5 w-2.5 text-black dark:text-white;
}
.container {
  @apply px-10 <sm:px-2;
}
.active {
  @apply dark:border-white;
}
.bg-default {
  @apply bg-gray-100 dark:border-gray-900;
}

/* skeleton placeholders: the card/background is instant, only these bars fade in so a fast response replaces them before they flicker */
.skeleton-bars {
  animation: skeleton-fade-in 0.8s ease-out 0.2s both;
}
@keyframes skeleton-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
