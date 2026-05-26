<template>
  <div 
    class="metric-card"
    :class="{
      'metric-card--clickable': clickable,
      'metric-card--loading': loading,
      'metric-card--realtime': realtime,
      [`metric-card--status-${status}`]: status
    }"
    @click="handleClick"
  >
    <!-- 加载状态 -->
    <div v-if="loading" class="metric-card__loading">
      <div class="metric-card__loading-spinner"></div>
      <span class="metric-card__loading-text">加载中...</span>
    </div>

    <!-- 正常状态 -->
    <div v-else class="metric-card__content">
      <!-- 头部 -->
      <div class="metric-card__header">
        <div class="metric-card__title">
          <span v-if="icon" class="metric-card__icon">
            <v-icon :name="icon" />
          </span>
          <span class="metric-card__title-text">{{ title }}</span>
        </div>
        <div class="metric-card__actions">
          <button
            v-if="showRefresh"
            class="metric-card__refresh"
            @click.stop="handleRefresh"
            title="刷新数据"
          >
            <v-icon name="refresh" />
          </button>
          <div class="metric-card__status-indicator" :class="`status-${status}`"></div>
        </div>
      </div>

      <!-- 主体 -->
      <div class="metric-card__body">
        <div class="metric-card__value">
          <span class="metric-card__value-number">{{ formattedValue }}</span>
          <span v-if="unit" class="metric-card__value-unit">{{ unit }}</span>
        </div>

        <!-- 进度条 -->
        <div v-if="showProgress" class="metric-card__progress">
          <div class="metric-card__progress-bar">
            <div 
              class="metric-card__progress-fill"
              :style="{
                width: `${progressPercentage}%`,
                'background-color': progressColor
              }"
            ></div>
            <div 
              v-if="threshold"
              class="metric-card__progress-threshold"
              :style="{ left: `${thresholdPercentage}%` }"
            ></div>
          </div>
          <div class="metric-card__progress-labels">
            <span class="metric-card__progress-min">0</span>
            <span class="metric-card__progress-max">{{ max }}</span>
          </div>
        </div>

        <!-- 趋势和描述 -->
        <div class="metric-card__footer">
          <div v-if="trend !== 'stable' && trendValue" class="metric-card__trend">
            <span class="metric-card__trend-icon" :class="`trend-${trend}`">
              <v-icon :name="trendIcon" />
            </span>
            <span class="metric-card__trend-value">{{ trendValue }}</span>
          </div>
          <div v-if="description" class="metric-card__description">
            {{ description }}
          </div>
        </div>
      </div>

      <!-- 实时更新指示器 -->
      <div v-if="realtime" class="metric-card__realtime-indicator">
        <div class="metric-card__realtime-dot"></div>
        <span class="metric-card__realtime-text">实时</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon as VIcon } from 'vant'

interface Props {
  // 基础属性
  title: string
  value: number | string
  unit?: string
  description?: string
  
  // 状态和趋势
  status?: 'healthy' | 'degraded' | 'unhealthy' | 'unknown'
  trend?: 'up' | 'down' | 'stable'
  trendValue?: string
  
  // 配置
  max?: number
  threshold?: number
  icon?: string
  
  // 交互
  clickable?: boolean
  showRefresh?: boolean
  showProgress?: boolean
  loading?: boolean
  realtime?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  status: 'unknown',
  trend: 'stable',
  max: 100,
  threshold: 80,
  clickable: true,
  showRefresh: true,
  showProgress: true,
  loading: false,
  realtime: false
})

const emit = defineEmits<{
  click: []
  refresh: []
}>()

// 计算属性
const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toFixed(1)
  }
  return props.value
})

const progressPercentage = computed(() => {
  if (typeof props.value === 'number') {
    return Math.min((props.value / props.max) * 100, 100)
  }
  return 0
})

const thresholdPercentage = computed(() => {
  return Math.min((props.threshold / props.max) * 100, 100)
})

const progressColor = computed(() => {
  const percentage = progressPercentage.value
  if (percentage >= 90) return 'var(--test-env-error)'
  if (percentage >= 80) return 'var(--test-env-warning)'
  if (percentage >= 60) return 'var(--test-env-info)'
  return 'var(--test-env-success)'
})

const trendIcon = computed(() => {
  switch (props.trend) {
    case 'up': return 'arrow-up'
    case 'down': return 'arrow-down'
    default: return 'minus'
  }
})

// 方法
const handleClick = () => {
  if (props.clickable && !props.loading) {
    emit('click')
  }
}

const handleRefresh = () => {
  if (!props.loading) {
    emit('refresh')
  }
}
</script>

<style scoped>
.metric-card {
  position: relative;
  background: var(--bg-base);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-card);
  transition: var(--transition-status);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  user-select: none;
}

.metric-card--clickable:not(.metric-card--loading):hover {
  border-color: var(--test-env-primary);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
  cursor: pointer;
}

.metric-card--clickable:not(.metric-card--loading):active {
  box-shadow: var(--shadow-card-active);
  transform: translateY(0);
}

/* 状态样式 */
.metric-card--status-healthy {
  border-left: 4px solid var(--status-healthy);
}

.metric-card--status-degraded {
  border-left: 4px solid var(--status-degraded);
}

.metric-card--status-unhealthy {
  border-left: 4px solid var(--status-unhealthy);
  animation: pulse 2s ease-in-out infinite;
}

.metric-card--status-unknown {
  border-left: 4px solid var(--status-unknown);
}

/* 加载状态 */
.metric-card__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: var(--text-secondary);
}

.metric-card__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-light);
  border-top-color: var(--test-env-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-inner-md);
}

.metric-card__loading-text {
  font-size: var(--font-size-xs);
}

/* 头部 */
.metric-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-inner-lg);
}

.metric-card__title {
  display: flex;
  align-items: center;
  gap: var(--spacing-icon-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.metric-card__icon {
  color: var(--test-env-primary);
  font-size: 16px;
}

.metric-card__actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-inner-sm);
}

.metric-card__refresh {
  background: none;
  border: none;
  padding: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: var(--transition-status);
}

.metric-card__refresh:hover {
  color: var(--test-env-primary);
  background: var(--test-env-primary-bg);
}

.metric-card__refresh:active {
  transform: scale(0.95);
}

/* 状态指示器 */
.metric-card__status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.metric-card__status-indicator.status-healthy {
  background-color: var(--status-healthy);
  box-shadow: var(--shadow-status-healthy);
}

.metric-card__status-indicator.status-degraded {
  background-color: var(--status-degraded);
  box-shadow: var(--shadow-status-degraded);
}

.metric-card__status-indicator.status-unhealthy {
  background-color: var(--status-unhealthy);
  box-shadow: var(--shadow-status-unhealthy);
}

.metric-card__status-indicator.status-unknown {
  background-color: var(--status-unknown);
  box-shadow: var(--shadow-status-unknown);
}

/* 主体 */
.metric-card__body {
  text-align: center;
}

.metric-card__value {
  margin-bottom: var(--spacing-inner-lg);
}

.metric-card__value-number {
  font-size: 32px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  line-height: 1;
}

.metric-card__value-unit {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-left: 2px;
}

/* 进度条 */
.metric-card__progress {
  margin-bottom: var(--spacing-inner-lg);
}

.metric-card__progress-bar {
  position: relative;
  height: 6px;
  background: var(--border-lighter);
  border-radius: var(--radius-full);
  margin-bottom: 4px;
  overflow: hidden;
}

.metric-card__progress-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.5s ease-out;
}

.metric-card__progress-threshold {
  position: absolute;
  top: 0;
  width: 2px;
  height: 100%;
  background: var(--test-env-warning);
  transform: translateX(-50%);
}

.metric-card__progress-threshold::after {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 4px solid var(--test-env-warning);
}

.metric-card__progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

/* 底部 */
.metric-card__footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-inner-md);
}

.metric-card__trend {
  display: flex;
  align-items: center;
  gap: 2px;
}

.metric-card__trend-icon {
  font-size: 12px;
}

.metric-card__trend-icon.trend-up {
  color: var(--test-env-error);
}

.metric-card__trend-icon.trend-down {
  color: var(--test-env-success);
}

.metric-card__trend-icon.trend-stable {
  color: var(--text-secondary);
}

.metric-card__trend-value {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.metric-card__description {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 实时指示器 */
.metric-card__realtime-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: var(--test-env-primary);
  animation: fadeInOut 2s ease-in-out infinite;
}

.metric-card__realtime-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--test-env-primary);
}

.metric-card__realtime-text {
  font-weight: var(--font-weight-medium);
}

/* 动画 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 响应式 */
@media (max-width: 768px) {
  .metric-card {
    padding: var(--spacing-inner-lg);
  }
  
  .metric-card__value-number {
    font-size: 28px;
  }
  
  .metric-card__description {
    max-width: 150px;
  }
}

@media (max-width: 480px) {
  .metric-card__value-number {
    font-size: 24px;
  }
  
  .metric-card__footer {
    flex-direction: column;
    gap: var(--spacing-inner-sm);
  }
}
</style>