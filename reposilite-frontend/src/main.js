import './assets/tailwind.css'

import Vue from 'vue'
import App from './App.vue'
import Axios from 'axios'
import router from './router'
import Notifications from 'vue-notification'
import Meta from 'vue-meta'
import mixins from './mixins'

import fontawesome from '@fortawesome/fontawesome'

fontawesome.config = { autoReplaceSvg: false }

Vue.config.productionTip = false
Vue.prototype.$http = Axios

Vue.use(Notifications)
Vue.use(Meta)
Vue.mixin(mixins)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
