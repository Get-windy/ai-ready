# 企智连数据可视化组件库 - 架构设计文档

## 文档信息
- **版本**: v1.0.0
- **日期**: 2026-04-17
- **作者**: AI应用开发工程师

---

## 1. 架构概览

### 1.1 设计目标
- **统一性**: 提供统一的API和交互体验
- **可扩展性**: 支持新图表类型的快速接入
- **主题化**: 支持多套主题和动态切换
- **响应式**: 适配不同屏幕尺寸
- **高性能**: 基于SVG实现，轻量高效

### 1.2 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    应用层 (Application)                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │ 报表中心    │ │ 数据仪表盘  │ │ 实时监控                │ │
│  └──────┬──────┘ └──────┬──────┘ └───────────┬─────────────┘ │
└─────────┼───────────────┼────────────────────┼───────────────┘
          │               │                    │
          └───────────────┴────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 组件层 (Components)                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ LineChart   │ │ BarChart    │ │ PieChart    │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ RadarChart  │ │ ScatterChart│ │ HeatmapChart│           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ DashboardLayout - 仪表盘布局组件                        ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  核心层 (Core)                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ BaseChart   │ │ ThemeManager│ │ Responsive  │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ ChartUtils  │ │ Animation   │ │ Interaction │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  类型层 (Types)                              │
│  - ChartType, ChartDataPoint, ChartSeries                   │
│  - ChartConfig, ChartEvents, ChartFilter                    │
│  - ExportOptions, ThemeColors                               │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 目录结构

```
src/components/Charts/
├── core/                          # 核心层
│   ├── BaseChart.vue              # 基础图表组件
│   ├── ThemeManager.ts            # 主题管理器
│   ├── ResponsiveManager.ts       # 响应式管理器
│   ├── ChartUtils.ts              # 图表工具函数
│   ├── AnimationEngine.ts         # 动画引擎
│   └── InteractionManager.ts      # 交互管理器
├── components/                    # 图表组件层
│   ├── LineChart.vue              # 折线图
│   ├── BarChart.vue               # 柱状图
│   ├── PieChart.vue               # 饼图
│   ├── RadarChart.vue             # 雷达图
│   ├── ScatterChart.vue           # 散点图
│   ├── HeatmapChart.vue           # 热力图
│   └── FunnelChart.vue            # 漏斗图
├── layout/                        # 布局组件层
│   ├── DashboardLayout.vue        # 仪表盘布局
│   ├── GridLayout.vue             # 网格布局
│   ├── CardLayout.vue             # 卡片布局
│   └── ResponsiveContainer.vue    # 响应式容器
├── composables/                   # 组合式函数
│   ├── useChartTheme.ts           # 主题管理
│   ├── useChartResponsive.ts      # 响应式
│   ├── useChartExport.ts          # 导出功能
│   ├── useChartFilter.ts          # 筛选功能
│   ├── useChartZoom.ts            # 缩放功能
│   └── useChartAnimation.ts       # 动画控制
├── themes/                        # 主题配置
│   ├── default.ts                 # 默认主题
│   ├── dark.ts                    # 暗色主题
│   ├── vintage.ts                 # 复古主题
│   ├── colorful.ts                # 彩色主题
│   └── index.ts                   # 主题导出
├── types/                         # 类型定义
│   └── index.ts                   # 所有类型
└── index.ts                       # 入口文件
```

---

## 2. 核心设计

### 2.1 组件设计模式

采用 **组合式组件** 设计模式：
- **BaseChart**: 提供基础SVG容器、标题、图例、Tooltip
- **具体图表**: 继承BaseChart，实现特定图表渲染逻辑
- **Composable**: 提供可复用的功能逻辑

### 2.2 主题系统设计

```typescript
// 主题配置结构
interface ChartTheme {
  name: string
  colors: {
    primary: string[]      // 主色板
    background: string     // 背景色
    text: string          // 文字颜色
    grid: string          // 网格线颜色
    border: string        // 边框颜色
  }
  font: {
    family: string
    size: number
  }
  animation: {
    enabled: boolean
    duration: number
    easing: string
  }
}
```

### 2.3 响应式设计

```typescript
// 响应式断点
interface ResponsiveBreakpoints {
  xs: number   // < 576px
  sm: number   // >= 576px
  md: number   // >= 768px
  lg: number   // >= 992px
  xl: number   // >= 1200px
}

// 响应式配置
interface ResponsiveConfig {
  breakpoints: ResponsiveBreakpoints
  resizeDelay: number      // 防抖延迟
  aspectRatio: number      // 宽高比
}
```

---

## 3. API规范

### 3.1 组件Props规范

所有图表组件统一Props接口：

```typescript
interface ChartProps {
  // 数据
  data: ChartDataPoint[] | ChartSeries[]
  
  // 基础配置
  title?: string
  subTitle?: string
  width?: number
  height?: number
  
  // 主题配置
  theme?: string | ChartTheme
  colors?: string[]
  
  // 交互配置
  interactive?: boolean
  animation?: boolean
  
  // 高级配置
  config?: ChartConfig
}
```

### 3.2 事件规范

```typescript
interface ChartEvents {
  onClick?: (data: ChartDataPoint, event: MouseEvent) => void
  onHover?: (data: ChartDataPoint, event: MouseEvent) => void
  onLeave?: (event: MouseEvent) => void
  onSelect?: (data: ChartDataPoint[]) => void
  onZoom?: (range: { start: number; end: number }) => void
}
```

---

## 4. 性能优化策略

### 4.1 渲染优化
- **虚拟渲染**: 大数据量时只渲染可视区域
- **防抖重绘**: resize事件防抖处理
- **Canvas缓存**: 复杂图表使用Canvas缓存

### 4.2 内存优化
- **组件卸载**: 清理事件监听和定时器
- **数据缓存**: LRU缓存策略
- **懒加载**: 按需加载图表组件

---

## 5. 扩展指南

### 5.1 添加新图表类型

1. 创建组件文件 `NewChart.vue`
2. 继承 `BaseChart` 或使用 `useChartCore()`
3. 实现渲染逻辑
4. 注册到组件库

### 5.2 添加新主题

1. 创建主题文件 `themes/custom.ts`
2. 实现 `ChartTheme` 接口
3. 注册到主题管理器

---

## 6. 测试策略

- **单元测试**: 组件功能测试
- **集成测试**: 多组件协作测试
- **视觉测试**: 截图对比测试
- **性能测试**: 大数据量渲染测试

---

## 7. 版本规划

### v1.0.0 (当前)
- 基础图表组件 (4种)
- 主题系统
- 响应式适配
- 基础交互

### v1.1.0 (计划)
- 高级图表组件 (4种)
- 仪表盘布局
- 数据联动
- 动画增强

### v1.2.0 (计划)
- 3D图表支持
- 实时数据流
- 更多导出格式
- 无障碍支持
