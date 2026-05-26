<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  type?: 'data' | 'search' | 'list' | 'cart' | 'custom'
  title?: string
  message?: string
  showAction?: boolean
  actionText?: string
  image?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'data',
  title: '',
  message: '',
  showAction: false,
  actionText: '',
  image: ''
})

const emit = defineEmits<{
  action: []
}>()

const emptyConfig = {
  data: { icon: '📊', defaultTitle: '暂无数据', defaultMessage: '还没有相关数据' },
  search: { icon: '🔍', defaultTitle: '搜索无结果', defaultMessage: '未找到匹配的内容' },
  list: { icon: '📋', defaultTitle: '列表为空', defaultMessage: '列表中暂无内容' },
  cart: { icon: '🛒', defaultTitle: '购物车为空', defaultMessage: '快去添加商品吧' },
  custom: { icon: '📄', defaultTitle: '暂无内容', defaultMessage: '' }
}

const config = computed(() => emptyConfig[props.type])

const displayTitle = computed(() => props.title || config.value.defaultTitle)
const displayMessage = computed(() => props.message || config.value.defaultMessage)
const displayActionText = computed(() => props.actionText || (props.type === 'cart' ? '去购物' : '添加'))

const handleAction = () => {
  emit('action')
}
</script>

<template>
  <div class="ar-empty">
    <div v-if="image" class="ar-empty-image">
      <img :src="image" alt="empty" />
    </div>
    <div v-else class="ar-empty-icon">{{ config.icon }}</div>
    
    <div class="ar-empty-content">
      <div class="ar-empty-title">{{ displayTitle }}</div>
      <div v-if="displayMessage" class="ar-empty-message">{{ displayMessage }}</div>
      <slot />
    </div>
    
    <button 
      v-if="showAction"
      class="ar-empty-action"
      @click="handleAction"
    >
      {{ displayActionText }}
    </button>
  </div>
</template>

<style lang="scss" scoped>
.ar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  text-align: center;
  
  .ar-empty-image {
    width: 160px;
    height: 160px;
    margin-bottom: 16px;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }
  
  .ar-empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.6;
  }
  
  .ar-empty-content {
    max-width: 300px;
    
    .ar-empty-title {
      font-size: 14px;
      color: #969799;
      margin-bottom: 8px;
    }
    
    .ar-empty-message {
      font-size: 12px;
      color: #c8c9cc;
      line-height: 1.5;
    }
  }
  
  .ar-empty-action {
    margin-top: 16px;
    padding: 8px 24px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
    
    &:hover {
      background: #0e7cd3;
    }
  }
}
</style>