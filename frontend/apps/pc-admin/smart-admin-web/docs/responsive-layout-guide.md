# AI-Ready 前端响应式布局优化文档

## 概述

本项目实现了完整的响应式布局优化，支持移动端、平板、桌面端三端适配，包含响应式栅格系统和布局工具。

## 核心文件

```
src/
├── utils/responsive.ts       # 响应式工具
├── styles/responsive.css     # 响应式样式
└── docs/
    └── responsive-layout-guide.md  # 使用文档
```

## 断点配置

| 断点 | 宽度范围 | 设备类型 | 列数 |
|------|---------|----------|------|
| xs | < 576px | 手机竖屏 | 4 |
| sm | ≥ 576px | 手机横屏 | 4 |
| md | ≥ 768px | 平板竖屏 | 8 |
| lg | ≥ 992px | 平板横屏 | 8 |
| xl | ≥ 1200px | 桌面 | 12 |
| xxl | ≥ 1600px | 大屏幕 | 12 |

## 使用方法

### 1. 响应式Hook

```vue
<template>
  <div :class="`layout-${deviceType}`">
    <p>当前设备: {{ deviceType }}</p>
    <p>屏幕宽度: {{ width }}px</p>
    <p>断点: {{ breakpoint }}</p>
  </div>
</template>

<script setup>
import { useResponsive } from '@/utils/responsive'

const { width, deviceType, breakpoint, isMobile, isTablet, isDesktop } = useResponsive()
</script>
```

### 2. 响应式栅格

```vue
<template>
  <div class="row">
    <div class="col-12 col-md-6 col-lg-4">
      <!-- 移动端12列，平板6列，桌面4列 -->
    </div>
  </div>
</template>
```

### 3. 响应式隐藏/显示

```vue
<template>
  <!-- 仅在移动端显示 -->
  <div class="visible-xs">移动端内容</div>
  
  <!-- 仅在桌面端显示 -->
  <div class="visible-lg">桌面端内容</div>
  
  <!-- 移动端隐藏 -->
  <div class="hidden-xs">非移动端内容</div>
</template>
```

### 4. 媒体查询

```vue
<script setup>
import { useMediaQuery, useBreakpoint } from '@/utils/responsive'

// 自定义媒体查询
const isDark = useMediaQuery('(prefers-color-scheme: dark)')

// 断点查询
const isMobile = useBreakpoint('md')
</script>
```

## 布局最佳实践

### 移动端布局

- 单列布局为主
- 使用flex-direction: column
- 隐藏非必要内容
- 底部导航栏

### 平板布局

- 双列或三列布局
- 保持侧边栏折叠
- 利用横向空间

### 桌面布局

- 多列布局
- 固定侧边栏
- 内容最大化展示

## 完成标准

✓ 移动端布局适配
✓ 平板端布局适配
✓ 桌面端布局适配
✓ 响应式栅格系统
✓ 响应式布局优化代码

---

_响应式布局优化完成。_