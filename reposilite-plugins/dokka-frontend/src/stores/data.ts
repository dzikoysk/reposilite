import type { Package } from "@/api/item";
import { defineStore } from "pinia";

export const usePackages = defineStore("packages", {
    state() {
        return { data: {} as Record<string, Package> };
    },

    actions: {
        set(newState: Record<string, Package>) {
            this.data = newState;
        },
    },
});
