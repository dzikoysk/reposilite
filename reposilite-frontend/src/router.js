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

import Vue from 'vue'
import Router from 'vue-router'
import Index from './views/Index.vue'
import Dashboard from './views/Dashboard.vue'
import DashboardIndex from './views/dashboard/Index.vue'
import DashboardUpload from './views/dashboard/Upload.vue'
import DashboardCli from './views/dashboard/Cli.vue'
import DashboardSettings from './views/dashboard/Settings.vue'

Vue.use(Router)

export default new Router({
  mode: 'history',
  base: process.env.NODE_ENV === 'production' ? '{{REPOSILITE.VUE_BASE_PATH}}' : '/',
  routes: [
    {
      path: '/dashboard',
      component: Dashboard,

      children: [
        {
          path: 'upload',
          name: 'Dashboard Upload',
          component: DashboardUpload
        },
        {
          path: 'settings',
          name: 'Dashboard Settings',
          component: DashboardSettings
        },
        {
          path: 'cli',
          name: 'Dashboard Cli',
          component: DashboardCli
        },
        {
          path: '',
          name: 'Dashboard Index',
          component: DashboardIndex
        },
        {
          path: ':qualifier(.*)',
          name: 'Dashboard Qualified Index',
          component: DashboardIndex
        }
      ]
    },
    {
      path: '/:qualifier(.*)',
      name: 'Index',
      component: Index
    }
  ]
})
