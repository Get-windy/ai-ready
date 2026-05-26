# AI-Ready 前端组件库封装文档

## 概述

本项目封装了完整的前端通用组件库，涵盖业务组件和UI组件两大类，提升开发效率，保证UI一致性。

## 核心文件

```
src/components/
├── index.ts                       # 统一导出索引
├── TableList/                     # 表格列表组件
│   └── TableList.vue
├── SearchBar/                     # 搜索栏组件
│   └── SearchBar.vue
├── FormData/                      # 动态表单生成器
│   └── FormBuilder.vue
├── ConfirmDialog/                 # 确认弹窗组件
│   └── ConfirmDialog.vue
├── LoadingButton/                 # 加载按钮组件
│   └── LoadingButton.vue
└── EmptyState/                    # 空状态组件
    └── EmptyState.vue
```

## 通用业务组件

### 1. TableList 表格列表组件

带分页、筛选、批量操作、状态标签等高级功能的表格组件。

#### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| columns | TableColumn[] | required | 列配置 |
| dataSource | any[] | required | 数据源 |
| loading | boolean | false | 加载状态 |
| bordered | boolean | true | 边框 |
| tableSize | 'small' \| 'middle' \| 'large' | 'middle' | 表格尺寸 |
| scroll | object | { x: '100%' } | 滚动配置 |
| pagination | object | - | 分页配置 |
| showToolbar | boolean | true | 显示工具栏 |
| showSearch | boolean | true | 显示搜索 |
| showAdd | boolean | true | 显示新增按钮 |
| showEdit | boolean | true | 显示编辑按钮 |
| showView | boolean | false | 显示查看按钮 |
| showDelete | boolean | true | 显示删除按钮 |
| showBatchDelete | boolean | true | 显示批量删除 |
| showRefresh | boolean | true | 显示刷新按钮 |
| showExport | boolean | false | 显示导出按钮 |
| emptyText | string | '暂无数据' | 空状态文本 |

#### Events

| Event | 参数 | 说明 |
|-------|------|------|
| add | - | 新增按钮点击 |
| edit | record | 编辑按钮点击 |
| view | record | 查看按钮点击 |
| delete | record | 删除按钮点击 |
| batch-delete | ids[] | 批量删除 |
| refresh | - | 刷新按钮点击 |
| search | keyword | 搜索 |
| export | - | 导出按钮点击 |
| page-change | page, pageSize | 分页改变 |
| selection-change | keys[], rows[] | 选择改变 |

#### 插槽

| 插槽 | 说明 |
|------|------|
| toolbar-left | 左侧工具栏 |
| toolbar-right | 右侧工具栏 |
| action | 自定义操作列 |
| empty | 自定义空状态 |

#### 示例

```vue
<template>
  <TableList
    :columns="columns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    @refresh="handleRefresh"
    @delete="handleDelete"
    @edit="handleEdit"
  >
    <!-- 自定义操作列 -->
    <template #action="{ record }">
      <a-space>
        <a-button type="link" size="small" @click="handleDetail(record)">详情</a-button>
        <a-button type="link" size="small" @click="handleEnable(record)">{{ record.enabled ? '禁用' : '启用' }}</a-button>
      </a-space>
    </template>
  </TableList>
</template>

<script setup>
import { ref } from 'vue'
import { TableList } from '@/components'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', ellipsis: true },
  { title: '邮箱', dataIndex: 'email' },
  { title: '状态', dataIndex: 'status', type: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', type: 'date' },
  { title: '操作', key: 'action', type: 'action' }
]

const list = ref([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const handleRefresh = () => {
  loading.value = true
  // 加载数据
  setTimeout(() => {
    loading.value = false
  }, 1000)
}
</script>
```

### 2. SearchBar 搜索栏组件

支持多种字段类型、展开/收起、条件统计的搜索栏组件。

#### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| fields | SearchField[] | required | 搜索字段 |
| modelValue | object | {} | 搜索表单数据 |
| loading | boolean | false | 加载状态 |
| expandable | boolean | true | 是否可展开 |
| showResultCount | boolean | false | 显示结果统计 |
| total | number | 0 | 总数量 |
| showClear | boolean | false | 显示清空 |

#### Event

| Event | 参数 | 说明 |
|-------|------|------|
| update:modelValue | formData | 表单数据变化 |
| search | formData | 搜索提交 |
| reset | - | 重置 |
| clear | - | 清空条件 |
| field-change | field, value | 字段变化 |

#### 字段类型

| Type | 说明 |
|------|------|
| input | 文本输入 |
| select | 下拉选择 |
| number | 数字输入 |
| date-range | 日期范围 |

#### 示例

```vue
<template>
  <SearchBar
    v-model="searchForm"
    :fields="fields"
    :loading="loading"
    :show-result-count="true"
    :total="total"
    @search="handleSearch"
    @reset="handleReset"
  />
</template>

<script setup>
import { ref } from 'vue'
import { SearchBar } from '@/components'

const searchForm = ref({
  keyword: '',
  status: undefined,
  createdAt: []
})

const fields = [
  {
    name: 'keyword',
    label: '关键词',
    type: 'input',
    placeholder: '请输入用户名或邮箱'
  },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  },
  {
    name: 'createdAt',
    label: '创建时间',
    type: 'date-range'
  },
  {
    name: 'deptId',
    label: '部门',
    type: 'select',
    options: deptOptions,
    fullWidth: true
  }
]

const handleSearch = async (formData) => {
  loading.value = true
  // 执行搜索
  const result = await api.search(formData)
  list.value = result.data
  total.value = result.total
  loading.value = false
}
</script>
```

### 3. FormBuilder 动态表单生成器

支持多种字段类型、动态配置的表单生成器。

#### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| fields | FormField[] | required | 表单字段配置 |
| modelValue | object | {} | 表单数据 |
| rules | object | {} | 验证规则 |
| layout | string | 'horizontal' | 表单布局 |

#### 字段类型

| Type | 说明 |
|------|------|
| input | 文本输入 |
| password | 密码输入 |
| number | 数字输入 |
| textarea | 文本域 |
| select | 下拉选择 |
| checkbox-group | 多选 |
| radio-group | 单选 |
| switch | 开关 |
| date | 日期 |
| datetime | 日期时间 |
| date-range | 日期范围 |
| custom | 自定义 |

#### 示例

```vue
<template>
  <FormBuilder
    v-model="formData"
    :fields="fields"
    :rules="rules"
    @submit="handleSubmit"
  />
</template>

<script setup>
import { ref } from 'vue'
import { FormBuilder, commonRules } from '@/components'

const formData = ref({
  username: '',
  email: '',
  status: 1,
  roles: []
})

const fields = [
  {
    name: 'username',
    label: '用户名',
    type: 'input',
    rules: [commonRules.required(), commonRules.minLength(3)]
  },
  {
    name: 'email',
    label: '邮箱',
    type: 'input',
    rules: [commonRules.required(), commonRules.email()]
  },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  },
  {
    name: 'roles',
    label: '角色',
    type: 'checkbox-group',
    options: roleOptions
  }
]

const rules = {
  username: [commonRules.required()],
  email: [commonRules.required(), commonRules.email()]
}

const handleSubmit = async (data) => {
  await api.save(data)
}
</script>
```

## UI组件

### 4. ConfirmDialog 确认弹窗组件

支持多种图标类型、带表单的确认弹窗。

#### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| visible | boolean | false | 显示状态 |
| title | string | '确认操作' | 标题 |
| message | string | required | 主要消息 |
| description | string | '' | 描述 |
| type | string | 'warning' | 图标类型 |
| cancelText | string | '取消' | 取消按钮文字 |
| okText | string | '确定' | 确定按钮文字 |
| confirmLoading | boolean | false | 加载状态 |
| showForm | boolean | false | 显示表单 |
| centered | boolean | false | 居中显示 |

#### Type类型

| Type | 图标 | 适用场景 |
|------|------|----------|
| info | 信息 | 普通提示 |
| success | 成功 | 成功提示 |
| warning | 警告 | 重要操作 |
| error | 错误 | 危险操作 |
| question | 问号 | 问题确认 |

#### 示例

```vue
<template>
  <ConfirmDialog
    v-model:visible="showDialog"
    title="确认删除"
    message="确定要删除这条记录吗？"
    description="此操作不可恢复，请谨慎操作"
    type="error"
    @ok="handleDelete"
  />
  
  <ConfirmDialog
    v-model:visible="showConfirm"
    title="修改设置"
    message="请确认以下设置"
    :show-form="true"
    @ok="handleUpdate"
  >
    <a-form :model="formData">
      <a-form-item label="备注">
        <a-input v-model:value="formData.remark" />
      </a-form-item>
    </a-form>
  </ConfirmDialog>
</template>

<script setup>
import { ref } from 'vue'
import { ConfirmDialog } from '@/components'

const showDialog = ref(false)
const showConfirm = ref(false)

const handleDelete = async () => {
  try {
    await api.delete(id)
    message.success('删除成功')
    showDialog.value = false
  } catch (error) {
    message.error('删除失败')
  }
}
</script>
```

### 5. LoadingButton 加载按钮组件

带加载状态、图标切换的增强按钮。

#### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | string | 'primary' | 按钮类型 |
| size | string | 'middle' | 尺寸 |
| danger | boolean | false | 危险按钮 |
| ghost | boolean | false | 幽灵按钮 |
| disabled | boolean | false | 禁用 |
| loading | boolean | false | 加载状态 |
| htmlType | string | 'button' | HTML类型 |
| block | boolean | false | 块级按钮 |
| icon | string | '' | 图标名称 |
| loadingText | string | '' | 加载文字 |
| style | string | '' | 自定义样式 |

#### 支持的图标

```typescript
'plus', 'delete', 'edit', 'search', 'save', 'close', 'check',
'info', 'warning', 'error', 'download', 'upload', 'eye',
'left', 'right', 'arrowRight', 'arrowLeft'
```

#### 示例

```vue
<template>
  <LoadingButton
    :loading="submitting"
    icon="save"
    loading-text="正在保存..."
    @click="handleSubmit"
  >
    提交
  </LoadingButton>
  
  <LoadingButton
    type="primary"
    icon="plus"
    @click="handleAdd"
  >
    新增
  </LoadingButton>
  
  <LoadingButton
    type="danger"
    icon="delete"
    :loading="deleting"
    @click="handleDelete"
  >
    删除
  </LoadingButton>
</template>
```

### 6. EmptyState 空状态组件

多种样式、可自定义操作的空状态组件。

#### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| image | string | 'default' | 图片样式 |
| size | string | 'middle' | 组件尺寸 |
| title | string | '暂无数据' | 标题 |
| description | string | '当前列表为空...' | 描述 |
| showActions | boolean | true | 显示操作按钮 |
| showRefresh | boolean | true | 显示刷新按钮 |
| showAdd | boolean | true | 显示新增按钮 |
| addText | string | '新增' | 新增按钮文字 |

#### 图片样式

| Type | 适用场景 |
|------|----------|
| default | 通用空状态 |
| simple | 表单空状态 |
| error | 网络错误 |
| permission | 无权限 |
| no-data | 暂无数据 |

#### 示例

```vue
<template>
  <!-- 表格空状态 -->
  <EmptyState
    v-if="list.length === 0"
    :title="searchKeyword ? '无搜索结果' : '暂无数据'"
    :description="searchKeyword ? '请尝试其他关键词' : '请先创建数据'"
    @refresh="handleRefresh"
    @add="handleAdd"
  />
  
  <!-- 错误空状态 -->
  <EmptyState
    v-if="error"
    image="error"
    title="加载失败"
    description="请检查网络连接后重试"
    @refresh="handleRefresh"
  />
  
  <!-- 自定义空状态 -->
  <EmptyState
    :show-actions="false"
  >
    <template #custom>
      <a-button type="primary" @click="goToCreate">去创建</a-button>
    </template>
  </EmptyState>
</template>
```

## 使用方法

### 1. 统一导入

```typescript
import { 
  TableList, 
  SearchBar, 
  FormBuilder, 
  ConfirmDialog, 
  LoadingButton, 
  EmptyState 
} from '@/components'
```

### 2. 按需导入

```typescript
import TableList from '@/components/TableList/TableList.vue'
import LoadingButton from '@/components/LoadingButton/LoadingButton.vue'
```

### 3. 简化使用

```vue
<template>
  <div class="page">
    <SearchBar v-model="searchForm" :fields="fields" @search="handleSearch" />
    
    <div class="toolbar mt-16">
      <LoadingButton type="primary" icon="plus" @click="handleAdd">新增</LoadingButton>
    </div>
    
    <TableList
      :columns="columns"
      :data-source="list"
      :pagination="pagination"
      @refresh="handleRefresh"
    />
    
    <ConfirmDialog
      v-model:visible="showDialog"
      title="确认操作"
      message="确定要执行此操作吗？"
      @ok="handleConfirm"
    />
  </div>
</template>
```

## 全局状态管理

建议使用 Pinia 管理组件状态：

```typescript
// stores/tableList.ts
import { defineStore } from 'pinia'

export const useTableListStore = defineStore('tableList', {
  state: () => ({
    loading: false,
    list: [],
    pagination: {
      current: 1,
      pageSize: 10,
      total: 0
    }
  }),
  
  actions: {
    async fetchList(params) {
      this.loading = true
      try {
        const res = await api.getList(params)
        this.list = res.data
        this.pagination.total = res.total
      } finally {
        this.loading = false
      }
    },
    
    refresh() {
      this.fetchList({
        current: this.pagination.current,
        pageSize: this.pagination.pageSize
      })
    }
  }
})
```

## 最佳实践

1. **统一导⼊**: 使用 `@/components` 统一导入所有组件
2. **状态管理**: 使用 Pinia 管理组件状态，避免 Prop Drilling
3. **事件命名**: 组件事件使用语义化命名（add/edit/delete/refresh）
4. **默认值**: 组件提供合理的默认值，减少配置工作
5. **插槽扩展**: 使用插槽提供自定义能力
6. **类型定义**: 使用 TypeScript 类型定义，提供完整类型提示

## 注意事项

1. TableList 组件默认展示所有操作列，可根据需求隐藏
2. SearchBar 的 date-range 类型会拆分为两个字段存储
3. FormBuilder 的自定义字段通过插槽实现
4. ConfirmDialog 的 type 决定图标和颜色主题
5. LoadingButton 的 icon 支持字符串匹配常用图标
6. EmptyState 的 image 支持多种预设样式

---

_组件库封装完成。组件已可直接在其他页面使用。_