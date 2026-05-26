<template>
  <div
    :class="[
      'ar-row',
      {
        'ar-row--flex': flex,
        'ar-row--wrap': wrap,
        'ar-row--no-gutter': noGutter
      }
    ]"
    :style="rowStyle"
  >
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  flex?: boolean;
  wrap?: boolean;
  noGutter?: boolean;
  align?: 'start' | 'end' | 'center' | 'stretch' | 'baseline';
  justify?: 'start' | 'end' | 'center' | 'space-between' | 'space-around' | 'space-evenly';
  gap?: string;
  margin?: string;
}

const props = withDefaults(defineProps<Props>(), {
  gap: '16px',
  margin: '0'
});

const rowStyle = computed(() => ({
  '--ar-row-gap': props.gap,
  '--ar-row-margin': props.margin,
  '--ar-row-align': props.align,
  '--ar-row-justify': props.justify
}));
</script>

<style lang="scss" scoped>
.ar-row {
  display: flex;
  flex-wrap: var(--ar-row-wrap, wrap);
  gap: var(--ar-row-gap, 16px);
  margin: var(--ar-row-margin, 0);

  &--flex {
    display: flex;
  }

  &--wrap {
    flex-wrap: wrap;
  }

  &--no-gutter {
    gap: 0;
  }

  // Alignment classes
  &.align-start {
    align-items: flex-start;
  }

  &.align-end {
    align-items: flex-end;
  }

  &.align-center {
    align-items: center;
  }

  &.align-stretch {
    align-items: stretch;
  }

  &.align-baseline {
    align-items: baseline;
  }

  // Justify classes
  &.justify-start {
    justify-content: flex-start;
  }

  &.justify-end {
    justify-content: flex-end;
  }

  &.justify-center {
    justify-content: center;
  }

  &.justify-space-between {
    justify-content: space-between;
  }

  &.justify-space-around {
    justify-content: space-around;
  }

  &.justify-space-evenly {
    justify-content: space-evenly;
  }
}
</style>