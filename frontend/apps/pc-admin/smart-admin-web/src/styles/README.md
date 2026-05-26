# AI-Ready 主题样式使用指南

## 概述

AI-Ready 项目现在提供了统一的主题样式系统，包括颜色变量、组件样式和工具类，确保整个应用的视觉一致性。

## 样式文件结构

```
src/styles/
├── variables.css    # CSS变量定义（颜色、间距、字体等）
├── components.css   # 组件样式（按钮、表单、表格、卡片等）
└── index.css       # 全局样式和工具类
```

## 使用指南

### 1. 颜色变量

使用CSS变量来访问预定义的颜色：

```css
.custom-button {
  background-color: var(--color-primary);
  color: #fff;
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--border-radius-base);
}
```

#### 可用的颜色变量

**主题色**
- `--color-primary` - 主蓝色
- `--color-success` - 成功绿色
- `--color-warning` - 警告黄色
- `--color-danger` - 危险红色
- `--color-info` - 信息灰色

**中性色**
- `--color-text-primary` - 主要文本
- `--color-text-secondary` - 次要文本
- `--color-text-tertiary` - 三级文本
- `--color-border-base` - 边框色
- `--color-bg-base` - 背景色

### 2. 组件样式

#### 按钮样式

```html
<!-- 主按钮 -->
<button class="ai-btn ai-btn-primary">主要按钮</button>

<!-- 默认按钮 -->
<button class="ai-btn ai-btn-default">默认按钮</button>

<!-- 危险按钮 -->
<button class="ai-btn ai-btn-danger">危险按钮</button>

<!-- 成功按钮 -->
<button class="ai-btn ai-btn-success">成功按钮</button>

<!-- 大小变体 -->
<button class="ai-btn ai-btn-primary ai-btn-lg">大按钮</button>
<button class="ai-btn ai-btn-primary ai-btn-sm">小按钮</button>
```

#### 表单样式

```html
<div class="ai-form-item ai-form-item-required">
  <label class="ai-form-item-label">用户名</label>
  <input type="text" class="ai-input" placeholder="请输入用户名">
  <div class="ai-form-item-error">用户名不能为空</div>
</div>

<div class="ai-form-item">
  <label class="ai-form-item-label">角色</label>
  <select class="ai-select">
    <option value="">请选择角色</option>
    <option value="admin">管理员</option>
    <option value="user">普通用户</option>
  </select>
</div>

<div class="ai-form-item">
  <label class="ai-form-item-label">描述</label>
  <textarea class="ai-input ai-textarea" placeholder="请输入描述"></textarea>
</div>
```

#### 表格样式

```html
<div class="ai-table-container">
  <table class="ai-table">
    <thead>
      <tr>
        <th>姓名</th>
        <th>年龄</th>
        <th>城市</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>张三</td>
        <td>25</td>
        <td>北京</td>
      </tr>
      <tr class="ai-table-row-selected">
        <td>李四</td>
        <td>30</td>
        <td>上海</td>
      </tr>
    </tbody>
  </table>
  
  <div class="ai-table-pagination">
    <div class="ai-table-pagination-info">
      共 100 条记录，当前第 1 页
    </div>
    <div class="ai-table-pagination-controls">
      <button class="ai-btn ai-btn-sm ai-btn-default">上一页</button>
      <button class="ai-btn ai-btn-sm ai-btn-default">下一页</button>
    </div>
  </div>
</div>
```

#### 卡片样式

```html
<!-- 基础卡片 -->
<div class="ai-card">
  <div class="ai-card-header">
    <h3 class="ai-card-title">卡片标题</h3>
    <p class="ai-card-subtitle">卡片副标题</p>
  </div>
  <div class="ai-card-body">
    <p>卡片内容...</p>
  </div>
  <div class="ai-card-footer">
    <button class="ai-btn ai-btn-primary">确定</button>
    <button class="ai-btn ai-btn-default">取消</button>
  </div>
</div>

<!-- 带边框卡片 -->
<div class="ai-card ai-card-bordered">
  <div class="ai-card-body">
    <p>带边框的卡片内容</p>
  </div>
</div>

<!-- 可悬停卡片 -->
<div class="ai-card ai-card-hoverable">
  <div class="ai-card-body">
    <p>可悬停的卡片内容</p>
  </div>
</div>
```

#### 徽章样式

```html
<span class="ai-badge ai-badge-primary">主色徽章</span>
<span class="ai-badge ai-badge-success">成功徽章</span>
<span class="ai-badge ai-badge-warning">警告徽章</span>
<span class="ai-badge ai-badge-danger">危险徽章</span>
```

#### 分割线样式

```html
<!-- 水平分割线 -->
<div class="ai-divider">
  <span class="ai-divider-text">分割线文本</span>
</div>

<!-- 垂直分割线 -->
<div class="ai-divider-vertical"></div>
```

#### 标签样式

```html
<span class="ai-tag ai-tag-primary">主色标签</span>
<span class="ai-tag ai-tag-success">成功标签</span>
<span class="ai-tag ai-tag-warning">警告标签</span>
<span class="ai-tag ai-tag-danger">危险标签</span>
<span class="ai-tag ai-tag-closeable">可关闭标签</span>
```

### 3. 工具类

#### 文本工具类

```html
<p class="text-primary">主色文本</p>
<p class="text-success">成功文本</p>
<p class="text-warning">警告文本</p>
<p class="text-danger">危险文本</p>
<p class="text-muted">次要文本</p>

<p class="text-xs">小号文本</p>
<p class="text-sm">小号文本</p>
<p class="text-base">基础文本</p>
<p class="text-lg">大号文本</p>
<p class="text-xl">超大文本</p>

<p class="font-normal">正常粗细</p>
<p class="font-medium">中等粗细</p>
<p class="font-bold">加粗文本</p>
```

#### 间距工具类

```html
<!-- Margin -->
<div class="mt-16">顶部间距 16px</div>
<div class="mb-16">底部间距 16px</div>
<div class="ml-8">左侧间距 8px</div>
<div class="mr-8">右侧间距 8px</div>
<div class="mx-16">左右间距 16px</div>
<div class="my-16">上下间距 16px</div>

<!-- Padding -->
<div class="pt-16">顶部内边距 16px</div>
<div class="pb-16">底部内边距 16px</div>
<div class="pl-8">左侧内边距 8px</div>
<div class="pr-8">右侧内边距 8px</div>
<div class="px-16">左右内边距 16px</div>
<div class="py-16">上下内边距 16px</div>
```

#### 布局工具类

```html
<!-- Flex 布局 -->
<div class="flex items-center justify-center">
  <div>居中对齐</div>
</div>

<div class="flex items-center justify-between">
  <div>左侧内容</div>
  <div>右侧内容</div>
</div>

<div class="flex items-center justify-around">
  <div>内容 1</div>
  <div>内容 2</div>
  <div>内容 3</div>
</div>

<!-- Grid 布局 -->
<div class="grid grid-cols-3 gap-16">
  <div>列 1</div>
  <div>列 2</div>
  <div>列 3</div>
</div>
```

#### 显示/隐藏

```html
<div class="d-none">隐藏元素</div>
<div class="d-block">块级显示</div>
<div class="d-inline-block">行内块显示</div>
<div class="d-flex">Flex 显示</div>

<!-- 响应式隐藏 -->
<div class="hidden-xs">小屏幕隐藏</div>
<div class="hidden-sm">平板隐藏</div>
<div class="hidden-md">中屏幕隐藏</div>
<div class="hidden-lg">大屏幕隐藏</div>
```

#### 边框和圆角

```html
<div class="border">边框</div>
<div class="border-top">上边框</div>
<div class="border-bottom">下边框</div>
<div class="border-left">左边框</div>
<div class="border-right">右边框</div>

<div class="rounded">圆角</div>
<div class="rounded-lg">大圆角</div>
<div class="rounded-full">圆形</div>
```

#### 阴影

```html
<div class="shadow-sm">小阴影</div>
<div class="shadow">基础阴影</div>
<div class="shadow-md">中等阴影</div>
<div class="shadow-lg">大阴影</div>
```

#### 背景色

```html
<div class="bg-white">白色背景</div>
<div class="bg-light">浅色背景</div>
<div class="bg-primary">主色背景</div>
<div class="bg-success">成功背景</div>
<div class="bg-warning">警告背景</div>
<div class="bg-danger">危险背景</div>
```

### 4. 在 Vue 组件中使用

#### 使用内联样式

```vue
<template>
  <div>
    <button 
      class="ai-btn ai-btn-primary ai-btn-lg"
      @click="handleClick"
    >
      主要按钮
    </button>
  </div>
</template>

<script setup lang="ts">
const handleClick = () => {
  console.log('按钮被点击')
}
</script>
```

#### 使用 CSS 变量

```vue
<template>
  <div class="custom-container">
    <h1>标题</h1>
    <p>内容</p>
  </div>
</template>

<style scoped>
.custom-container {
  padding: var(--spacing-lg);
  background-color: var(--color-bg-container);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-base);
}

.custom-container h1 {
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-md);
}

.custom-container p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
</style>
```

#### 动态样式绑定

```vue
<template>
  <div>
    <button 
      :class="[
        'ai-btn',
        {
          'ai-btn-primary': buttonType === 'primary',
          'ai-btn-danger': buttonType === 'danger',
          'ai-btn-success': buttonType === 'success',
          'ai-btn-disabled': disabled
        },
        `ai-btn-${size}`
      ]"
      :disabled="disabled"
    >
      {{ buttonText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const buttonType = ref<'primary' | 'danger' | 'success'>('primary')
const size = ref<'lg' | 'sm' | ''>('lg')
const disabled = ref(false)
const buttonText = ref('点击我')
</script>
```

### 5. 暗色模式支持

主题样式系统内置了暗色模式支持，使用 `prefers-color-scheme` 媒体查询自动适配系统设置：

```css
@media (prefers-color-scheme: dark) {
  :root {
    /* 暗色模式下的颜色变量 */
  }
}
```

用户在系统设置中切换到暗色模式时，应用会自动应用暗色主题。

### 6. 自定义主题

如果需要自定义主题色，可以在项目的全局样式文件中覆盖CSS变量：

```css
/* src/styles/custom-theme.css */
:root {
  /* 自定义主色 */
  --color-primary: #ff6b6b;
  --color-primary-hover: #ff8787;
  --color-primary-active: #fa5252;
  
  /* 自定义间距 */
  --spacing-lg: 20px;
  --spacing-xl: 24px;
  
  /* 自定义圆角 */
  --border-radius-base: 6px;
  --border-radius-lg: 12px;
}
```

然后在 `main.ts` 中引入：

```typescript
import './styles/custom-theme.css'
```

### 7. 最佳实践

1. **优先使用预定义的组件样式**：使用 `ai-btn`、`ai-input` 等预定义的组件样式，而不是从头开始编写样式。

2. **使用CSS变量而非硬编码颜色**：使用 `var(--color-primary)` 而不是 `#1890ff`，便于主题切换和维护。

3. **合理使用工具类**：在快速原型开发或简单布局时使用工具类，复杂组件仍建议使用CSS或scoped样式。

4. **保持样式一致性**：在整个项目中保持使用相同的主题色和间距系统。

5. **响应式设计**：使用预定义的响应式工具类（如 `hidden-xs`、`hidden-sm`）来处理不同屏幕尺寸。

### 8. 浏览器兼容性

主题样式系统支持所有现代浏览器：
- Chrome/Edge (最新版本)
- Firefox (最新版本)
- Safari (最新版本)
- iOS Safari (iOS 14+)
- Android Chrome (Android 8+)

不支持IE浏览器。

### 9. 迁移指南

如果你的项目已有自定义样式，建议逐步迁移到新的主题样式系统：

1. **第一步**：引入新的样式文件
2. **第二步**：替换硬编码的颜色为CSS变量
3. **第三步**：使用预定义的组件样式替换自定义样式
4. **第四步**：删除不再使用的旧样式代码

### 10. 常见问题

**Q: 如何添加新的颜色变量？**

A: 在 `variables.css` 文件的 `:root` 选择器中添加新的CSS变量。

**Q: 如何自定义组件样式？**

A: 可以创建自己的CSS文件覆盖默认样式，或者在组件中使用 `scoped` 样式。

**Q: 主题样式系统支持Sass/Less吗？**

A: 目前主题样式系统使用纯CSS编写，但可以在Vue组件中结合Sass/Less使用。

**Q: 如何禁用暗色模式？**

A: 可以在 `variables.css` 中删除或注释掉 `@media (prefers-color-scheme: dark)` 部分。

## 更新日志

### 2026-04-15
- ✅ 创建主题色变量系统
- ✅ 实现组件统一样式（按钮、表单、表格、卡片等）
- ✅ 添加丰富的工具类
- ✅ 支持暗色模式
- ✅ 创建使用指南文档

## 贡献

如果你发现了问题或有改进建议，欢迎提交Issue或Pull Request。