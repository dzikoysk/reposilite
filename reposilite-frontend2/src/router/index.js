import { createWebHistory, createRouter } from "vue-router"
import Index from "../pages/Index.vue"
import Dashboard from "../pages/Dashboard.vue"

const routes = [
  {
    path: "/",
    name: "Index",
    component: Index,
  },
  {
    path: "/dashboard",
    name: "Dashboard",
    component: Dashboard,
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router