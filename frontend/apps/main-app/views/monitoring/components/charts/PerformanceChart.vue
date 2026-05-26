<template>
  <div class="performance-chart">
    <div class="chart-header">
      <div class="chart-title">
        <h3>{{ metric.metricName }}</h3>
        <div class="current-value" :class="getValueClass(metric.currentValue)">
          {{ metric.currentValue }}{{ metric.data[0]?.unit || '' }}
        </div>
      </div>
      <div class="chart-trend">
        <van-tag :type="getTrendType(metric.trend)" size="small">
          <van-icon :name="getTrendIcon(metric.trend)" />
          {{ getTrendText(metric.trend) }}
        </van-tag>
      </div>
    </div>
    
    <div ref="chartRef" class="chart-container"></div>
    
    <div class="chart-footer">
      <div class="threshold-info">
        <span class="threshold-item">
          <span class="threshold-dot warning"></span>
          警告: {{ metric.threshold.warning }}{{ metric.data[0]?.unit || '' }}
        </span>
        <span class="threshold-item">
          <span class="threshold-dot critical"></span>
          严重: {{ metric.threshold.critical }}{{ metric.data[0]?.unit || '' }}
        </span>
      </div>
      <div class="time-range">
        <van-tag type="default" size="small">
          最近24小时
        </van-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { PerformanceData } from '@/types/monitoring'

interface Props {
  metric: PerformanceData
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  height: '200px'
})

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

const getValueClass = (value: number) => {
  if (value >= props.metric.threshold.critical) return 'value-critical'
  if (value >= props.metric.threshold.warning) return 'value-warning'
  return 'value-normal'
}

const getTrendType = (trend: 'up' | 'down' | 'stable') => {
  const typeMap = {
    up: 'danger',
    down: 'success',
    stable: 'primary'
  }
  return typeMap[trend]
}

const getTrendIcon = (trend: 'up' | 'down' | 'stable') => {
  const iconMap = {
    up: 'arrow-up',
    down: 'arrow-down',
    stable: 'minus'
  }
  return iconMap[trend]
}

const getTrendText = (trend: 'up' | 'down' | 'stable') => {
  const textMap = {
    up: '上升',
    down: '下降',
    stable: '稳定'
  }
  return textMap[trend]
}

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '15%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        const date = new Date(params[0].value[0])
        const time = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
        return `${time}<br/>${params[0].marker} ${params[0].seriesName}: ${params[0].value[1]}${props.metric.data[0]?.unit || ''}`
      }
    },
    xAxis: {
      type: 'time',
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: '#dcdee0'
        }
      },
      axisLabel: {
        color: '#969799',
        formatter: (value: number) => {
          const date = new Date(value)
          return `${date.getHours().toString().padStart(2, '0')}:00`
        }
      },
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#969799'
      },
      splitLine: {
        lineStyle: {
          color: '#f2f3f5',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: props.metric.metricName,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 3,
          color: getLineColor()
        },
        itemStyle: {
          color: getLineColor(),
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: getAreaColor(0.3) },
            { offset: 1, color: getAreaColor(0.1) }
          ])
        },
        data: props.metric.data.map(item => [
          new Date(item.timestamp).getTime(),
          item.value
        ])
      },
      // 警告阈值线
      {
        name: '警告阈值',
        type: 'line',
        markLine: {
          silent: true,
          lineStyle: {
            color: '#ff976a',
            width: 1,
            type: 'dashed'
          },
          data: [{
            yAxis: props.metric.threshold.warning,
            label: {
              show: false
            }
          }]
        }
      },
      // 严重阈值线
      {
        name: '严重阈值',
        type: 'line',
        markLine: {
          silent: true,
          lineStyle: {
            color: '#ee0a24',
            width: 1,
            type: 'dashed'
          },
          data: [{
            yAxis: props.metric.threshold.critical,
            label: {
              show: false
            }
          }]
        }
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const getLineColor = () => {
  const value = props.metric.currentValue
  if (value >= props.metric.threshold.critical) return '#ee0a24'
  if (value >= props.metric.threshold.warning) return '#ff976a'
  return '#07c160'
}

const getAreaColor = (opacity: number) => {
  const value = props.metric.currentValue
  if (value >= props.metric.threshold.critical) return `rgba(238, 10, 36, ${opacity})`
  if (value >= props.metric.threshold.warning) return `rgba(255, 151, 106, ${opacity})`
  return `rgba(7, 193, 96, ${opacity})`
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  nextTick(() => {
    initChart()
    window.addEventListener('resize', resizeChart)
  })
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', resizeChart)
})

watch(() => props.metric, () => {
  nextTick(() => {
    initChart()
  })
}, { deep: true })
</script>

<style lang="less" scoped>
.performance-chart {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  .chart-title {
    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .current-value {
      font-size: 24px;
      font-weight: 700;
      margin-top: 4px;
      
      &.value-normal {
        color: #07c160;
      }
      
      &.value-warning {
        color: #ff976a;
      }
      
      &.value-critical {
        color: #ee0a24;
      }
    }
  }
  
  .chart-trend {
    .van-tag {
      font-size: 12px;
      
      .van-icon {
        margin-right: 2px;
      }
    }
  }
}

.chart-container {
  width: 100%;
  height: v-bind(height);
}

.chart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f2f3f5;
  
  .threshold-info {
    display: flex;
    gap: 16px;
    
    .threshold-item {
      font-size: 12px;
      color: #969799;
      display: flex;
      align-items: center;
      
      .threshold-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 4px;
        
        &.warning {
          background-color: #ff976a;
        }
        
        &.critical {
          background-color: #ee0a24;
        }
      }
    }
  }
  
  .time-range {
    .van-tag {
      font-size: 11px;
    }
  }
}
</style>