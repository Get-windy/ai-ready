# CategoryListLayout 通用分类列表布局组件

## 概述

`CategoryListLayout` 是一个通用的分类列表布局组件，用于所有带分类树的列表页面（商品、客户、供应商等）。它提供了左侧分类树 + 右侧列表的标准布局，完全复刻了生产级系统中的经典设计模式。

## 特性

- **左侧分类树**：支持多级分类、搜索、新增、编辑、删除
- **顶部Tab栏**：支持多Tab切换
- **工具栏**：支持自定义左右操作按钮
- **搜索栏**：支持多条件筛选
- **表格区域**：支持自定义表格内容
- **底部分页**：标准分页组件

## 使用示例

### 1. 基础用法

```vue
<template>
  <CategoryListLayout
    category-title="商品分类"
    :category-tree-data="categoryTreeData"
    :tabs="tabs"
    :active-tab="activeTab"
    @category-select="onCategorySelect"
    @tab-change="onTabChange"
    @search="handleSearch"
    @page-change="handlePageChange"
  >
    <template #toolbar-left>
      <a-button type="primary">新增</a-button>
    </template>

    <template #table>
      <vxe-table :data="tableData" :columns="columns" />
    </template>
  </CategoryListLayout>
</template>

<script setup>
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'

const tabs = [
  { key: 'all', label: '全部商品' },
  { key: 'package', label: '套餐' },
]
const activeTab = ref('all')
const categoryTreeData = ref([])
const tableData = ref([])
const columns = ref([...])

function onCategorySelect(keys) {
  // 处理分类选择
}

function onTabChange(key) {
  activeTab.value = key
}

function handleSearch() {
  // 执行搜索
}

function handlePageChange(page, pageSize) {
  // 处理分页
}
</script>
```

### 2. 使用组合式函数

#### useCategoryTree

管理分类树的加载、选择、编辑等操作：

```typescript
import { useCategoryTree } from '@/composables'
import { productCategoryApi } from '@/api/erp/product'

const selectedCategoryId = ref(0)

const {
  categoryTreeData,
  categoryLoading,
  categoryError,
  expandedKeys,
  categoryModalVisible,
  categoryForm,
  fetchCategoryTree,
  onCategorySelect,
  showCategoryModal,
  handleCategoryOk,
} = useCategoryTree(productCategoryApi, {
  selectedId: selectedCategoryId,
  onDataRefresh: fetchProducts,
})
```

#### useSearchForm

管理搜索表单的状态和参数：

```typescript
import { useSearchForm } from '@/composables'

const {
  searchForm,
  showHierarchy,
  getSearchParams,
  resetSearchForm,
} = useSearchForm([
  { name: 'keyword', label: '关键词', type: 'input', width: 200 },
  { name: 'status', label: '状态', type: 'select', options: [...] },
  { name: 'createTime', label: '创建时间', type: 'daterange' },
])

// 获取搜索参数
const params = getSearchParams()
```

### 3. 完整示例（商品列表页）

参见：`src/views/erp/product/index.vue`

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| showCategoryPanel | boolean | true | 是否显示分类面板 |
| categoryTitle | string | '分类' | 分类面板标题 |
| categoryTreeData | any[] | [] | 分类树数据 |
| categoryLoading | boolean | false | 分类加载状态 |
| categoryError | boolean | false | 分类加载错误 |
| selectedCategoryId | number | 0 | 选中的分类ID |
| categoryExpandedKeys | number[] | [] | 展开的节点keys |
| tabs | TabItem[] | [] | Tab标签列表 |
| activeTab | string | '' | 当前激活的Tab |
| paginationCurrent | number | 1 | 当前页码 |
| paginationPageSize | number | 20 | 每页条数 |
| paginationTotal | number | 0 | 总条数 |
| currentPath | string | '' | 当前路径文本 |

## Events

| 事件 | 参数 | 说明 |
|------|------|------|
| category-add | - | 点击新增分类 |
| category-retry | - | 重试加载分类 |
| category-select | keys: number[] | 选择分类 |
| category-expand | keys: number[] | 展开/折叠分类 |
| tab-change | key: string | 切换Tab |
| search | - | 点击查询 |
| page-change | page: number, pageSize: number | 分页变化 |

## Slots

| 插槽 | 说明 |
|------|------|
| toolbar-left | 工具栏左侧 |
| toolbar-right | 工具栏右侧 |
| search-fields | 自定义搜索字段 |
| table | 表格内容 |

## 类型定义

```typescript
interface TabItem {
  key: string
  label: string
}

interface SearchField {
  name: string
  label: string
  type: 'input' | 'select' | 'date' | 'checkbox'
  width?: number
  placeholder?: string
  options?: Array<{ label: string; value: any }>
  value?: any
}
```

## 样式定制

组件使用 scoped styles，可以通过以下方式覆盖样式：

```css
/* 全局样式覆盖 */
.category-list-layout .category-panel {
  width: 250px;
}

/* Tab样式 */
.tab-item.active {
  color: #ff6b35;
  border-bottom-color: #ff6b35;
}
```

## 注意事项

1. **分类树数据格式**：必须包含 `id`、`categoryName` 字段，可选 `children`、`productCount`
2. **表格高度**：表格区域会自动填充剩余空间，确保父容器有明确高度
3. **分页参数**：分页事件会同时返回页码和每页条数
4. **搜索表单**：使用 `useSearchForm` 组合式函数可以更方便地管理搜索状态

## 迁移指南

### 从旧版商品列表页迁移

旧版代码直接使用内联的HTML和逻辑，现在改为使用通用组件：

```diff
- <div class="product-main">
-   <div class="category-panel">...</div>
-   <div class="product-panel">...</div>
- </div>
+ <CategoryListLayout ...>
+   <template #toolbar-left>...</template>
+   <template #table>...</template>
+ </CategoryListLayout>
```

组合式函数迁移：

```diff
- const categoryLoading = ref(false)
- const categoryTree = ref([])
- async function fetchCategoryTree() { ... }
+ const { categoryTreeData, categoryLoading, fetchCategoryTree } = useCategoryTree(...)
```
