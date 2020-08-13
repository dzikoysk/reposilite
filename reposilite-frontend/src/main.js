/*
 * Copyright (c) 2020 Dzikoysk
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

import './assets/tailwind.css'

import Vue from 'vue'
import App from './App.vue'
import Axios from 'axios'
import router from './router'
import Notifications from 'vue-notification'
import Meta from 'vue-meta'
import uri from './mixins/uri'
import orDefault from './mixins/default'

import fontawesome from '@fortawesome/fontawesome'
fontawesome.config = { autoReplaceSvg: false }

Vue.config.productionTip = false
Vue.prototype.$http = Axios

// Map placeholders into Vue configuration object
Vue.prototype.$reposilite = {
  message: orDefault(window.REPOSILITE_MESSAGE, 'unknown message'),
  basePath: orDefault(window.REPOSILITE_BASE_PATH, '/'),
  vueBasePath: orDefault(window.REPOSILITE_VUE_BASE_PATH, ''),
  title: orDefault(window.REPOSILITE_TITLE, 'Reposilite'),
  description: orDefault(
    window.REPOSILITE_DESCRIPTION,
    'Reposilite description'
  ),
  accentColor: orDefault(window.REPOSILITE_ACCENT_COLOR, '#000000')
}

Vue.use(Notifications)
Vue.use(Meta)
Vue.mixin(uri)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
