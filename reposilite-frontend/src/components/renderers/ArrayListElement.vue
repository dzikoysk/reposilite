<!--
  ~ Copyright (c) 2020-2026 dzikoysk
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<template>
  <div :class="styles.arrayList.item">
    <div :class="toolbarClasses">
      <button
        type="button"
        class="flex-1 self-stretch text-left"
        :aria-expanded="expanded"
        :aria-label="`${expanded ? 'Collapse' : 'Expand'} ${label}`"
        @click="expandClicked"
      >
        <span :class="styles.arrayList.itemLabel">{{ label }}</span>
      </button>
      <button
        @click="moveUpClicked"
        :disabled="!moveUpEnabled"
        :class="styles.arrayList.itemMoveUp"
        type="button"
        aria-label="Move item up"
      >
        ↑
      </button>
      <button
        @click="moveDownClicked"
        :disabled="!moveDownEnabled"
        :class="styles.arrayList.itemMoveDown"
        type="button"
        aria-label="Move item down"
      >
        ↓
      </button>
      <button
        @click="deleteClicked"
        class="font-mono text-xl"
        :class="styles.arrayList.itemDelete"
        type="button"
        aria-label="Delete item"
      >
        x
      </button>
    </div>
    <div :class="contentClasses">
      <slot></slot>
    </div>
  </div>
</template>

<script>
import { classes } from '@jsonforms/vue-vanilla'

export default {
  name: 'array-list-element',
  props: {
    initiallyExpanded: {
      required: false,
      type: Boolean,
      default: false
    },
    label: {
      required: false,
      type: String,
      default: ''
    },
    moveUpEnabled: {
      required: false,
      type: Boolean,
      default: true
    },
    moveDownEnabled: {
      required: false,
      type: Boolean,
      default: true
    },
    moveUp: {
      required: false,
      type: Function,
      default: undefined
    },
    moveDown: {
      required: false,
      type: Function,
      default: undefined
    },
    delete: {
      required: false,
      type: Function,
      default: undefined
    },
    styles: {
      required: true,
      type: Object
    }
  },
  data() {
    return {
      expanded: this.initiallyExpanded
    }
  },
  computed: {
    contentClasses() {
      return classes`${this.styles.arrayList.itemContent} ${this.expanded && this.styles.arrayList.itemExpanded}`
    },
    toolbarClasses() {
      return classes`${this.styles.arrayList.itemToolbar} ${this.expanded && this.styles.arrayList.itemExpanded}`
    }
  },
  methods: {
    expandClicked() {
      this.expanded = !this.expanded
    },
    moveUpClicked(event) {
      event.stopPropagation()
      this.moveUp?.()
    },
    moveDownClicked(event) {
      event.stopPropagation()
      this.moveDown?.()
    },
    deleteClicked(event) {
      event.stopPropagation()
      this.delete?.()
    }
  }
}
</script>
