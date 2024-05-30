import "./assets/main.css";
import "prismjs";
import "prismjs/themes/prism.min.css";
import "./assets/prism-custom.css";

import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import eruda from "eruda";
import hljs from "highlight.js/lib/core";
import kotlin from "highlight.js/lib/languages/kotlin";
import hljsVuePlugin from "@highlightjs/vue-plugin";
import { load } from "./api/loader";
import { usePackages } from "./stores/data";
import { createPinia } from "pinia";

eruda.init();
hljs.registerLanguage("kotlin", kotlin);

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
app.use(hljsVuePlugin);
app.use(router);
app.mount("#app");

load().then((data) => usePackages().set(data));
