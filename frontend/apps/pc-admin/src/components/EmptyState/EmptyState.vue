<template>
  <div
    class="empty-state"
    :class="`size-${size}`"
  >
    <div class="empty-image">
      <component :is="imageComponent" />
    </div>
    <div class="empty-text">
      <div
        v-if="title"
        class="title"
      >
        {{ title }}
      </div>
      <div
        v-if="description"
        class="description"
      >
        {{ description }}
      </div>
    </div>
    
    <!-- 操作按钮 -->
    <div
      v-if="showActions"
      class="empty-actions"
    >
      <slot name="actions">
        <a-button
          v-if="showRefresh"
          type="primary"
          @click="handleRefresh"
        >
          <template #icon>
            <ReloadOutlined />
          </template>
          刷新
        </a-button>
        <a-button
          v-if="showAdd && !showRefresh"
          type="primary"
          @click="handleAdd"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          {{ addText }}
        </a-button>
      </slot>
    </div>
    
    <!-- 高级用法 -->
    <div
      v-if="$slots.custom"
      class="empty-custom"
    >
      <slot name="custom" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

// Props
const props = defineProps({
  image: {
    type: String as PropType<'default' | 'simple' | 'error' | 'permission' | 'no-data' | 'custom'>,
    default: 'default'
  },
  size: {
    type: String as PropType<'small' | 'middle' | 'large'>,
    default: 'middle'
  },
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前列表为空，请尝试其他操作'
  },
  showActions: {
    type: Boolean,
    default: true
  },
  showRefresh: {
    type: Boolean,
    default: true
  },
  showAdd: {
    type: Boolean,
    default: true
  },
  addText: {
    type: String,
    default: '新增'
  }
})

// Emits
const emit = defineEmits<{
  'refresh': []
  'add': []
}>()

// 使用 Fallback 图标组件（SVG 全局变量可能不存在）
import { InboxOutlined, WarningOutlined, LockOutlined, FileSearchOutlined } from '@ant-design/icons-vue'

// 计算图片组件
const imageComponent = computed(() => {
  // 尝试使用全局 SVG 变量，如果不存在则使用 fallback
  const images: Record<string, object> = {
    'default': typeof Standard中图 !== 'undefined' ? Standard中图 : InboxOutlined,
    'simple': typeof EmptySimple !== 'undefined' ? EmptySimple : InboxOutlined,
    'error': typeof NetworkError !== 'undefined' ? NetworkError : WarningOutlined,
    'permission': typeof NoPermission !== 'undefined' ? NoPermission : LockOutlined,
    'no-data': typeof NoData !== 'undefined' ? NoData : FileSearchOutlined,
    'custom': typeof Standard中图 !== 'undefined' ? Standard中图 : InboxOutlined
  }

  return images[props.image] || images['default']
})

// 处理刷新
const handleRefresh = () => {
  emit('refresh')
}

// 处理新增
const handleAdd = () => {
  emit('add')
}
</script>

 * <style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: var(--ar-text-color-secondary, #909399);
}

.empty-image {
  margin-bottom: 16px;
}

.empty-image svg {
  width: 120px;
  height: 120px;
  fill: var(--ar-border-color-dark, #d4d7de);
}

.empty-text {
  text-align: center;
  margin-bottom: 24px;
}

.title {
  font-size: 18px;
  color: var(--ar-text-color-primary, #303133);
  margin-bottom: 8px;
}

.description {
  font-size: 14px;
}

.empty-actions {
  margin-top: 24px;
}

.empty-custom {
  margin-top: 24px;
}

/* 尺寸变体 */
.empty-state.size-small {
  padding: 20px 0;
}

.empty-state.size-small .empty-image svg {
  width: 80px;
  height: 80px;
}

.empty-state.size-small .title {
  font-size: 16px;
}

.empty-state.size-small .description {
  font-size: 12px;
}

.empty-state.size-large {
  padding: 60px 0;
}

.empty-state.size-large .empty-image svg {
  width: 180px;
  height: 180px;
}

.empty-state.size-large .title {
  font-size: 24px;
}

.empty-state.size-large .description {
  font-size: 16px;
}
</style>
