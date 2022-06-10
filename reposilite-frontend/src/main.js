/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createApp } from 'vue'
import { createHead } from '@vueuse/head'
import axios from 'axios'
import VueAxios from 'vue-axios'
import Tabs from 'vue3-tabs'
import App from './App.vue'
import router from './router'

import 'virtual:windi.css'
import 'mosha-vue-toastify/dist/style.css'

const app = createApp(App)

app.config.globalProperties.append = (path, pathToAppend) =>
  path + (path.endsWith('/') ? '' : '/') + pathToAppend

app.config.globalProperties.drop = (path) =>
  (path.endsWith('/') ? path.slice(0, -1) : path).split("/")
    .slice(0, -1)
    .join('/')

app
  .use(createHead())
  .use(VueAxios, axios)
  .use(Tabs)
  .use(router)
  .mount('#app')