<script setup lang="ts">
import Sidebar from "@/components/Sidebar.vue";
</script>

<template>
    <main class="main">
        <Sidebar />
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
}
</style>
