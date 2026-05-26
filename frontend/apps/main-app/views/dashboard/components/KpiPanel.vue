<template>
  <div class="kpi-panel" :class="`theme-${theme}`">
    <div class="kpi-content">
      <!-- Value and Unit -->
      <div class="kpi-value">
        <span class="value">{{ formatValue(panel.config.value) }}</span>
        <span class="unit">{{ panel.config.unit || '' }}</span>
      </div>

      <!-- Title -->
      <div class="kpi-title">
        <el-icon v-if="panel.config.icon" :size="16" :color="statusColor">
          <component :is="panel.config.icon" />
        </el-icon>
        <span>{{ panel.title }}</span>
      </div>

      <!-- Metadata -->
      <div class="kpi-meta">
        <div class="kpi-stat">
          <el-icon 
            v-if="panel.config.trend" 
            :class="`trend-icon ${panel.config.trend}`"
          >
            <component :is="trendIcon" />
          </el-icon>
          <span :class="`trend-value ${panel.config.trend}`">
            {{ panel.config.trendValue || '0%' }}
          </span>
        </div>
        
        <div class="kpi-status" :class="`status-${panel.config.status || 'normal'}`">
          <el-icon v-if="statusIcon" :size="14">
            <component :is="statusIcon" />
          </el-icon>
          <span>{{ statusText }}</span>
        </div>
      </div>

      <!-- Sparkline Chart -->
      <div v-if="panel.config.sparklineData && panel.config.sparklineData.length > 0" class="sparkline">
        <div ref="sparklineRef" class="sparkline-chart"></div>
      </div>

      <!-- Description -->
      <div v-if="panel.config.description" class="kpi-description">
        {{ panel.config.description }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import * as ECharts from 'echarts';
import { TrendUp, TrendDown,安定, CircleCheck, CircleClose, CircleWarning } from '@element-plus/icons-vue';

// Types
interface Props {
  panel: {
    type: string;
    title: string;
    config: {
      value?: number | string;
      unit?: string;
      trend?: 'up' | 'down' | 'stable';
      trendValue?: string;
      status?: 'success' | 'warning' | 'danger' | 'normal';
      icon?: string;
      description?: string;
      sparklineData?: number[];
      lastUpdated?: string;
    };
  };
  theme: 'light' | 'dark';
}

const props = defineProps<Props>();

// Computed properties
const trendIcon = computed(() => {
  switch (props.panel.config.trend) {
    case 'up': return TrendUp;
    case 'down': return TrendDown;
    default: return 安定;
  }
});

const statusColor = computed(() => {
  switch (props.panel.config.status) {
    case 'success': return '#67C23A';
    case 'warning': return '#E6A23C';
    case 'danger': return '#F56C6C';
    default: return '#909399';
  }
});

const statusIcon = computed(() => {
  switch (props.panel.config.status) {
    case 'success': return CircleCheck;
    case 'warning': return CircleWarning;
    case 'danger': return CircleClose;
    default: return CircleCheck;
  }
});

const statusText = computed(() => {
  switch (props.panel.config.status) {
    case 'success': return '正常';
    case 'warning': return '预警';
    case 'danger': return '异常';
    default: return '正常';
  }
});

const sparklineRef = ref<HTMLElement | null>(null);
let sparklineChart: ECharts.ECharts | null = null;

// Methods
const formatValue = (value: number | string | undefined): string => {
  if (value === undefined) return 'N/A';
  
  if (typeof value === 'number') {
    if (value >= 1000000) {
      return `${(value / 1000000).toFixed(2)}M`;
    }
    if (value >= 1000) {
      return `${(value / 1000).toFixed(2)}K`;
    }
    return value.toFixed(2);
  }
  
  return String(value);
};

const initSparkline = () => {
  if (!sparklineRef.value) return;
  
  sparklineChart = ECharts.init(sparklineRef.value, theme === 'dark' ? 'dark' : undefined);
  
  const option = {
    grid: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    },
    xAxis: {
      type: 'category',
      show: false
    },
    yAxis: {
      type: 'value',
      show: false
    },
    series: [{
      data: props.panel.config.sparklineData || [0, 1, 2, 3, 4, 5, 6],
      type: 'line',
      smooth: true,
      lineStyle: {
        color: statusColor.value,
        width: 2
      },
      areaStyle: {
        color: statusColor.value,
        opacity: 0.1
      }
    }],
    backgroundColor: 'transparent'
  };
  
  sparklineChart.setOption(option);
};

const updateSparkline = () => {
  if (sparklineChart && props.panel.config.sparklineData) {
    sparklineChart.setOption({
      series: [{
        data: props.panel.config.sparklineData
      }]
    });
  }
};

// Lifecycle
onMounted(() => {
  initSparkline();
});

onUnmounted(() => {
  if (sparklineChart) {
    sparklineChart.dispose();
  }
});

// Watch for data changes
const watch('__v', () => {
  updateSparkline();
});
</script>

<style scoped>
.kpi-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.kpi-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.kpi-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-value .value {
  font-size: 32px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.kpi-value .unit {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.kpi-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.kpi-title .el-icon {
  color: var(--el-color-primary);
}

.kpi-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.trend-icon {
  width: 16px;
  height: 16px;
}

.trend-icon.up {
  color: #67C23A;
}

.trend-icon.down {
  color: #F56C6C;
}

.trend-icon.stable {
  color: #909399;
}

.trend-value {
  font-weight: 500;
}

.trend-value.up {
  color: #67C23A;
}

.trend-value.down {
  color: #F56C6C;
}

.trend-value.stable {
  color: #909399;
}

.kpi-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.kpi-status .el-icon {
  margin-right: 4px;
}

.status-success {
  color: #67C23A;
}

.status-warning {
  color: #E6A23C;
}

.status-danger {
  color: #F56C6C;
}

.kpi-description {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-style: italic;
}

.sparkline {
  height: 60px;
}

.sparkline-chart {
  width: 100%;
  height: 100%;
}

/* Dark theme */
:deep(.kpi-panel.theme-dark) {
  /* Dark theme styles */
}
</style>
