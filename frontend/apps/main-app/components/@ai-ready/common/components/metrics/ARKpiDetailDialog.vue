<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    :fullscreen="fullscreen"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    :show-close="showClose"
    @close="handleClose"
  >
    <!-- 弹窗头部 -->
    <template #header="{ close }">
      <div class="ar-kpi-detail__header">
        <div class="ar-kpi-detail__header-left">
          <el-icon v-if="icon" class="ar-kpi-detail__icon">
            <component :is="icon" />
          </el-icon>
          <span class="ar-kpi-detail__title">{{ title }}</span>
          <el-tag v-if="status" :type="statusType" size="small">
            {{ status }}
          </el-tag>
        </div>
        <div class="ar-kpi-detail__header-right">
          <el-button-group>
            <el-button 
              :icon="fullscreen ? 'FullScreen' : 'FullScreen'" 
              circle
              size="small"
              @click="toggleFullscreen"
            />
            <el-button 
              :icon="Download" 
              circle
              size="small"
              @click="handleExport"
            />
            <el-button 
              :icon="Printer" 
              circle
              size="small"
              @click="handlePrint"
            />
          </el-button-group>
        </div>
      </div>
    </template>
    
    <!-- 弹窗内容 -->
    <div class="ar-kpi-detail__content">
      <!-- 概览区域 -->
      <div class="ar-kpi-detail__overview">
        <div class="ar-kpi-detail__metric">
          <div class="ar-kpi-detail__value">
            {{ formattedValue }}
            <span v-if="unit" class="ar-kpi-detail__unit">{{ unit }}</span>
          </div>
          <div v-if="description" class="ar-kpi-detail__description">
            {{ description }}
          </div>
        </div>
        
        <div class="ar-kpi-detail__stats">
          <div v-if="trendValue !== null" class="ar-kpi-detail__trend">
            <el-icon 
              v-if="trendDirection === 'up'" 
              class="ar-kpi-detail__trend-icon ar-kpi-detail__trend-icon--up"
            >
              <Top />
            </el-icon>
            <el-icon 
              v-else-if="trendDirection === 'down'" 
              class="ar-kpi-detail__trend-icon ar-kpi-detail__trend-icon--down"
            >
              <Bottom />
            </el-icon>
            <span class="ar-kpi-detail__trend-value" :class="trendClass">
              {{ trendValue > 0 ? '+' : '' }}{{ formattedTrendValue }}
            </span>
            <span class="ar-kpi-detail__trend-label">环比</span>
          </div>
          
          <div v-if="comparison" class="ar-kpi-detail__comparison">
            <div class="ar-kpi-detail__comparison-label">{{ comparison.label }}</div>
            <div class="ar-kpi-detail__comparison-value">{{ comparison.value }}</div>
          </div>
        </div>
      </div>
      
      <!-- 时间范围选择器 -->
      <div v-if="showTimeRange" class="ar-kpi-detail__timerange">
        <el-radio-group v-model="selectedTimeRange" size="small">
          <el-radio-button label="1h">1小时</el-radio-button>
          <el-radio-button label="24h">24小时</el-radio-button>
          <el-radio-button label="7d">7天</el-radio-button>
          <el-radio-button label="30d">30天</el-radio-button>
          <el-radio-button label="custom">自定义</el-radio-button>
        </el-radio-group>
        
        <el-date-picker
          v-if="selectedTimeRange === 'custom'"
          v-model="customDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          style="margin-left: 12px; width: 300px;"
        />
      </div>
      
      <!-- 图表区域 -->
      <div class="ar-kpi-detail__chart">
        <div class="ar-kpi-detail__chart-header">
          <div class="ar-kpi-detail__chart-title">历史趋势</div>
          <div class="ar-kpi-detail__chart-actions">
            <el-button-group size="small">
              <el-button 
                :type="chartType === 'line' ? 'primary' : 'default'"
                @click="chartType = 'line'"
              >
                折线图
              </el-button>
              <el-button 
                :type="chartType === 'bar' ? 'primary' : 'default'"
                @click="chartType = 'bar'"
              >
                柱状图
              </el-button>
              <el-button 
                :type="chartType === 'area' ? 'primary' : 'default'"
                @click="chartType = 'area'"
              >
                面积图
              </el-button>
            </el-button-group>
          </div>
        </div>
        
        <div class="ar-kpi-detail__chart-container">
          <!-- 图表占位 -->
          <div v-if="!chartData || chartData.length === 0" class="ar-kpi-detail__chart-empty">
            <el-empty description="暂无图表数据" :image-size="80" />
          </div>
          <div v-else class="ar-kpi-detail__chart-placeholder">
            <!-- 这里可以集成实际的图表库 -->
            <div class="ar-kpi-detail__chart-mock">
              <div class="ar-kpi-detail__chart-mock-title">
                {{ chartType === 'line' ? '折线图' : chartType === 'bar' ? '柱状图' : '面积图' }}
              </div>
              <div class="ar-kpi-detail__chart-mock-graph">
                <div 
                  v-for="(item, index) in mockChartData" 
                  :key="index"
                  class="ar-kpi-detail__chart-mock-bar"
                  :style="{ height: `${item.value}%` }"
                  :title="`${item.label}: ${item.value}`"
                ></div>
              </div>
              <div class="ar-kpi-detail__chart-mock-labels">
                <span 
                  v-for="(item, index) in mockChartData" 
                  :key="index"
                  class="ar-kpi-detail__chart-mock-label"
                >
                  {{ item.label }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 数据表格 -->
      <div class="ar-kpi-detail__table">
        <div class="ar-kpi-detail__table-header">
          <div class="ar-kpi-detail__table-title">详细数据</div>
          <div class="ar-kpi-detail__table-actions">
            <el-button size="small" :icon="Download" @click="handleExportData">
              导出CSV
            </el-button>
            <el-button size="small" :icon="Refresh" @click="handleRefreshData">
              刷新
            </el-button>
          </div>
        </div>
        
        <el-table
          v-if="tableData && tableData.length > 0"
          :data="tableData"
          height="300"
          style="width: 100%"
          stripe
        >
          <el-table-column prop="timestamp" label="时间" width="180" />
          <el-table-column prop="value" label="数值" width="120">
            <template #default="{ row }">
              {{ row.value }} {{ unit }}
            </template>
          </el-table-column>
          <el-table-column prop="change" label="变化量" width="120">
            <template #default="{ row }">
              <span :class="row.change >= 0 ? 'positive' : 'negative'">
                {{ row.change >= 0 ? '+' : '' }}{{ row.change }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="changePercent" label="变化率" width="120">
            <template #default="{ row }">
              <span :class="row.changePercent >= 0 ? 'positive' : 'negative'">
                {{ row.changePercent >= 0 ? '+' : '' }}{{ row.changePercent }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="备注" />
        </el-table>
        
        <div v-else class="ar-kpi-detail__table-empty">
          <el-empty description="暂无表格数据" :image-size="60" />
        </div>
      </div>
      
      <!-- 额外信息 -->
      <div v-if="extraInfo" class="ar-kpi-detail__extra">
        <el-collapse v-model="activeCollapse">
          <el-collapse-item title="额外信息" name="extra">
            <div class="ar-kpi-detail__extra-content">
              <div v-for="(info, key) in extraInfo" :key="key" class="ar-kpi-detail__extra-item">
                <span class="ar-kpi-detail__extra-label">{{ info.label }}:</span>
                <span class="ar-kpi-detail__extra-value">{{ info.value }}</span>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
    
    <!-- 弹窗底部 -->
    <template #footer>
      <div class="ar-kpi-detail__footer">
        <div class="ar-kpi-detail__footer-left">
          <span class="ar-kpi-detail__footer-text">
            最后更新: {{ lastUpdated }}
          </span>
        </div>
        <div class="ar-kpi-detail__footer-right">
          <el-button @click="handleClose">关闭</el-button>
          <el-button type="primary" @click="handleSave">保存设置</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, CSSProperties } from 'vue'
import { 
  Download,
  Printer,
  Top,
  Bottom,
  Refresh,
  FullScreen
} from '@element-plus/icons-vue'

defineOptions({
  name: 'ARKpiDetailDialog',
})

export interface KpiDetailDialogProps {
  /** 弹窗是否可见 */
  modelValue: boolean
  /** 弹窗标题 */
  title: string
  /** 指标值 */
  value: number | string
  /** 数值单位 */
  unit?: string
  /** 标题图标 */
  icon?: string
  /** 描述信息 */
  description?: string
  /** 趋势方向 */
  trendDirection?: 'up' | 'down' | 'neutral'
  /** 趋势值 */
  trendValue?: number
  /** 对比数据 */
  comparison?: {
    label: string
    value: string | number
  }
  /** 状态标签 */
  status?: string
  /** 状态类型 */
  statusType?: 'success' | 'warning' | 'danger' | 'info'
  /** 弹窗宽度 */
  width?: string | number
  /** 是否全屏 */
  fullscreen?: boolean
  /** 是否显示时间范围选择器 */
  showTimeRange?: boolean
  /** 图表数据 */
  chartData?: Array<{ timestamp: string; value: number }>
  /** 表格数据 */
  tableData?: Array<{
    timestamp: string
    value: number
    change: number
    changePercent: number
    note?: string
  }>
  /** 额外信息 */
  extraInfo?: Array<{ label: string; value: string }>
  /** 是否销毁关闭 */
  destroyOnClose?: boolean
  /** 是否点击遮罩关闭 */
  closeOnClickModal?: boolean
  /** 是否显示关闭按钮 */
  showClose?: boolean
  /** 最后更新时间 */
  lastUpdated?: string
}

const props = withDefaults(defineProps<KpiDetailDialogProps>(), {
  modelValue: false,
  title: '',
  value: 0,
  unit: '',
  icon: '',
  description: '',
  trendDirection: 'neutral',
  trendValue: null,
  status: '',
  statusType: 'info',
  width: '800px',
  fullscreen: false,
  showTimeRange: true,
  chartData: () => [],
  tableData: () => [],
  extraInfo: () => [],
  destroyOnClose: true,
  closeOnClickModal: false,
  showClose: true,
  lastUpdated: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
  save: []
  export: [type: string]
  print: []
  'time-range-change': [range: string]
  'chart-type-change': [type: string]
  'refresh-data': []
}>()

// 本地状态
const dialogVisible = ref(props.modelValue)
const localFullscreen = ref(props.fullscreen)
const selectedTimeRange = ref('24h')
const customDateRange = ref([])
const chartType = ref('line')
const activeCollapse = ref(['extra'])

// 监听props变化
watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
})

watch(() => props.fullscreen, (val) => {
  localFullscreen.value = val
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

watch(selectedTimeRange, (val) => {
  if (val !== 'custom') {
    emit('time-range-change', val)
  }
})

watch(chartType, (val) => {
  emit('chart-type-change', val)
})

// 格式化数值
const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  
  const value = Number(props.value)
  
  if (value >= 1000000) {
    return `${(value / 1000000).toFixed(2)}M`
  }
  if (value >= 1000) {
    return `${(value / 1000).toFixed(2)}K`
  }
  
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
})

// 格式化趋势值
const formattedTrendValue = computed(() => {
  if (props.trendValue === null) return ''
  
  return `${Math.abs(props.trendValue).toFixed(2)}%`
})

// 趋势类名
const trendClass = computed(() => ({
  'ar-kpi-detail__trend-value--up': props.trendDirection === 'up',
  'ar-kpi-detail__trend-value--down': props.trendDirection === 'down',
  'ar-kpi-detail__trend-value--neutral': props.trendDirection === 'neutral',
}))

// 模拟图表数据
const mockChartData = computed(() => {
  const data = []
  const hours = ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00']
  
  for (let i = 0; i < 6; i++) {
    data.push({
      label: hours[i],
      value: Math.floor(Math.random() * 60) + 20
    })
  }
  
  return data
})

// 处理关闭
const handleClose = () => {
  dialogVisible.value = false
  emit('close')
}

// 切换全屏
const toggleFullscreen = () => {
  localFullscreen.value = !localFullscreen.value
}

// 处理导出
const handleExport = () => {
  emit('export', 'pdf')
}

// 处理打印
const handlePrint = () => {
  emit('print')
}

// 处理导出数据
const handleExportData = () => {
  emit('export', 'csv')
}

// 处理刷新数据
const handleRefreshData = () => {
  emit('refresh-data')
}

// 处理保存
const handleSave = () => {
  emit('save')
  handleClose()
}
</script>

<style scoped>
.ar-kpi-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.ar-kpi-detail__header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ar-kpi-detail__icon {
  font-size: 20px;
  color: #409eff;
}

.ar-kpi-detail__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.ar-kpi-detail__header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ar-kpi-detail__content {
  padding: 0 4px;
}

/* 概览区域 */
.ar-kpi-detail__overview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border-radius: 8px;
}

.ar-kpi-detail__metric {
  flex: 1;
}

.ar-kpi-detail__value {
  font-size: 48px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.ar-kpi-detail__unit {
  font-size: 24px;
  color: #909399;
  margin-left: 4px;
}

.ar-kpi-detail__description {
  font-size: 14px;
  color: #606266;
  max-width: 400px;
}

.ar-kpi-detail__stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.ar-kpi-detail__trend {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ar-kpi-detail__trend-icon {
  font-size: 18px;
}

.ar-kpi-detail__trend-icon--up {
  color: #67c23a;
}

.ar-kpi-detail__trend-icon--down {
  color: #f56c6c;
}

.ar-kpi-detail__trend-value {
  font-size: 20px;
  font-weight: 600;
}

.ar-kpi-detail__trend-value--up {
  color: #67c23a;
}

.ar-kpi-detail__trend-value--down {
  color: #f56c6c;
}

.ar-kpi-detail__trend-value--neutral {
  color: #909399;
}

.ar-kpi-detail__trend-label {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.ar-kpi-detail__comparison {
  text-align: right;
}

.ar-kpi-detail__comparison-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.ar-kpi-detail__comparison-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 时间范围选择器 */
.ar-kpi-detail__timerange {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

/* 图表区域 */
.ar-kpi-detail__chart {
  margin-bottom: 24px;
}

.ar-kpi-detail__chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ar-kpi-detail__chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.ar-kpi-detail__chart-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ar-kpi-detail__chart-container {
  height: 300px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.ar-kpi-detail__chart-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ar-kpi-detail__chart-placeholder {
  height: 100%;
  padding: 20px;
}

.ar-kpi-detail__chart-mock {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ar-kpi-detail__chart-mock-title {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.ar-kpi-detail__chart-mock-graph {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 20px;
  padding: 0 20px;
}

.ar-kpi-detail__chart-mock-bar {
  width: 40px;
  background: linear-gradient(to top, #409eff, #79bbff);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
  position: relative;
}

.ar-kpi-detail__chart-mock-bar:hover {
  opacity: 0.8;
}

.ar-kpi-detail__chart-mock-labels {
  display: flex;
  justify-content: space-around;
  padding: 8px 20px;
  border-top: 1px solid #f0f0f0;
}

.ar-kpi-detail__chart-mock-label {
  font-size: 12px;
  color: #909399;
}

/* 数据表格 */
.ar-kpi-detail__table {
  margin-bottom: 24px;
}

.ar-kpi-detail__table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ar-kpi-detail__table-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.ar-kpi-detail__table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ar-kpi-detail__table-empty {
  padding: 40px 0;
  border: 1px dashed #f0f0f0;
  border-radius: 8px;
}

/* 额外信息 */
.ar-kpi-detail__extra {
  margin-bottom: 24px;
}

.ar-kpi-detail__extra-content {
  padding: 16px;
}

.ar-kpi-detail__extra-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.ar-kpi-detail__extra-item:last-child {
  border-bottom: none;
}

.ar-kpi-detail__extra-label {
  font-size: 14px;
  color: #606266;
}

.ar-kpi-detail__extra-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

/* 弹窗底部 */
.ar-kpi-detail__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.ar-kpi-detail__footer-left {
  flex: 1;
}

.ar-kpi-detail__footer-text {
  font-size: 12px;
  color: #909399;
}

.ar-kpi-detail__footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 表格样式 */
.positive {
  color: #67c23a;
}

.negative {
  color: #f56c6c;
}

/* 深色模式 */
[data-theme='dark'] .ar-kpi-detail__overview {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(102, 177, 255, 0.05) 100%);
}

[data-theme='dark'] .ar-kpi-detail__value,
[data-theme='dark'] .ar-kpi-detail__title {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-kpi-detail__unit,
[data-theme='dark'] .ar-kpi-detail__description {
  color: #a8a8a8;
}

[data-theme='dark'] .ar-kpi-detail__comparison-value {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-kpi-detail__chart-container {
  background: #2a2a2a;
  border-color: #333;
}

[data-theme='dark'] .ar-kpi-detail__chart-mock-bar {
  background: linear-gradient(to top, #409eff, #66b1ff);
}

[data-theme='dark'] .ar-kpi-detail__timerange {
  border-bottom-color: #333;
}

[data-theme='dark'] .ar-kpi-detail__table-empty {
  border-color: #333;
}

[data-theme='dark'] .ar-kpi-detail__extra-item {
  border-bottom-color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ar-kpi-detail__overview {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .ar-kpi-detail__stats {
    align-items: flex-start;
  }
  
  .ar-kpi-detail__timerange {
    overflow-x: auto;
    padding-bottom: 8px;
  }
  
  .ar-kpi-detail__chart-header,
  .ar-kpi-detail__table-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .ar-kpi-detail__chart-actions,
  .ar-kpi-detail__table-actions {
    justify-content: flex-start;
  }
  
  .ar-kpi-detail__footer {
    flex-direction: column;
    gap: 12px;
  }
  
  .ar-kpi-detail__footer-left {
    text-align: center;
  }
}
</style>