<script setup lang="ts">
import { type ItemMeta, loadData } from "@/api/package";
import Sidebar from "@/components/Sidebar.vue";
import { usePackages } from "@/stores/data";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import Prism from "prismjs";

const route = useRoute();
const pkgs = usePackages();
const pkg = computed(() => pkgs.data[route.params.name as string]);
const meta = ref<Record<string, ItemMeta>>({});

onMounted(async () => {
    window.Prism = window.Prism || {};
    window.Prism.manual = true;
});

watch(() => pkg.value, async () => {
    meta.value = await loadData(pkg.value);

    // Delay so Vue has a chance to create the elements
    setTimeout(() => {
        Prism.highlightAll();
    }, 5);
});
</script>

<template>
    <main class="main">
        <Sidebar />

        <div class="content" v-if="pkg">
            <h3 class="title">
                Package
                <a class="pkg" :href="`/package/${pkg.name}`">{{ pkg.name }}</a>
            </h3>

            <div class="item" v-for="item in meta">
                <a class="name" :href="`/item/${pkg.name}/${item.item}`">{{ item.item }}</a>
                <div class="snippet" v-html="item.snippet" />
                <p class="desc" v-html="item.desc" />
            </div>
        </div>
    </main>
</template>

<style scoped lang="scss">
.main {
    width: 100%;
    height: 100%;

    display: flex;
    flex-direction: row;
    align-items: flex-start;
    justify-content: flex-start;

    .content {
        width: 75%;
        height: 100%;

        display: flex;
        flex-direction: column;
        align-items: flex-start;
        justify-content: flex-start;

        .title {
            width: 100%;
            padding: 0.5rem;

            .pkg {
                padding: 0;
                color: inherit;
                margin-left: 0.1rem;
                border-bottom: 1px solid #8b8b8b;

                &:hover {
                    background-color: unset;
                    border-color: #dddddd;
                }
            }
        }

        .item {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            justify-content: flex-start;

            background-color: #2d2d2d;
            width: calc(100% - 1rem);
            padding: 0.5rem;
            font-size: 10pt;
            border-bottom: 1px solid #3e3e3e;

            .name, .desc {
                margin: 0.25rem;
            }

            .name {
                padding: 0;
                color: inherit;
                border-bottom: 1px solid #8b8b8b;

                &:hover {
                    background-color: unset;
                    border-color: #dddddd;
                }
            }

            .snippet {
                margin: 0.25rem;
                background-color: #333333;
                width: calc(100% - 1rem);
                padding: 0.5rem;

                * {
                    padding: 0;
                    margin: 0;
                }
            }
        }
    }
}
</style>
