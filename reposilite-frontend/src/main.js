import Vue from 'vue'
import App from './App.vue'
import Axios from 'axios'
import router from './router'
import './assets/tailwind.css'

import fontawesome from '@fortawesome/fontawesome'
fontawesome.config = { autoReplaceSvg: false }

Vue.config.productionTip = false
Vue.prototype.$http = Axios

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
