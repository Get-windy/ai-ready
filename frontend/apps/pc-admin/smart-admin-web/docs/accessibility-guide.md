# AI-Ready 前端无障碍访问实现文档

## 概述

本项目实现了完整的前端无障碍访问功能，遵循 WCAG 2.1 标准，支持屏幕阅读器、键盘导航、高对比度模式等。

## 核心文件

```
src/
├── utils/accessibility.ts    # 无障碍工具函数
├── styles/accessibility.css  # 无障碍样式
└── docs/accessibility-guide.md
```

## 功能特性

### 1. 焦点管理

```typescript
import { focusManager } from '@/utils/accessibility'

// 聚焦第一个可聚焦元素
focusManager.focusFirst(container)

// 陷阱焦点（模态框）
const cleanup = focusManager.trapFocus(modalElement)

// 保存和恢复焦点
focusManager.saveFocus()
focusManager.restoreFocus()
```

### 2. 键盘导航

```vue
<script setup>
import { useKeyboardNavigation } from '@/utils/accessibility'

const container = ref(null)

useKeyboardNavigation(container, {
  onEnter: () => handleSelect(),
  onEscape: () => handleClose(),
  onArrowUp: () => handlePrevious(),
  onArrowDown: () => handleNext()
})
</script>
```

### 3. 高对比度模式

```vue
<script setup>
import { useHighContrast } from '@/utils/accessibility'

const { isHighContrast, toggleHighContrast } = useHighContrast()
</script>

<template>
  <a-button @click="toggleHighContrast">
    {{ isHighContrast ? '关闭' : '开启' }}高对比度
  </a-button>
</template>
```

### 4. 屏幕阅读器公告

```vue
<script setup>
import { useAnnounce } from '@/utils/accessibility'

const { announce } = useAnnounce()

// 公告消息
announce('操作成功') // 礼貌模式
announce('错误：请填写必填项', 'assertive') // 强制模式
</script>
```

### 5. 键盘快捷键

```vue
<script setup>
import { useKeyboardShortcuts } from '@/utils/accessibility'

useKeyboardShortcuts([
  { key: 's', ctrl: true, handler: () => handleSave() },
  { key: '/', handler: () => focusSearch() },
  { key: 'Escape', handler: () => closeModal() }
])
</script>
```

### 6. 跳过导航链接

```vue
<template>
  <a href="#main-content" class="skip-link" @click.prevent="skipToContent">
    跳到主要内容
  </a>
  
  <nav><!-- 导航内容 --></nav>
  
  <main id="main-content">
    <!-- 主要内容 -->
  </main>
</template>
```

### 7. 减少动画模式

```vue
<script setup>
import { useReducedMotion } from '@/utils/accessibility'

const { prefersReducedMotion } = useReducedMotion()
</script>
```

## ARIA 标签使用

### 角色 (role)

```html
<!-- 对话框 -->
<div role="dialog" aria-labelledby="dialog-title">
  <h2 id="dialog-title">标题</h2>
</div>

<!-- 菜单 -->
<ul role="menu">
  <li role="menuitem">选项1</li>
  <li role="menuitem">选项2</li>
</ul>

<!-- 标签页 -->
<div role="tablist">
  <button role="tab" aria-selected="true">标签1</button>
  <button role="tab">标签2</button>
</div>

<!-- 进度条 -->
<div role="progressbar" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100">
  <div style="width: 50%"></div>
</div>
```

### 状态属性

```html
<!-- 展开状态 -->
<button aria-expanded="false" aria-controls="menu1">展开菜单</button>

<!-- 禁用状态 -->
<button aria-disabled="true">禁用按钮</button>

<!-- 选中状态 -->
<div role="option" aria-selected="true">选项A</div>

<!-- 当前页面 -->
<a href="/" aria-current="page">首页</a>

<!-- 隐藏元素 -->
<div aria-hidden="true">装饰性内容</div>

<!-- 忙碌状态 -->
<div aria-busy="true">加载中...</div>
```

## 高对比度模式

启用高对比度模式后，HTML 根元素会添加 `high-contrast` 类：

```css
.high-contrast {
  --text-color: #000;
  --background-color: #fff;
  --primary-color: #000;
}
```

## 完成标准

✓ 添加ARIA标签和属性
✓ 实现键盘导航支持
✓ 优化屏幕阅读器兼容
✓ 实现高对比度模式
✓ 输出无障碍访问实现代码

---

_无障碍访问实现完成。_