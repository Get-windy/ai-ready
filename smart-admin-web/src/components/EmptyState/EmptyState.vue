<template>
  <div class="empty-state" :class="`size-${size}`">
    <div class="empty-image">
      <component :is="imageComponent" />
    </div>
    <div class="empty-text">
      <div class="title" v-if="title">{{ title }}</div>
      <div class="description" v-if="description">{{ description }}</div>
    </div>
    
    <!-- 操作按钮 -->
    <div v-if="showActions" class="empty-actions">
      <slot name="actions">
        <a-button
          v-if="showRefresh"
          type="primary"
          @click="handleRefresh"
        >
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button
          v-if="showAdd && !showRefresh"
          type="primary"
          @click="handleAdd"
        >
          <template #icon><PlusOutlined /></template>
          {{ addText }}
        </a-button>
      </slot>
    </div>
    
    <!-- 高级用法 -->
    <div v-if="$slots.custom" class="empty-custom">
      <slot name="custom" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import {
  Standard两大图,
  Standard中图,
  Standard小图,
  EmptyNormal,
  EmptySimple,
  NetworkError,
  NoPermission,
  NoData
} from '@ant-design/icons-vue'

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

// 计算图片组件
const imageComponent = computed(() => {
  const images: Record<string, any> = {
    'default': Standard中图,
    'simple': EmptySimple,
    'error': NetworkError,
    'permission': NoPermission,
    'no-data': NoData,
    'custom': Standard中图
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

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
}

.empty-image {
  margin-bottom: 16px;
}

.empty-image svg {
  width: 120px;
  height: 120px;
  fill: #d9d9d9;
}

.empty-text {
  text-align: center;
  margin-bottom: 24px;
}

.title {
  font-size: 18px;
  color: #333;
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

/**
 * EmptyState 空状态组件库
 * 
 * 使用方法：
 * 
 * 1. 基本使用
 * ```vue
 * <template>
 *   <EmptyState
 *     v-if="list.length === 0"
 *     title="暂无数据"
 *     description="请先创建一些数据"
 *   />
 *   
 *   <a-table
 *     v-else
 *     :data-source="list"
 *     :columns="columns"
 *   />
 * </template>
 * ```
 * 
 * 2. 显示刷新按钮
 * ```vue
 * <EmptyState
 *   v-if="!data"
 *   title="暂无数据"
 *   description="请刷新页面重试"
 *   @refresh="handleRefresh"
 * />
 * ```
 * 
 * 3. 自定义操作
 * ```vue
 * <EmptyState
 *   :show-actions="false"
 * >
 *   <template #actions>
 *     <a-button type="primary" @click="goToCreate">
 *       去创建
 *     </a-button>
 *     <a-button @click="goToHelp">
 *       帮助文档
 *     </a-button>
 *   </template>
 * </EmptyState>
 * ```
 * 
 * 4. 不同尺寸
 * ```vue
 * <!-- 小尺寸 -->
 * <EmptyState size="small" />
 * 
 * <!-- 大尺寸 -->
 * <EmptyState size="large" />
 * ```
 * 
 * Props说明：
 * - image: 'default' | 'simple' | 'error' | 'permission' | 'no-data' | 'custom' - 图片样式（默认：'default'）
 * - size: 'small' | 'middle' | 'large' - 组件尺寸（默认：'middle'）
 * - title: string - 标题（默认：'暂无数据'）
 * - description: string - 描述信息（默认：'当前列表为空，请尝试其他操作'）
 * - showActions: boolean - 是否显示操作按钮（默认：true）
 * - showRefresh: boolean - 是否显示刷新按钮（默认：true）
 * - showAdd: boolean - 是否显示新增按钮（默认：true）
 * - addText: string - 新增按钮文字（默认：'新增'）
 * 
 * Events：
 * - refresh: 点击刷新按钮时触发
 * - add: 点击新增按钮时触发
 * 
 * 插槽：
 * - default: 默认内容
 * - actions: 自定义操作按钮
 * - custom: 自定义内容
 * 
 * 图片样式说明：
 * - default: 默认大图，通用场景
 * - simple: 简约小图，适合表单等场景
 * - error: 网络错误图标
 * - permission: 无权限图标
 * - no-data: 暂无数据图标
 * 
 * 最佳实践：
 * 1. 表格、列表为空时使用空状态组件
 * 2. 搜索无结果时显示空状态
 * 3. 加载失败时根据类型显示不同图标
 * 4. 提供明确的操作指引（刷新、新增、帮助等）
 */
