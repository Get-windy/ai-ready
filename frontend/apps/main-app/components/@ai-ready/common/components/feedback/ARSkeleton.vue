<template>
  <div :class="skeletonClass" :style="skeletonStyle">
    <!-- 标题骨架 -->
    <div v-if="showTitle" class="ar-skeleton__title" :style="titleStyle"></div>
    
    <!-- 段落骨架 -->
    <template v-if="showParagraph">
      <div 
        v-for="(line, index) in paragraphLines" 
        :key="index"
        class="ar-skeleton__paragraph" 
        :class="`ar-skeleton__paragraph--${line}`"
      ></div>
    </template>
    
    <!-- 头像骨架 -->
    <div v-if="showAvatar" class="ar-skeleton__avatar" :style="avatarStyle"></div>
    
    <!-- 按钮骨架 -->
    <div v-if="showButton" class="ar-skeleton__button" :style="buttonStyle"></div>
    
    <!-- 自定义内容 -->
    <div 
      v-if="customContent" 
      class="ar-skeleton__custom" 
      :style="customStyle"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties } from 'vue'

defineOptions({
  name: 'ARSkeleton',
})

export interface SkeletonProps {
  /** 是否显示骨架屏 */
  active?: boolean
  /** 是否显示标题 */
  title?: boolean
  /** 标题宽度 */
  titleWidth?: string | number
  /** 是否显示段落 */
  paragraph?: boolean
  /** 段落行数 */
  paragraphRows?: number
  /** 段落宽度，可以是数组，每行一个宽度 */
  paragraphWidth?: string | number | (string | number)[]
  /** 是否显示头像 */
  avatar?: boolean
  /** 头像大小 */
  avatarSize?: string | number
  /** 头像形状，circle 或 square */
  avatarShape?: 'circle' | 'square'
  /** 是否显示按钮 */
  button?: boolean
  /** 按钮大小 */
  buttonSize?: 'large' | 'default' | 'small'
  /** 自定义内容 */
  customContent?: boolean
  /** 自定义内容样式 */
  customStyle?: CSSProperties
  /** 骨架屏宽度 */
  width?: string | number
  /** 骨架屏高度 */
  height?: string | number
  /** 是否显示动画 */
  animated?: boolean
  /** 圆角大小 */
  borderRadius?: string | number
  /** 骨架屏类型 */
  type?: 'text' | 'card' | 'list' | 'table'
}

const props = withDefaults(defineProps<SkeletonProps>(), {
  active: true,
  title: false,
  titleWidth: '40%',
  paragraph: true,
  paragraphRows: 4,
  paragraphWidth: ['100%', '100%', '80%', '60%'],
  avatar: false,
  avatarSize: 40,
  avatarShape: 'circle',
  button: false,
  buttonSize: 'default',
  customContent: false,
  animated: true,
  borderRadius: 4,
  type: 'text',
})

// 计算段落行数配置
const paragraphLines = computed(() => {
  const rows = props.paragraphRows || 4
  const widths = Array.isArray(props.paragraphWidth) 
    ? props.paragraphWidth 
    : Array(rows).fill(props.paragraphWidth || '100%')
  
  // 确保数组长度与行数匹配
  return Array.from({ length: rows }, (_, i) => {
    const width = widths[i] || widths[widths.length - 1] || '100%'
    return typeof width === 'number' ? `${width}px` : width
  })
})

// 骨架屏类名
const skeletonClass = computed(() => ({
  'ar-skeleton': true,
  'ar-skeleton--active': props.active,
  'ar-skeleton--animated': props.animated,
  [`ar-skeleton--${props.type}`]: true,
}))

// 骨架屏样式
const skeletonStyle = computed<CSSProperties>(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  borderRadius: typeof props.borderRadius === 'number' 
    ? `${props.borderRadius}px` 
    : props.borderRadius,
}))

// 标题样式
const titleStyle = computed<CSSProperties>(() => ({
  width: typeof props.titleWidth === 'number' 
    ? `${props.titleWidth}px` 
    : props.titleWidth,
  borderRadius: '4px',
}))

// 头像样式
const avatarStyle = computed<CSSProperties>(() => {
  const size = typeof props.avatarSize === 'number' 
    ? `${props.avatarSize}px` 
    : props.avatarSize
  
  return {
    width: size,
    height: size,
    borderRadius: props.avatarShape === 'circle' ? '50%' : '4px',
  }
})

// 按钮样式
const buttonStyle = computed<CSSProperties>(() => {
  const sizes = {
    large: { width: '100px', height: '40px' },
    default: { width: '80px', height: '32px' },
    small: { width: '60px', height: '24px' },
  }
  
  const size = sizes[props.buttonSize] || sizes.default
  return {
    width: size.width,
    height: size.height,
    borderRadius: '4px',
  }
})

// 显示控制
const showTitle = computed(() => props.title && props.active)
const showParagraph = computed(() => props.paragraph && props.active)
const showAvatar = computed(() => props.avatar && props.active)
const showButton = computed(() => props.button && props.active)
</script>

<style scoped>
.ar-skeleton {
  --skeleton-color: #f2f2f2;
  --skeleton-to-color: #e8e8e8;
  --skeleton-animation: skeleton-loading 1.5s ease-in-out infinite;
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-size: 14px;
  line-height: 1.571;
}

/* 动画效果 */
@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.ar-skeleton--animated .ar-skeleton__title,
.ar-skeleton--animated .ar-skeleton__paragraph,
.ar-skeleton--animated .ar-skeleton__avatar,
.ar-skeleton--animated .ar-skeleton__button,
.ar-skeleton--animated .ar-skeleton__custom {
  background: linear-gradient(
    90deg,
    var(--skeleton-color) 25%,
    var(--skeleton-to-color) 37%,
    var(--skeleton-color) 63%
  );
  background-size: 400% 100%;
  animation: var(--skeleton-animation);
}

.ar-skeleton__title,
.ar-skeleton__paragraph,
.ar-skeleton__avatar,
.ar-skeleton__button,
.ar-skeleton__custom {
  background: var(--skeleton-color);
  border-radius: 4px;
}

/* 标题 */
.ar-skeleton__title {
  height: 16px;
  margin-top: 0;
  margin-bottom: 16px;
}

/* 段落 */
.ar-skeleton__paragraph {
  height: 16px;
  margin-bottom: 16px;
}

.ar-skeleton__paragraph:last-child {
  margin-bottom: 0;
}

/* 头像 */
.ar-skeleton__avatar {
  display: inline-block;
  vertical-align: top;
  margin-right: 16px;
}

/* 按钮 */
.ar-skeleton__button {
  display: inline-block;
  margin-top: 16px;
}

/* 不同类型的骨架屏 */
.ar-skeleton--card {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.ar-skeleton--list {
  padding: 12px 0;
}

.ar-skeleton--list .ar-skeleton__avatar {
  margin-right: 12px;
}

.ar-skeleton--list .ar-skeleton__title {
  margin-bottom: 8px;
}

.ar-skeleton--list .ar-skeleton__paragraph {
  margin-bottom: 8px;
}

.ar-skeleton--table {
  width: 100%;
}

.ar-skeleton--table .ar-skeleton__title {
  height: 20px;
  margin-bottom: 12px;
}

.ar-skeleton--table .ar-skeleton__paragraph {
  height: 20px;
  margin-bottom: 8px;
}

/* 深色模式 */
[data-theme='dark'] .ar-skeleton {
  --skeleton-color: #2a2a2a;
  --skeleton-to-color: #3a3a3a;
}

[data-theme='dark'] .ar-skeleton--card {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
</style>