import { createRouter, createWebHistory } from "vue-router";
import BaseView from "@/views/BaseView.vue";
import PackageView from "@/views/PackageView.vue";
import ItemView from "@/views/ItemView.vue";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: "/",
            name: "base",
            component: BaseView,
        },
        {
            path: "/package/:name",
            name: "package",
            component: PackageView,
        },
        {
            path: "/item/:package/:name",
            name: "item",
            component: ItemView,
        },
    ],
});

export default router;
