# AI-Ready 响应式布局指南

## 概述

AI-Ready 系统已实现完整的响应式布局，支持 PC 端、平板和移动端的多终端适配。

## 断点定义

| 断点 | 宽度 | 设备类型 | 布局建议 |
|------|------|----------|----------|
| xs | < 576px | 手机竖屏 | 单列布局，隐藏侧边栏 |
| sm | ≥ 576px | 手机横屏 | 单列布局，隐藏侧边栏 |
| md | ≥ 768px | 平板竖屏 | 双列布局，紧凑侧边栏 |
| lg | ≥ 992px | 平板横屏/PC | 三列布局，完整侧边栏 |
| xl | ≥ 1200px | 大屏 PC | 四列布局，完整侧边栏 |
| xxl | ≥ 1600px | 超大屏 PC | 四列布局，完整侧边栏 |

## 快速开始

### 1. 使用响应式 Composable

```typescript
import { useResponsive } from '@/composables/useResponsiveState'

const {
  windowWidth,        // 窗口宽度
  currentBreakpoint,  // 当前断点
  isMobileView,       // 是否为移动端
  isTabletView,       // 是否为平板
  isDesktopView       // 是否为PC端
} = useResponsive()
```

### 2. 响应式组件

```vue
<template>
  <div>
    <div v-if="isMobileView">移动端内容</div>
    <div v-if="isTabletView">平板端内容</div>
    <div v-if="isDesktopView">PC端内容</div>
  </div>
</template>

<script setup lang="ts">
import { useResponsive } from '@/composables/useResponsiveState'
const { isMobileView, isTabletView, isDesktopView } = useResponsive()
</script>
```

### 3. 响应式样式

```vue
<template>
  <div class="responsive-grid">
    <div>项目 1</div>
    <div>项目 2</div>
    <div>项目 3</div>
  </div>
</template>

<style>
@import '@/styles/responsive.css';
</style>
```

## 布局适配

### PC端（≥ 992px）

**布局特点**：
- 侧边栏完整显示（256px）
- 宽松的内容间距（24px）
- 多列布局（4列）
- 完整的导航菜单
- 显示用户名和语言切换

**样式配置**：
```css
--header-height: 64px;
--content-padding: 24px;
--sidebar-width: 256px;
```

### 平板端（768px - 991px）

**布局特点**：
- 侧边栏完整显示但更紧凑（200px）
- 适中的内容间距（16px）
- 双列布局（2-3列）
- 完整的导航菜单
- 隐藏部分次要信息

**样式配置**：
```css
--header-height: 56px;
--content-padding: 16px;
--sidebar-width: 200px;
```

### 移动端（< 768px）

**布局特点**：
- 侧边栏隐藏，使用抽屉菜单
- 紧凑的内容间距（12px）
- 单列布局
- 抽屉式导航
- 隐藏用户名和语言切换
- 优化触摸操作

**样式配置**：
```css
--header-height: 48px;
--content-padding: 12px;
--sidebar-width: 0;
```

## 响应式工具类

### 显示/隐藏

```vue
<!-- 仅在移动端隐藏 -->
<div class="hidden-xs">移动端隐藏</div>

<!-- 仅在移动端显示 -->
<div class="visible-xs">移动端显示</div>

<!-- 仅在PC端隐藏 -->
<div class="hidden-lg">PC端隐藏</div>

<!-- 仅在PC端显示 -->
<div class="visible-lg">PC端显示</div>
```

### 网格布局

```vue
<!-- 响应式网格 -->
<div class="responsive-grid">
  <div>项目 1</div>
  <div>项目 2</div>
  <div>项目 3</div>
  <div>项目 4</div>
</div>

<!-- 响应式列数： -->
<!-- 移动端: 1列 -->
<!-- 平板端: 2列 -->
<!-- PC端: 4列 -->
```

### Flex布局

```vue
<!-- 响应式Flex -->
<div class="responsive-flex">
  <div>项目 1</div>
  <div>项目 2</div>
  <div>项目 3</div>
  <div>项目 4</div>
</div>

<!-- 响应式方向： -->
<!-- 移动端: 纵向 -->
<!-- 平板/PC端: 横向 -->
```

### 文本大小

```vue
<p class="responsive-text-xs">小号文本</p>
<p class="responsive-text-sm">常规文本</p>
<p class="responsive-text-md">中等文本</p>
```

### 间距

```vue
<!-- 响应式内边距 -->
<div class="responsive-padding">内容</div>

<!-- 响应式外边距 -->
<div class="responsive-margin">内容</div>
```

### 表格

```vue
<!-- 响应式表格容器 -->
<div class="responsive-table-container">
  <a-table :scroll="{ x: 800 }">
    <!-- 表格内容 -->
  </a-table>
</div>
```

## 布局组件

### BasicLayout

主布局组件，已内置响应式支持。

```vue
<template>
  <BasicLayout>
    <!-- 页面内容 -->
  </BasicLayout>
</template>
```

**特性**：
- 自动适配不同终端
- PC端显示侧边栏
- 移动端使用抽屉菜单
- 响应式头部高度
- 响应式内容间距

## 开发指南

### 1. 响应式组件开发

```vue
<template>
  <div class="my-component">
    <!-- 移动端特定内容 -->
    <div v-if="isMobileView" class="mobile-only">
      <a-button block>移动端按钮</a-button>
    </div>
    
    <!-- PC端特定内容 -->
    <div v-if="isDesktopView" class="desktop-only">
      <a-space>
        <a-button>按钮1</a-button>
        <a-button>按钮2</a-button>
      </a-space>
    </div>
    
    <!-- 通用内容 -->
    <div class="responsive-content">
      <!-- 使用响应式工具类 -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { useResponsive } from '@/composables/useResponsiveState'

const { isMobileView, isDesktopView } = useResponsive()
</script>

<style scoped>
.my-component {
  padding: 12px;
}

@media (min-width: 768px) {
  .my-component {
    padding: 16px;
  }
}

@media (min-width: 992px) {
  .my-component {
    padding: 24px;
  }
}
</style>
```

### 2. 响应式表格

```vue
<template>
  <div class="responsive-table-container">
    <a-table
      :columns="columns"
      :data-source="data"
      :scroll="{ x: 1200 }"
      :pagination="{ 
        pageSize: isMobileView ? 5 : 10,
        showSizeChanger: isDesktopView 
      }"
    >
      <!-- 表格内容 -->
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { useResponsive } from '@/composables/useResponsiveState'
import { computed } from 'vue'

const { isMobileView, isDesktopView } = useResponsive()

const columns = computed(() => {
  // 移动端隐藏部分列
  if (isMobileView.value) {
    return baseColumns.filter(col => col.key !== 'note')
  }
  return baseColumns
})
</script>
```

### 3. 响应式表单

```vue
<template>
  <a-form :layout="formLayout">
    <a-row :gutter="gutter">
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="字段1">
          <a-input />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="字段2">
          <a-input />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup lang="ts">
import { useResponsive } from '@/composables/useResponsiveState'
import { computed } from 'vue'

const { isMobileView } = useResponsive()

const formLayout = computed(() => 
  isMobileView.value ? 'vertical' : 'horizontal'
)

const gutter = computed(() => 
  isMobileView.value ? 8 : 16
)
</script>
```

### 4. 响应式图表

```vue
<template>
  <BarChart
    :width="chartWidth"
    :height="chartHeight"
    :data="data"
  />
</template>

<script setup lang="ts">
import { useResponsive } from '@/composables/useResponsiveState'
import { computed } from 'vue'

const { windowWidth, isMobileView } = useResponsive()

const chartWidth = computed(() => {
  if (isMobileView.value) return windowWidth.value - 48
  return 600
})

const chartHeight = computed(() => {
  return isMobileView.value ? 250 : 300
})
</script>
```

## 最佳实践

### 1. 移动优先

优先开发移动端布局，然后逐步适配更大的屏幕。

```css
/* 基础样式（移动端） */
.component {
  padding: 12px;
}

/* 平板端 */
@media (min-width: 768px) {
  .component {
    padding: 16px;
  }
}

/* PC端 */
@media (min-width: 992px) {
  .component {
    padding: 24px;
  }
}
```

### 2. 使用相对单位

使用相对单位（rem, em, %）而不是固定像素。

```css
/* 推荐 */
.container {
  width: 100%;
  padding: 1rem;
  font-size: 1rem;
}

/* 不推荐 */
.container {
  width: 1200px;
  padding: 16px;
  font-size: 16px;
}
```

### 3. 优化触摸操作

为移动端优化触摸区域和交互。

```css
/* 移动端优化 */
@media (max-width: 768px) {
  .button {
    min-height: 44px;
    min-width: 44px;
    padding: 12px 24px;
  }
  
  .touch-target {
    padding: 16px;
  }
}
```

### 4. 图片响应式

使用响应式图片和占位符。

```vue
<template>
  <img
    :src="imageUrl"
    :alt="imageAlt"
    class="responsive-image"
    loading="lazy"
  />
</template>

<style scoped>
.responsive-image {
  max-width: 100%;
  height: auto;
  display: block;
}
</style>
```

### 5. 性能优化

避免在移动端加载不必要的内容。

```vue
<template>
  <div>
    <!-- 移动端不加载大型组件 -->
    <HeavyComponent v-if="isDesktopView" />
    
    <!-- 使用轻量级替代 -->
    <LightComponent v-else />
  </div>
</template>
```

## 测试

### 测试页面

访问响应式测试页面：

```
http://localhost:5173/responsive
```

### 浏览器开发者工具

使用浏览器开发者工具测试不同设备：

1. 打开开发者工具（F12）
2. 切换到设备模拟模式（Ctrl+Shift+M / Cmd+Shift+M）
3. 选择预设设备或自定义尺寸
4. 测试布局和交互

### 常用测试尺寸

| 设备 | 尺寸 | 用途 |
|------|------|------|
| iPhone SE | 375x667 | 小屏手机 |
| iPhone 12/13 | 390x844 | 中屏手机 |
| iPhone 14 Pro Max | 430x932 | 大屏手机 |
| iPad | 768x1024 | 平板竖屏 |
| iPad Pro | 1024x1366 | 大屏平板 |
| Desktop | 1920x1080 | 标准PC |
| Wide Desktop | 2560x1440 | 宽屏PC |

## 兼容性

### 浏览器支持

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+
- iOS Safari 13+
- Android Chrome 80+

### 特性支持

- CSS Grid
- Flexbox
- CSS Variables
- Viewport Units (vw, vh)
- Media Queries
- Touch Events

## 故障排除

### 常见问题

#### 1. 移动端布局错乱

**原因**：未正确导入响应式样式

**解决**：
```vue
<style>
@import '@/styles/responsive.css';
</style>
```

#### 2. 侧边栏不显示

**原因**：屏幕宽度小于断点

**解决**：检查 `isDesktopView` 的值，确保在 PC 端

#### 3. 表格在移动端无法滚动

**原因**：未使用响应式表格容器

**解决**：
```vue
<div class="responsive-table-container">
  <a-table :scroll="{ x: 800 }">
    <!-- 表格内容 -->
  </a-table>
</div>
```

#### 4. 触摸事件不响应

**原因**：触摸区域过小

**解决**：增大触摸目标的最小尺寸

```css
@media (max-width: 768px) {
  .touch-target {
    min-height: 44px;
    min-width: 44px;
  }
}
```

## 相关文件

- `src/composables/useResponsive.ts` - 响应式配置
- `src/composables/useResponsiveState.ts` - 响应式 Composable
- `src/layouts/BasicLayout.vue` - 主布局组件
- `src/styles/responsive.css` - 响应式样式
- `src/views/responsive/index.vue` - 响应式测试页面

---

**最后更新**: 2026-04-11
**维护者**: AI-Ready 前端团队