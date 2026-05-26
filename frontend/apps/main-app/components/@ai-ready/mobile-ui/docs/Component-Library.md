# AI-Ready Mobile UI 组件库文档

**版本**: 1.0.0  
**更新日期**: 2026-04-25  
**框架**: Vue 3.4 + TypeScript + SCSS

---

## 目录

1. [快速开始](#快速开始)
2. [设计规范](#设计规范)
3. [组件列表](#组件列表)
4. [样式变量](#样式变量)
5. [工具类](#工具类)
6. [最佳实践](#最佳实践)

---

## 快速开始

### 安装

```bash
npm install @ai-ready/mobile-ui
```

### 引入样式

```typescript
// main.ts
import '@ai-ready/mobile-ui/styles/index.scss'
```

### 使用组件

```vue
<template>
  <ar-button type="primary" @click="handleClick">
    点击我
  </ar-button>
</template>

<script setup>
import { ARButton } from '@ai-ready/mobile-ui'

const handleClick = () => {
  console.log('按钮被点击')
}
</script>
```

---

## 设计规范

### 色彩系统

#### 品牌色
| 名称 | 变量 | 色值 | 用途 |
|------|------|------|------|
| 主色 | `--ar-color-primary` | #1989fa | 主按钮、链接、高亮 |
| 成功 | `--ar-color-success` | #07c160 | 成功状态、通过 |
| 警告 | `--ar-color-warning` | #ff976a | 警告、提示 |
| 危险 | `--ar-color-danger` | #ee0a24 | 错误、删除 |
| 信息 | `--ar-color-info` | #969799 | 次要信息 |

#### 文字色
| 名称 | 变量 | 色值 | 用途 |
|------|------|------|------|
| 主要文字 | `--ar-color-text-primary` | #323233 | 标题、正文 |
| 次要文字 | `--ar-color-text-secondary` | #646566 | 副标题 |
| 辅助文字 | `--ar-color-text-tertiary` | #969799 | 说明文字 |
| 禁用文字 | `--ar-color-text-quaternary` | #c8c9cc | 禁用状态 |

### 字体规范

#### 字体大小
| 名称 | 变量 | 大小 | 用途 |
|------|------|------|------|
| 超小 | `--ar-font-size-xs` | 10px | 标签、徽章 |
| 小 | `--ar-font-size-sm` | 12px | 辅助文字 |
| 正常 | `--ar-font-size-md` | 14px | 正文 |
| 大 | `--ar-font-size-lg` | 16px | 小标题 |
| 超大 | `--ar-font-size-xl` | 18px | 标题 |
| 特大 | `--ar-font-size-xxl` | 20px | 大标题 |

#### 字重
| 名称 | 变量 | 值 | 用途 |
|------|------|-----|------|
| 正常 | `$font-weight-normal` | 400 | 正文 |
| 加粗 | `$font-weight-bold` | 500 | 按钮、标签 |
| 特粗 | `$font-weight-bolder` | 600 | 标题 |

### 间距规范

| 名称 | 变量 | 值 | 用途 |
|------|------|-----|------|
| 超小 | `--ar-spacing-xs` | 4px | 图标间距 |
| 小 | `--ar-spacing-sm` | 8px | 紧凑间距 |
| 正常 | `--ar-spacing-md` | 12px | 默认间距 |
| 大 | `--ar-spacing-lg` | 16px | 卡片内边距 |
| 超大 | `--ar-spacing-xl` | 20px | 模块间距 |
| 特大 | `--ar-spacing-xxl` | 24px | 大模块间距 |

### 圆角规范

| 名称 | 变量 | 值 | 用途 |
|------|------|-----|------|
| 小 | `--ar-border-radius-sm` | 2px | 标签、徽章 |
| 正常 | `--ar-border-radius-md` | 4px | 输入框 |
| 大 | `--ar-border-radius-lg` | 8px | 按钮 |
| 超大 | `--ar-border-radius-xl` | 12px | 卡片 |
| 最大 | `--ar-border-radius-max` | 999px | 胶囊形 |

---

## 组件列表

### ARButton 按钮

#### 基础用法
```vue
<ar-button type="primary">主要按钮</ar-button>
<ar-button type="success">成功按钮</ar-button>
<ar-button type="warning">警告按钮</ar-button>
<ar-button type="danger">危险按钮</ar-button>
```

#### Props
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | `'primary' \| 'success' \| 'warning' \| 'danger' \| 'info'` | `'primary'` | 按钮类型 |
| variant | `'solid' \| 'outline' \| 'ghost' \| 'text'` | `'solid'` | 按钮变体 |
| size | `'large' \| 'normal' \| 'small' \| 'mini'` | `'normal'` | 按钮尺寸 |
| shape | `'default' \| 'round' \| 'circle'` | `'default'` | 按钮形状 |
| disabled | `boolean` | `false` | 是否禁用 |
| loading | `boolean` | `false` | 是否加载中 |
| block | `boolean` | `false` | 是否块级按钮 |
| icon | `string` | `''` | 图标 |
| iconPosition | `'left' \| 'right'` | `'left'` | 图标位置 |

#### Events
| 事件 | 说明 | 回调参数 |
|------|------|----------|
| click | 点击时触发 | `event: MouseEvent` |
| touchstart | 触摸开始时触发 | `event: TouchEvent` |
| touchend | 触摸结束时触发 | `event: TouchEvent` |

---

### ARCard 卡片

#### 基础用法
```vue
<ar-card title="卡片标题" subtitle="副标题">
  卡片内容
</ar-card>
```

#### Props
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| title | `string` | `''` | 标题 |
| subtitle | `string` | `''` | 副标题 |
| extra | `string` | `''` | 额外内容 |
| cover | `string` | `''` | 封面图片URL |
| bordered | `boolean` | `false` | 是否显示边框 |
| hoverable | `boolean` | `false` | 是否可点击 |
| loading | `boolean` | `false` | 是否加载中 |
| shadow | `'none' \| 'sm' \| 'md' \| 'lg'` | `'sm'` | 阴影大小 |
| actions | `CardAction[]` | `[]` | 底部操作按钮 |

#### Slots
| 插槽 | 说明 |
|------|------|
| default | 卡片内容 |
| cover | 自定义封面 |
| header | 自定义头部 |
| extra | 自定义额外内容 |
| footer | 自定义底部 |

---

### ARInput 输入框

#### 基础用法
```vue
<ar-input
  v-model="value"
  placeholder="请输入内容"
  clearable
/>
```

#### Props
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | `'text' \| 'password' \| 'number' \| 'tel' \| 'email' \| 'url' \| 'search'` | `'text'` | 输入框类型 |
| modelValue | `string \| number` | `''` | 绑定值 |
| placeholder | `string` | `''` | 占位文本 |
| disabled | `boolean` | `false` | 是否禁用 |
| readonly | `boolean` | `false` | 是否只读 |
| clearable | `boolean` | `false` | 是否可清空 |
| maxlength | `number` | `undefined` | 最大长度 |
| showWordLimit | `boolean` | `false` | 是否显示字数统计 |
| size | `'large' \| 'normal' \| 'small'` | `'normal'` | 输入框尺寸 |
| showPassword | `boolean` | `false` | 是否显示密码切换 |
| prefixIcon | `string` | `''` | 前缀图标 |
| suffixIcon | `string` | `''` | 后缀图标 |
| error | `string` | `''` | 错误提示 |

#### Events
| 事件 | 说明 | 回调参数 |
|------|------|----------|
| update:modelValue | 值改变时触发 | `value: string` |
| input | 输入时触发 | `value: string` |
| change | 值改变时触发 | `value: string` |
| focus | 获得焦点时触发 | `event: FocusEvent` |
| blur | 失去焦点时触发 | `event: FocusEvent` |
| clear | 清空时触发 | - |
| keydown | 键盘按下时触发 | `event: KeyboardEvent` |

#### Methods
| 方法 | 说明 |
|------|------|
| focus | 使输入框获得焦点 |
| blur | 使输入框失去焦点 |
| clear | 清空输入框 |
| setError | 设置错误提示 |
| clearError | 清除错误提示 |

#### Slots
| 插槽 | 说明 |
|------|------|
| prefix | 前缀内容 |
| suffix | 后缀内容 |

---

### ARList 列表

#### 基础用法
```vue
<ar-list :items="listItems" @click="handleClick" />
```

#### Props
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| items | `ListItem[]` | `[]` | 列表数据 |
| itemKey | `string` | `'id'` | 项的唯一标识字段 |
| loading | `boolean` | `false` | 是否加载中 |
| itemClickable | `boolean` | `true` | 列表项是否可点击 |
| showArrow | `boolean` | `true` | 是否显示箭头 |
| divider | `boolean` | `true` | 是否显示分割线 |
| emptyText | `string` | `'暂无数据'` | 空状态文本 |
| loadingText | `string` | `'加载中...'` | 加载文本 |

#### ListItem 结构
```typescript
interface ListItem {
  id?: string | number
  title?: string
  description?: string
  image?: string
  icon?: string
  tag?: string
  tagType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  rightText?: string
  [key: string]: unknown
}
```

#### Events
| 事件 | 说明 | 回调参数 |
|------|------|----------|
| click | 点击列表项时触发 | `item: ListItem, index: number` |

#### Slots
| 插槽 | 说明 |
|------|------|
| default | 自定义列表项内容 |
| empty | 自定义空状态 |

---

## 样式变量

### 完整变量列表

```scss
// 品牌色
$color-primary: #1989fa;
$color-success: #07c160;
$color-warning: #ff976a;
$color-danger: #ee0a24;
$color-info: #969799;

// 文字色
$color-text-primary: #323233;
$color-text-secondary: #646566;
$color-text-tertiary: #969799;
$color-text-quaternary: #c8c9cc;

// 背景色
$color-background: #f7f8fa;
$color-background-white: #ffffff;
$color-background-gray: #f2f3f5;

// 边框色
$color-border: #ebedf0;
$color-border-dark: #dcdee0;

// 字体大小
$font-size-xs: 10px;
$font-size-sm: 12px;
$font-size-md: 14px;
$font-size-lg: 16px;
$font-size-xl: 18px;
$font-size-xxl: 20px;

// 字体粗细
$font-weight-normal: 400;
$font-weight-bold: 500;
$font-weight-bolder: 600;

// 间距
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 12px;
$spacing-lg: 16px;
$spacing-xl: 20px;
$spacing-xxl: 24px;

// 圆角
$border-radius-sm: 2px;
$border-radius-md: 4px;
$border-radius-lg: 8px;
$border-radius-xl: 12px;
$border-radius-max: 999px;

// 阴影
$box-shadow-sm: 0 2px 4px rgba(0, 0, 0, 0.05);
$box-shadow-md: 0 4px 8px rgba(0, 0, 0, 0.08);
$box-shadow-lg: 0 8px 16px rgba(0, 0, 0, 0.12);

// 动画
$animation-duration-fast: 0.2s;
$animation-duration-normal: 0.3s;
$animation-duration-slow: 0.5s;
$animation-timing-function: cubic