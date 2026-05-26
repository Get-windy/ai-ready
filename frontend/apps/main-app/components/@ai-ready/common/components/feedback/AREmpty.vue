<template>
  <div :class="emptyClass" :style="emptyStyle">
    <!-- 空状态图标 -->
    <div class="ar-empty__icon" :style="iconStyle">
      <el-icon v-if="type === 'no-data'">
        <Document />
      </el-icon>
      <el-icon v-else-if="type === 'no-result'">
        <Search />
      </el-icon>
      <el-icon v-else-if="type === 'no-permission'">
        <Lock />
      </el-icon>
      <el-icon v-else-if="type === 'no-network'">
        <Connection />
      </el-icon>
      <el-icon v-else-if="type === 'no-file'">
        <Folder />
      </el-icon>
      <el-icon v-else-if="type === 'no-message'">
        <ChatDotRound />
      </el-icon>
      <el-icon v-else>
        <Box />
      </el-icon>
    </div>
    
    <!-- 空状态标题 -->
    <div v-if="title" class="ar-empty__title">
      {{ title }}
    </div>
    
    <!-- 空状态描述 -->
    <div v-if="description" class="ar-empty__description">
      {{ description }}
    </div>
    
    <!-- 自定义图片 -->
    <div v-else-if="image" class="ar-empty__image">
      <img :src="image" :alt="title || '空状态'" :style="imageStyle" />
    </div>
    
    <!-- 操作按钮 -->
    <div v-if="showActions" class="ar-empty__actions">
      <el-button 
        v-if="showCreate" 
        type="primary" 
        :icon="Plus" 
        @click="handleCreate"
      >
        创建内容
      </el-button>
      
      <el-button 
        v-if="showRefresh" 
        :icon="Refresh" 
        @click="handleRefresh"
      >
        刷新
      </el-button>
      
      <el-button 
        v-if="showImport" 
        :icon="Upload" 
        @click="handleImport"
      >
        导入数据
      </el-button>
      
      <el-button 
        v-if="showBack" 
        :icon="Back" 
        @click="handleBack"
      >
        返回
      </el-button>
      
      <!-- 自定义操作 -->
      <slot name="actions" />
    </div>
    
    <!-- 自定义内容 -->
    <slot v-if="$slots.default" />
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties } from 'vue'
import { 
  Document, 
  Search, 
  Lock, 
  Connection, 
  Folder, 
  ChatDotRound, 
  Box,
  Plus,
  Refresh,
  Upload,
  Back
} from '@element-plus/icons-vue'

defineOptions({
  name: 'AREmpty',
})

export interface EmptyProps {
  /** 空状态类型 */
  type?: 'no-data' | 'no-result' | 'no-permission' | 'no-network' | 'no-file' | 'no-message' | 'custom'
  /** 空状态标题 */
  title?: string
  /** 空状态描述 */
  description?: string
  /** 自定义图片URL */
  image?: string
  /** 图片宽度 */
  imageWidth?: number
  /** 图片高度 */
  imageHeight?: number
  /** 是否显示创建按钮 */
  showCreate?: boolean
  /** 是否显示刷新按钮 */
  showRefresh?: boolean
  /** 是否显示导入按钮 */
  showImport?: boolean
  /** 是否显示返回按钮 */
  showBack?: boolean
  /** 是否居中显示 */
  center?: boolean
  /** 是否全屏显示 */
  fullscreen?: boolean
  /** 图标大小 */
  iconSize?: number
  /** 图标颜色 */
  iconColor?: string
  /** 自定义类名 */
  customClass?: string
}

const props = withDefaults(defineProps<EmptyProps>(), {
  type: 'no-data',
  title: '',
  description: '',
  image: '',
  imageWidth: 120,
  imageHeight: 120,
  showCreate: true,
  showRefresh: true,
  showImport: false,
  showBack: false,
  center: true,
  fullscreen: false,
  iconSize: 64,
})

const emit = defineEmits<{
  create: []
  refresh: []
  import: []
  back: []
}>()

// 空状态类型配置
const emptyConfig = computed(() => {
  const configs = {
    'no-data': {
      title: '暂无数据',
      description: '当前没有数据，您可以创建新的内容',
      icon: Document,
      color: '#909399',
    },
    'no-result': {
      title: '没有找到结果',
      description: '尝试调整搜索条件或关键词',
      icon: Search,
      color: '#909399',
    },
    'no-permission': {
      title: '无访问权限',
      description: '您没有权限查看此内容',
      icon: Lock,
      color: '#e6a23c',
    },
    'no-network': {
      title: '网络连接失败',
      description: '请检查网络连接后重试',
      icon: Connection,
      color: '#f56c6c',
    },
    'no-file': {
      title: '没有文件',
      description: '当前文件夹为空，可以上传文件',
      icon: Folder,
      color: '#909399',
    },
    'no-message': {
      title: '暂无消息',
      description: '您还没有收到任何消息',
      icon: ChatDotRound,
      color: '#909399',
    },
    custom: {
      title: '空状态',
      description: '这里什么都没有',
      icon: Box,
      color: '#909399',
    },
  }
  
  return configs[props.type] || configs['no-data']
})

// 空状态类名
const emptyClass = computed(() => ({
  'ar-empty': true,
  [`ar-empty--${props.type}`]: true,
  'ar-empty--center': props.center,
  'ar-empty--fullscreen': props.fullscreen,
  [props.customClass || '']: !!props.customClass,
}))

// 空状态样式
const emptyStyle = computed<CSSProperties>(() => {
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
  color: props.iconColor || emptyConfig.value.color,
}))

// 图片样式
const imageStyle = computed<CSSProperties>(() => ({
  width: `${props.imageWidth}px`,
  height: `${props.imageHeight}px`,
  objectFit: 'contain',
}))

// 是否显示操作按钮
const showActions = computed(() => 
  props.showCreate || 
  props.showRefresh || 
  props.showImport || 
  props.showBack ||
  !!props.$slots.actions
)

// 获取默认标题
const computedTitle = computed(() => 
  props.title || emptyConfig.value.title
)

// 获取默认描述
const computedDescription = computed(() => 
  props.description || emptyConfig.value.description
)

// 处理创建
const handleCreate = () => {
  emit('create')
}

// 处理刷新
const handleRefresh = () => {
  emit('refresh')
}

// 处理导入
const handleImport = () => {
  emit('import')
}

// 处理返回
const handleBack = () => {
  emit('back')
}
</script>

<style scoped>
.ar-empty {
  padding: 40px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.ar-empty--center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.ar-empty--fullscreen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: rgba(255, 255, 255, 0.95);
}

.ar-empty__icon {
  margin-bottom: 20px;
}

.ar-empty__image {
  margin-bottom: 20px;
}

.ar-empty__image img {
  display: block;
  margin: 0 auto;
}

.ar-empty__title {
  margin-bottom: 12px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.ar-empty__description {
  margin-bottom: 24px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  max-width: 400px;
}

.ar-empty__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  align-items: center;
}

/* 不同类型样式 */
.ar-empty--no-data .ar-empty__icon {
  color: #909399;
}

.ar-empty--no-result .ar-empty__icon {
  color: #909399;
}

.ar-empty--no-permission .ar-empty__icon {
  color: #e6a23c;
}

.ar-empty--no-network .ar-empty__icon {
  color: #f56c6c;
}

.ar-empty--no-file .ar-empty__icon {
  color: #909399;
}

.ar-empty--no-message .ar-empty__icon {
  color: #909399;
}

/* 深色模式 */
[data-theme='dark'] .ar-empty {
  background: #1f1f1f;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}

[data-theme='dark'] .ar-empty--fullscreen {
  background: rgba(31, 31, 31, 0.95);
}

[data-theme='dark'] .ar-empty__title {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-empty__description {
  color: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ar-empty {
    padding: 24px 16px;
  }
  
  .ar-empty__title {
    font-size: 18px;
  }
  
  .ar-empty__description {
    font-size: 13px;
  }
  
  .ar-empty__actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .ar-empty__actions .el-button {
    width: 100%;
    max-width: 200px;
  }
  
  .ar-empty__image img {
    max-width: 80px;
    max-height: 80px;
  }
}

/* 动画效果 */
@keyframes empty-fade-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ar-empty {
  animation: empty-fade-in 0.3s ease-out;
}

/* 悬停效果 */
.ar-empty:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
  transition: box-shadow 0.3s ease;
}

[data-theme='dark'] .ar-empty:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.4);
}
</style>