# AI-Ready 数据可视化组件使用指南

## 目录

- [快速开始](#快速开始)
- [基础图表](#基础图表)
- [交互功能](#交互功能)
- [导出功能](#导出功能)
- [高级用法](#高级用法)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 快速开始

### 1. 引入组件

```typescript
// 单独引入
import { BarChart } from '@/components/Charts'

// 批量引入
import {
  BarChart,
  LineChart,
  PieChart,
  RadarChart,
  Chart
} from '@/components/Charts'

// 引入 Composable
import { useChartFilter, useChartExport, useChartZoom } from '@/components/Charts'
```

### 2. 基础用法

```vue
<template>
  <BarChart
    :data="data"
    title="销售统计"
    :width="600"
    :height="300"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { BarChart } from '@/components/Charts'

const data = ref([
  { name: '一月', value: 100 },
  { name: '二月', value: 200 },
  { name: '三月', value: 150 }
])
</script>
```

## 基础图表

### 柱状图 (BarChart)

适用于对比不同类别的数值。

```vue
<BarChart
  :data="data"
  title="标题"
  :width="400"
  :height="300"
  :colors="['#5470c6', '#91cc75']"
  :show-legend="true"
  :show-value="true"
  @click="onClick"
/>
```

#### 数据格式

```typescript
const data = [
  { name: '第一季度', value: 1200 },
  { name: '第二季度', value: 1800 },
  { name: '第三季度', value: 1500 },
  { name: '第四季度', value: 2100 }
]
```

#### 属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 图表数据 |
| title | string | '' | 图表标题 |
| width | number | 400 | 图表宽度 |
| height | number | 300 | 图表高度 |
| colors | string[] | [] | 自定义颜色数组 |
| showLegend | boolean | true | 是否显示图例 |
| showValue | boolean | false | 是否显示数值 |

#### 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| click | ChartDataPoint | 点击柱子时触发 |
| hover | ChartDataPoint | 鼠标悬停时触发 |

### 折线图 (LineChart)

适用于展示数据随时间变化的趋势。

```vue
<LineChart
  :data="singleData"
  :series="multiData"
  title="趋势图"
  :smooth="true"
  :width="600"
  :height="300"
  @click="onClick"
/>
```

#### 单系列数据

```typescript
const singleData = [
  { name: '1月', value: 100 },
  { name: '2月', value: 200 },
  { name: '3月', value: 150 },
  { name: '4月', value: 300 }
]
```

#### 多系列数据

```typescript
const multiData = [
  {
    name: '2024年',
    data: [
      { name: 'Q1', value: 1200 },
      { name: 'Q2', value: 1800 }
    ],
    color: '#5470c6'
  },
  {
    name: '2025年',
    data: [
      { name: 'Q1', value: 1400 },
      { name: 'Q2', value: 2000 }
    ],
    color: '#91cc75'
  }
]
```

#### 属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 单系列数据 |
| series | ChartSeries[] | [] | 多系列数据 |
| smooth | boolean | false | 是否使用平滑曲线 |

### 饼图 (PieChart)

适用于展示占比分布。

```vue
<PieChart
  :data="data"
  title="占比分布"
  :inner-radius="0.5"
  :show-label="true"
  :width="400"
  :height="400"
  @click="onClick"
/>
```

#### 数据格式

```typescript
const data = [
  { name: '手机', value: 4500, color: '#5470c6' },
  { name: '电脑', value: 3200, color: '#91cc75' },
  { name: '平板', value: 1800, color: '#fac858' },
  { name: '配件', value: 900, color: '#ee6666' }
]
```

#### 属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | ChartDataPoint[] | [] | 图表数据 |
| innerRadius | number | 0 | 环形图半径 (0-1) |
| showLabel | boolean | true | 是否显示标签 |

### 统一图表组件 (Chart)

根据 type 属性自动渲染对应图表类型。

```vue
<Chart
  type="bar"
  :data="data"
  title="动态图表"
  :width="600"
  :height="300"
/>

<!-- 动态切换 -->
<Chart
  :type="chartType"
  :data="data"
/>
```

#### 类型

| type | 图表类型 |
|------|----------|
| bar | 柱状图 |
| line | 折线图 |
| pie | 饼图 |
| radar | 雷达图 |

## 交互功能

### 数据筛选

使用 `useChartFilter` composable 实现数据筛选。

```vue
<template>
  <div>
    <a-button @click="filterByCategory('电子')">电子产品</a-button>
    <a-button @click="clearFilter">清除筛选</a-button>
    
    <BarChart :data="filteredData" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { BarChart, useChartFilter } from '@/components/Charts'

const data = ref([
  { name: '产品A', value: 100, category: '电子' },
  { name: '产品B', value: 200, category: '服装' },
  { name: '产品C', value: 150, category: '电子' }
])

const { filters, addFilter, clearFilters, applyFilters } = useChartFilter()

const filteredData = computed(() => {
  return applyFilters(data.value)
})

const filterByCategory = (category: string) => {
  addFilter('category', category)
}

const clearFilter = () => {
  clearFilters()
}
</script>
```

#### API

| 方法 | 参数 | 说明 |
|------|------|------|
| addFilter | (field: string, value: any) | 添加筛选条件 |
| removeFilter | (field: string) | 移除筛选条件 |
| clearFilters | () | 清除所有筛选 |
| applyFilters | (data: T) | 应用筛选 |

### 图表缩放

使用 `useChartZoom` composable 实现图表缩放。

```vue
<template>
  <div>
    <a-space>
      <a-button @click="zoomIn">放大</a-button>
      <a-button @click="zoomOut">缩小</a-button>
      <a-button @click="resetZoom">重置</a-button>
    </a-space>
    <BarChart :data="data" />
  </div>
</template>

<script setup lang="ts">
import { useChartZoom } from '@/components/Charts'

const { zoomIn, zoomOut, resetZoom } = useChartZoom()
</script>
```

#### API

| 方法 | 参数 | 说明 |
|------|------|------|
| zoomIn | (step?: number) | 放大 |
| zoomOut | (step?: number) | 缩小 |
| resetZoom | () | 重置 |
| setZoom | (start: number, end: number) | 设置缩放范围 |

## 导出功能

### 导出图片

```vue
<template>
  <div>
    <a-button @click="exportPNG">导出PNG</a-button>
    <a-button @click="exportSVG">导出SVG</a-button>
    <BarChart ref="chartRef" :data="data" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { BarChart, useChartExport } from '@/components/Charts'

const chartRef = ref()
const { exportAsImage } = useChartExport()

const exportPNG = async () => {
  const svg = document.querySelector('.bar-chart svg') as SVGElement
  if (svg) {
    await exportAsImage(svg, {
      type: 'png',
      filename: 'chart'
    })
  }
}

const exportSVG = async () => {
  const svg = document.querySelector('.bar-chart svg') as SVGElement
  if (svg) {
    await exportAsImage(svg, {
      type: 'svg',
      filename: 'chart'
    })
  }
}
</script>
```

### 导出数据

```vue
<template>
  <div>
    <a-button @click="exportJSON">导出JSON</a-button>
    <a-button @click="exportCSV">导出CSV</a-button>
    <BarChart :data="data" />
  </div>
</template>

<script setup lang="ts">
import { useChartExport } from '@/components/Charts'

const { exportAsData } = useChartExport()

const exportJSON = () => {
  exportAsData(data.value, {
    type: 'json',
    filename: 'chart-data'
  })
}

const exportCSV = () => {
  exportAsData(data.value, {
    type: 'csv',
    filename: 'chart-data'
  })
}
</script>
```

#### 导出选项

```typescript
interface ExportOptions {
  type: 'png' | 'svg' | 'csv' | 'json'
  filename?: string
  backgroundColor?: string
  pixelRatio?: number
}
```

## 高级用法

### 主题配置

```vue
<template>
  <BarChart
    :data="data"
    :colors="ThemeColors.vintage"
    :config="chartConfig"
  />
</template>

<script setup lang="ts">
import { ThemeColors } from '@/components/Charts'

const chartConfig = {
  theme: 'vintage',
  animation: true,
  grid: {
    top: 40,
    right: 20,
    bottom: 40,
    left: 60
  }
}
</script>
```

#### 可用主题

- `default` - 默认主题
- `dark` - 暗色主题
- `vintage` - 复古主题
- `colorful` - 彩色主题

### 自定义颜色

```vue
<BarChart
  :data="data"
  :colors="['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24']"
/>
```

### 条件渲染

```vue
<template>
  <Chart
    v-if="showChart"
    :type="chartType"
    :data="data"
  />
  <a-empty v-else description="暂无数据" />
</template>
```

### 响应式图表

```vue
<template>
  <div ref="container" class="chart-container">
    <BarChart
      :data="data"
      :width="chartWidth"
      :height="chartHeight"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const container = ref<HTMLElement>()
const chartWidth = ref(600)
const chartHeight = ref(300)

const handleResize = () => {
  if (container.value) {
    chartWidth.value = container.value.clientWidth
    chartHeight.value = 400
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 400px;
}
</style>
```

## 最佳实践

### 1. 数据准备

```typescript
// ✅ 推荐：使用计算属性
const chartData = computed(() => {
  return rawData.value.map(item => ({
    name: item.label,
    value: item.count,
    category: item.type
  }))
})

// ❌ 不推荐：直接在模板中处理
<BarChart :data="rawData.map(item => ({ name: item.label, value: item.count }))" />
```

### 2. 性能优化

```typescript
// ✅ 推荐：大数据量时使用分页或抽样
const sampledData = computed(() => {
  return largeData.value.filter((_, i) => i % 10 === 0)
})

// ❌ 不推荐：直接渲染大量数据
<BarChart :data="largeData" /> // 可能导致性能问题
```

### 3. 错误处理

```typescript
// ✅ 推荐：添加数据验证
const safeData = computed(() => {
  if (!data.value || !Array.isArray(data.value)) {
    return []
  }
  return data.value.filter(item => 
    item.name && typeof item.value === 'number'
  )
})

// ✅ 推荐：添加空状态
<BarChart v-if="safeData.length > 0" :data="safeData" />
<a-empty v-else description="暂无数据" />
```

### 4. 事件处理

```typescript
// ✅ 推荐：使用防抖处理频繁事件
import { debounce } from 'lodash-es'

const handleHover = debounce((data: ChartDataPoint) => {
  console.log('Hover:', data)
}, 100)
```

## 常见问题

### Q1: 图表不显示？

**A**: 检查以下几点：
1. 数据格式是否正确
2. 确保数据不为空数组
3. 检查 width 和 height 是否设置
4. 查看浏览器控制台是否有错误

### Q2: 如何自定义图表样式？

**A**: 可以通过以下方式：
1. 使用 `colors` 属性自定义颜色
2. 使用 `config` 属性配置全局样式
3. 通过 CSS 覆盖默认样式

### Q3: 导出功能不工作？

**A**: 检查以下几点：
1. 浏览器是否支持 Canvas 和 Blob API
2. 确保正确获取了 SVG 元素
3. 检查导出选项是否正确

### Q4: 如何处理大数据量？

**A**:
1. 使用数据抽样
2. 实现分页加载
3. 使用虚拟滚动
4. 限制数据点数量

### Q5: 如何实现图表联动？

**A**:
```typescript
// 通过事件实现联动
const handleBarClick = (data: ChartDataPoint) => {
  // 更新其他图表的数据
  lineData.value = getRelatedData(data.name)
}
```

## 完整示例

查看完整示例代码：`src/views/charts/index.vue`

```bash
# 运行示例
cd I:\AI-Ready\smart-admin-web
npm run dev

# 访问示例页面
http://localhost:5173/charts
```

## 相关文档

- [组件 README](./README.md)
- [类型定义](./types.ts)
- [Composable API](./composables.ts)

---

**最后更新**: 2026-04-11
**维护者**: AI-Ready 前端团队