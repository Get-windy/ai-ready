<template>
  <div class="chart-panel" :class="`theme-${theme}`">
    <div class="chart-container">
      <div ref="chartRef" class="echarts-container"></div>
    </div>
    <div class="chart-footer">
      <el-select 
        v-if="chartTypes.length > 1" 
        v-model="selectedChartType" 
        size="small"
        style="width: 120px"
        @change="handleChartTypeChange"
      >
        <el-option 
          v-for="type in chartTypes" 
          :key="type.value" 
          :label="type.label" 
          :value="type.value" 
        />
      </el-select>
      
      <el-select 
        v-if="metrics.length > 0" 
        v-model="selectedMetric" 
        size="small"
        style="width: 150px"
        @change="handleMetricChange"
      >
        <el-option 
          v-for="metric in metrics" 
          :key="metric.value" 
          :label="metric.label" 
          :value="metric.value" 
        />
      </el-select>
      
      <el-select 
        v-model="timeRange" 
        size="small"
        style="width: 120px"
        @change="handleTimeRangeChange"
      >
        <el-option label="最近5分钟" value="5m" />
        <el-option label="最近1小时" value="1h" />
        <el-option label="最近6小时" value="6h" />
        <el-option label="最近24小时" value="24h" />
        <el-option label="最近7天" value="7d" />
      </el-select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import * as ECharts from 'echarts';

// Types
interface Props {
  panel: {
    type: string;
    title: string;
    config: {
      metric?: string;
      chartType?: string;
      series?: Array<{ name: string; data: number[] }>;
      xLabels?: string[];
      options?: Record<string, any>;
      lastUpdated?: string;
    };
  };
  theme: 'light' | 'dark';
}

const props = defineProps<Props>();

// Emits
const emit = defineEmits<{
  (e: 'dataLoaded', data: any): void;
}>();

// State
const chartRef = ref<HTMLElement | null>(null);
let chartInstance: ECharts.ECharts | null = null;

const chartTypes = [
  { label: '折线图', value: 'line' },
  { label: '面积图', value: 'area' },
  { label: '柱状图', value: 'bar' },
  { label: '饼图', value: 'pie' }
];

const metrics = [
  { label: 'CPU使用率', value: 'cpu' },
  { label: '内存使用率', value: 'memory' },
  { label: '磁盘使用率', value: 'disk' },
  { label: '网络流量', value: 'network' },
  { label: '请求次数', value: 'requests' },
  { label: '错误率', value: 'errors' }
];

const selectedChartType = ref(props.panel.config.chartType || 'line');
const selectedMetric = ref(props.panel.config.metric || 'cpu');
const timeRange = ref('1h');

// Computed
const seriesData = computed(() => {
  if (props.panel.config.series) {
    return props.panel.config.series;
  }
  
  // Generate mock data
  const now = Date.now();
  const interval = 5 * 60 * 1000; // 5 minutes
  
  return [{
    name: selectedMetric.value,
    data: Array.from({ length: 20 }, (_, i) => ({
      timestamp: new Date(now - (20 - i) * interval).toISOString(),
      value: Math.random() * 100
    }))
  }];
});

const xLabels = computed(() => {
  if (props.panel.config.xLabels) {
    return props.panel.config.xLabels;
  }
  
  // Generate time labels
  const now = Date.now();
  const interval = 5 * 60 * 1000;
  
  return seriesData.value[0]?.data.map((_, i) => {
    const date = new Date(now - (seriesData.value[0].data.length - i) * interval);
    return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
  }) || [];
});

// Methods
const initChart = () => {
  if (!chartRef.value) return;
  
  chartInstance = ECharts.init(chartRef.value, props.theme === 'dark' ? 'dark' : undefined);
  
  updateChart();
};

const updateChart = () => {
  if (!chartInstance) return;
  
  const rangeMap: Record<string, number> = {
    '5m': 5,
    '1h': 60,
    '6h': 360,
    '24h': 1440,
    '7d': 10080
  };
  
  const minutes = rangeMap[timeRange.value] || 60;
  const pointCount = Math.min(minutes / 5, 100);
  
  // Generate data for selected metric
  const data = Array.from({ length: pointCount }, (_, i) => ({
    timestamp: new Date(Date.now() - (pointCount - i) * 5 * 60 * 1000).toISOString(),
    value: Math.random() * 100
  }));
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const params0 = params[0];
        return `${params0.name}<br/>${params0.seriesName}: ${params0.value.toFixed(2)}`;
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(d => d.timestamp)
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '%',
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed'
        }
      }
    },
    series: [{
      name: selectedMetric.value,
      type: selectedChartType.value,
      data: data.map(d => d.value),
      smooth: true,
      areaStyle: selectedChartType.value === 'area' ? {} : undefined,
      lineStyle: {
        width: 2
      },
      itemStyle: {
        color: '#409EFF'
      }
    }],
    dataZoom: [
      {
        type: 'inside',
        start: 80,
        end: 100
      },
      {
        show: true,
        type: 'slider',
        top: '90%',
        start: 80,
        end: 100
      }
    ]
  };
  
  chartInstance.setOption(option);
};

const handleChartTypeChange = () => {
  updateChart();
};

const handleMetricChange = () => {
  updateChart();
};

const handleTimeRangeChange = () => {
  updateChart();
};

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

// Lifecycle
onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
  
  // Emit data loaded event
  emit('dataLoaded', {
    metric: selectedMetric.value,
    chartType: selectedChartType.value,
    data: seriesData.value
  });
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (chartInstance) {
    chartInstance.dispose();
  }
});

// Watch for prop changes
watch(() => props.theme, () => {
  if (chartInstance) {
    chartInstance.dispose();
    initChart();
  }
});

watch(() => props.panel.config.series, () => {
  updateChart();
});
</script>

<style scoped>
.chart-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chart-container {
  flex: 1;
  min-height: 200px;
  position: relative;
}

.echarts-container {
  width: 100%;
  height: 100%;
}

.chart-footer {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-top: 1px solid var(--el-border-color-light);
}

/* Dark theme */
:deep(.chart-panel.theme-dark) .chart-footer {
  border-top-color: var(--el-border-color-dark);
}
</style>
