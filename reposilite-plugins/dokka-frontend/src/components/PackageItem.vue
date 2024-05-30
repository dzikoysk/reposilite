<script setup lang="ts">
import Item from "./Item.vue";
import DownIcon from "@/images/arrow_down.svg";
import type { Package } from "@/api/item";
import { type PropType, ref } from "vue";
import router from "@/router";

const open = ref(false);

const props = defineProps({
    data: { type: Object as PropType<Package>, required: true },
});

const nav = (ev: MouseEvent) => {
    if (ev.target instanceof HTMLDivElement || ev.target instanceof HTMLSpanElement)
        router.push(`/package/${props.data.name}`);
};
</script>

<template>
    <div class="item" @click="nav">
        <DownIcon class="icon" :class="{ open }" @click="open = !open" />
        <span class="name">{{ props.data.name }}</span>
    </div>

    <div class="items" v-if="open">
        <Item v-for="item in props.data.items" :data="item" />
    </div>
</template>

<style scoped lang="scss">
    .item {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: flex-start;

        background-color: #2d2d2d;
        width: calc(100% - 1rem);
        padding: 0.5rem;
        font-size: 10pt;
        cursor: pointer;
        user-select: none;

        transition: background-color 0.25s ease;
        
        &:hover {
            background-color: #3e3e3e;
        }

        .icon {
            margin: 0;
            transition: transform 0.25s ease;

            &.open {
                transform: rotateZ(90deg);
            }
        }
    }

    .items {
        width: 100%;
    }
</style>
