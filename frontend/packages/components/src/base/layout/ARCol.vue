<template>
  <div
    :class="[
      'ar-col',
      `ar-col--${span}`,
      {
        [`ar-col--offset-${offset}`]: offset,
        [`ar-col--order-${order}`]: order,
        'ar-col--flex': flex,
        'ar-col--auto': auto
      }
    ]"
    :style="colStyle"
  >
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  span?: number | string;
  offset?: number;
  order?: number;
  flex?: boolean;
  auto?: boolean;
  minWidth?: string;
  maxWidth?: string;
  padding?: string;
}

const props = withDefaults(defineProps<Props>(), {
  span: 24,
  padding: '0'
});

const colStyle = computed(() => ({
  '--ar-col-min-width': props.minWidth,
  '--ar-col-max-width': props.maxWidth,
  '--ar-col-padding': props.padding
}));
</script>

<style lang="scss" scoped>
.ar-col {
  position: relative;
  padding: var(--ar-col-padding, 0);
  min-width: var(--ar-col-min-width, 0);
  max-width: var(--ar-col-max-width, 100%);

  &--flex {
    display: flex;
    flex-direction: column;
  }

  &--auto {
    flex: 1;
    flex-basis: auto;
  }

  // Generate column classes from 1 to 24
  @for $i from 1 through 24 {
    &--#{$i} {
      flex: 0 0 calc($i / 24 * 100%);
      max-width: calc($i / 24 * 100%);
    }

    &--offset-#{$i} {
      margin-left: calc($i / 24 * 100%);
    }

    &--order-#{$i} {
      order: $i;
    }
  }
}
</style>