# AI-Ready 数据可视化组件

## 概述

AI-Ready 数据可视化组件是一套基于 Vue 3 + SVG 的轻量级图表组件库，支持柱状图、折线图、饼图、雷达图四种图表类型。无需安装额外的图表库，开箱即用。

## 特性

- 🎨 **轻量级**：基于原生 SVG 实现，无第三方依赖
- 📦 **开箱即用**：四种基础图表类型，配置简单
- 🎭 **主题支持**：预设多种配色方案
- 🖱️ **交互支持**：支持点击、悬停、缩放、筛选
- 📥 **导出支持**：支持导出 PNG、SVG、CSV、JSON

## 安装

```bash
# 无需安装，组件已内置
# 如需更新组件，请参考 smart-admin-web/src/components/Charts
```

## 快速开始

```vue
<template>
  <BarChart 
    :data="chartData" 
    title="销售统计"
    :width="600"
    :height="400"
    @click="handleClick"
  />
</template>

<script setup lang="ts">
import { BarChart } from '@/components/Charts'

const chartData = [
  { name: '一月', value: 100 },
  { name: '二月', value: 200 },
  { name: '三月', value: 150 }
]

const handleClick = (data) => {
  console.log('点击了:', data)
}
</script>
```

## 组件列表

### 1. 柱状图 (BarChart)

```vue
<BarChart
  :data="data"
  title="标题"
  :width="400"
  :height="300"
  :colors="['#5470c6', '#91cc75']"
  :show-legend="true"
  @click="onClick"
  @hover="onHover"
/>
```

**属性**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 图表数据 |
| title | string | '' | 图表标题 |
| width | number | 400 | 图表宽度 |
| height | number | 300 | 图表高度 |
| colors | string[] | [] | 自定义颜色 |
| showLegend | boolean | true | 是否显示图例 |
| showValue | boolean | false | 是否显示数值 |

### 2. 折线图 (LineChart)

```vue
<LineChart
  :data="singleData"
  :series="multiData"
  title="趋势图"
  :smooth="true"
  @click="onClick"
/>
```

**属性**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 单系列数据 |
| series | ChartSeries[] | [] | 多系列数据 |
| smooth | boolean | false | 是否平滑曲线 |

### 3. 饼图 (PieChart)

```vue
<PieChart
  :data="data"
  title="占比分布"
  :inner-radius="0.5"
  :show-label="true"
  @click="onClick"
/>
```

**属性**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 图表数据 |
| innerRadius | number | 0 | 环形图半径 (0-1) |
| showLabel | boolean | true | 是否显示标签 |

### 4. 雷达图 (RadarChart)

```vue
<RadarChart
  :series="multiData"
  title="多维分析"
  @click="onClick"
/>
```

**属性**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 单系列数据 |
| series | ChartSeries[] | [] | 多系列数据 |

## 统一图表组件

使用 `Chart` 组件可以根据 type 自动渲染对应图表：

```vue
<template>
  <Chart
    type="bar"
    :data="data"
    title="动态图表"
  />
  <!-- 改为 line 会渲染折线图 -->
  <Chart
    type="line"
    :data="data"
  />
</template>

<script setup>
import { Chart } from '@/components/Charts'
</script>
```

## 类型定义

```typescript
// 图表数据类型
interface ChartDataPoint {
  name: string      // 数据名称
  value: number     // 数值
  color?: string    // 自定义颜色
  category?: string // 分类
  extra?: Record<string, any> // 扩展数据
}

// 图表系列（用于多系列图表）
interface ChartSeries {
  name: string
  data: ChartDataPoint[]
  color?: string
}

// 图表配置
interface ChartConfig {
  title?: string
  subTitle?: string
  width?: number
  height?: number
  theme?: 'default' | 'dark' | 'vintage' | 'colorful'
  colors?: string[]
  animation?: boolean
  grid?: { top?: number; right?: number; bottom?: number; left?: number }
  tooltip?: { show?: boolean; trigger?: 'item' | 'axis' }
  legend?: { show?: boolean; position?: 'top' | 'bottom' | 'left' | 'right' }
}
```

## 主题颜色

组件内置四种主题：

```typescript
import { ThemeColors } from '@/components/Charts'

ThemeColors.default   // 默认主题
ThemeColors.dark      // 暗色主题
ThemeColors.vintage   // 复古主题
ThemeColors.colorful  // 彩色主题
```

使用主题：
```vue
<BarChart
  :data="data"
  :colors="ThemeColors.vintage"
/>
```

## 交互功能

### 事件处理

```vue
<BarChart
  :data="data"
  @click="handleClick"
  @hover="handleHover"
/>

<script setup>
const handleClick = (data) => {
  alert(`点击了: ${data.name}, 值: ${data.value}`)
}

const handleHover = (data) => {
  console.log('悬停:', data)
}
</script>
```

### 使用 Composable

```vue
<script setup lang="ts">
import { useChartFilter, useChartZoom, useChartExport } from '@/components/Charts'

// 筛选功能
const { filters, addFilter, applyFilters } = useChartFilter()

// 缩放功能
const { zoomRange, zoomIn, zoomOut, resetZoom } = useChartZoom()

// 导出功能
const { exportAsImage, exportAsData } = useChartExport()
</script>
```

### 导出图表

```vue
<template>
  <div>
    <BarChart ref="chartRef" :data="data" />
    <button @click="handleExport">导出PNG</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useChartExport } from '@/components/Charts'

const chartRef = ref()
const { exportAsImage } = useChartExport()

const handleExport = async () => {
  const svg = document.querySelector('.bar-chart svg')
  if (svg) {
    await exportAsImage(svg as SVGElement, { 
      type: 'png', 
      filename: 'chart' 
    })
  }
}
</script>
```

## 数据格式示例

### 柱状图/折线图

```typescript
const barData = [
  { name: '第一季度', value: 1200 },
  { name: '第二季度', value: 1800 },
  { name: '第三季度', value: 1500 },
  { name: '第四季度', value: 2100 }
]
```

### 饼图

```typescript
const pieData = [
  { name: '手机', value: 4500 },
  { name: '电脑', value: 3200 },
  { name: '平板', value: 1800 },
  { name: '配件', value: 900 }
]
```

### 多系列（折线图/雷达图）

```typescript
const multiSeries = [
  {
    name: '2024年',
    data: [
      { name: '性能', value: 85 },
      { name: '功能', value: 90 },
      { name: '易用性', value: 75 },
      { name: '稳定', value: 88 }
    ]
  },
  {
    name: '2025年',
    data: [
      { name: '性能', value: 92 },
      { name: '功能', value: 95 },
      { name: '易用性', value: 82 },
      { name: '稳定', value: 91 }
    ]
  }
]
```

## 单元测试

运行测试：

```bash
cd smart-admin-web
npm test -- --run Charts
```

## 浏览器兼容性

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## 注意事项

1. 图表基于 SVG 实现，确保在支持 SVG 的环境中使用
2. 数据量大时建议使用分页或虚拟滚动
3. 导出功能需要浏览器支持 Canvas 和 Blob API
4. 多系列数据建议不超过 5 个系列，每系列不超过 20 个数据点

## 扩展

如需更强大的图表功能，建议集成：
- [ECharts](https://echarts.apache.org/) - 功能全面的图表库
- [Chart.js](https://www.chartjs.org/) - 轻量级图表库
- [Visx](https://airbnb.io/visx/) - React 图表库