<template>
  <div
    ref="containerRef"
    class="workflow-canvas-container"
    @wheel="handleWheel"
    @mousedown="handleMouseDown"
    @mousemove="handleMouseMove"
    @mouseup="handleMouseUp"
    @mouseleave="handleMouseUp"
  >
    <svg
      ref="svgRef"
      class="workflow-canvas-svg"
      :width="containerSize.width"
      :height="containerSize.height"
    >
      <defs>
        <!-- 网格图案 -->
        <pattern
          :id="gridPatternId"
          :width="config.grid.size"
          :height="config.grid.size"
          patternUnits="userSpaceOnUse"
        >
          <path
            :d="`M ${config.grid.size} 0 L 0 0 0 ${config.grid.size}`"
            fill="none"
            :stroke="config.grid.subColor"
            stroke-width="0.5"
          />
        </pattern>
        <!-- 主网格图案 -->
        <pattern
          :id="mainGridPatternId"
          :width="config.grid.size * 5"
          :height="config.grid.size * 5"
          patternUnits="userSpaceOnUse"
        >
          <rect
            width="100%"
            height="100%"
            fill="url(#{{ gridPatternId }})"
          />
          <path
            :d="`M ${config.grid.size * 5} 0 L 0 0 0 ${config.grid.size * 5}`"
            fill="none"
            :stroke="config.grid.color"
            stroke-width="1"
          />
        </pattern>
      </defs>

      <!-- 背景 -->
      <rect
        width="100%"
        height="100%"
        fill="#f5f5f5"
      />

      <!-- 网格 -->
      <g v-if="config.grid.show">
        <rect
          width="100%"
          height="100%"
          fill="url(#{{ mainGridPatternId }})"
        />
      </g>

      <!-- 变换组（缩放和平移） -->
      <g
        :transform="`translate(${state.offsetX}, ${state.offsetY}) scale(${state.scale})`"
      >
        <!-- 连线层 -->
        <slot name="connections" />

        <!-- 节点层 -->
        <slot name="nodes" />
      </g>
    </svg>

    <!-- 缩放控制 -->
    <div class="canvas-controls">
      <a-space direction="vertical">
        <a-button
          size="small"
          :disabled="state.scale >= config.maxScale"
          @click="zoomIn"
        >
          <template #icon>
            <PlusOutlined />
          </template>
        </a-button>
        <a-button
          size="small"
          :disabled="state.scale <= config.minScale"
          @click="zoomOut"
        >
          <template #icon>
            <MinusOutlined />
          </template>
        </a-button>
        <a-button
          size="small"
          @click="resetView"
        >
          重置
        </a-button>
        <div class="zoom-info">
          {{ Math.round(state.scale * 100) }}%
        </div>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { PlusOutlined, MinusOutlined } from '@ant-design/icons-vue'
import type { CanvasState, CanvasConfig, CanvasSize } from '@/types/workflow/canvas'

/**
 * Props
 */
interface Props {
  /** 画布配置 */
  config?: Partial<CanvasConfig>
}

const props = withDefaults(defineProps<Props>(), {
  config: () => ({})
})

/**
 * Emits
 */
const emit = defineEmits<{
  /** 画布状态变化 */
  stateChange: [state: CanvasState]
  /** 节点选中 */
  nodeSelect: [nodeId: string]
  /** 连线选中 */
  connectionSelect: [connectionId: string]
  /** 连线删除 */
  connectionDelete: [connectionId: string]
  /** 画布点击 */
  canvasClick: [event: MouseEvent]
}>()

/**
 * 状态
 */
const containerRef = ref<HTMLElement | null>(null)
const svgRef = ref<SVGSVGElement | null>(null)

/** 画布尺寸 */
const containerSize = reactive<CanvasSize>({
  width: 2000,
  height: 1500
})

/** 画布状态 */
const state = reactive<CanvasState>({
  scale: 1,
  offsetX: 0,
  offsetY: 0,
  isPanning: false,
  panStartX: 0,
  panStartY: 0
})

/** 默认配置 */
const defaultConfig: CanvasConfig = {
  minScale: 0.1,
  maxScale: 5,
  scaleStep: 0.1,
  grid: {
    show: true,
    size: 20,
    color: '#d9d9d9',
    subSize: 5,
    subColor: '#f0f0f0'
  }
}

/** 合并配置 */
const config = computed<CanvasConfig>(() => ({
  ...defaultConfig,
  ...props.config,
  grid: {
    ...defaultConfig.grid,
    ...(props.config.grid || {})
  }
}))

/** 网格图案ID */
const gridPatternId = computed(() => `workflow-grid-${Date.now()}`)
const mainGridPatternId = computed(() => `workflow-main-grid-${Date.now()}`)

/**
 * 生命周期
 */
onMounted(() => {
  if (containerRef.value) {
    containerSize.width = containerRef.value.clientWidth
    containerSize.height = containerRef.value.clientHeight
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

/**
 * 方法
 */

/**
 * 窗口大小调整
 */
function handleResize(): void {
  if (containerRef.value) {
    containerSize.width = containerRef.value.clientWidth
    containerSize.height = containerRef.value.clientHeight
  }
}

/**
 * 鼠标滚轮缩放
 */
function handleWheel(event: WheelEvent): void {
  event.preventDefault()

  const delta = event.deltaY > 0 ? -config.value.scaleStep : config.value.scaleStep
  const newScale = Math.max(
    config.value.minScale,
    Math.min(config.value.maxScale, state.scale + delta)
  )

  // 以鼠标位置为中心缩放
  if (containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    const mouseX = event.clientX - rect.left
    const mouseY = event.clientY - rect.top

    state.offsetX = mouseX - (mouseX - state.offsetX) * (newScale / state.scale)
    state.offsetY = mouseY - (mouseY - state.offsetY) * (newScale / state.scale)
  }

  state.scale = newScale
  emit('stateChange', { ...state })
}

/**
 * 鼠标按下（开始平移）
 */
function handleMouseDown(event: MouseEvent): void {
  // 如果按住空格键或中键，开始平移
  if (event.button === 1 || (event.button === 0 && event.getModifierState('Space'))) {
    event.preventDefault()
    state.isPanning = true
    state.panStartX = event.clientX - state.offsetX
    state.panStartY = event.clientY - state.offsetY
    containerRef.value?.style.setProperty('cursor', 'grabbing')
  } else if (event.button === 0) {
    // 左键点击画布，触发画布点击事件
    emit('canvasClick', event)
  }
}

/**
 * 鼠标移动（平移）
 */
function handleMouseMove(event: MouseEvent): void {
  if (state.isPanning) {
    state.offsetX = event.clientX - state.panStartX
    state.offsetY = event.clientY - state.panStartY
    emit('stateChange', { ...state })
  }
}

/**
 * 鼠标松开（结束平移）
 */
function handleMouseUp(): void {
  if (state.isPanning) {
    state.isPanning = false
    containerRef.value?.style.removeProperty('cursor')
  }
}

/**
 * 放大
 */
function zoomIn(): void {
  const newScale = Math.min(config.value.maxScale, state.scale + config.value.scaleStep)
  state.scale = newScale
  emit('stateChange', { ...state })
}

/**
 * 缩小
 */
function zoomOut(): void {
  const newScale = Math.max(config.value.minScale, state.scale - config.value.scaleStep)
  state.scale = newScale
  emit('stateChange', { ...state })
}

/**
 * 重置视图
 */
function resetView(): void {
  state.scale = 1
  state.offsetX = 0
  state.offsetY = 0
  emit('stateChange', { ...state })
}

/**
 * 暴露给父组件的方法
 */
defineExpose({
  /** 放大 */
  zoomIn,
  /** 缩小 */
  zoomOut,
  /** 重置视图 */
  resetView,
  /** 获取画布状态 */
  getState: () => ({ ...state }),
  /** 设置画布状态 */
  setState: (newState: Partial<CanvasState>) => {
    Object.assign(state, newState)
    emit('stateChange', { ...state })
  }
})
</script>

<style scoped>
.workflow-canvas-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #f5f5f5;
  cursor: grab;
}

.workflow-canvas-container:active {
  cursor: grabbing;
}

.workflow-canvas-svg {
  display: block;
}

.canvas-controls {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.zoom-info {
  text-align: center;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  margin-top: 4px;
}
</style>