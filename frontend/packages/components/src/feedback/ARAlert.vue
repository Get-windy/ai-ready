<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  type?: 'info' | 'success' | 'warning' | 'error'
  title?: string
  message?: string
  closable?: boolean
  showIcon?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  title: '',
  message: '',
  closable: false,
  showIcon: true
})

const emit = defineEmits<{
  close: []
}>()

const typeColors = {
  info: { bg: '#e6f7ff', border: '#1890ff', text: '#1890ff' },
  success: { bg: '#f6ffed', border: '#52c41a', text: '#52c41a' },
  warning: { bg: '#fffbe6', border: '#faad14', text: '#faad14' },
  error: { bg: '#fff2f0', border: '#f5222d', text: '#f5222d' }
}

const colors = computed(() => typeColors[props.type])

const handleClose = () => {
  emit('close')
}

const iconSymbols = {
  info: 'ℹ',
  success: '✓',
  warning: '⚠',
  error: '✕'
}
</script>

<template>
  <div 
    class="ar-alert"
    :style="{
      backgroundColor: colors.bg,
      borderColor: colors.border,
      color: colors.text
    }"
  >
    <div v-if="showIcon" class="ar-alert-icon">
      {{ iconSymbols[type] }}
    </div>
    
    <div class="ar-alert-content">
      <div v-if="title" class="ar-alert-title">{{ title }}</div>
      <div v-if="message" class="ar-alert-message">{{ message }}</div>
      <slot />
    </div>
    
    <button 
      v-if="closable"
      class="ar-alert-close"
      @click="handleClose"
    >
      ✕
    </button>
  </div>
</template>

<style lang="scss" scoped>
.ar-alert {
  display: flex;
  align-items: flex-start;
  padding: 8px 16px;
  border-radius: 4px;
  border: 1px solid;
  font-size: 14px;
  line-height: 1.5;
  
  .ar-alert-icon {
    flex-shrink: 0;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    margin-right: 8px;
  }
  
  .ar-alert-content {
    flex: 1;
    min-width: 0;
    
    .ar-alert-title {
      font-weight: 600;
      margin-bottom: 4px;
    }
    
    .ar-alert-message {
      opacity: 0.9;
    }
  }
  
  .ar-alert-close {
    flex-shrink: 0;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    cursor: pointer;
    opacity: 0.6;
    
    &:hover {
      opacity: 1;
    }
  }
}
</style>