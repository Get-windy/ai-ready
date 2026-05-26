<template>
  <div
    :class="[
      'ar-container',
      {
        'ar-container--fluid': fluid,
        'ar-container--fullscreen': fullscreen
      }
    ]"
    :style="containerStyle"
  >
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  fluid?: boolean;
  fullscreen?: boolean;
  maxWidth?: string;
  padding?: string;
  backgroundColor?: string;
}

const props = withDefaults(defineProps<Props>(), {
  maxWidth: '1200px',
  padding: '0 20px'
});

const containerStyle = computed(() => ({
  '--ar-container-max-width': props.maxWidth,
  '--ar-container-padding': props.padding,
  '--ar-container-bg': props.backgroundColor
}));
</script>

<style lang="scss" scoped>
.ar-container {
  width: 100%;
  margin: 0 auto;
  padding: var(--ar-container-padding, 0 20px);
  max-width: var(--ar-container-max-width, 1200px);
  background-color: var(--ar-container-bg, transparent);

  &--fluid {
    max-width: 100%;
  }

  &--fullscreen {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }
}
</style>