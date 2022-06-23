<template>
  <div :class="styles.arrayList.item">
    <div @click="expandClicked" :class="toolbarClasses">
      <div :class="styles.arrayList.itemLabel">{{ label }}</div>
      <button
        @click="moveUpClicked"
        :disabled="!moveUpEnabled"
        :class="styles.arrayList.itemMoveUp"
        type="button"
      >
        ↑
      </button>
      <button
        @click="moveDownClicked"
        :disabled="!moveDownEnabled"
        :class="styles.arrayList.itemMoveDown"
        type="button"
      >
        ↓
      </button>
      <button
        @click="deleteClicked"
        :class="styles.arrayList.itemDelete"
        type="button"
      >
        🗙
      </button>
    </div>
    <div :class="contentClasses">
      <slot></slot>
    </div>
  </div>
</template>

<script>
import { classes } from '@dzikoysk/vue-vanilla'

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
