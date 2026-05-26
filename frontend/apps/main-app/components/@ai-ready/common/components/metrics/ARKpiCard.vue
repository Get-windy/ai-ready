<template>
  <div :class="cardClass" :style="cardStyle" @click="handleClick">
    <!-- 卡片头部 -->
    <div class="ar-kpi-card__header">
      <div class="ar-kpi-card__title-wrapper">
        <!-- 标题图标 -->
        <el-icon v-if="icon" class="ar-kpi-card__icon">
          <component :is="icon" />
        </el-icon>
        
        <!-- 标题 -->
        <div class="ar-kpi-card__title" :title="title">
          {{ title }}
        </div>
        
        <!-- 帮助提示 -->
        <el-tooltip 
          v-if="tooltip" 
          :content="tooltip" 
          placement="top"
        >
          <el-icon class="ar-kpi-card__help">
            <QuestionFilled />
          </el-icon>
        </el-tooltip>
      </div>
      
      <!-- 菜单按钮 -->
      <el-dropdown 
        v-if="showMenu" 
        trigger="click" 
        @command="handleMenuCommand"
      >
        <el-icon class="ar-kpi-card__menu">
          <More />
        </el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="details">查看详情</el-dropdown-item>
            <el-dropdown-item command="export">导出数据</el-dropdown-item>
            <el-dropdown-item command="refresh" divided>刷新数据</el-dropdown-item>
            <el-dropdown-item command="configure">配置</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    
    <!-- 卡片内容 -->
    <div class="ar-kpi-card__content">
      <!-- 主要指标 -->
      <div class="ar-kpi-card__metric">
        <div class="ar-kpi-card__value" :style="valueStyle">
          {{ formattedValue }}
        </div>
        <div v-if="unit" class="ar-kpi-card__unit">
          {{ unit }}
        </div>
      </div>
      
      <!-- 趋势指示器 -->
      <div v-if="showTrend" class="ar-kpi-card__trend">
        <el-icon 
          v-if="trendDirection === 'up'" 
          class="ar-kpi-card__trend-icon ar-kpi-card__trend-icon--up"
        >
          <Top />
        </el-icon>
        <el-icon 
          v-else-if="trendDirection === 'down'" 
          class="ar-kpi-card__trend-icon ar-kpi-card__trend-icon--down"
        >
          <Bottom />
        </el-icon>
        
        <span 
          v-if="trendValue !== null" 
          class="ar-kpi-card__trend-value"
          :class="trendClass"
        >
          {{ trendValue > 0 ? '+' : '' }}{{ formattedTrendValue }}
        </span>
      </div>
    </div>
    
    <!-- 卡片底部 -->
    <div class="ar-kpi-card__footer">
      <!-- 对比数据 -->
      <div v-if="comparison" class="ar-kpi-card__comparison">
        <span class="ar-kpi-card__comparison-label">{{ comparison.label }}:</span>
        <span class="ar-kpi-card__comparison-value">{{ comparison.value }}</span>
      </div>
      
      <!-- 时间范围 -->
      <div v-if="timeRange" class="ar-kpi-card__timerange">
        {{ timeRange }}
      </div>
      
      <!-- 状态指示器 -->
      <div v-if="status" class="ar-kpi-card__status">
        <el-tag 
          :type="statusType" 
          size="small" 
          :effect="statusEffect"
        >
          {{ status }}
        </el-tag>
      </div>
    </div>
    
    <!-- 点击效果遮罩 -->
    <div v-if="clickable" class="ar-kpi-card__overlay">
      <span class="ar-kpi-card__overlay-text">点击查看详情</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties } from 'vue'
import { 
  QuestionFilled, 
  More,
  Top,
  Bottom
} from '@element-plus/icons-vue'

defineOptions({
  name: 'ARKpiCard',
})

export interface KpiCardProps {
  /** 卡片标题 */
  title: string
  /** 指标值 */
  value: number | string
  /** 数值单位 */
  unit?: string
  /** 标题图标 */
  icon?: string
  /** 工具提示 */
  tooltip?: string
  /** 趋势方向 */
  trendDirection?: 'up' | 'down' | 'neutral'
  /** 趋势值 */
  trendValue?: number
  /** 对比数据 */
  comparison?: {
    label: string
    value: string | number
  }
  /** 时间范围 */
  timeRange?: string
  /** 状态标签 */
  status?: string
  /** 状态类型 */
  statusType?: 'success' | 'warning' | 'danger' | 'info'
  /** 状态效果 */
  statusEffect?: 'light' | 'dark' | 'plain'
  /** 是否可点击 */
  clickable?: boolean
  /** 是否显示菜单 */
  showMenu?: boolean
  /** 是否显示趋势 */
  showTrend?: boolean
  /** 数值格式 */
  format?: 'number' | 'currency' | 'percent' | 'duration'
  /** 货币符号 */
  currencySymbol?: string
  /** 小数位数 */
  decimals?: number
  /** 是否紧凑模式 */
  compact?: boolean
  /** 是否加载中 */
  loading?: boolean
  /** 自定义类名 */
  customClass?: string
  /** 自定义样式 */
  customStyle?: CSSProperties
}

const props = withDefaults(defineProps<KpiCardProps>(), {
  title: '',
  value: 0,
  unit: '',
  icon: '',
  tooltip: '',
  trendDirection: 'neutral',
  trendValue: null,
  timeRange: '',
  status: '',
  statusType: 'info',
  statusEffect: 'light',
  clickable: true,
  showMenu: true,
  showTrend: true,
  format: 'number',
  currencySymbol: '¥',
  decimals: 2,
  compact: false,
  loading: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
  'menu-command': [command: string]
  'trend-click': [direction: string]
}>()

// 格式化数值
const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  
  const value = Number(props.value)
  
  switch (props.format) {
    case 'currency':
      return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'CNY',
        minimumFractionDigits: props.decimals,
        maximumFractionDigits: props.decimals,
      }).format(value)
    
    case 'percent':
      return new Intl.NumberFormat('zh-CN', {
        style: 'percent',
        minimumFractionDigits: props.decimals,
        maximumFractionDigits: props.decimals,
      }).format(value / 100)
    
    case 'duration':
      if (value < 60) return `${value}秒`
      if (value < 3600) return `${(value / 60).toFixed(1)}分钟`
      if (value < 86400) return `${(value / 3600).toFixed(1)}小时`
      return `${(value / 86400).toFixed(1)}天`
    
    default: // number
      if (value >= 1000000) {
        return `${(value / 1000000).toFixed(props.decimals)}M`
      }
      if (value >= 1000) {
        return `${(value / 1000).toFixed(props.decimals)}K`
      }
      return new Intl.NumberFormat('zh-CN', {
        minimumFractionDigits: props.decimals,
        maximumFractionDigits: props.decimals,
      }).format(value)
  }
})

// 格式化趋势值
const formattedTrendValue = computed(() => {
  if (props.trendValue === null) return ''
  
  if (props.format === 'percent') {
    return `${(props.trendValue).toFixed(props.decimals)}%`
  }
  
  if (Math.abs(props.trendValue) >= 1000000) {
    return `${(props.trendValue / 1000000).toFixed(props.decimals)}M`
  }
  
  if (Math.abs(props.trendValue) >= 1000) {
    return `${(props.trendValue / 1000).toFixed(props.decimals)}K`
  }
  
  return props.trendValue.toFixed(props.decimals)
})

// 卡片类名
const cardClass = computed(() => ({
  'ar-kpi-card': true,
  'ar-kpi-card--clickable': props.clickable,
  'ar-kpi-card--compact': props.compact,
  'ar-kpi-card--loading': props.loading,
  [`ar-kpi-card--trend-${props.trendDirection}`]: props.trendDirection !== 'neutral',
  [props.customClass || '']: !!props.customClass,
}))

// 卡片样式
const cardStyle = computed<CSSProperties>(() => ({
  cursor: props.clickable ? 'pointer' : 'default',
  ...props.customStyle,
}))

// 数值样式
const valueStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {}
  
  // 根据数值大小调整字体大小
  const valueStr = String(props.value)
  if (valueStr.length > 6) {
    style.fontSize = '24px'
  } else if (valueStr.length > 4) {
    style.fontSize = '28px'
  } else {
    style.fontSize = '32px'
  }
  
  return style
})

// 趋势类名
const trendClass = computed(() => ({
  'ar-kpi-card__trend-value--up': props.trendDirection === 'up',
  'ar-kpi-card__trend-value--down': props.trendDirection === 'down',
  'ar-kpi-card__trend-value--neutral': props.trendDirection === 'neutral',
}))

// 处理点击
const handleClick = (event: MouseEvent) => {
  if (props.clickable) {
    emit('click', event)
  }
}

// 处理菜单命令
const handleMenuCommand = (command: string) => {
  emit('menu-command', command)
}

// 获取状态标签类型
const computedStatusType = computed(() => {
  if (props.statusType) return props.statusType
  
  // 根据数值自动判断状态
  const value = Number(props.value)
  if (props.trendDirection === 'up') {
    return value > 0 ? 'success' : 'danger'
  }
  if (props.trendDirection === 'down') {
    return value > 0 ? 'danger' : 'success'
  }
  return 'info'
})
</script>

<style scoped>
.ar-kpi-card {
  position: relative;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
}

.ar-kpi-card--compact {
  padding: 16px;
}

.ar-kpi-card--clickable:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px 0 rgba(0, 0, 0, 0.15);
}

.ar-kpi-card--loading {
  opacity: 0.7;
  pointer-events: none;
}

/* 卡片头部 */
.ar-kpi-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.ar-kpi-card__title-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.ar-kpi-card__icon {
  font-size: 18px;
  color: #409eff;
  flex-shrink: 0;
}

.ar-kpi-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.ar-kpi-card--compact .ar-kpi-card__title {
  font-size: 14px;
}

.ar-kpi-card__help {
  font-size: 14px;
  color: #c0c4cc;
  cursor: help;
  flex-shrink: 0;
}

.ar-kpi-card__menu {
  font-size: 18px;
  color: #c0c4cc;
  cursor: pointer;
  transition: color 0.3s;
  flex-shrink: 0;
}

.ar-kpi-card__menu:hover {
  color: #909399;
}

/* 卡片内容 */
.ar-kpi-card__content {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.ar-kpi-card__metric {
  display: flex;
  align-items: baseline;
  gap: 4px;
  min-width: 0;
}

.ar-kpi-card__value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ar-kpi-card--compact .ar-kpi-card__value {
  font-size: 24px;
}

.ar-kpi-card__unit {
  font-size: 14px;
  color: #909399;
  white-space: nowrap;
}

/* 趋势指示器 */
.ar-kpi-card__trend {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.ar-kpi-card__trend-icon {
  font-size: 14px;
}

.ar-kpi-card__trend-icon--up {
  color: #67c23a;
}

.ar-kpi-card__trend-icon--down {
  color: #f56c6c;
}

.ar-kpi-card__trend-value {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.ar-kpi-card__trend-value--up {
  color: #67c23a;
}

.ar-kpi-card__trend-value--down {
  color: #f56c6c;
}

.ar-kpi-card__trend-value--neutral {
  color: #909399;
}

/* 卡片底部 */
.ar-kpi-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.ar-kpi-card__comparison {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ar-kpi-card__comparison-label {
  opacity: 0.8;
}

.ar-kpi-card__comparison-value {
  font-weight: 600;
  color: #303133;
}

.ar-kpi-card__timerange {
  white-space: nowrap;
  opacity: 0.8;
}

.ar-kpi-card__status {
  flex-shrink: 0;
}

/* 点击遮罩 */
.ar-kpi-card__overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(64, 158, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}

.ar-kpi-card--clickable:hover .ar-kpi-card__overlay {
  opacity: 1;
}

.ar-kpi-card__overlay-text {
  padding: 8px 16px;
  background: rgba(64, 158, 255, 0.9);
  color: white;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

/* 趋势样式装饰 */
.ar-kpi-card--trend-up {
  border-top: 3px solid #67c23a;
}

.ar-kpi-card--trend-down {
  border-top: 3px solid #f56c6c;
}

/* 深色模式 */
[data-theme='dark'] .ar-kpi-card {
  background: #1f1f1f;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}

[data-theme='dark'] .ar-kpi-card--clickable:hover {
  box-shadow: 0 8px 24px 0 rgba(0, 0, 0, 0.5);
}

[data-theme='dark'] .ar-kpi-card__title {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-kpi-card__value {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-kpi-card__unit {
  color: #a8a8a8;
}

[data-theme='dark'] .ar-kpi-card__comparison-value {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-kpi-card__footer {
  border-top-color: #333;
}

[data-theme='dark'] .ar-kpi-card__overlay {
  background: rgba(64, 158, 255, 0.2);
}

[data-theme='dark'] .ar-kpi-card__overlay-text {
  background: rgba(64, 158, 255, 0.8);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ar-kpi-card {
    padding: 16px;
  }
  
  .ar-kpi-card__content {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .ar-kpi-card__footer {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .ar-kpi-card__timerange {
    order: 1;
    width: 100%;
    text-align: center;
  }
  
  .ar-kpi-card__comparison {
    order: 2;
    flex: 1;
  }
  
  .ar-kpi-card__status {
    order: 3;
  }
}

/* 加载动画 */
@keyframes kpi-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.ar-kpi-card--loading .ar-kpi-card__value,
.ar-kpi-card--loading .ar-kpi-card__title {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: kpi-loading 1.5s ease-in-out infinite;
  color: transparent;
  border-radius: 4px;
}

[data-theme='dark'] .ar-kpi-card--loading .ar-kpi-card__value,
[data-theme='dark'] .ar-kpi-card--loading .ar-kpi-card__title {
  background: linear-gradient(90deg, #2a2a2a 25%, #3a3a3a 50%, #2a2a2a 75%);
}
</style>