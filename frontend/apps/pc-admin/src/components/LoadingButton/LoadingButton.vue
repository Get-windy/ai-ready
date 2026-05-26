<template>
  <a-button
    :type="type"
    :size="size"
    :danger="danger"
    :ghost="ghost"
    :disabled="disabled || loading"
    :loading="loading"
    :html-type="htmlType"
    :block="block"
    :style="buttonStyle"
    @click="handleClick"
  >
    <template v-if="loading && loadingIcon">
      <LoadingOutlined :spin="true" />
      <span v-if="loadingText">{{ loadingText }}</span>
    </template>
    <template v-else-if="icon && !loading">
      <component :is="iconComponent" />
    </template>
    
    <template #default>
      <slot />
    </template>
  </a-button>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { LoadingOutlined } from '@ant-design/icons-vue'

// Props
const props = defineProps({
  type: {
    type: String as PropType<'default' | 'primary' | 'dashed' | 'link' | 'text'>,
    default: 'primary'
  },
  size: {
    type: String as PropType<'small' | 'middle' | 'large'>,
    default: 'middle'
  },
  danger: {
    type: Boolean,
    default: false
  },
  ghost: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  htmlType: {
    type: String as PropType<'button' | 'submit' | 'reset'>,
    default: 'button'
  },
  block: {
    type: Boolean,
    default: false
  },
  icon: {
    type: String,
    default: ''
  },
  loadingText: {
    type: String,
    default: ''
  },
  style: {
    type: String,
    default: ''
  }
})

// Emits
const emit = defineEmits<{
  'click': [event: MouseEvent]
}>()

// 计算图标组件
const iconComponent = computed(() => {
  if (!props.icon) return null
  
  // 支持的图标列表
  const icons: Record<string, any> = {
    plus: () => import('@ant-design/icons-vue').then(m => m.PlusOutlined),
    delete: () => import('@ant-design/icons-vue').then(m => m.DeleteOutlined),
    edit: () => import('@ant-design/icons-vue').then(m => m.EditOutlined),
    search: () => import('@ant-design/icons-vue').then(m => m.SearchOutlined),
    save: () => import('@ant-design/icons-vue').then(m => m.SaveOutlined),
    close: () => import('@ant-design/icons-vue').then(m => m.CloseOutlined),
    check: () => import('@ant-design/icons-vue').then(m => m.CheckOutlined),
    info: () => import('@ant-design/icons-vue').then(m => m.InfoCircleOutlined),
    warning: () => import('@ant-design/icons-vue').then(m => m.WarningOutlined),
    error: () => import('@ant-design/icons-vue').then(m => m.ErrorOutlined),
    download: () => import('@ant-design/icons-vue').then(m => m.DownloadOutlined),
    upload: () => import('@ant-design/icons-vue').then(m => m.UploadOutlined),
    eye: () => import('@ant-design/icons-vue').then(m => m.EyeOutlined),
    left: () => import('@ant-design/icons-vue').then(m => m.LeftOutlined),
    right: () => import('@ant-design/icons-vue').then(m => m.RightOutlined),
    arrowRight: () => import('@ant-design/icons-vue').then(m => m.ArrowRightOutlined),
    arrowLeft: () => import('@ant-design/icons-vue').then(m => m.ArrowLeftOutlined)
  }
  
  return icons[props.icon] || null
})

// 计算按钮样式
const buttonStyle = computed(() => {
  const style: any = {}
  
  if (props.style) {
    Object.assign(style, props.style)
  }
  
  return style
})

// 点击处理
const handleClick = (event: MouseEvent) => {
  if (!props.loading && !props.disabled) {
    emit('click', event)
  }
}
</script>

/**
 * LoadingButton 加载按钮组件库
 * 
 * 增强版 Ant Design 的 a-button，支持：
 * - 自动加载状态
 * - 图标切换
 * - 加载时显示额外文本
 * 
 * 使用方法：
 * 
 * 1. 基本使用
 * ```vue
 * <template>
 *   <LoadingButton
 *     :loading="submitting"
 *     @click="handleSubmit"
 *   >
 *     提交
 *   </LoadingButton>
 * </template>
 * 
 * <script setup>
 * const submitting = ref(false)
 * 
 * const handleSubmit = async () => {
 *   submitting.value = true
 *   try {
 *     await api.submit()
 *   } finally {
 *     submitting.value = false
 *   }
 * }
 * </script>
 * ```
 * 
 * 2. 带图标的按钮
 * ```vue
 * <LoadingButton
 *   type="primary"
 *   icon="plus"
 *   @click="handleAdd"
 * >
 *   新增
 * </LoadingButton>
 * ```
 * 
 * 3. 加载时显示额外文本
 * ```vue
 * <LoadingButton
 *   :loading="loading"
 *   loading-text="正在提交..."
 *   @click="handleSubmit"
 * >
 *   提交
 * </LoadingButton>
 * ```
 * 
 * Props说明：
 * - type: 'default' | 'primary' | 'dashed' | 'link' | 'text' - 按钮类型（默认：'primary'）
 * - size: 'small' | 'middle' | 'large' - 按钮尺寸（默认：'middle'）
 * - danger: boolean - 危险按钮（默认：false）
 * - ghost: boolean - 幽灵按钮（默认：false）
 * - disabled: boolean - 是否禁用（默认：false）
 * - loading: boolean - 是否加载中（默认：false）
 * - htmlType: 'button' | 'submit' | 'reset' - HTML类型（默认：'button'）
 * - block: boolean - 是否块级按钮（默认：false）
 * - icon: string - 图标名称（空字符串表示不显示图标）
 * - loadingText: string - 加载时显示的额外文本（空字符串表示不显示）
 * - style: string - 自定义样式
 * 
 * Events：
 * - click: 点击按钮时触发
 * 
 * 支持的图标：
 * - plus, delete, edit, search, save, close, check
 * - info, warning, error, download, upload, eye
 * - left, right, arrowRight, arrowLeft
 */
