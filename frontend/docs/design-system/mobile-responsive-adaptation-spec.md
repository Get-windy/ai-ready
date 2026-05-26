# AI-Ready 移动端响应式适配规范

> 为 AI-Ready 项目的移动端应用制定响应式适配规范，确保在不同屏幕尺寸和设备上的显示效果一致性
> 设计师: ui-mnj0fukd
> 创建日期: 2026-04-30
> 版本: v1.0
> 任务ID: task_1777516926421_4lun64oew

---

## 目录

1. [响应式设计原则](#1-响应式设计原则)
2. [断点系统与适配策略](#2-断点系统与适配策略)
3. [屏幕尺寸适配规范](#3-屏幕尺寸适配规范)
4. [横竖屏适配规范](#4-横竖屏适配规范)
5. [触摸交互适配规范](#5-触摸交互适配规范)
6. [内容优先级调整方案](#6-内容优先级调整方案)
7. [字体与间距响应式规则](#7-字体与间距响应式规则)
8. [图片与媒体资源适配](#8-图片与媒体资源适配)
9. [组件适配指南](#9-组件适配指南)
10. [性能与可访问性](#10-性能与可访问性)

---

## 1. 响应式设计原则

### 1.1 移动优先设计 (Mobile-First)

**核心原则**: 优先为移动设备设计，然后逐步增强到更大屏幕

**实施方法**:
1. **从最小屏幕开始**: 先设计 320px 宽度的界面
2. **渐进增强**: 使用 `min-width` 媒体查询逐步增加功能
3. **内容优先**: 核心内容在小屏幕上完整显示
4. **功能降级**: 非核心功能在大屏幕上才显示

```css
/* 移动优先的 CSS 结构 */
/* 1. 移动端基础样式 */
.component {
  width: 100%;
  padding: 16px;
}

/* 2. 平板增强 (768px+) */
@media (min-width: 768px) {
  .component {
    width: 50%;
    padding: 24px;
  }
}

/* 3. 桌面增强 (992px+) */
@media (min-width: 992px) {
  .component {
    width: 33.333%;
    padding: 32px;
  }
}
```

### 1.2 断点策略

**基于内容的断点 (Content-First Breakpoints)**:
- 当布局因内容显示不完整而需要调整时设置断点
- 避免基于特定设备尺寸的断点

**推荐的断点设置**:
```css
/* 移动优先的断点系统 */
:root {
  /* 移动端断点 */
  --breakpoint-mobile-xs: 320px;    /* 小屏手机 */
  --breakpoint-mobile-sm: 375px;    /* 标准手机 */
  --breakpoint-mobile-md: 414px;    /* 大屏手机 */
  --breakpoint-mobile-lg: 480px;    /* 超大屏手机 */
  
  /* 平板断点 */
  --breakpoint-tablet: 768px;       /* 平板竖屏 */
  --breakpoint-tablet-lg: 1024px;   /* 平板横屏 */
  
  /* 桌面断点 */
  --breakpoint-desktop: 1200px;     /* 桌面 */
  --breakpoint-desktop-lg: 1440px;  /* 大桌面 */
}
```

### 1.3 内容优先级调整

**核心内容优先原则**:
1. **信息层次**: 确保核心信息在任何屏幕上都清晰可见
2. **功能聚焦**: 移动端只展示核心功能，隐藏次要功能
3. **渐进披露**: 复杂功能逐步展开，避免信息过载

**内容优先级调整策略**:
| 优先级 | 移动端 (<768px) | 平板端 (768-1200px) | 桌面端 (>1200px) |
|--------|-----------------|---------------------|------------------|
| P0 核心内容 | 完整显示 | 完整显示 | 完整显示 |
| P1 重要功能 | 完整显示 | 完整显示 | 完整显示 |
| P2 辅助功能 | 折叠/隐藏 | 部分显示 | 完整显示 |
| P3 高级功能 | 隐藏 | 折叠/图标 | 完整显示 |
| P4 管理功能 | 隐藏 | 隐藏 | 完整显示 |

---

## 2. 断点系统与适配策略

### 2.1 断点应用策略

| 断点范围 | 设备类型 | 布局策略 | 导航模式 |
|----------|----------|----------|----------|
| < 576px | 小屏手机 | 单列布局 | 底部Tab导航 |
| 576-768px | 大屏手机 | 1-2列布局 | 底部Tab导航 |
| 768-992px | 平板竖屏 | 2-3列布局 | 侧边栏+底部 |
| 992-1200px | 平板横屏 | 多列布局 | 侧边栏+顶部 |
| > 1200px | 桌面 | 多列布局 | 顶部+侧边栏 |

### 2.2 布局系统

#### 2.2.1 流式布局 (Fluid Layout)

```css
/* 基础容器 */
.mobile-container {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 16px;
}

/* 响应式容器 */
@media (min-width: 576px) {
  .mobile-container {
    padding: 0 24px;
  }
}

@media (min-width: 768px) {
  .mobile-container {
    max-width: 720px;
  }
}

@media (min-width: 992px) {
  .mobile-container {
    max-width: 960px;
  }
}

@media (min-width: 1200px) {
  .mobile-container {
    max-width: 1140px;
  }
}
```

#### 2.2.2 弹性网格 (Flexible Grid)

```css
/* 移动端网格系统 */
.responsive-grid {
  display: grid;
  gap: 12px;
}

/* 单列（手机） */
.responsive-grid {
  grid-template-columns: 1fr;
}

/* 双列（大屏手机/平板） */
@media (min-width: 576px) {
  .responsive-grid--2col {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 三列（平板） */
@media (min-width: 768px) {
  .responsive-grid--3col {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 四列（桌面） */
@media (min-width: 992px) {
  .responsive-grid--4col {
    grid-template-columns: repeat(4, 1fr);
  }
}
```

#### 2.2.3 弹性盒子 (Flexbox)

```css
/* 响应式Flex布局 */
.responsive-flex {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

/* 响应式子项 */
.responsive-flex-item {
  flex: 1 1 100%; /* 手机：全宽 */
  min-width: 0;
}

@media (min-width: 576px) {
  .responsive-flex-item {
    flex: 1 1 calc(50% - 6px); /* 平板：2列 */
  }
}

@media (min-width: 768px) {
  .responsive-flex-item {
    flex: 1 1 calc(33.333% - 8px); /* 平板：3列 */
  }
}
```

---

## 3. 屏幕尺寸适配规范

### 3.1 常见移动设备尺寸分类

| 设备分类 | 屏幕宽度 | 分辨率 | 设备示例 |
|----------|----------|--------|----------|
| 小屏手机 | 320-374px | 1x-2x | iPhone SE, 小型Android |
| 标准手机 | 375-413px | 2x-3x | iPhone 12-15, 主流Android |
| 大屏手机 | 414-479px | 2x-3x | iPhone Plus, 大屏Android |
| 超大屏手机 | 480-767px | 2x-3x | 折叠屏展开, 大屏Android |
| 小平板 | 768-1023px | 2x | iPad Mini, 小型平板 |
| 大平板 | 1024-1199px | 2x | iPad Pro, 大型平板 |

### 3.2 不同屏幕尺寸的布局适配方案

#### 3.2.1 手机端 (< 768px) - 单列布局

```css
/* 手机端布局 */
.mobile-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.mobile-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: white;
  padding: 12px 16px;
}

.mobile-content {
  flex: 1;
  padding: 16px;
}

.mobile-footer {
  position: sticky;
  bottom: 0;
  z-index: 100;
  background: white;
  padding: 8px 16px;
  border-top: 1px solid #f0f0f0;
}
```

#### 3.2.2 平板端 (768-1199px) - 多列布局

```css
/* 平板端布局 */
.tablet-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  min-height: 100vh;
}

.tablet-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  border-right: 1px solid #f0f0f0;
}

.tablet-content {
  padding: 24px;
  overflow-y: auto;
}
```

#### 3.2.3 桌面端 (≥ 1200px) - 完整布局

```css
/* 桌面端布局 */
.desktop-layout {
  display: grid;
  grid-template-columns: 240px 1fr 300px;
  min-height: 100vh;
  max-width: 1440px;
  margin: 0 auto;
}

.desktop-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.desktop-main {
  padding: 32px;
  overflow-y: auto;
}

.desktop-aside {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  border-left: 1px solid #f0f0f0;
}
```

---

## 4. 横竖屏适配规范

### 4.1 横屏模式适配方案

#### 4.1.1 横屏布局调整

```css
/* 横屏检测与适配 */
@media (orientation: landscape) and (max-height: 600px) {
  /* 横屏手机 */
  .landscape-mobile {
    /* 调整布局为横屏优化 */
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }
  
  /* 隐藏不必要的元素 */
  .landscape-mobile .secondary-info {
    display: none;
  }
  
  /* 调整字体大小 */
  .landscape-mobile .content {
    font-size: 14px;
  }
}

@media (orientation: landscape) and (min-width: 768px) {
  /* 横屏平板 */
  .landscape-tablet {
    /* 充分利用横屏宽度 */
    display: grid;
    grid-template-columns: 300px 1fr 300px;
  }
}
```

#### 4.1.2 横屏导航调整

```css
/* 横屏导航适配 */
@media (orientation: landscape) {
  /* 手机横屏：底部导航移到侧边 */
  .mobile-nav.landscape {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    width: 60px;
    flex-direction: column;
  }
  
  /* 平板横屏：显示更多导航项 */
  .tablet-nav.landscape {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
  }
}
```

### 4.2 竖屏模式优化

#### 4.2.1 竖屏内容优化

```css
/* 竖屏内容优化 */
@media (orientation: portrait) {
  /* 优化长内容阅读 */
  .portrait-content {
    line-height: 1.6;
    font-size: 16px;
  }
  
  /* 调整图片显示 */
  .portrait-image {
    max-height: 50vh;
    object-fit: contain;
  }
  
  /* 优化表单输入 */
  .portrait-form input {
    height: 48px;
    font-size: 16px;
  }
}
```

#### 4.2.2 竖屏交互优化

```css
/* 竖屏交互优化 */
@media (orientation: portrait) {
  /* 增大触控目标 */
  .portrait-button {
    min-height: 48px;
    min-width: 48px;
  }
  
  /* 优化滚动体验 */
  .portrait-scroll {
    -webkit-overflow-scrolling: touch;
  }
  
  /* 防止键盘遮挡 */
  .portrait-input:focus {
    scroll-margin-top: 100px;
  }
}
```

---

## 5. 触摸交互适配规范

### 5.1 触控目标规范

**最小触控尺寸标准**:
- **按钮**: 44×44px (最小), 48×48px (推荐)
- **列表项**: 44px 高度 (最小), 48-56px (推荐)
- **输入框**: 44px 高度 (最小), 48px (推荐)
- **图标按钮**: 44×44px (最小), 48×48px (推荐)
- **复选框/单选**: 44×44px (最小)

```css
/* 触控目标优化 */
.touch-target {
  min-width: 44px;
  min-height: 44px;
  padding: 12px 16px;
  position: relative;
}

/* 扩大触控区域（视觉小但触控大） */
.touch-expand::before {
  content: '';
  position: absolute;
  top: -8px;
  right: -8px;
  bottom: -8px;
  left: -8px;
}

/* 触摸反馈 */
.touch-feedback:active {
  opacity: 0.7;
  transform: scale(0.98);
  transition: opacity 0.2s, transform 0.1s;
}
```

### 5.2 手势支持规范

#### 5.2.1 基础手势库

```typescript
// 手势配置接口
interface GestureConfig {
  onTap?: (e: TouchEvent) => void
  onLongPress?: (e: TouchEvent) => void
  onSwipeLeft?: (e: TouchEvent) => void
  onSwipeRight?: (e: TouchEvent) => void
  onSwipeUp?: (e: TouchEvent) => void
  onSwipeDown?: (e: TouchEvent) => void
  onPinch?: (scale: number, e: TouchEvent) => void
  onRotate?: (angle: number, e: TouchEvent) => void
}

// 手势阈值配置
const GESTURE_THRESHOLDS = {
  TAP_MAX_DISTANCE: 10,      // 点击最大移动距离
  TAP_MAX_DURATION: 300,     // 点击最大持续时间
  SWIPE_MIN_DISTANCE: 50,    // 滑动最小距离
  LONG_PRESS_DURATION: 500,  // 长按持续时间
}
```

#### 5.2.2 常用手势实现

```css
/* 滑动删除 */
.swipe-item {
  position: relative;
  overflow: hidden;
  transition: transform 0.3s ease;
}

.swipe-actions {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  display: flex;
}

/* 下拉刷新 */
.pull-refresh-head {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 0;
  overflow: hidden;
  transition: height 0.3s;
}

.pull-refresh-content {
  min-height: 100vh;
}
```

### 5.3 触摸优化技巧

```css
/* 防止误触 */
* {
  touch-action: manipulation; /* 禁用双击缩放 */
}

.no-context-menu {
  -webkit-touch-callout: none; /* 禁用长按菜单 */
  -webkit-user-select: none;
  user-select: none;
}

/* 滚动优化 */
.scroll-container {
  overflow-y: auto;
  -webkit-overflow-scrolling: touch; /* 惯性滚动 */
}

.hide-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.hide-scrollbar::-webkit-scrollbar {
  display: none;
}

/* 输入优化 */
input, textarea, select {
  font-size: 16px; /* iOS 防止缩放 */
}
```

---

## 6. 内容优先级调整方案

### 6.1 信息层次调整

#### 6.1.1 移动端信息简化

```css
/* 移动端：隐藏次要信息 */
.mobile-simplify .secondary-info {
  display: none;
}

.mobile-simplify .detailed-description {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 移动端：聚焦核心操作 */
.mobile-focus .primary-actions {
  display: flex;
  gap: 8px;
}

.mobile-focus .secondary-actions {
  display: none;
}

@media (min-width: 768px) {
  .mobile-focus .secondary-actions {
    display: flex;
  }
}
```

#### 6.1.2 渐进式信息披露

```vue
<template>
  <div class="progressive-disclosure">
    <!-- 第一层：核心信息 -->
    <div class="core-info">
      <h3>{{ title }}</h3>
      <p class="summary">{{ summary }}</p>
    </div>
    
    <!-- 第二层：详细信息（默认折叠） -->
    <div class="detailed-info" :class="{ expanded: isExpanded }">
      <div class="content" v-if="isExpanded">
        <slot name="details"></slot>
      </div>
      <button 
        class="toggle-btn" 
        @click="isExpanded = !isExpanded"
      >
        {{ isExpanded ? '收起详情' : '展开详情' }}
      </button>
    </div>
    
    <!-- 第三层：高级功能（大屏幕显示） -->
    <div class="advanced-features" v-if="!isMobile">
      <slot name="advanced"></slot>
    </div>
  </div>
</template>
```

### 6.2 功能聚焦策略

#### 6.2.1 移动端功能聚焦

| 功能类型 | 移动端策略 | 平板端策略 | 桌面端策略 |
|----------|------------|------------|------------|
| 核心操作 | 突出显示 | 突出显示 | 正常显示 |
| 常用功能 | 直接访问 | 直接访问 | 工具栏 |
| 高级功能 | 折叠菜单 | 二级菜单 | 完整菜单 |
| 管理功能 | 设置页面 | 侧边栏 | 完整界面 |
| 分析功能 | 简化视图 | 标准视图 | 详细视图 |

#### 6.2.2 响应式工具栏

```css
/* 移动端工具栏 */
.mobile-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px 16px;
  border-top: 1px solid #f0f0f0;
}

.mobile-toolbar .primary-action {
  flex: 1;
}

.mobile-toolbar .more-actions {
  display: none;
}

/* 平板端工具栏 */
@media (min-width: 768px) {
  .mobile-toolbar {
    gap: 12px;
    padding: 12px 24px;
  }
  
  .mobile-toolbar .more-actions {
    display: flex;
    gap: 8px;
  }
}

/* 桌面端工具栏 */
@media (min-width: 992px) {
  .mobile-toolbar {
    display: none; /* 使用完整工具栏 */
  }
  
  .desktop-toolbar {
    display: flex;
    gap: 16px;
    padding: 16px 32px;
    background: #f8f9fa;
  }
}
```

---

## 7. 字体与间距响应式规则

### 7.1 响应式字体系统

```css
/* 响应式字体变量 */
:root {
  /* 基础字体大小 */
  --font-size-base: 14px;
  --font-size-sm: 12px;
  --font-size-lg: 16px;
  
  /* 标题字体大小 */
  --font-size-h1: 24px;
  --font-size-h2: 20px;
  --font-size-h3: 18px;
  --font-size-h4: 16px;
  --font-size-h5: 14px;
  --font-size-h6: 12px;
  
  /* 行高 */
  --line-height-base: 1.5;
  --line-height-heading: 1.3;
}

/* 移动端字体适配 */
@media (max-width: 767px) {
  :root {
    --font-size-base: 15px;
    --font-size-h1: 22px;
    --font-size-h2: 18px;
    --font-size-h3: 16px;
  }
}

/* 平板端字体适配 */
@media (min-width: 768px) and (max-width: 1199px) {
  :root {
    --font-size-base: 15px;
    --font-size-h1: 26px;
    --font-size-h2: 22px;
    --font-size-h3: 18px;
  }
}

/* 桌面端字体适配 */
@media (min-width: 1200px) {
  :root {
    --font-size-base: 16px;
    --font-size-h1: 32px;
    --font-size-h2: 24px;
    --font-size-h3: 20px;
  }
}

/* 使用 clamp() 实现流畅缩放 */
.responsive-text {
  font-size: clamp(14px, 4vw, 18px);
  line-height: clamp(1.4, 4vw, 1.6);
}

.responsive-heading {
  font-size: clamp(18px, 5vw, 32px);
  line-height: clamp(1.3, 5vw, 1.2);
}
```

### 7.2 响应式间距系统

```css
/* 响应式间距变量 */
:root {
  /* 基础间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-base: 16px;
  --spacing-md: 24px;
  --spacing-lg: 32px;
  --spacing-xl: 48px;
  --spacing-2xl: 64px;
  
  /* 安全区域 */
  --safe-area-top: env(safe-area-inset-top);
  --safe-area-bottom: env(safe-area-inset-bottom);
  --safe-area-left: env(safe-area-inset-left);
  --safe-area-right: env(safe-area-inset-right);
}

/* 移动端间距适配 */
@media (max-width: 767px) {
  .mobile-spacing {
    padding: var(--spacing-base);
    margin-bottom: var(--spacing-base);
  }
  
  .mobile-spacing--compact {
    padding: var(--spacing-sm);
    margin-bottom: var(--spacing-sm);
  }
  
  /* 安全区域适配 */
  .mobile-safe-area {
    padding-top: calc(var(--spacing-base) + var(--safe-area-top));
    padding-bottom: calc(var(--spacing-base) + var(--safe-area-bottom));
    padding-left: calc(var(--spacing-base) + var(--safe-area-left));
    padding-right: calc(var(--spacing-base) + var(--safe-area-right));
  }
}

/* 平板端间距适配 */
@media (min-width: 768px) and (max-width: 1199px) {
  .tablet-spacing {
    padding: var(--spacing-md);
    margin-bottom: var(--spacing-md);
  }
}

/* 桌面端间距适配 */
@media (min-width: 1200px) {
  .desktop-spacing {
    padding: var(--spacing-lg);
    margin-bottom: var(--spacing-lg);
  }
}

/* 使用 min() 函数限制最大间距 */
.responsive-padding {
  padding: min(5vw, var(--spacing-xl));
}

.responsive-margin {
  margin: min(3vw, var(--spacing-lg));
}
```

### 7.3 1px边框适配方案

```css
/* 高清屏 1px 边框解决方案 */
.hairline-border {
  position: relative;
}

.hairline-border::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 1px;
  background: #e8e8e8;
  transform: scaleY(0.5);
  transform-origin: 0 0;
}

/* 根据设备像素比适配 */
@media (-webkit-min-device-pixel-ratio: 2) {
  .hairline-border::after {
    transform: scaleY(0.5);
  }
}

@media (-webkit-min-device-pixel-ratio: 3) {
  .hairline-border::after {
    transform: scaleY(0.333);
  }
}

/* 四边边框 */
.hairline-all::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 200%;
  height: 200%;
  border: 1px solid #e8e8e8;
  transform: scale(0.5);
  transform-origin: 0 0;
  pointer-events: none;
}
```

---

## 8. 图片与媒体资源适配

### 8.1 响应式图片

#### 8.1.1 图片适配方案

```html
<!-- 响应式图片 -->
<picture>
  <!-- 大屏幕：高清图 -->
  <source 
    media="(min-width: 1200px)" 
    srcset="image-large.jpg 1x, image-large@2x.jpg 2x"
  >
  
  <!-- 平板：中等图 -->
  <source 
    media="(min-width: 768px)" 
    srcset="image-medium.jpg 1x, image-medium@2x.jpg 2x"
  >
  
  <!-- 手机：小图 -->
  <img 
    src="image-small.jpg" 
    srcset="image-small.jpg 1x, image-small@2x.jpg 2x"
    alt="描述文字"
    loading="lazy"
    class="responsive-image"
  >
</picture>
```

#### 8.1.2 CSS图片适配

```css
/* 响应式图片容器 */
.responsive-image-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

/* 保持宽高比 */
.responsive-image-container--16-9 {
  padding-bottom: 56.25%; /* 16:9 */
}

.responsive-image-container--4-3 {
  padding-bottom: 75%; /* 4:3 */
}

.responsive-image-container--1-1 {
  padding-bottom: 100%; /* 1:1 */
}

/* 图片填充 */
.responsive-image-container img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover; /* 或 contain */
}

/* 背景图片适配 */
.responsive-bg-image {
  background-image: url('image-small.jpg');
  background-size: cover;
  background-position: center;
}

@media (min-width: 768px) {
  .responsive-bg-image {
    background-image: url('image-medium.jpg');
  }
}

@media (min-width: 1200px) {
  .responsive-bg-image {
    background-image: url('image-large.jpg');
  }
}
```

### 8.2 视频与媒体适配

```css
/* 响应式视频容器 */
.responsive-video-container {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%; /* 16:9 */
  height: 0;
  overflow: hidden;
}

.responsive-video-container iframe,
.responsive-video-container video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* 移动端视频优化 */
@media (max-width: 767px) {
  .mobile-video {
    /* 禁用自动播放 */
    autoplay: false;
    
    /* 显示播放控件 */
    controls: true;
    
    /* 优化触摸控制 */
    touch-action: manipulation;
  }
}
```

### 8.3 图片懒加载

```html
<!-- 原生懒加载 -->
<img 
  src="placeholder.jpg" 
  data-src="image.jpg" 
  alt="描述"
  loading="lazy"
  class="lazy-image"
>

<!-- 背景图片懒加载 -->
<div 
  class="lazy-bg-image" 
  data-bg="image.jpg"
  style="background-image: url('placeholder.jpg');"
></div>
```

```javascript
// 懒加载实现
const lazyLoad = () => {
  const lazyImages = document.querySelectorAll('.lazy-image')
  const lazyBgImages = document.querySelectorAll('.lazy-bg-image')
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const img = entry.target
        if (img.dataset.src) {
          img.src = img.dataset.src
          img.classList.add('loaded')
        }
        if (img.dataset.bg) {
          img.style.backgroundImage = `url(${img.dataset.bg})`
          img.classList.add('loaded')
        }
        observer.unobserve(img)
      }
    })
  }, {
    rootMargin: '50px 0px',
    threshold: 0.1
  })
  
  lazyImages.forEach(img => observer.observe(img))
  lazyBgImages.forEach(div => observer.observe(div))
}

// 初始化懒加载
document.addEventListener('DOMContentLoaded', lazyLoad)
```

---

## 9. 组件适配指南

### 9.1 通用组件适配规则

| 组件类型 | 移动端适配 | 平板端适配 | 桌面端适配 |
|----------|------------|------------|------------|
| **按钮** | 高度44px+，全宽或大触控区 | 高度40px，自适应宽度 | 高度32-36px，自适应 |
| **输入框** | 高度44px+，字体16px | 高度40px，字体15px | 高度32px，字体14px |
| **表格** | 卡片式列表，垂直堆叠 | 可横向滚动表格 | 完整表格，固定列 |
| **导航** | 底部Tab，汉堡菜单 | 侧边栏+底部Tab | 顶部导航+侧边栏 |
| **弹窗** | 底部弹出，全屏或90%宽 | 居中，80%宽 | 居中，400-600px宽 |
| **表单** | 垂直堆叠，单列 | 双列布局 | 多列布局，行内 |
| **分页** | 加载更多，无限滚动 | 简化分页 | 完整分页控件 |

### 9.2 具体组件适配示例

#### 9.2.1 响应式表格

```vue
<template>
  <div class="responsive-table">
    <!-- 移动端：卡片列表 -->
    <div class="mobile-table" v-if="isMobile">
      <div 
        v-for="item in data" 
        :key="item.id" 
        class="mobile-table-item"
        @click="viewDetail(item)"
      >
        <div class="item-header">
          <h4>{{ item.name }}</h4>
          <span class="status" :class="item.status">{{ item.statusText }}</span>
        </div>
        <div class="item-content">
          <div class="info-row">
            <span class="label">联系人:</span>
            <span class="value">{{ item.contact }}</span>
          </div>
          <div class="info-row">
            <span class="label">电话:</span>
            <span class="value">{{ item.phone }}</span>
          </div>
        </div>
        <div class="item-actions">
          <button @click.stop="editItem(item)">编辑</button>
          <button @click.stop="deleteItem(item)">删除</button>
        </div>
      </div>
    </div>
    
    <!-- 平板/桌面端：标准表格 -->
    <table class="desktop-table" v-else>
      <thead>
        <tr>
          <th>客户名称</th>
          <th>联系人</th>
          <th>电话</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in data" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.contact }}</td>
          <td>{{ item.phone }}</td>
          <td>
            <span class="status" :class="item.status">{{ item.statusText }}</span>
          </td>
          <td class="actions">
            <button @click="editItem(item)">编辑</button>
            <button @click="deleteItem(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useWindowSize } from '@vueuse/core'

const { width } = useWindowSize()
const isMobile = computed(() => width.value < 768)
</script>
```

#### 9.2.2 响应式表单

```css
/* 响应式表单布局 */
.responsive-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 移动端：垂直堆叠 */
@media (max-width: 767px) {
  .responsive-form .form-row {
    display: block;
  }
  
  .responsive-form .form-item {
    margin-bottom: 16px;
  }
  
  .responsive-form .form-label {
    display: block;
    margin-bottom: 8px;
    font-weight: 500;
  }
  
  .responsive-form .form-control {
    width: 100%;
    height: 44px;
    font-size: 16px;
  }
}

/* 平板端：双列布局 */
@media (min-width: 768px) and (max-width: 1199px) {
  .responsive-form {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px 24px;
  }
  
  .responsive-form .form-item--full {
    grid-column: 1 / -1;
  }
}

/* 桌面端：三列布局 */
@media (min-width: 1200px) {
  .responsive-form {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px 32px;
  }
}
```

---

## 10. 性能与可访问性

### 10.1 性能优化

#### 10.1.1 图片优化

```html
<!-- 使用