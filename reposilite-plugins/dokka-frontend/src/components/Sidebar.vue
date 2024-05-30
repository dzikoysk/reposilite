<script setup lang="ts">
import PackageItem from "@/components/PackageItem.vue";
import { usePackages } from "@/stores/data";

const packages = usePackages();
</script>

<template>
    <div class="sidebar">
        <PackageItem v-for="item in packages.data" :data="item" />
    </div>
</template>

<style scoped lang="scss">
.sidebar {
    width: 25%;
    height: 100%;

    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;

    overflow: scroll;
}
</style>
