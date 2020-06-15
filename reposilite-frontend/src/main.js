import Vue from 'vue'
import App from './App.vue'
import Axios from 'axios'
import router from './router'
import './assets/tailwind.css'
import fontawesome from '@fortawesome/fontawesome'
import Meta from 'vue-meta'
import mixins from './mixins'

Vue.config.productionTip = false
Vue.prototype.$http = Axios

fontawesome.config = { autoReplaceSvg: false }

Vue.use(Meta)

Vue.mixin(mixins)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
