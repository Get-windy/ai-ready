<template>
  <div :class="errorClass" :style="errorStyle">
    <!-- 错误图标 -->
    <div class="ar-error__icon" :style="iconStyle">
      <el-icon v-if="type === 'network'">
        <Connection />
      </el-icon>
      <el-icon v-else-if="type === 'permission'">
        <Lock />
      </el-icon>
      <el-icon v-else-if="type === 'validation'">
        <Warning />
      </el-icon>
      <el-icon v-else-if="type === 'server'">
        <Service />
      </el-icon>
      <el-icon v-else>
        <CircleCloseFilled />
      </el-icon>
    </div>
    
    <!-- 错误标题 -->
    <div v-if="title" class="ar-error__title">
      {{ title }}
    </div>
    
    <!-- 错误描述 -->
    <div v-if="description" class="ar-error__description">
      {{ description }}
    </div>
    
    <!-- 错误详情 -->
    <div v-if="details && showDetails" class="ar-error__details">
      <pre class="ar-error__details-content">{{ details }}</pre>
    </div>
    
    <!-- 操作按钮 -->
    <div v-if="showActions" class="ar-error__actions">
      <el-button 
        v-if="showRetry" 
        type="primary" 
        :icon="Refresh" 
        @click="handleRetry"
      >
        重试
      </el-button>
      
      <el-button 
        v-if="showBack" 
        :icon="Back" 
        @click="handleBack"
      >
        返回
      </el-button>
      
      <el-button 
        v-if="showDetailsToggle" 
        :icon="showDetails ? 'ArrowUp' : 'ArrowDown'" 
        text 
        @click="toggleDetails"
      >
        {{ showDetails ? '隐藏详情' : '显示详情' }}
      </el-button>
      
      <el-button 
        v-if="showReport" 
        :icon="Message" 
        text 
        @click="handleReport"
      >
        报告问题
      </el-button>
      
      <!-- 自定义操作 -->
      <slot name="actions" />
    </div>
    
    <!-- 自定义内容 -->
    <slot v-if="$slots.default" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, CSSProperties } from 'vue'
import { 
  CircleCloseFilled, 
  Refresh, 
  Back, 
  Message,
  Connection,
  Lock,
  Warning,
  Service
} from '@element-plus/icons-vue'

defineOptions({
  name: 'ARError',
})

export interface ErrorProps {
  /** 错误类型 */
  type?: 'network' | 'permission' | 'validation' | 'server' | 'generic'
  /** 错误标题 */
  title?: string
  /** 错误描述 */
  description?: string
  /** 错误详情 */
  details?: string
  /** 是否显示详情 */
  showDetails?: boolean
  /** 是否显示重试按钮 */
  showRetry?: boolean
  /** 是否显示返回按钮 */
  showBack?: boolean
  /** 是否显示报告按钮 */
  showReport?: boolean
  /** 是否显示详情切换按钮 */
  showDetailsToggle?: boolean
  /** 错误代码 */
  code?: string | number
  /** 是否全屏显示 */
  fullscreen?: boolean
  /** 是否居中显示 */
  center?: boolean
  /** 自定义图标大小 */
  iconSize?: number
  /** 自定义图标颜色 */
  iconColor?: string
  /** 自定义类名 */
  customClass?: string
}

const props = withDefaults(defineProps<ErrorProps>(), {
  type: 'generic',
  title: '',
  description: '',
  details: '',
  showDetails: false,
  showRetry: true,
  showBack: true,
  showReport: false,
  showDetailsToggle: true,
  fullscreen: false,
  center: true,
  iconSize: 64,
})

const emit = defineEmits<{
  retry: []
  back: []
  report: []
  'toggle-details': [show: boolean]
}>()

// 响应式的详情显示状态
const localShowDetails = ref(props.showDetails)

// 错误类型配置
const errorConfig = computed(() => {
  const configs = {
    network: {
      title: '网络连接错误',
      description: '无法连接到服务器，请检查网络连接后重试',
      icon: Connection,
      color: '#f56c6c',
    },
    permission: {
      title: '权限不足',
      description: '您没有权限访问此资源',
      icon: Lock,
      color: '#e6a23c',
    },
    validation: {
      title: '数据验证错误',
      description: '输入的数据不符合要求',
      icon: Warning,
      color: '#e6a23c',
    },
    server: {
      title: '服务器错误',
      description: '服务器内部错误，请稍后重试',
      icon: Service,
      color: '#f56c6c',
    },
    generic: {
      title: '发生错误',
      description: '抱歉，发生了未知错误',
      icon: CircleCloseFilled,
      color: '#f56c6c',
    },
  }
  
  return configs[props.type] || configs.generic
})

// 错误类名
const errorClass = computed(() => ({
  'ar-error': true,
  [`ar-error--${props.type}`]: true,
  'ar-error--fullscreen': props.fullscreen,
  'ar-error--center': props.center,
  [props.customClass || '']: !!props.customClass,
}))

// 错误样式
const errorStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {}
  
  if (props.fullscreen) {
    style.position = 'fixed'
    style.top = '0'
    style.left = '0'
    style.right = '0'
    style.bottom = '0'
    style.zIndex = '9999'
  }
  
  return style
})

// 图标样式
const iconStyle = computed<CSSProperties>(() => ({
  fontSize: `${props.iconSize}px`,
  color: props.iconColor || errorConfig.value.color,
}))

// 是否显示操作按钮
const showActions = computed(() => 
  props.showRetry || 
  props.showBack || 
  props.showReport || 
  props.showDetailsToggle ||
  !!props.$slots.actions
)

// 处理重试
const handleRetry = () => {
  emit('retry')
}

// 处理返回
const handleBack = () => {
  emit('back')
}

// 处理报告
const handleReport = () => {
  emit('report')
}

// 切换详情显示
const toggleDetails = () => {
  localShowDetails.value = !localShowDetails.value
  emit('toggle-details', localShowDetails.value)
}

// 获取默认标题
const computedTitle = computed(() => 
  props.title || errorConfig.value.title
)

// 获取默认描述
const computedDescription = computed(() => 
  props.description || errorConfig.value.description
)
</script>

<style scoped>
.ar-error {
  padding: 40px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.ar-error--center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.ar-error--fullscreen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: rgba(255, 255, 255, 0.95);
}

.ar-error__icon {
  margin-bottom: 20px;
}

.ar-error__title {
  margin-bottom: 12px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.ar-error__description {
  margin-bottom: 24px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  max-width: 400px;
}

.ar-error__details {
  width: 100%;
  max-width: 600px;
  margin-bottom: 24px;
  text-align: left;
}

.ar-error__details-content {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.ar-error__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  align-items: center;
}

/* 网络错误 */
.ar-error--network .ar-error__icon {
  color: #f56c6c;
}

/* 权限错误 */
.ar-error--permission .ar-error__icon {
  color: #e6a23c;
}

/* 验证错误 */
.ar-error--validation .ar-error__icon {
  color: #e6a23c;
}

/* 服务器错误 */
.ar-error--server .ar-error__icon {
  color: #f56c6c;
}

/* 通用错误 */
.ar-error--generic .ar-error__icon {
  color: #f56c6c;
}

/* 深色模式 */
[data-theme='dark'] .ar-error {
  background: #1f1f1f;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}

[data-theme='dark'] .ar-error--fullscreen {
  background: rgba(31, 31, 31, 0.95);
}

[data-theme='dark'] .ar-error__title {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-error__description {
  color: #a8a8a8;
}

[data-theme='dark'] .ar-error__details-content {
  background: #2a2a2a;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ar-error {
    padding: 24px 16px;
  }
  
  .ar-error__title {
    font-size: 18px;
  }
  
  .ar-error__description {
    font-size: 13px;
  }
  
  .ar-error__actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .ar-error__actions .el-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>