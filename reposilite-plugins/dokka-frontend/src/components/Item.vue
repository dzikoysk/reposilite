<script setup lang="ts">
import type { DokkaItem } from "@/api/item";
import type { PropType } from "vue";
import ItemIcon from "./ItemIcon.vue";
import router from "@/router";

const props = defineProps({
    data: { type: Object as PropType<DokkaItem>, required: true },
});

const nav = () => {
    router.push(`/item/${props.data.pkg}/${props.data.name}`);
};
</script>

<template>
    <div class="item" @click.capture="nav">
        <ItemIcon :type="props.data.type" class="icon" />
        <span class="name">{{ props.data.name }}</span>
    </div>
</template>

<style scoped lang="scss">
    .item {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: flex-start;

        background-color: #2d2d2d;
        width: calc(100% - 2.5rem);
        padding: 0.5rem;
        padding-left: 2rem !important;
        font-size: 10pt;
        cursor: pointer;
        user-select: none;

        transition: background-color 0.25s ease;
        
        &:hover {
            background-color: #3e3e3e;
        }

        .name {
            margin-left: 0.3rem;
        }
    }
</style>