<template>
  <div :class="loadingClass" :style="loadingStyle">
    <!-- 旋转加载器 -->
    <div v-if="type === 'spinner'" class="ar-loading__spinner" :style="spinnerStyle">
      <div 
        v-for="n in spinnerBars" 
        :key="n"
        class="ar-loading__spinner-bar"
        :style="{
          animationDelay: `${(n - 1) * 0.1}s`,
          transform: `rotate(${(n - 1) * (360 / spinnerBars)}deg)`
        }"
      ></div>
    </div>
    
    <!-- 进度条加载器 -->
    <div v-else-if="type === 'progress'" class="ar-loading__progress">
      <div class="ar-loading__progress-track" :style="trackStyle">
        <div class="ar-loading__progress-bar" :style="progressBarStyle"></div>
      </div>
      <div v-if="showPercent" class="ar-loading__progress-text">
        {{ progressPercent }}%
      </div>
    </div>
    
    <!-- 点状加载器 -->
    <div v-else-if="type === 'dots'" class="ar-loading__dots">
      <div 
        v-for="n in dotsCount" 
        :key="n"
        class="ar-loading__dot"
        :style="{ animationDelay: `${(n - 1) * 0.2}s` }"
      ></div>
    </div>
    
    <!-- 骨架屏加载器 -->
    <div v-else-if="type === 'skeleton'" class="ar-loading__skeleton">
      <ARSkeleton :type="skeletonType" :animated="true" />
    </div>
    
    <!-- 加载文本 -->
    <div v-if="text" class="ar-loading__text">
      {{ text }}
    </div>
    
    <!-- 子内容 -->
    <div v-if="$slots.default" class="ar-loading__content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties } from 'vue'
import ARSkeleton from './ARSkeleton.vue'

defineOptions({
  name: 'ARLoading',
})

export interface LoadingProps {
  /** 加载器类型 */
  type?: 'spinner' | 'progress' | 'dots' | 'skeleton'
  /** 加载器大小 */
  size?: 'small' | 'default' | 'large' | number
  /** 加载器颜色 */
  color?: string
  /** 加载文本 */
  text?: string
  /** 是否全屏显示 */
  fullscreen?: boolean
  /** 是否显示遮罩 */
  overlay?: boolean
  /** 遮罩颜色 */
  overlayColor?: string
  /** 遮罩透明度 */
  overlayOpacity?: number
  /** 旋转器条数 */
  spinnerBars?: number
  /** 点状加载器点数 */
  dotsCount?: number
  /** 进度条进度（0-100） */
  progress?: number
  /** 是否显示百分比 */
  showPercent?: boolean
  /** 骨架屏类型 */
  skeletonType?: 'text' | 'card' | 'list' | 'table'
  /** 自定义类名 */
  customClass?: string
  /** 是否内联显示 */
  inline?: boolean
  /** 是否垂直排列 */
  vertical?: boolean
}

const props = withDefaults(defineProps<LoadingProps>(), {
  type: 'spinner',
  size: 'default',
  color: '#409eff',
  text: '',
  fullscreen: false,
  overlay: false,
  overlayColor: 'rgba(255, 255, 255, 0.9)',
  overlayOpacity: 0.9,
  spinnerBars: 8,
  dotsCount: 3,
  progress: 0,
  showPercent: false,
  skeletonType: 'text',
  inline: false,
  vertical: false,
})

// 加载器类名
const loadingClass = computed(() => ({
  'ar-loading': true,
  [`ar-loading--${props.type}`]: true,
  'ar-loading--fullscreen': props.fullscreen,
  'ar-loading--overlay': props.overlay,
  'ar-loading--inline': props.inline,
  'ar-loading--vertical': props.vertical,
  [props.customClass || '']: !!props.customClass,
}))

// 加载器样式
const loadingStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {}
  
  if (props.fullscreen) {
    style.position = 'fixed'
    style.top = '0'
    style.left = '0'
    style.right = '0'
    style.bottom = '0'
    style.zIndex = '9999'
  }
  
  if (props.overlay || props.fullscreen) {
    style.backgroundColor = props.overlayColor
    style.opacity = props.overlayOpacity
  }
  
  return style
})

// 旋转器样式
const spinnerStyle = computed<CSSProperties>(() => {
  const sizeMap = {
    small: 24,
    default: 32,
    large: 48,
  }
  
  const size = typeof props.size === 'number' 
    ? props.size 
    : sizeMap[props.size] || sizeMap.default
  
  return {
    width: `${size}px`,
    height: `${size}px`,
  }
})

// 进度条轨道样式
const trackStyle = computed<CSSProperties>(() => {
  const sizeMap = {
    small: { height: '4px', borderRadius: '2px' },
    default: { height: '6px', borderRadius: '3px' },
    large: { height: '8px', borderRadius: '4px' },
  }
  
  const size = typeof props.size === 'string' 
    ? sizeMap[props.size] || sizeMap.default
    : sizeMap.default
  
  return {
    height: size.height,
    borderRadius: size.borderRadius,
    backgroundColor: 'rgba(0, 0, 0, 0.1)',
  }
})

// 进度条样式
const progressBarStyle = computed<CSSProperties>(() => {
  const sizeMap = {
    small: { height: '4px', borderRadius: '2px' },
    default: { height: '6px', borderRadius: '3px' },
    large: { height: '8px', borderRadius: '4px' },
  }
  
  const size = typeof props.size === 'string' 
    ? sizeMap[props.size] || sizeMap.default
    : sizeMap.default
  
  return {
    width: `${Math.min(100, Math.max(0, props.progress))}%`,
    height: size.height,
    borderRadius: size.borderRadius,
    backgroundColor: props.color,
    transition: 'width 0.3s ease',
  }
})

// 进度百分比
const progressPercent = computed(() => Math.round(props.progress))

// 点状加载器样式
const dotsStyle = computed<CSSProperties>(() => {
  const sizeMap = {
    small: 4,
    default: 6,
    large: 8,
  }
  
  const size = typeof props.size === 'number' 
    ? props.size / 4 
    : sizeMap[props.size] || sizeMap.default
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    backgroundColor: props.color,
  }
})
</script>

<style scoped>
.ar-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
  transition: opacity 0.3s;
}

.ar-loading--inline {
  display: inline-flex;
}

.ar-loading--vertical {
  flex-direction: column;
}

.ar-loading--overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.9);
}

/* 旋转器 */
.ar-loading__spinner {
  position: relative;
  display: inline-block;
}

.ar-loading__spinner-bar {
  position: absolute;
  top: 0;
  left: 50%;
  width: 2px;
  height: 25%;
  margin-left: -1px;
  border-radius: 1px;
  background: var(--loading-color, #409eff);
  opacity: 0.3;
  animation: spinner-bar 1.2s linear infinite;
}

@keyframes spinner-bar {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0.3;
  }
}

/* 进度条 */
.ar-loading__progress {
  width: 200px;
  max-width: 100%;
}

.ar-loading__progress-track {
  width: 100%;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.1);
}

.ar-loading__progress-bar {
  height: 100%;
  background: var(--loading-color, #409eff);
  transition: width 0.3s ease;
}

.ar-loading__progress-text {
  margin-top: 8px;
  font-size: 12px;
  color: var(--loading-color, #409eff);
  text-align: center;
}

/* 点状加载器 */
.ar-loading__dots {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.ar-loading__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--loading-color, #409eff);
  opacity: 0.3;
  animation: dot-pulse 1.4s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

/* 骨架屏加载器 */
.ar-loading__skeleton {
  width: 100%;
  max-width: 400px;
}

/* 文本 */
.ar-loading__text {
  margin-top: 12px;
  font-size: 14px;
  color: var(--loading-color, #409eff);
  text-align: center;
}

/* 内容区域 */
.ar-loading__content {
  margin-top: 16px;
}

/* 深色模式 */
[data-theme='dark'] .ar-loading--overlay {
  background: rgba(0, 0, 0, 0.7);
}

[data-theme='dark'] .ar-loading__progress-track {
  background: rgba(255, 255, 255, 0.1);
}

[data-theme='dark'] .ar-loading__text {
  color: rgba(255, 255, 255, 0.85);
}
</style>