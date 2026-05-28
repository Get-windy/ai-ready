<template>
  <div class="supplier-table">
    <ARTable
      :data="data"
      :columns="mergedColumns"
      :loading="loading"
      :pagination="pagination"
      :page-size="pageSize"
      :total="total"
      :current-page="currentPage"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
    >
      <template v-if="showActions" #actions="{ row }">
        <div class="table-actions">
          <ARButton size="small" type="primary" @click="$emit('view', row)">
            查看
          </ARButton>
          <ARButton size="small" type="default" @click="$emit('edit', row)">
            编辑
          </ARButton>
          <ARButton size="small" type="danger" @click="$emit('delete', row)">
            删除
          </ARButton>
        </div>
      </template>
    </ARTable>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ARTable from '../../../base/table/ARTable.vue'
import ARButton from '../../../base/button/ARButton.vue'
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
  currentPage: 1,
})

const emit = defineEmits<{
  (e: 'selection-change', selection: Supplier[]): void
  (e: 'edit', row: Supplier): void
  (e: 'delete', row: Supplier): void
  (e: 'view', row: Supplier): void
  (e: 'page-change', page: number): void
  (e: 'sort-change', sort: { prop: string; order: 'ascending' | 'descending' }): void
}>()

const mergedColumns = computed(() => {
  if (props.columns.length > 0) {
    return props.columns
  }
  return defaultColumns
})

const handlePageChange = (page: number) => {
  emit('page-change', page)
}

const handleSortChange = (sort: { prop: string; order: 'ascending' | 'descending' }) => {
  emit('sort-change', sort)
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