# AI-Ready 基础组件库

> 专为 Sprint 27+1 测试环境开发的前端组件库
> 基于 Vue 3 + TypeScript + Element Plus 构建

---

## 组件清单

### 基础组件

| 组件名 | 说明 | 文件路径 |
|--------|------|----------|
| ARButton | 按钮组件 | `components/base/ARButton.vue` |
| ARInput | 输入框组件 | `components/base/ARInput.vue` |
| ARCard | 卡片组件 | `components/base/ARCard.vue` |

### 表单组件

| 组件名 | 说明 | 文件路径 |
|--------|------|----------|
| ARForm | 表单组件 | `components/base/ARForm.vue` |
| ARFormItem | 表单项组件 | `components/base/ARFormItem.vue` |

---

## 设计规范

### 色彩系统

```css
/* 主色调 */
--ar-primary: #1890FF;
--ar-primary-light: #40A9FF;
--ar-primary-dark: #096DD9;

/* 功能色 */
--ar-success: #52C41A;
--ar-warning: #FAAD14;
--ar-error: #F5222D;
--ar-info: #1890FF;

/* 中性色 */
--ar-text-primary: #262626;
--ar-text-regular: #595959;
--ar-text-secondary: #8C8C8C;
```

### 字体系统

```css
--ar-font-size-xl: 30px;
--ar-font-size-lg: 24px;
--ar-font-size-md: 20px;
--ar-font-size-base: 14px;
--ar-font-size-sm: 12px;
```

### 间距系统

```css
--ar-space-1: 4px;
--ar-space-2: 8px;
--ar-space-3: 12px;
--ar-space-4: 16px;
--ar-space-5: 20px;
--ar-space-6: 24px;
```

---

## 使用方法

### 全局注册

```typescript
// main.ts
import { createApp } from 'vue'
import ARCommon from './components/@ai-ready/common/components'

const app = createApp(App)
app.use(ARCommon)
app.mount('#app')
```

### 按需引入

```vue
<script setup lang="ts">
import { ARButton, ARInput } from '@/components/@ai-ready/common/components'
</script>

<template>
  <ARButton type="primary">点击我</ARButton>
  <ARInput v-model="inputValue" placeholder="请输入内容" />
</template>
```

---

## 文件结构

```
common/
├── components/
│   ├── index.ts          # 组件库入口
│   └── base/
│       ├── ARButton.vue   # 按钮组件
│       ├── ARInput.vue    # 输入框组件
│       ├── ARCard.vue     # 卡片组件
│       ├── ARForm.vue     # 表单组件
│       └── ARFormItem.vue # 表单项组件
├── demo/
│   └── ComponentDemo.vue  # 组件展示页面
└── styles/
    └── design-tokens.css  # 设计令牌
```

---

## 特性

- ✅ TypeScript 类型支持
- ✅ 深色模式支持
- ✅ 响应式设计
- ✅ 无障碍支持
- ✅ 表单验证
- ✅ 全局主题定制

---

## 文档

- [设计规范文档](../../../docs/design-system/design-specification.md)
- [组件使用文档](../../../docs/design-system/components-usage.md)

---

**版本**: v1.0
**创建日期**: 2026-04-27
**维护者**: ui-mnj0fukd