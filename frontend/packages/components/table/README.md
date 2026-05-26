# DataTable 数据表格组件

基于 Vue 3 + Vant 4 的数据表格组件，支持排序、筛选、分页和响应式设计。

## 功能特性

- ✅ 排序功能 - 支持单列排序，升序/降序切换
- ✅ 筛选功能 - 支持多列筛选，侧边栏筛选面板
- ✅ 分页功能 - 支持页码切换，每页条数设置
- ✅ 响应式设计 - 适配移动端，支持横向滚动
- ✅ 自定义列 - 支持固定列、自定义列宽、格式化显示
- ✅ 插槽支持 - 支持自定义单元格内容

## 安装使用

```vue
<template>
  <DataTable
    :data="tableData"
    :columns="columns"
    :loading="loading"
    @sort-change="handleSort"
    @filter-change="handleFilter"
    @page-change="handlePageChange"
  />
</template>

<script setup>
import { ref } from 'vue';
import DataTable from './DataTable.vue';

const loading = ref(false);
const tableData = ref([
  { id: 1, name: '张三', age: 28, department: '技术部', salary: 15000 },
  { id: 2, name: '李四', age: 32, department: '产品部', salary: 18000 },
  { id: 3, name: '王五', age: 25, department: '设计部', salary: 12000 },
  // ...
]);

const columns = [
  { key: 'id', title: 'ID', width: 80, sortable: true },
  { key: 'name', title: '姓名', sortable: true, filterable: true, filterType: 'text' },
  { key: 'age', title: '年龄', width: 100, sortable: true },
  { key: 'department', title: '部门', sortable: true, filterable: true, filterType: 'text' },
  { 
    key: 'salary', 
    title: '薪资', 
    sortable: true,
    formatter: (val) => `¥${val.toLocaleString()}`
  },
];

const handleSort = ({ key, order }) => {
  console.log('排序:', key, order);
};

const handleFilter = (filters) => {
  console.log('筛选:', filters);
};

const handlePageChange = ({ page, pageSize }) => {
  console.log('分页:', page, pageSize);
};
</script>
```

## Props 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | Array | [] | 表格数据源 |
| columns | Array | required | 列定义配置 |
| showToolbar | Boolean | true | 是否显示工具栏 |
| showSearch | Boolean | true | 是否显示搜索框 |
| searchPlaceholder | String | '请输入搜索内容' | 搜索框占位符 |
| showFilter | Boolean | true | 是否显示筛选按钮 |
| showPagination | Boolean | true | 是否显示分页 |
| defaultPage | Number | 1 | 默认页码 |
| defaultPageSize | Number | 10 | 默认每页条数 |
| pageSizeOptions | Array | [...] | 每页条数选项 |
| striped | Boolean | false | 是否斑马纹 |
| bordered | Boolean | true | 是否显示边框 |
| emptyText | String | '暂无数据' | 空数据提示 |
| loading | Boolean | false | 加载状态 |
| rowKey | String | 'id' | 行唯一标识字段 |

## Column 列配置

| 属性 | 类型 | 说明 |
|------|------|------|
| key | String | 字段名 |
| title | String | 列标题 |
| sortable | Boolean | 是否可排序 |
| filterable | Boolean | 是否可筛选 |
| filterType | String | 筛选类型: text/number |
| width | Number/String | 列宽 |
| fixed | String | 固定列: left/right |
| hidden | Boolean | 是否隐藏 |
| formatter | Function | 格式化函数 |

## Events 事件

| 事件名 | 参数 | 说明 |
|--------|------|------|
| sort-change | { key, order } | 排序变化 |
| filter-change | filters | 筛选变化 |
| page-change | { page, pageSize } | 分页变化 |
| search | query | 搜索 |

## 插槽

| 插槽名 | 参数 | 说明 |
|--------|------|------|
| cell-[key] | { row, value, index } | 自定义单元格内容 |

## 示例：自定义单元格

```vue
<DataTable :data="data" :columns="columns">
  <template #cell-salary="{ value }">
    <span style="color: #f56c6c; font-weight: bold;">
      ¥{{ value.toLocaleString() }}
    </span>
  </template>
  
  <template #cell-status="{ value }">
    <van-tag :type="value === 'active' ? 'success' : 'danger'">
      {{ value === 'active' ? '启用' : '禁用' }}
    </van-tag>
  </template>
</DataTable>
```
