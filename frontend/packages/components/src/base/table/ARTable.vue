<template>
  <a-table
    ref="tableRef"
    :columns="columns"
    :data-source="data"
    :row-key="rowKey"
    :loading="loading"
    :bordered="bordered"
    :size="size"
    :show-header="showHeader"
    :pagination="paginationConfig"
    :row-selection="rowSelectionConfig"
    :scroll="scrollConfig"
    :locale="localeConfig"
    @change="handleChange"
  >
    <template v-for="(_, slot) in $slots" :key="slot" #[slot]="scope">
      <slot :name="slot" v-bind="scope" />
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { TableProps, TablePaginationConfig } from 'ant-design-vue'

interface Column {
  title: string
  dataIndex?: string
  key?: string
  width?: number | string
  minWidth?: number | string
  fixed?: boolean | 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  ellipsis?: boolean
  sorter?: boolean | ((a: any, b: any) => number)
  defaultSortOrder?: 'ascend' | 'descend'
  filters?: { text: string; value: any }[]
  customRender?: (args: { text: any; record: any; index: number }) => any
  slots?: { customRender?: string }
  [key: string]: any
}

interface Props {
  data?: any[]
  columns?: Column[]
  rowKey?: string | ((record: any) => string)
  loading?: boolean
  bordered?: boolean
  size?: 'default' | 'middle' | 'small'
  showHeader?: boolean
  pagination?: TablePaginationConfig | false
  rowSelection?: TableProps['rowSelection']
  scrollX?: number | string | true
  scrollY?: number | string
  emptyText?: string
  stripe?: boolean
  fit?: boolean
  highlightCurrentRow?: boolean
  currentRowKey?: string | number
  rowClassName?: TableProps['rowClassName']
  rowStyle?: TableProps['rowStyle']
  cellClassName?: TableProps['cellClassName']
  cellStyle?: TableProps['cellStyle']
  headerRowClassName?: TableProps['headerRowClassName']
  headerRowStyle?: TableProps['headerRowStyle']
  headerCellClassName?: TableProps['headerCellClassName']
  headerCellStyle?: TableProps['headerCellStyle']
  defaultExpandAll?: boolean
  expandRowKeys?: any[]
  showSummary?: boolean
  sumText?: string
  summaryMethod?: TableProps['summaryMethod']
  spanMethod?: TableProps['spanMethod']
  selectOnIndeterminate?: boolean
  indent?: number
  treeProps?: any
  tableLayout?: 'auto' | 'fixed'
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  columns: () => [],
  rowKey: 'id',
  loading: false,
  bordered: false,
  size: 'default',
  showHeader: true,
  pagination: undefined,
  stripe: false,
  fit: true,
  highlightCurrentRow: false,
  showSummary: false,
  selectOnIndeterminate: true,
  indent: 16,
  tableLayout: 'auto'
})

const emit = defineEmits<{
  (e: 'change', pagination: any, filters: any, sorter: any): void
  (e: 'select', record: any, selected: boolean, selectedRows: any[]): void
  (e: 'select-all', selected: boolean, selectedRows: any[], changeRows: any[]): void
  (e: 'selection-change', selectedRowKeys: any[], selectedRows: any[]): void
  (e: 'cell-click', record: any, column: any, event: Event): void
  (e: 'row-click', record: any, index: number, event: Event): void
  (e: 'row-dblclick', record: any, index: number, event: Event): void
  (e: 'current-change', currentRow: any): void
  (e: 'expand-change', expanded: boolean, record: any): void
  (e: 'sorter-change', sorter: any): void
  (e: 'page-change', page: number): void
  (e: 'page-size-change', size: number): void
}>()

const tableRef = ref()

const paginationConfig = computed(() => {
  if (props.pagination === false) return false
  if (props.pagination) {
    const p = { ...props.pagination }
    return {
      ...p,
      onChange: (page: number) => {
        emit('page-change', page)
        if (props.pagination && typeof props.pagination === 'object') {
          props.pagination.onChange?.(page)
        }
      },
      onShowSizeChange: (current: number, size: number) => {
        emit('page-size-change', size)
        if (props.pagination && typeof props.pagination === 'object') {
          props.pagination.onShowSizeChange?.(current, size)
        }
      }
    }
  }
  return undefined
})

const rowSelectionConfig = computed(() => {
  if (!props.rowSelection) return undefined
  return {
    ...props.rowSelection,
    onChange: (selectedRowKeys: any[], selectedRows: any[]) => {
      emit('selection-change', selectedRowKeys, selectedRows)
      props.rowSelection?.onChange?.(selectedRowKeys, selectedRows)
    },
    onSelect: (record: any, selected: boolean, selectedRows: any[]) => {
      emit('select', record, selected, selectedRows)
      props.rowSelection?.onSelect?.(record, selected, selectedRows)
    },
    onSelectAll: (selected: boolean, selectedRows: any[], changeRows: any[]) => {
      emit('select-all', selected, selectedRows, changeRows)
      props.rowSelection?.onSelectAll?.(selected, selectedRows, changeRows)
    }
  }
})

const scrollConfig = computed(() => {
  if (!props.scrollX && !props.scrollY) return undefined
  return {
    x: props.scrollX ?? undefined,
    y: props.scrollY ?? undefined
  }
})

const localeConfig = computed(() => {
  if (!props.emptyText) return undefined
  return { emptyText: props.emptyText }
})

const handleChange = (pagination: any, filters: any, sorter: any) => {
  emit('change', pagination, filters, sorter)
}

// 暴露方法
defineExpose({
  tableRef
})
</script>

<style lang="scss" scoped>
.ar-table {
  &__header {
    background-color: var(--ar-bg-color, #f0f2f5);
    font-weight: 600;
    color: var(--ar-text-color-primary, #303133);
  }

  &__row {
    &:hover {
      background-color: var(--ar-bg-color-hover, #f5f7fa);
    }

    &--selected {
      background-color: var(--ar-bg-color-selected, #ecf5ff);
    }
  }

  &__cell {
    padding: 12px 0;
  }

  &__empty {
    padding: 40px 0;
    color: var(--ar-text-color-secondary, #909399);
    text-align: center;
  }
}
</style>
