<template>
  <div class="supplier-table">
    <a-table
      :columns="mergedColumns"
      :data-source="data"
      :loading="loading"
      :pagination="paginationConfig"
      :row-selection="selectable ? rowSelection : undefined"
      row-key="id"
      @change="handleTableChange"
    >
      <template v-if="showActions" #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <div class="table-actions">
            <a-button size="small" type="primary" @click="handleView(record)">查看</a-button>
            <a-button size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" danger @click="handleDelete(record)">删除</a-button>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Supplier, TableColumn } from '../../../types'
import { defaultColumns } from './index.ts'

interface Props {
  data: Supplier[]
  loading?: boolean
  columns?: TableColumn<Supplier>[]
  showActions?: boolean
  selectable?: boolean
  pagination?: boolean
  pageSize?: number
  total?: number
  currentPage?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  columns: () => [],
  showActions: true,
  selectable: false,
  pagination: true,
  pageSize: 10,
  total: 0,
  currentPage: 1
})

const emit = defineEmits<{
  (e: 'selection-change', selection: Supplier[]): void
  (e: 'edit', row: Supplier): void
  (e: 'delete', row: Supplier): void
  (e: 'view', row: Supplier): void
  (e: 'page-change', page: number): void
  (e: 'sort-change', sort: { prop: string; order: 'ascending' | 'descending' }): void
}>()

const selectedRowKeys = ref<(string | number)[]>([])

const handleView = (row: any) => emit('view', row)
const handleEdit = (row: any) => emit('edit', row)
const handleDelete = (row: any) => emit('delete', row)

const mergedColumns = computed(() => {
  let cols = props.columns.length > 0 ? props.columns : defaultColumns as any
  if (props.showActions) {
    cols = [
      ...cols,
      { title: '操作', key: 'action', width: 220, fixed: 'right' as const }
    ]
  }
  return cols
})

const paginationConfig = computed(() => {
  if (!props.pagination) return false
  return {
    current: props.currentPage,
    pageSize: props.pageSize,
    total: props.total,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`
  }
})

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[], rows: Supplier[]) => {
    selectedRowKeys.value = keys
    emit('selection-change', rows)
  }
}))

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  if (pag.current !== props.currentPage) {
    emit('page-change', pag.current)
  }
  if (sorter.field) {
    emit('sort-change', {
      prop: sorter.field,
      order: sorter.order === 'ascend' ? 'ascending' : 'descending'
    })
  }
}
</script>

<style scoped lang="scss">
.supplier-table {
  width: 100%;
}

.table-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
