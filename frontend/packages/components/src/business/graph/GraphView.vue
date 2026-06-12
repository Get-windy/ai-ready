<template>
  <div class="ar-graph-view">
    <div class="ar-graph-toolbar">
      <div class="ar-graph-toolbar-left">
        <select
          v-model="selectedChartType"
          class="ar-graph-type-select"
          @change="handleChartTypeChange"
        >
          <option value="bar">柱状图</option>
          <option value="line">折线图</option>
          <option value="pie">饼图</option>
          <option value="area">面积图</option>
        </select>
      </div>
      <div class="ar-graph-toolbar-right">
        <button
          class="ar-graph-refresh-btn"
          @click="handleRefresh"
        >
          <span class="ar-graph-refresh-icon">🔄</span>
          <span class="ar-graph-refresh-text">刷新</span>
        </button>
        <slot name="toolbar-right" />
      </div>
    </div>

    <div class="ar-graph-content">
      <div
        ref="chartContainer"
        class="ar-graph-chart"
      >
        <ARChart
          v-if="chartData"
          :type="selectedChartType"
          :data="chartData"
          :options="chartOptions"
          :width="chartWidth"
          :height="chartHeight"
        />
        <div
          v-else
          class="ar-graph-empty"
        >
          <span class="ar-graph-empty-text">暂无数据</span>
        </div>
      </div>
    </div>

    <div
      v-if="showSummary"
      class="ar-graph-summary"
    >
      <div
        v-for="item in summaryData"
        :key="item.label"
        class="ar-graph-summary-item"
      >
        <span class="ar-graph-summary-label">{{ item.label }}</span>
        <span class="ar-graph-summary-value">{{ item.value }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import ARChart from '../charts/ARChart.vue'

interface ChartDataPoint {
  label: string
  value: number
  [key: string]: any
}

interface SummaryItem {
  label: string
  value: string | number
}

interface Props {
  data?: ChartDataPoint[]
  chartType?: 'bar' | 'line' | 'pie' | 'area'
  showSummary?: boolean
  summaryData?: SummaryItem[]
  chartOptions?: any
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  chartType: 'bar',
  showSummary: true,
  summaryData: () => [],
  chartOptions: () => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'top'
      },
      tooltip: {
        enabled: true
      }
    },
    scales: {
      y: {
        beginAtZero: true
      }
    }
  })
})

const emit = defineEmits<{
  chartTypeChange: [type: string]
  refresh: []
}>()

const selectedChartType = ref(props.chartType)
const chartContainer = ref<HTMLElement | null>(null)
const chartWidth = ref(800)
const chartHeight = ref(400)

watch(() => props.chartType, (val) => {
  selectedChartType.value = val
})

const chartData = computed(() => {
  if (props.data.length === 0) return null

  return {
    labels: props.data.map(item => item.label),
    datasets: [
      {
        label: '数据',
        data: props.data.map(item => item.value),
        backgroundColor: [
          'rgba(64, 158, 255, 0.6)',
          'rgba(103, 194, 58, 0.6)',
          'rgba(230, 162, 60, 0.6)',
          'rgba(245, 108, 108, 0.6)',
          'rgba(144, 147, 153, 0.6)'
        ],
        borderColor: [
          'rgba(64, 158, 255, 1)',
          'rgba(103, 194, 58, 1)',
          'rgba(230, 162, 60, 1)',
          'rgba(245, 108, 108, 1)',
          'rgba(144, 147, 153, 1)'
        ],
        borderWidth: 1
      }
    ]
  } as any
})

const updateChartSize = () => {
  if (chartContainer.value) {
    chartWidth.value = chartContainer.value.clientWidth
    chartHeight.value = chartContainer.value.clientHeight
  }
}

const handleChartTypeChange = () => {
  emit('chartTypeChange', selectedChartType.value)
}

const handleRefresh = () => {
  emit('refresh')
}

onMounted(() => {
  updateChartSize()
  window.addEventListener('resize', updateChartSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateChartSize)
})
</script>

<style lang="scss" scoped>
.ar-graph-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--ar-bg-color, #ffffff);
}

.ar-graph-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-graph-toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-graph-type-select {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  border-radius: var(--ar-border-radius-base, 4px);
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-primary, #303133);
  background-color: var(--ar-bg-color, #ffffff);
  cursor: pointer;

  &:focus {
    border-color: var(--ar-color-primary, #409eff);
    outline: none;
  }
}

.ar-graph-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-graph-refresh-btn {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }
}

.ar-graph-refresh-icon {
  font-size: var(--ar-font-size-base, 14px);
}

.ar-graph-refresh-text {
  font-size: var(--ar-font-size-small, 13px);
}

.ar-graph-content {
  flex: 1;
  padding: var(--ar-spacing-lg, 16px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ar-graph-chart {
  width: 100%;
  height: 100%;
  min-height: 300px;
}

.ar-graph-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.ar-graph-empty-text {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-placeholder, #c0c4cc);
}

.ar-graph-summary {
  display: flex;
  gap: var(--ar-spacing-lg, 16px);
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  border-top: 1px solid var(--ar-border-color-light, #e4e7ed);
  background-color: var(--ar-fill-color-light, #f5f7fa);
}

.ar-graph-summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--ar-spacing-xs, 4px);
}

.ar-graph-summary-label {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-secondary, #909399);
}

.ar-graph-summary-value {
  font-size: var(--ar-font-size-medium, 16px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
}

@media (max-width: 768px) {
  .ar-graph-toolbar {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-graph-content {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-graph-chart {
    min-height: 200px;
  }

  .ar-graph-summary {
    flex-wrap: wrap;
    padding: var(--ar-spacing-sm, 8px);
  }
}
</style>