# 基础组件库使用文档

> AI-Ready 前端基础组件库使用指南
> 创建日期: 2026-04-27
> 版本: v1.0

---

## 目录

1. [快速开始](#快速开始)
2. [ARButton 按钮组件](#arbutton-按钮组件)
3. [ARInput 输入框组件](#arinput-输入框组件)
4. [ARCard 卡片组件](#arcard-卡片组件)
5. [ARForm 表单组件](#arform-表单组件)
6. [ARFormItem 表单项组件](#arformitem-表单项组件)

---

## 快速开始

### 全局注册

```typescript
// main.ts
import { createApp } from 'vue'
import ARCommon from '@/components/@ai-ready/common/components'

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

## ARButton 按钮组件

### 基础用法

```vue
<template>
  <ARButton>默认按钮</ARButton>
  <ARButton type="primary">主要按钮</ARButton>
  <ARButton type="success">成功按钮</ARButton>
  <ARButton type="warning">警告按钮</ARButton>
  <ARButton type="danger">危险按钮</ARButton>
</template>
```

### 尺寸

```vue
<template>
  <ARButton size="large">大号按钮</ARButton>
  <ARButton>默认按钮</ARButton>
  <ARButton size="small">小号按钮</ARButton>
</template>
```

### 禁用状态

```vue
<template>
  <ARButton disabled>禁用按钮</ARButton>
  <ARButton type="primary" disabled>禁用主按钮</ARButton>
</template>
```

### Loading 状态

```vue
<template>
  <ARButton loading>加载中</ARButton>
  <ARButton type="primary" loading>提交中</ARButton>
</template>
```

### 图标按钮

```vue
<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
</script>

<template>
  <ARButton icon="Search">搜索</ARButton>
  <ARButton type="primary" :icon="Search">搜索</ARButton>
</template>
```

### 圆形和圆角

```vue
<template>
  <ARButton round>圆角按钮</ARButton>
  <ARButton circle>
    <el-icon><Search /></el-icon>
  </ARButton>
</template>
```

### Props

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| type | 类型 | string | primary/default/dashed/text/link | default |
| size | 尺寸 | string | large/default/small | default |
| plain | 朴素按钮 | boolean | - | false |
| round | 圆角按钮 | boolean | - | false |
| circle | 圆形按钮 | boolean | - | false |
| loading | 加载状态 | boolean | - | false |
| disabled | 禁用状态 | boolean | - | false |
| icon | 图标类名 | string | - | - |
| block | 块级按钮 | boolean | - | false |

### Events

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| click | 点击事件 | event: MouseEvent |

---

## ARInput 输入框组件

### 基础用法

```vue
<script setup lang="ts">
import { ref } from 'vue'

const inputValue = ref('')
</script>

<template>
  <ARInput v-model="inputValue" placeholder="请输入内容" />
</template>
```

### 禁用状态

```vue
<template>
  <ARInput disabled placeholder="禁用状态" />
</template>
```

### 可清空

```vue
<template>
  <ARInput v-model="inputValue" clearable placeholder="可清空" />
</template>
```

### 图标

```vue
<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
</script>

<template>
  <ARInput v-model="inputValue" prefix-icon="User" placeholder="请输入用户名" />
  <ARInput v-model="passwordValue" prefix-icon="Lock" type="password" placeholder="请输入密码" />
</template>
```

### 尺寸

```vue
<template>
  <ARInput size="large" placeholder="大号输入框" />
  <ARInput placeholder="默认输入框" />
  <ARInput size="small" placeholder="小号输入框" />
</template>
```

### 字数限制

```vue
<template>
  <ARInput v-model="inputValue" maxlength="50" show-word-limit placeholder="限制字数" />
</template>
```

### Props

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| type | 输入框类型 | string | text/password/number/email/url/tel | text |
| modelValue | 绑定值 | string/number | - | - |
| placeholder | 占位符 | string | - | - |
| disabled | 禁用状态 | boolean | - | false |
| readonly | 只读状态 | boolean | - | false |
| clearable | 可清空 | boolean | - | false |
| maxlength | 最大长度 | number | - | - |
| showWordLimit | 显示字数统计 | boolean | - | false |
| size | 尺寸 | string | large/default/small | default |
| prefixIcon | 前缀图标 | string | - | - |
| suffixIcon | 后缀图标 | string | - | - |

### Events

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| input | 输入事件 | value: string |
| change | 值改变事件 | value: string |
| focus | 获得焦点 | event: FocusEvent |
| blur | 失去焦点 | event: FocusEvent |
| clear | 清空事件 | - |

---

## ARCard 卡片组件

### 基础用法

```vue
<template>
  <ARCard>
    这是一张基础卡片
  </ARCard>
</template>
```

### 带标题

```vue
<template>
  <ARCard header="卡片标题">
    这是一张带标题的卡片
  </ARCard>
</template>
```

### 带头部和底部

```vue
<template>
  <ARCard header="卡片标题" footer="卡片底部">
    卡片内容
  </ARCard>
</template>
```

### 自定义头部和底部

```vue
<template>
  <ARCard>
    <template #header>
      <div>自定义标题</div>
    </template>
    卡片内容
    <template #footer>
      <div>自定义底部</div>
    </template>
  </ARCard>
</template>
```

### 阴影效果

```vue
<template>
  <ARCard shadow="always">总是显示阴影</ARCard>
  <ARCard shadow="hover">悬浮显示阴影</ARCard>
  <ARCard shadow="never">从不显示阴影</ARCard>
</template>
```

### 悬浮效果

```vue
<template>
  <ARCard hoverable>
    悬浮时会有上浮效果
  </ARCard>
</template>
```

### Props

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| header | 卡片标题 | string | - | - |
| footer | 卡片底部 | string | - | - |
| shadow | 阴影显示时机 | string | always/hover/never | hover |
| bordered | 是否有边框 | boolean | - | true |
| size | 卡片尺寸 | string | large/default/small | default |
| hoverable | 是否悬浮 | boolean | - | false |

### Slots

| 插槽名 | 说明 |
|--------|------|
| default | 卡片内容 |
| header | 卡片头部 |
| footer | 卡片底部 |
| extra | 头部额外内容 |

---

## ARForm 表单组件

### 基础用法

```vue
<script setup lang="ts">
import { reactive } from 'vue'

const formData = reactive({
  username: '',
  password: '',
})

const handleSubmit = () => {
  console.log('提交表单', formData)
}
</script>

<template>
  <ARForm :model="formData" @submit="handleSubmit">
    <ARFormItem label="用户名" prop="username">
      <ARInput v-model="formData.username" />
    </ARFormItem>
    <ARFormItem label="密码" prop="password">
      <ARInput v-model="formData.password" type="password" />
    </ARFormItem>
    <ARFormItem>
      <ARButton type="primary" native-type="submit">提交</ARButton>
    </ARFormItem>
  </ARForm>
</template>
```

### 表单验证

```vue
<script setup lang="ts">
import { reactive } from 'vue'

const formData = reactive({
  email: '',
  age: '',
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  age: [
    { required: true, message: '请输入年龄', trigger: 'blur' },
    { type: 'number', message: '年龄必须为数字', trigger: 'blur' },
    { min: 18, max: 100, message: '年龄必须在18-100之间', trigger: 'blur' },
  ],
}
</script>

<template>
  <ARForm :model="formData" :rules="rules">
    <ARFormItem label="邮箱" prop="email">
      <ARInput v-model="formData.email" />
    </ARFormItem>
    <ARFormItem label="年龄" prop="age">
      <ARInput v-model="formData.age" type="number" />
    </ARFormItem>
  </ARForm>
</template>
```

### 标签位置

```vue
<template>
  <!-- 标签在左侧 -->
  <ARForm :model="formData" label-position="left">
    <ARFormItem label="用户名" prop="username">
      <ARInput v-model="formData.username" />
    </ARFormItem>
  </ARForm>

  <!-- 标签在右侧 -->
  <ARForm :model="formData" label-position="right">
    <ARFormItem label="用户名" prop="username">
      <ARInput v-model="formData.username" />
    </ARFormItem>
  </ARForm>

  <!-- 标签在顶部 -->
  <ARForm :model="formData" label-position="top">
    <ARFormItem label="用户名" prop="username">
      <ARInput v-model="formData.username" />
    </ARFormItem>
  </ARForm>
</template>
```

### Props

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| model | 表单数据对象 | object | - | {} |
| rules | 表单验证规则 | object | - | {} |
| labelPosition | 表单域标签的位置 | string | left/right/top | right |
| labelWidth | 标签的宽度 | string/number | - | 100px |
| labelSuffix | 表单域标签的后缀 | string | - | - |
| hideRequiredAsterisk | 是否隐藏必填标记 | boolean | - | false |
| showMessage | 是否显示校验错误信息 | boolean | - | true |
| disabled | 是否禁用该表单内的所有组件 | boolean | - | false |
| size | 用于控制该表单内组件的尺寸 | string | large/default/small | - |

### Methods

| 方法名 | 说明 | 参数 |
|--------|------|------|
| validate | 对整个表单进行验证 | callback: (valid: boolean) => void |
| validateField | 对部分表单字段进行验证 | props: string \| string[], callback |
| resetFields | 对整个表单进行重置 | - |
| clearValidate | 清除验证结果 | props: string \| string[] |

---

## ARFormItem 表单项组件

### 基础用法

```vue
<template>
  <ARFormItem label="用户名" prop="username">
    <ARInput v-model="formData.username" />
  </ARFormItem>
</template>
```

### 必填项

```vue
<template>
  <ARFormItem label="邮箱" prop="email" required>
    <ARInput v-model="formData.email" />
  </ARFormItem>
</template>
```

### 自定义验证规则

```vue
<script setup lang="ts">
const customRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value && value.length < 3) {
          callback(new Error('用户名长度不能少于3位'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}
</script>

<template>
  <ARForm :model="formData" :rules="customRules">
    <ARFormItem label="用户名" prop="username">
      <ARInput v-model="formData.username" />
    </ARFormItem>
  </ARForm>
</template>
```

### Props

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| label | 标签文本 | string | - | - |
| prop | 表单域 model 字段名 | string | - | - |
| required | 是否必填 | boolean | - | false |
| rules | 表单验证规则 | object \| array | - | - |
| error | 表单域验证错误信息 | string | - | - |
| validateStatus | 表单域的校验状态 | string | success/warning/error | - |
| showMessage | 是否显示校验错误信息 | boolean | - | true |
| inlineMessage | 是否以行内形式展示校验信息 | boolean | - | false |

---

## 深色模式

所有组件都支持深色模式，只需在根元素上添加 `data-theme='dark'` 属性即可：

```html
<html data-theme="dark">
  <div id="app"></div>
</html>
```

或在代码中切换：

```typescript
document.documentElement.setAttribute('data-theme', 'dark')
```

---

## TypeScript 支持

所有组件都提供了完整的 TypeScript 类型定义：

```typescript
import type { ButtonProps, InputProps, CardProps, FormProps, FormItemProps } from '@/components/@ai-ready/common/components'

// 使用类型
const buttonProps: ButtonProps = {
  type: 'primary',
  size: 'large',
}
```

---

**文档版本**: v1.0
**最后更新**: 2026-04-27