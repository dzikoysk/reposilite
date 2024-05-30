<script setup lang="ts">
import ClassIcon from "@/images/nav-icons/class.svg";
import ClassKtIcon from "@/images/nav-icons/class-kotlin.svg";
import FunctionIcon from "@/images/nav-icons/function.svg";
import EnumClassIcon from "@/images/nav-icons/enum.svg";
import EnumClassKtIcon from "@/images/nav-icons/enum-kotlin.svg";
import AnnotationClassIcon from "@/images/nav-icons/annotation.svg";
import AnnotationClassKtIcon from "@/images/nav-icons/annotation-kotlin.svg";
import AbstractClassIcon from "@/images/nav-icons/abstract-class.svg";
import AbstractClassKtIcon from "@/images/nav-icons/abstract-class-kotlin.svg";
import ExceptionClassIcon from "@/images/nav-icons/exception-class.svg";
import InterfaceIcon from "@/images/nav-icons/interface.svg";
import InterfaceKtIcon from "@/images/nav-icons/interface-kotlin.svg";
import ObjectIcon from "@/images/nav-icons/object.svg";
import TypeAliasKtIcon from "@/images/nav-icons/typealias-kotlin.svg";
import ValIcon from "@/images/nav-icons/field-value.svg";
import VarIcon from "@/images/nav-icons/field-variable.svg";

import type { ItemType } from '@/api/item';
import type { PropType } from 'vue';

const props = defineProps({
    type: { type: String as PropType<ItemType>, required: true },
});
</script>

<template>
    <ClassIcon v-if="props.type == 'class'" />
    <ClassKtIcon v-if="props.type == 'class-kt'" />
    <FunctionIcon v-if="props.type == 'function'" />
    <EnumClassIcon v-if="props.type == 'enum-class'" />
    <EnumClassKtIcon v-if="props.type == 'enum-class-kt'" />
    <AnnotationClassIcon v-if="props.type == 'annotation-class'" />
    <AnnotationClassKtIcon v-if="props.type == 'annotation-class-kt'" />
    <AbstractClassIcon v-if="props.type == 'abstract-class'" />
    <AbstractClassKtIcon v-if="props.type == 'abstract-class-kt'" />
    <ExceptionClassIcon v-if="props.type == 'exception-class'" />
    <InterfaceIcon v-if="props.type == 'interface'" />
    <InterfaceKtIcon v-if="props.type == 'interface-kt'" />
    <ObjectIcon v-if="props.type == 'object'" />
    <TypeAliasKtIcon v-if="props.type == 'typealias-kt'" />
    <ValIcon v-if="props.type == 'val'" />
    <VarIcon v-if="props.type == 'var'" />
</template>
