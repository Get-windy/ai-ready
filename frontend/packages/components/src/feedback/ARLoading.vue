<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  loading?: boolean
  text?: string
  size?: 'small' | 'default' | 'large'
  fullscreen?: boolean
  spinner?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: true,
  text: '加载中...',
  size: 'default',
  fullscreen: false,
  spinner: true
})

const sizeMap = {
  small: { spinner: 20, text: 12 },
  default: { spinner: 32, text: 14 },
  large: { spinner: 48, text: 16 }
}

const sizes = computed(() => sizeMap[props.size])
</script>

<template>
  <div 
    v-if="loading"
    class="ar-loading"
    :class="{ 'ar-loading-fullscreen': fullscreen }"
  >
    <div class="ar-loading-wrapper">
      <div 
        v-if="spinner"
        class="ar-loading-spinner"
        :style="{ width: `${sizes.spinner}px`, height: `${sizes.spinner}px` }"
      />
      <div 
        v-if="text"
        class="ar-loading-text"
        :style="{ fontSize: `${sizes.text}px` }"
      >
        {{ text }}
      </div>
      <slot />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ar-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  
  &.ar-loading-fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.9);
    z-index: 9999;
  }
  
  .ar-loading-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  
  .ar-loading-spinner {
    border: 2px solid #ebedf0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: ar-loading-spin 0.8s linear infinite;
  }
  
  .ar-loading-text {
    color: #969799;
  }
}

@keyframes ar-loading-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>