import { createApp } from 'vue'
import { createHead } from '@vueuse/head'
import axios from 'axios'
import VueAxios from 'vue-axios'
import Tabs from 'vue3-tabs'
import App from './App.vue'
import router from './router'

import 'virtual:windi.css'

createApp(App)
  .use(createHead())
  .use(VueAxios, axios)
  .use(Tabs)
  .use(router)
  .mount('#app')