# AI-Ready 测试环境组件设计文档

**项目**: AI-Ready (智企连)
**版本**: Sprint 27+1
**创建日期**: 2026-04-27
**负责人**: UI设计师 (ui-mnj0fukd)
**任务ID**: task_1777265176124_6cv0pi6d9

---

## 目录

1. [组件设计规范](#1-组件设计规范)
2. [基础组件设计](#2-基础组件设计)
3. [表单组件设计](#3-表单组件设计)
4. [反馈组件设计](#4-反馈组件设计)
5. [导航组件设计](#5-导航组件设计)
6. [数据展示组件设计](#6-数据展示组件设计)

---

## 1. 组件设计规范

### 1.1 设计原则

#### 1.1.1 功能性
- 组件功能单一，职责明确
- Props命名语义化
- 事件回调参数规范

#### 1.1.2 可复用性
- 组件高度可配置
- 支持插槽扩展
- 支持样式定制

#### 1.1.3 一致性
- 统一的命名规范
- 统一的API设计
- 统一的视觉风格

#### 1.1.4 可访问性
- 支持键盘导航
- 支持屏幕阅读器
- 符合WCAG标准

### 1.2 组件规范

#### 1.2.1 命名规范

**组件名**: PascalCase + AR前缀
- 示例: `ARButton`, `ARInput`, `ARCard`

**文件名**: kebab-case
- 示例: `ar-button.vue`, `ar-input.vue`

**类名**: kebab-case + ar-前缀
- 示例: `.ar-button`, `.ar-input`

#### 1.2.2 Props规范

**通用Props**:
```typescript
interface ARComponentBaseProps {
  class?: string;           // 自定义类名
  style?: CSSProperties;    // 自定义样式
  id?: string;             // 唯一标识
  testId?: string;         // 测试ID（用于测试）
}
```

**Props类型**:
```typescript
// String
label: string;

// Number
width: number;

// Boolean
disabled: boolean;

// Array
options: Array<Option>;

// Object
config: Config;

// Enum
type: 'primary' | 'secondary' | 'success' | 'danger';

// Union
size: 'small' | 'medium' | 'large' | number;

// Function
onClick: (event: MouseEvent) => void;

// Slot
default?: Slot;
```

**Props默认值**:
- 尽量提供合理的默认值
- 默认值应该在Props类型中标注
- 必填Props使用`required`标注

#### 1.2.3 Events规范

**事件命名**: on + 驼峰命名
```typescript
onClick: (event: MouseEvent) => void;
onChange: (value: any) => void;
onInput: (value: string) => void;
onFocus: (event: FocusEvent) => void;
onBlur: (event: FocusEvent) => void;
```

**事件参数**:
- 鼠标事件: `MouseEvent`
- 键盘事件: `KeyboardEvent`
- 焦点事件: `FocusEvent`
- 值变更: 当前值
- 状态变更: `{ key: string, value: any }`

#### 1.2.4 Slots规范

**命名插槽**:
```typescript
// 默认插槽
default: Slot;

// 命名插槽
header: Slot;
footer: Slot;
prefix: Slot;    // 前缀
suffix: Slot;    // 后缀
icon: Slot;      // 图标
```

**作用域插槽**:
```typescript
// 列表项作用域插槽
item: (props: { item: any, index: number }) => Slot;
```

#### 1.2.5 样式规范

**使用CSS变量**:
```css
.ar-button {
  /* 基础样式 */
  background-color: var(--ar-color-primary);
  color: var(--ar-color-white);
  padding: var(--ar-spacing-sm) var(--ar-spacing-md);
  border-radius: var(--ar-radius-md);

  /* 变体样式 */
  &.ar-button--secondary {
    background-color: var(--ar-color-background-page);
    color: var(--ar-color-text-primary);
  }

  /* 状态样式 */
  &:hover {
    background-color: var(--ar-color-primary-light);
  }

  &:active {
    background-color: var(--ar-color-primary-dark);
  }

  &:disabled {
    opacity: var(--ar-opacity-disabled);
    cursor: not-allowed;
  }
}
```

**避免内联样式**:
- ❌ `<div style="color: red;">`
- ✅ `<div class="ar-text-danger">`

**BEM命名规范**:
```css
/* Block */
.ar-button { }

/* Element */
.ar-button__icon { }
.ar-button__text { }

/* Modifier */
.ar-button--primary { }
.ar-button--disabled { }
```

### 1.3 组件文档规范

#### 1.3.1 组件说明

每个组件应包含:
- 组件名称
- 组件描述
- 使用场景
- 最佳实践

#### 1.3.2 API文档

**Props表格**:
| 参数 | 说明 | 类型 | 默认值 | 必填 |
|------|------|------|--------|------|
| type | 按钮类型 | 'primary' \| 'secondary' | 'primary' | 否 |
| size | 按钮尺寸 | 'small' \| 'medium' \| 'large' | 'medium' | 否 |
| disabled | 是否禁用 | boolean | false | 否 |
| onClick | 点击事件 | (event: MouseEvent) => void | - | 否 |

**Events表格**:
| 事件名 | 说明 | 回调参数 |
|--------|------|---------|
| onClick | 点击事件 | event: MouseEvent |

**Slots表格**:
| 插槽名 | 说明 |
|--------|------|
| default | 默认内容 |
| icon | 图标 |

#### 1.3.3 示例代码

**基础示例**:
```vue
<template>
  <ARButton type="primary" @click="handleClick">
    点击我
  </ARButton>
</template>

<script setup>
const handleClick = (event) => {
  console.log('Button clicked', event);
};
</script>
```

**高级示例**:
```vue
<template>
  <ARButton type="primary" :loading="loading" @click="handleAsyncClick">
    <template #icon>
      <Icon name="save" />
    </template>
    保存
  </ARButton>
</template>

<script setup>
import { ref } from 'vue';
import Icon from './Icon.vue';

const loading = ref(false);

const handleAsyncClick = async () => {
  loading.value = true;
  try {
    await saveData();
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## 2. 基础组件设计

### 2.1 ARButton（按钮）

#### 2.1.1 组件说明

基础按钮组件，支持多种类型、尺寸和状态。

#### 2.1.2 API设计

**Props**:
```typescript
interface ARButtonProps {
  type?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'text';
  size?: 'small' | 'medium' | 'large';
  disabled?: boolean;
  loading?: boolean;
  block?: boolean;        // 是否块级按钮
  round?: boolean;        // 是否圆角
  circle?: boolean;       // 是否圆形按钮
  icon?: string;          // 图标名称
  onClick?: (event: MouseEvent) => void;
}
```

**Slots**:
- `default`: 按钮内容
- `icon`: 图标插槽

#### 2.1.3 使用示例

```vue
<!-- 基础按钮 -->
<ARButton>默认按钮</ARButton>

<!-- 类型按钮 -->
<ARButton type="primary">主要按钮</ARButton>
<ARButton type="success">成功按钮</ARButton>
<ARButton type="danger">危险按钮</ARButton>

<!-- 尺寸按钮 -->
<ARButton size="small">小按钮</ARButton>
<ARButton size="medium">中按钮</ARButton>
<ARButton size="large">大按钮</ARButton>

<!-- 状态按钮 -->
<ARButton disabled>禁用按钮</ARButton>
<ARButton loading>加载中...</ARButton>

<!-- 特殊按钮 -->
<ARButton block>块级按钮</ARButton>
<ARButton round>圆角按钮</ARButton>
<ARButton circle>圆形</ARButton>

<!-- 图标按钮 -->
<ARButton icon="save">
  <template #icon>
    <Icon name="save" />
  </template>
  保存
</ARButton>
```

#### 2.1.4 优化建议

1. **Loading状态优化**
   - 添加旋转Loading图标
   - 禁用点击事件
   - 保持按钮尺寸

2. **Disabled状态优化**
   - 降低透明度至0.5
   - 禁止手势光标
   - 保持视觉层次

3. **图标按钮优化**
   - 统一图标尺寸（16px）
   - 图标与文字间距8px
   - 纯图标按钮时居中对齐

### 2.2 ARInput（输入框）

#### 2.2.1 组件说明

文本输入框组件，支持多种类型、验证和交互。

#### 2.2.2 API设计

```typescript
interface ARInputProps {
  type?: 'text' | 'password' | 'number' | 'email' | 'tel';
  modelValue?: string;
  placeholder?: string;
  disabled?: boolean;
  readonly?: boolean;
  clearable?: boolean;     // 是否显示清空按钮
  maxlength?: number;      // 最大长度
  showWordLimit?: boolean; // 是否显示字数统计
  prefix?: string;         // 前缀图标
  suffix?: string;         // 后缀图标
  size?: 'small' | 'medium' | 'large';
  error?: boolean;         // 是否错误状态
  errorMessage?: string;   // 错误信息
  onInput?: (value: string) => void;
  onChange?: (value: string) => void;
  onFocus?: (event: FocusEvent) => void;
  onBlur?: (event: FocusEvent) => void;
  onClear?: () => void;
}
```

#### 2.2.3 使用示例

```vue
<!-- 基础输入框 -->
<ARInput v-model="value" placeholder="请输入内容" />

<!-- 类型输入框 -->
<ARInput type="password" placeholder="请输入密码" />
<ARInput type="number" placeholder="请输入数字" />

<!-- 功能输入框 -->
<ARInput clearable placeholder="可清空" />
<ARInput maxlength="100" showWordLimit placeholder="限制长度" />

<!-- 前后缀 -->
<ARInput prefix="user" placeholder="用户名" />
<ARInput suffix="search" placeholder="搜索" />

<!-- 错误状态 -->
<ARInput v-model="value" error errorMessage="用户名已存在" />

<!-- 尺寸 -->
<ARInput size="small" placeholder="小尺寸" />
<ARInput size="medium" placeholder="中尺寸" />
<ARInput size="large" placeholder="大尺寸" />
```

#### 2.2.4 优化建议

1. **清空按钮优化**
   - 有内容时显示
   - 悬停时高亮
   - 点击时清空 + 聚焦

2. **密码切换优化**
   - 图标位置居右12px
   - 切换时保持光标位置
   - 图标清晰可见

3. **错误提示优化**
   - 红色边框 + 红色文字
   - 错误图标（✗）
   - 错误信息在输入框下方

### 2.3 ARCard（卡片）

#### 2.3.1 组件说明

卡片容器组件，用于包裹内容和展示信息。

#### 2.3.2 API设计

```typescript
interface ARCardProps {
  title?: string;          // 卡片标题
  subtitle?: string;       // 卡片副标题
  extra?: string;          // 额外操作
  shadow?: 'never' | 'hover' | 'always';
  bodyStyle?: CSSProperties;
  headerStyle?: CSSProperties;
  loading?: boolean;
}
```

#### 2.3.3 使用示例

```vue
<!-- 基础卡片 -->
<ARCard title="卡片标题">
  卡片内容
</ARCard>

<!-- 完整卡片 -->
<ARCard
  title="标题"
  subtitle="副标题"
  extra="更多"
  shadow="hover"
>
  卡片内容
</ARCard>

<!-- 加载状态 -->
<ARCard loading>
  加载中...
</ARCard>
```

#### 2.3.4 优化建议

1. **阴影效果优化**
   - never: 无阴影
   - hover: 悬停时阴影（box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1)）
   - always: 始终阴影

2. **悬停效果优化**
   - 轻微上移（transform: translateY(-2px)）
   - 阴影加深
   - 过渡动画300ms

3. **加载状态优化**
   - 骨架屏效果
   - 保持卡片尺寸
   - 避免内容跳动

---

## 3. 表单组件设计

### 3.1 ARSelect（选择器）

#### 3.1.1 组件说明

下拉选择器组件，支持单选、多选、搜索等功能。

#### 3.1.2 API设计

```typescript
interface ARSelectProps {
  modelValue?: string | number | Array<any>;
  options?: Array<{ label: string; value: any; disabled?: boolean }>;
  placeholder?: string;
  disabled?: boolean;
  clearable?: boolean;
  multiple?: boolean;       // 是否多选
  searchable?: boolean;     // 是否可搜索
  filterable?: boolean;     // 是否可筛选
  remote?: boolean;         // 是否远程搜索
  loading?: boolean;
  size?: 'small' | 'medium' | 'large';
  onChange?: (value: any) => void;
  onSearch?: (query: string) => void;
}
```

#### 3.1.3 使用示例

```vue
<!-- 基础选择器 -->
<ARSelect v-model="value" :options="options" />

<!-- 可清空 -->
<ARSelect v-model="value" :options="options" clearable />

<!-- 可搜索 -->
<ARSelect v-model="value" :options="options" searchable />

<!-- 多选 -->
<ARSelect v-model="value" :options="options" multiple />

<!-- 远程搜索 -->
<ARSelect
  v-model="value"
  :options="options"
  :loading="loading"
  searchable
  remote
  @search="handleSearch"
/>
```

### 3.2 ARCheckbox（复选框）

#### 3.2.1 组件说明

复选框组件，用于多项选择。

#### 3.2.2 API设计

```typescript
interface ARCheckboxProps {
  modelValue?: boolean | string | number;
  label?: string;
  disabled?: boolean;
  indeterminate?: boolean;  // 半选状态
  trueValue?: any;          // 选中时的值
  falseValue?: any;         // 未选中时的值
  onChange?: (value: any) => void;
}
```

#### 3.2.3 使用示例

```vue
<!-- 基础复选框 -->
<ARCheckbox v-model="checked">选项</ARCheckbox>

<!-- 自定义值 -->
<ARCheckbox v-model="value" :true-value="1" :false-value="0">
  同意协议
</ARCheckbox>

<!-- 禁用 -->
<ARCheckbox v-model="checked" disabled>禁用</ARCheckbox>

<!-- 半选 -->
<ARCheckbox :indeterminate="true">半选状态</ARCheckbox>
```

### 3.3 ARRadioButton（单选框）

#### 3.3.1 组件说明

单选框组件，用于单项选择。

#### 3.3.2 API设计

```typescript
interface ARRadioButtonProps {
  modelValue?: any;
  label?: string;
  value?: any;
  disabled?: boolean;
  onChange?: (value: any) => void;
}
```

#### 3.3.3 使用示例

```vue
<!-- 基础单选框 -->
<ARRadioButtonGroup v-model="value">
  <ARRadioButton label="选项1" :value="1" />
  <ARRadioButton label="选项2" :value="2" />
  <ARRadioButton label="选项3" :value="3" />
</ARRadioButtonGroup>

<!-- 禁用 -->
<ARRadioButton v-model="value" label="禁用" :value="1" disabled />
```

---

## 4. 反馈组件设计

### 4.1 ARMessage（轻提示）

#### 4.1.1 组件说明

全局轻提示组件，用于显示操作结果。

#### 4.1.2 API设计

```typescript
interface ARMessageProps {
  type?: 'success' | 'warning' | 'error' | 'info';
  message: string;
  duration?: number;       // 显示时长，0为不关闭
  showClose?: boolean;     // 是否显示关闭按钮
  center?: boolean;        // 是否居中
  onClose?: () => void;
}
```

#### 4.1.3 使用示例

```vue
<script setup>
import { ARMessage } from '@ai-ready/components';

// 成功提示
ARMessage.success('操作成功');

// 警告提示
ARMessage.warning('请注意');

// 错误提示
ARMessage.error('操作失败');

// 信息提示
ARMessage.info('提示信息');

// 自定义时长
ARMessage.success('5秒后关闭', 5000);

// 不自动关闭
ARMessage.success('手动关闭', 0);
</script>
```

### 4.2 ARAlert（警告提示）

#### 4.2.1 组件说明

页面级警告提示组件。

#### 4.2.2 API设计

```typescript
interface ARAlertProps {
  type?: 'success' | 'warning' | 'error' | 'info';
  title?: string;          // 标题
  description?: string;    // 描述
  closable?: boolean;      // 是否可关闭
  showIcon?: boolean;      // 是否显示图标
  onClose?: () => void;
}
```

#### 4.2.3 使用示例

```vue
<!-- 基础警告 -->
<ARAlert type="success" title="成功提示" />

<!-- 带描述 -->
<ARAlert
  type="warning"
  title="警告提示"
  description="这是一段警告描述文本"
/>

<!-- 可关闭 -->
<ARAlert
  type="error"
  title="错误提示"
  closable
  @close="handleClose"
/>
```

### 4.3 ARTooltip（文字提示）

#### 4.3.1 组件说明

鼠标悬停显示补充信息。

#### 4.3.2 API设计

```typescript
interface ARTooltipProps {
  content: string;         // 提示内容
  placement?: 'top' | 'bottom' | 'left' | 'right';
  disabled?: boolean;
  delay?: number;          // 延迟显示(ms)
}
```

#### 4.3.3 使用示例

```vue
<ARTooltip content="这是提示内容">
  <ARButton>悬停查看</ARButton>
</ARTooltip>

<ARTooltip content="底部提示" placement="bottom">
  <span>底部提示</span>
</ARTooltip>
```

---

## 5. 导航组件设计

### 5.1 ARTabs（标签页）

#### 5.1.1 组件说明

标签页切换组件。

#### 5.1.2 API设计

```typescript
interface ARTabsProps {
  modelValue?: string | number;
  type?: 'line' | 'card' | 'border-card';
  tabPosition?: 'top' | 'right' | 'bottom' | 'left';
  closable?: boolean;      // 是否可关闭
  addable?: boolean;       // 是否可添加
  editable?: boolean;      // 是否可编辑
  onTabClick?: (tab: any) => void;
  onTabRemove?: (tab: any) => void;
  onTabAdd?: () => void;
}
```

#### 5.1.3 使用示例

```vue
<!-- 基础标签页 -->
<ARTabs v-model="activeTab">
  <ARTabPane label="标签1" name="1">内容1</ARTabPane>
  <ARTabPane label="标签2" name="2">内容2</ARTabPane>
  <ARTabPane label="标签3" name="3">内容3</ARTabPane>
</ARTabs>

<!-- 可关闭 -->
<ARTabs v-model="activeTab" closable @tab-remove="handleRemove">
  <ARTabPane label="标签1" name="1">内容1</ARTabPane>
  <ARTabPane label="标签2" name="2">内容2</ARTabPane>
</ARTabs>
```

### 5.2 ARBreadcrumb（面包屑）

#### 5.2.1 组件说明

面包屑导航组件。

#### 5.2.2 API设计

```typescript
interface ARBreadcrumbProps {
  separator?: string;      // 分隔符，默认'/'
}
```

#### 5.2.3 使用示例

```vue
<ARBreadcrumb>
  <ARBreadcrumbItem to="/">首页</ARBreadcrumbItem>
  <ARBreadcrumbItem to="/module">模块</ARBreadcrumbItem>
  <ARBreadcrumbItem>当前页面</ARBreadcrumbItem>
</ARBreadcrumb>

<!-- 自定义分隔符 -->
<ARBreadcrumb separator=">">
  <ARBreadcrumbItem>首页</ARBreadcrumbItem>
  <ARBreadcrumbItem>模块</ARBreadcrumbItem>
  <ARBreadcrumbItem>当前页面</ARBreadcrumbItem>
</ARBreadcrumb>
```

---

## 6. 数据展示组件设计

### 6.1 ARTag（标签）

#### 6.1.1 组件说明

标签组件，用于标记和分类。

#### 6.1.2 API设计

```typescript
interface ARTagProps {
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  size?: 'small' | 'medium' | 'large';
  closable?: boolean;      // 是否可关闭
  hit?: boolean;           // 是否有边框
  color?: string;          // 自定义颜色
  onClose?: () => void;
}
```

#### 6.1.3 使用示例

```vue
<!-- 基础标签 -->
<ARTag>标签</ARTag>

<!-- 类型标签 -->
<ARTag type="primary">主要</ARTag>
<ARTag type="success">成功</ARTag>
<ARTag type="warning">警告</ARTag>
<ARTag type="danger">危险</ARTag>

<!-- 可关闭 -->
<ARTag closable @close="handleClose">可关闭</ARTag>
```

### 6.2 ARBadge（徽标）

#### 6.2.1 组件说明

徽标组件，用于显示数量或状态。

#### 6.2.2 API设计

```typescript
interface ARBadgeProps {
  value?: number | string; // 显示值
  max?: number;            // 最大值，超过显示+n
  isDot?: boolean;         // 是否小圆点
  hidden?: boolean;        // 是否隐藏
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
}
```

#### 6.2.3 使用示例

```vue
<!-- 基础徽标 -->
<ARBadge :value="5">
  <ARButton>按钮</ARButton>
</ARBadge>

<!-- 小圆点 -->
<ARBadge isDot>
  <ARButton>按钮</ARButton>
</ARBadge>

<!-- 最大值 -->
<ARBadge :value="99" :max="99">
  <ARButton>按钮</ARButton>
</ARBadge>
```

---

## 7. 组件开发优先级

### 7.1 Sprint 27+1（当前）

| 组件 | 优先级 | 状态 |
|------|--------|------|
| ARAlert | P1 | ⏳ 待开发 |
| ARTooltip | P1 | ⏳ 待开发 |

### 7.2 Sprint 28

| 组件 | 优先级 | 状态 |
|------|--------|------|
| ARSteps | P2 | ⏳ 待开发 |
| ARWizard | P2 | ⏳ 待开发 |
| ARSelect | P1 | ⏳ 待开发 |
| ARCheckbox | P1 | ⏳ 待开发 |
| ARCheckboxGroup | P1 | ⏳ 待开发 |
| ARRadiobutton | P1 | ⏳ 待开发 |
| ARRadiobuttonGroup | P1 | ⏳ 待开发 |

### 7.3 Sprint 29

| 组件 | 优先级 | 状态 |
|------|--------|------|
| ARTabs | P1 | ⏳ 待开发 |
| ARMenu | P1 | ⏳ 待开发 |
| ARMessage | P1 | ⏳ 待开发 |
| ARDialog | P1 | ⏳ 待开发 |
| ARTag | P1 | ⏳ 待开发 |
| ARBadge | P1 | ⏳ 待开发 |

---

## 附录

### A. 组件类型定义

```typescript
// 基础Props
interface ARComponentBaseProps {
  class?: string;
  style?: CSSProperties;
  id?: string;
  testId?: string;
}

// 尺寸类型
type ARSize = 'small' | 'medium' | 'large';

// 类型类型
type ARType = 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'info';

// 事件类型
interface AREvents {
  onClick?: (event: MouseEvent) => void;
  onChange?: (value: any) => void;
  onInput?: (value: string) => void;
  onFocus?: (event: FocusEvent) => void;
  onBlur?: (event: FocusEvent) => void;
}
```

### B. 组件测试清单

每个组件开发完成后，应进行以下测试：

- [ ] 功能测试（所有Props和Events）
- [ ] 样式测试（所有变体和状态）
- [ ] 交互测试（用户操作流程）
- [ ] 可访问性测试（键盘导航、屏幕阅读器）
- [ ] 响应式测试（不同设备尺寸）
- [ ] 性能测试（渲染性能、内存占用）
- [ ] 兼容性测试（浏览器兼容）

---

**文档版本**: v1.0
**最后更新**: 2026-04-27
**审核状态**: 待审核