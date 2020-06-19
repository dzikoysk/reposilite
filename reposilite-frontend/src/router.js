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
    base: process.env.BASE_URL,
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