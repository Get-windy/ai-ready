# AI-Ready Mobile UI 组件库

企智连移动端UI组件库 - 基于Vue 3的移动端组件库

## 简介

`@ai-ready/mobile-ui` 是专为企智连项目打造的移动端UI组件库，提供一套统一、美观、易用的移动端组件。

## 特性

- 🚀 **Vue 3 Composition API** - 使用最新的Vue 3组合式API
- 📱 **移动端优先** - 专为移动端设计的交互和样式
- 🎨 **统一设计规范** - 遵循企智连设计系统
- 🔧 **TypeScript支持** - 完整的类型定义
- 📦 **按需引入** - 支持按需加载组件
- 🎯 **组合式函数** - 提供常用的组合式函数

## 安装

```bash
npm install @ai-ready/mobile-ui
```

## 使用

### 全局引入

```typescript
import { createApp } from 'vue'
import MobileUI from '@ai-ready/mobile-ui'
import '@ai-ready/mobile-ui/styles/index.scss'

const app = createApp(App)
app.use(MobileUI)
```

### 按需引入

```vue
<template>
  <ARButton type="primary" @click="handleClick">点击我</ARButton>
</template>

<script setup>
import { ARButton } from '@ai-ready/mobile-ui'
</script>
```

## 组件列表

### 基础组件

| 组件名 | 说明 | 状态 |
|--------|------|------|
| ARButton | 按钮组件 | ✅ 已完成 |
| ARInput | 输入框组件 | ✅ 已完成 |
| ARList | 列表组件 | ✅ 已完成 |
| ARCard | 卡片组件 | ✅ 已完成 |

### 组合式函数

| 函数名 | 说明 | 状态 |
|--------|------|------|
| useTouch | 触摸手势 | ✅ 已完成 |
| useCountDown | 倒计时 | ✅ 已完成 |
| useDebounce | 防抖/节流 | ✅ 已完成 |

## 组件文档

### ARButton 按钮

按钮组件用于触发操作。

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | 'primary' \| 'success' \| 'warning' \| 'danger' \| 'info' | 'primary' | 按钮类型 |
| variant | 'solid' \| 'outline' \| 'ghost' \| 'text' | 'solid' | 按钮变体 |
| size | 'large' \| 'normal' \| 'small' \| 'mini' | 'normal' | 按钮尺寸 |
| shape | 'default' \| 'round' \| 'circle' | 'default' | 按钮形状 |
| disabled | boolean | false | 是否禁用 |
| loading | boolean | false | 是否加载中 |
| block | boolean | false | 是否块级按钮 |
| icon | string | '' | 图标 |
| iconPosition | 'left' \| 'right' | 'left' | 图标位置 |

#### 示例

```vue
<template>
  <ARButton type="primary" size="large" block>主要按钮</ARButton>
  <ARButton type="success" variant="outline">成功按钮</ARButton>
  <ARButton type="danger" loading>加载中</ARButton>
</template>
```

### ARInput 输入框

输入框组件用于接收用户输入。

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | string | 'text' | 输入框类型 |
| modelValue | string \| number | '' | 绑定值 |
| placeholder | string | '' | 占位符 |
| disabled | boolean | false | 是否禁用 |
| readonly | boolean | false | 是否只读 |
| clearable | boolean | false | 是否可清空 |
| maxlength | number | - | 最大长度 |
| showWordLimit | boolean | false | 是否显示字数统计 |
| showPassword | boolean | false | 是否显示密码切换 |
| size | 'large' \| 'normal' \| 'small' | 'normal' | 尺寸 |

#### 示例

```vue
<template>
  <ARInput v-model="value" placeholder="请输入" clearable />
  <ARInput type="password" v-model="password" showPassword />
</template>
```

### ARList 列表

列表组件用于展示一组数据。

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| items | Array | [] | 列表数据 |
| itemKey | string | 'id' | 项的唯一标识字段 |
| loading | boolean | false | 是否加载中 |
| itemClickable | boolean | true | 项是否可点击 |
| showArrow | boolean | true | 是否显示箭头 |
| divider | boolean | true | 是否显示分割线 |
| emptyText | string | '暂无数据' | 空状态文本 |

#### 示例

```vue
<template>
  <ARList
    :items="listData"
    @click="handleItemClick"
  >
    <template #default="{ item, index }">
      <div class="custom-item">{{ item.title }}</div>
    </template>
  </ARList>
</template>
```

### ARCard 卡片

卡片组件用于展示内容块。

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| title | string | '' | 卡片标题 |
| subtitle | string | '' | 卡片副标题 |
| extra | string | '' | 额外内容 |
| cover | string | '' | 封面图片 |
| bordered | boolean | false | 是否显示边框 |
| hoverable | boolean | false | 是否可悬浮 |
| shadow | 'none' \| 'sm' \| 'md' \| 'lg' | 'sm' | 阴影大小 |
| actions | Array | [] | 操作按钮 |

#### 示例

```vue
<template>
  <ARCard
    title="卡片标题"
    subtitle="卡片副标题"
    cover="https://example.com/image.jpg"
    :actions="[
      { text: '查看', type: 'primary' },
      { text: '删除', type: 'danger' }
    ]"
    @action="handleAction"
  >
    <p>卡片内容</p>
  </ARCard>
</template>
```

## 组合式函数

### useTouch

触摸手势组合式函数。

```typescript
import { useTouch } from '@ai-ready/mobile-ui'

const touch = useTouch({ threshold: 50 })

// 在模板中使用
// @touchstart="touch.start"
// @touchmove="touch.move"
// @touchend="touch.end"
```

### useCountDown

倒计时组合式函数。

```typescript
import { useCountDown } from '@ai-ready/mobile-ui'

const countdown = useCountDown({
  total: 60,
  autoStart: true,
  onFinish: () => console.log('倒计时结束')
})

// countdown.formatted.value - 格式化后的时间
// countdown.start() - 开始
// countdown.pause() - 暂停
// countdown.reset() - 重置
```

### useDebounce / useThrottle

防抖和节流组合式函数。

```typescript
import { useDebounce, useThrottle } from '@ai-ready/mobile-ui'

const debouncedFn = useDebounce((value) => {
  console.log('搜索:', value)
}, { wait: 300 })

const throttledFn = useThrottle(() => {
  console.log('滚动事件')
}, { wait: 200 })
```

## 样式变量

组件库提供了一套CSS变量，可以在项目中自定义主题。

```scss
:root {
  --ar-color-primary: #1989fa;
  --ar-color-success: #07c160;
  --ar-color-warning: #ff976a;
  --ar-color-danger: #ee0a24;
  --ar-color-info: #969799;
  
  --ar-font-size-md: 14px;
  --ar-border-radius-lg: 8px;
  // ...
}
```

## 浏览器支持

- iOS Safari >= 10
- Android Chrome >= 60
- 微信内置浏览器 >= 7.0

## 贡献

欢迎提交Issue和PR！

## License

MIT
