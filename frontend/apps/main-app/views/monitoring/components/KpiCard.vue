<template>
  <el-card 
    class="kpi-card" 
    :class="[`status-${data.status}`]"
    shadow="hover"
    @click="handleClick"
  >
    <div class="kpi-content">
      <div class="kpi-header">
        <div class="kpi-icon-wrapper" :class="`bg-${data.status}`">
          <el-icon :size="24">
            <component :is="iconComponent" />
          </el-icon>
        </div>
        <div class="kpi-trend" :class="`trend-${data.trend}`">
          <el-icon>
            <ArrowUp v-if="data.trend === 'up'" />
            <ArrowDown v-else-if="data.trend === 'down'" />
            <Minus v-else />
          </el-icon>
          <span>{{ data.trendValue }}</span>
        </div>
      </div>
      
      <div class="kpi-body">
        <div class="kpi-value-wrapper">
          <span class="kpi-value">{{ formattedValue }}</span>
          <span class="kpi-unit">{{ data.unit }}</span>
        </div>
        <div class="kpi-title">{{ data.title }}</div>
        <div v-if="data.description" class="kpi-description">
          {{ data.description }}
        </div>
      </div>
      
      <!-- Sparkline Chart -->
      <div v-if="data.sparklineData && data.sparklineData.length > 0" class="kpi-sparkline">
        <svg :width="sparklineWidth" :height="sparklineHeight" class="sparkline-svg">
          <path
            :d="sparklinePath"
            fill="none"
            :stroke="sparklineColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <defs>
            <linearGradient :id="`gradient-${data.id}`" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" :stop-color="sparklineColor" stop-opacity="0.3" />
              <stop offset="100%" :stop-color="sparklineColor" stop-opacity="0" />
            </linearGradient>
          </defs>
          <path
            :d="`${sparklinePath} L${sparklineWidth},${sparklineHeight} L0,${sparklineHeight} Z`"
            :fill="`url(#gradient-${data.id})`"
          />
        </svg>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  ArrowUp,
  ArrowDown,
  Minus,
  CircleCheck,
  Warning,
  CircleClose,
  InfoFilled,
  Timer,
  TrendCharts,
  DataLine,
  Bell
} from '@element-plus/icons-vue';
import type { KpiCardData } from '../types/monitoring';

interface Props {
  data: KpiCardData;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  click: [id: string];
}>;

// Icon mapping
const iconMap: Record<string, any> = {
  'CircleCheck': CircleCheck,
  'Warning': Warning,
  'CircleClose': CircleClose,
  'InfoFilled': InfoFilled,
  'Timer': Timer,
  'TrendCharts': TrendCharts,
  'DataLine': DataLine,
  'Bell': Bell
};

const iconComponent = computed(() => {
  return iconMap[props.data.icon] || InfoFilled;
});

const formattedValue = computed(() => {
  const val = props.data.value;
  if (typeof val === 'number') {
    if (val >= 1000000) {
      return (val / 1000000).toFixed(1) + 'M';
    } else if (val >= 1000) {
      return (val / 1000).toFixed(1) + 'K';
    }
    return val.toFixed(val % 1 === 0 ? 0 : 1);
  }
  return val;
});

// Sparkline configuration
const sparklineWidth = 120;
const sparklineHeight = 40;

const sparklineColor = computed(() => {
  const colors: Record<string, string> = {
    success: '#67C23A',
    warning: '#E6A23C',
    danger: '#F56C6C',
    info: '#409EFF'
  };
  return colors[props.data.status] || '#409EFF';
});

const sparklinePath = computed(() => {
  const data = props.data.sparklineData;
  if (!data || data.length === 0) return '';
  
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  
  const points = data.map((value, index) => {
    const x = (index / (data.length - 1)) * sparklineWidth;
    const y = sparklineHeight - ((value - min) / range) * sparklineHeight;
    return `${index === 0 ? 'M' : 'L'}${x},${y}`;
  });
  
  return points.join(' ');
});

const handleClick = () => {
  emit('click', props.data.id);
};
</script>

<style scoped>
.kpi-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
}

.kpi-card:hover {
  transform: translateY(-4px);
}

.kpi-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.bg-success {
  background: linear-gradient(135deg, #67C23A 0%, #85CE61 100%);
}

.bg-warning {
  background: linear-gradient(135deg, #E6A23C 0%, #EBB563 100%);
}

.bg-danger {
  background: linear-gradient(135deg, #F56C6C 0%, #F78989 100%);
}

.bg-info {
  background: linear-gradient(135deg, #409EFF 0%, #66B1FF 100%);
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 12px;
}

.trend-up {
  color: #67C23A;
  background-color: rgba(103, 194, 58, 0.1);
}

.trend-down {
  color: #F56C6C;
  background-color: rgba(245, 108, 108, 0.1);
}

.trend-stable {
  color: #909399;
  background-color: rgba(144, 147, 153, 0.1);
}

.kpi-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-value-wrapper {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.kpi-unit {
  font-size: 14px;
  color: #909399;
}

.kpi-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.kpi-description {
  font-size: 12px;
  color: #909399;
}

.kpi-sparkline {
  margin-top: 8px;
}

.sparkline-svg {
  width: 100%;
  overflow: visible;
}

/* Status-specific styles */
.status-success .kpi-value {
  color: #67C23A;
}

.status-warning .kpi-value {
  color: #E6A23C;
}

.status-danger .kpi-value {
  color: #F56C6C;
}

.status-info .kpi-value {
  color: #409EFF;
}
</style>
