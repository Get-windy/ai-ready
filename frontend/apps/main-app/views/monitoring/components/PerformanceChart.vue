<template>
  <div class="performance-chart">
    <div class="chart-header">
      <div class="chart-title">
        <el-icon><TrendCharts /></el-icon>
        <span>{{ title }}</span>
      </div>
      <div class="chart-actions">
        <el-radio-group v-model="timeRange" size="small">
          <el-radio-button label="1h">1小时</el-radio-button>
          <el-radio-button label="6h">6小时</el-radio-button>
          <el-radio-button label="24h">24小时</el-radio-button>
          <el-radio-button label="7d">7天</el-radio-button>
        </el-radio-group>
        <el-tooltip content="刷新数据">
          <el-button 
            :icon="Refresh" 
            size="small" 
            circle
            :loading="loading"
            @click="handleRefresh"
          />
        </el-tooltip>
      </div>
    </div>
    
    <div class="chart-content" v-loading="loading">
      <div ref="chartRef" class="chart-container"></div>
    </div>
    
    <div class="chart-footer" v-if="showStats">
      <div class="stat-item">
        <span class="stat-label">平均值</span>
        <span class="stat-value">{{ formatValue(stats.avg) }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">最大值</span>
        <span class="stat-value highlight">{{ formatValue(stats.max) }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">最小值</span>
        <span class="stat-value">{{ formatValue(stats.min) }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">当前值</span>
        <span class="stat-value" :class="getCurrentValueClass">{{ formatValue(stats.current) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { TrendCharts, Refresh } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import type { MetricSeries, TimeRange } from '../types/monitoring';

interface Props {
  title: string;
  series: MetricSeries[];
  loading?: boolean;
  showStats?: boolean;
  chartType?: 'line' | 'bar' | 'area';
  unit?: string;
  thresholds?: {
    warning?: number;
    danger?: number;
  };
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showStats: true,
  chartType: 'line',
  unit: ''
});

const emit = defineEmits<{
  refresh: [timeRange: TimeRange];
  timeRangeChange: [timeRange: TimeRange];
}>();

const chartRef = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const timeRange = ref<TimeRange>('1h');

// Calculate statistics
const stats = computed(() => {
  if (!props.series.length || !props.series[0].data.length) {
    return { avg: 0, max: 0, min: 0, current: 0 };
  }
  
  const allValues = props.series.flatMap(s => s.data.map(d => d.value));
  const current = props.series[0].data[props.series[0].data.length - 1]?.value || 0;
  
  return {
    avg: allValues.reduce((a, b) => a + b, 0) / allValues.length,
    max: Math.max(...allValues),
    min: Math.min(...allValues),
    current
  };
});

const getCurrentValueClass = computed(() => {
  if (!props.thresholds) return '';
  const current = stats.value.current;
  if (props.thresholds.danger && current >= props.thresholds.danger) {
    return 'danger';
  }
  if (props.thresholds.warning && current >= props.thresholds.warning) {
    return 'warning';
  }
  return 'good';
});

const formatValue = (value: number): string => {
  if (value >= 1000000) {
    return (value / 1000000).toFixed(1) + 'M';
  } else if (value >= 1000) {
    return (value / 1000).toFixed(1) + 'K';
  }
  return value.toFixed(value % 1 === 0 ? 0 : 1) + props.unit;
};

const initChart = () => {
  if (!chartRef.value) return;
  
  chartInstance = echarts.init(chartRef.value);
  updateChart();
  
  // Handle resize
  const resizeObserver = new ResizeObserver(() => {
    chartInstance?.resize();
  });
  resizeObserver.observe(chartRef.value);
};

const updateChart = () => {
  if (!chartInstance) return;
  
  const option: echarts.EChartsOption = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#EBEEF5',
      borderWidth: 1,
      textStyle: {
        color: '#303133'
      },
      formatter: (params: any) => {
        let result = `<div style="font-weight: 600; margin-bottom: 8px;">${params[0].axisValue}</div>`;
        params.forEach((param: any) => {
          const color = param.color;
          result += `<div style="display: flex; align-items: center; gap: 8px; margin: 4px 0;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${color};"></span>
            <span>${param.seriesName}: </span>
            <span style="font-weight: 600;">${param.value}${props.unit}</span>
          </div>`;
        });
        return result;
      }
    },
    legend: {
      show: props.series.length > 1,
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: {
        color: '#606266'
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: props.chartType === 'bar',
      data: props.series[0]?.data.map(d => {
        const date = new Date(d.timestamp);
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      }) || [],
      axisLine: {
        lineStyle: {
          color: '#DCDFE6'
        }
      },
      axisLabel: {
        color: '#909399',
        fontSize: 11
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: '#EBEEF5',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#909399',
        fontSize: 11,
        formatter: (value: number) => {
          if (value >= 1000) {
            return (value / 1000).toFixed(1) + 'k';
          }
          return value;
        }
      }
    },
    series: props.series.map((s, index) => ({
      name: s.name,
      type: props.chartType === 'area' ? 'line' : props.chartType,
      smooth: true,
      symbol: 'none',
      lineStyle: {
        width: 2
      },
      areaStyle: props.chartType === 'area' ? {
        opacity: 0.1
      } : undefined,
      data: s.data.map(d => d.value),
      itemStyle: {
        color: s.color || ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C'][index % 4]
      }
    }))
  };
  
  // Add markLine for thresholds
  if (props.thresholds) {
    const markLines = [];
    if (props.thresholds.warning) {
      markLines.push({
        yAxis: props.thresholds.warning,
        lineStyle: {
          color: '#E6A23C',
          type: 'dashed'
        },
        label: {
          formatter: '警告线',
          color: '#E6A23C'
        }
      });
    }
    if (props.thresholds.danger) {
      markLines.push({
        yAxis: props.thresholds.danger,
        lineStyle: {
          color: '#F56C6C',
          type: 'dashed'
        },
        label: {
          formatter: '危险线',
          color: '#F56C6C'
        }
      });
    }
    
    if (markLines.length > 0) {
      (option.series as any[])[0].markLine = {
        silent: true,
        data: markLines
      };
    }
  }
  
  chartInstance.setOption(option);
};

const handleRefresh = () => {
  emit('refresh', timeRange.value);
};

watch(() => props.series, () => {
  nextTick(() => {
    updateChart();
  });
}, { deep: true });

watch(timeRange, (newRange) => {
  emit('timeRangeChange', newRange);
});

onMounted(() => {
  initChart();
});

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
});
</script>

<style scoped>
.performance-chart {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #EBEEF5;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.chart-title .el-icon {
  color: #409EFF;
}

.chart-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-content {
  padding: 20px;
  height: 300px;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.chart-footer {
  display: flex;
  justify-content: space-around;
  padding: 12px 20px;
  border-top: 1px solid #EBEEF5;
  background: #F5F7FA;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.stat-value.highlight {
  color: #409EFF;
}

.stat-value.good {
  color: #67C23A;
}

.stat-value.warning {
  color: #E6A23C;
}

.stat-value.danger {
  color: #F56C6C;
}
</style>