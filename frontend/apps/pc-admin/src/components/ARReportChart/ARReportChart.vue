<template>
  <div
    class="ar-report-chart"
    :style="{ height: height + 'px' }"
  >
    <div
      v-if="title || $slots.extra"
      class="ar-report-chart__header"
    >
      <span class="ar-report-chart__title">{{ title }}</span>
      <slot name="extra" />
    </div>
    <div
      v-show="!isEmpty || loading"
      ref="chartRef"
      class="ar-report-chart__body"
    />
    <div
      v-if="isEmpty && !loading"
      class="ar-report-chart__empty"
    >
      <a-empty
        :description="emptyText"
        :image-style="{ height: '64px' }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

defineOptions({ name: 'ARReportChart' })

const props = withDefaults(defineProps<{
  /** echarts option（series 无数据时自动展示空态） */
  option?: Record<string, any> | null
  /** 图表标题 */
  title?: string
  /** 卡片总高度 px，默认 320 */
  height?: number
  /** 加载中（使用 echarts 内置 loading 遮罩） */
  loading?: boolean
  /** 空态文案 */
  emptyText?: string
}>(), {
  option: null,
  title: '',
  height: 320,
  loading: false,
  emptyText: '暂无数据'
})

const chartRef = ref<HTMLElement | null>(null)
let chart: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

/** 判断 option 的 series 是否含有任何数据点 */
const isEmpty = computed(() => {
  const opt = props.option
  if (!opt || !opt.series) return true
  const seriesArr = Array.isArray(opt.series) ? opt.series : [opt.series]
  return !seriesArr.some((s: any) => {
    const d = s?.data
    return Array.isArray(d) ? d.length > 0 : d !== null && d !== undefined
  })
})

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  syncLoading()
  if (isEmpty.value) {
    chart.clear()
    return
  }
  chart.setOption(props.option as any, true)
  // 容器可能刚从空态切换为可见，下一帧校准尺寸
  nextTick(() => chart?.resize())
}

function syncLoading() {
  if (!chart) return
  if (props.loading) {
    chart.showLoading({
      text: '加载中…',
      color: '#1890ff',
      textColor: '#666',
      maskColor: 'rgba(255, 255, 255, 0.65)'
    })
  } else {
    chart.hideLoading()
  }
}

function handleResize() {
  chart?.resize()
}

watch(() => props.option, renderChart, { deep: true })
watch(() => props.loading, syncLoading)

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
  if (typeof ResizeObserver !== 'undefined' && chartRef.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.ar-report-chart {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ar-report-chart__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ar-report-chart__title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.ar-report-chart__body {
  flex: 1;
  min-height: 0;
}

.ar-report-chart__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
