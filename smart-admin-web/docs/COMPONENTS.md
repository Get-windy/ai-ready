# AI-Ready 前端组件库文档

## 概述

AI-Ready 前端组件库基于 Vue 3 + TypeScript + Ant Design Vue 开发，提供一组高质量的公共组件和业务组件。

---

## 一、公共组件

### 1. TableList 表格列表组件

**功能特性**：
- 支持分页、排序、筛选
- 支持行选择（单选/多选）
- 内置工具栏（新增、删除、刷新、导出）
- 支持自定义列渲染（状态、日期、数字、省略）
- 支持插槽自定义

**Props**：
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| columns | TableColumn[] | [] | 列配置 |
| dataSource | any[] | [] | 数据源 |
| loading | boolean | false | 加载状态 |
| showToolbar | boolean | true | 显示工具栏 |
| showAdd | boolean | true | 显示新增按钮 |
| showEdit | boolean | true | 显示编辑按钮 |
| showDelete | boolean | true | 显示删除按钮 |
| pagination | PaginationConfig | {} | 分页配置 |

**Events**：
| 事件 | 参数 | 说明 |
|------|------|------|
| add | - | 新增按钮点击 |
| edit | record | 编辑按钮点击 |
| delete | record | 删除按钮点击 |
| refresh | - | 刷新按钮点击 |
| page-change | page, pageSize | 分页变化 |

**使用示例**：
```vue
<TableList
  :columns="columns"
  :data-source="data"
  :loading="loading"
  @add="handleAdd"
  @edit="handleEdit"
  @delete="handleDelete"
/>
```

---

### 2. SearchBar 搜索栏组件

**功能特性**：
- 动态渲染搜索字段
- 支持多种输入类型（文本、选择、日期）
- 自动布局响应式
- 支持展开/收起

---

### 3. FormBuilder 表单构建器

**功能特性**：
- 根据配置动态生成表单
- 支持多种字段类型
- 内置表单验证
- 支持联动规则

---

### 4. ConfirmDialog 确认对话框

**功能特性**：
- 支持 Promise 调用
- 可配置按钮文本
- 支持异步确认

**使用示例**：
```vue
<script setup>
import { ConfirmDialog } from '@/components'

const handleDelete = async () => {
  const confirmed = await ConfirmDialog.show({
    title: '确认删除',
    content: '删除后无法恢复，确定继续？'
  })
  if (confirmed) {
    // 执行删除
  }
}
</script>
```

---

### 5. LoadingButton 加载按钮

**功能特性**：
- 自动显示加载状态
- 防止重复点击
- 支持 Promise 自动处理

**使用示例**：
```vue
<LoadingButton 
  type="primary" 
  :on-click="handleSubmit"
>
  提交
</LoadingButton>
```

---

### 6. EmptyState 空状态

**功能特性**：
- 友好的空数据展示
- 支持自定义图标和文本
- 支持操作按钮

---

### 7. ErrorBoundary 错误边界

**功能特性**：
- 捕获子组件渲染错误
- 显示友好的错误页面
- 支持重试和返回

---

## 二、业务组件

### 1. FormEnhanced 增强表单

基于 FormBuilder 扩展，提供：
- 表单布局配置
- 字段分组
- 条件显隐

### 2. LocaleSwitch 多语言切换

支持动态切换语言，自动保存用户偏好。

---

## 三、组件测试

### 测试覆盖

| 模块 | 测试用例数 | 覆盖范围 |
|------|------------|----------|
| Button组件 | 6 | 渲染、状态、事件 |
| Input组件 | 5 | 绑定、校验、状态 |
| Table组件 | 3 | 渲染、数据 |
| Modal组件 | 5 | 显示、事件 |
| Pagination组件 | 5 | 页码、事件 |

### 测试命令

```bash
# 运行所有测试
npm run test

# 运行组件测试
npx vitest run src/__tests__/components

# 生成覆盖率报告
npm run test:coverage
```

---

## 四、目录结构

```
src/components/
├── ConfirmDialog/       # 确认对话框
├── EmptyState/          # 空状态
├── ErrorBoundary/       # 错误边界
├── Form/                # 表单组件
│   ├── FormEnhanced.vue
│   └── FormFieldEnhanced.vue
├── FormBuilder/         # 表单构建器
├── Loading/             # 加载组件
│   ├── LoadingButton.vue
│   ├── RouteLoading.vue
│   └── RouteError.vue
├── LocaleSwitch/        # 多语言切换
├── SearchBar/           # 搜索栏
├── TableList/           # 表格列表
└── index.ts             # 统一导出
```

---

## 五、使用方式

### 按需导入

```vue
<script setup>
import { TableList, SearchBar, FormBuilder } from '@/components'
</script>
```

### 全局注册

```typescript
// main.ts
import Components from '@/components'

app.use(Components)
```

---

*文档版本: 1.0.0*
*更新时间: 2026-04-04*
