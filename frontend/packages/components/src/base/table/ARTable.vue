<template>
  <el-table
    ref="tableRef"
    :data="data"
    :height="height"
    :max-height="maxHeight"
    :stripe="stripe"
    :border="border"
    :size="size"
    :fit="fit"
    :show-header="showHeader"
    :highlight-current-row="highlightCurrentRow"
    :current-row-key="currentRowKey"
    :row-class-name="rowClassName"
    :row-style="rowStyle"
    :cell-class-name="cellClassName"
    :cell-style="cellStyle"
    :header-row-class-name="headerRowClassName"
    :header-row-style="headerRowStyle"
    :header-cell-class-name="headerCellClassName"
    :header-cell-style="headerCellStyle"
    :row-key="rowKey"
    :empty-text="emptyText"
    :default-expand-all="defaultExpandAll"
    :expand-row-keys="expandRowKeys"
    :default-sort="defaultSort"
    :tooltip-effect="tooltipEffect"
    :show-summary="showSummary"
    :sum-text="sumText"
    :summary-method="summaryMethod"
    :span-method="spanMethod"
    :select-on-indeterminate="selectOnIndeterminate"
    :indent="indent"
    :lazy="lazy"
    :load="load"
    :tree-props="treeProps"
    :table-layout="tableLayout"
    :scrollbar-always-on="scrollbarAlwaysOn"
    :flexible="flexible"
    @select="emit('select', $event)"
    @select-all="emit('select-all', $event)"
    @selection-change="emit('selection-change', $event)"
    @cell-mouse-enter="emit('cell-mouse-enter', $event)"
    @cell-mouse-leave="emit('cell-mouse-leave', $event)"
    @cell-click="emit('cell-click', $event)"
    @cell-dblclick="emit('cell-dblclick', $event)"
    @row-click="emit('row-click', $event)"
    @row-contextmenu="emit('row-contextmenu', $event)"
    @row-dblclick="emit('row-dblclick', $event)"
    @header-click="emit('header-click', $event)"
    @header-contextmenu="emit('header-contextmenu', $event)"
    @sort-change="emit('sort-change', $event)"
    @filter-change="emit('filter-change', $event)"
    @current-change="emit('current-change', $event)"
    @header-dragend="emit('header-dragend', $event)"
    @expand-change="emit('expand-change', $event)"
  >
    <slot />
    
    <!-- 默认插槽 -->
    <template v-if="$slots.empty" #empty>
      <slot name="empty" />
    </template>
    
    <template v-if="$slots.append" #append>
      <slot name="append" />
    </template>
  </el-table>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ElTable } from 'element-plus'
import type {
  TableProps,
  TableColumnCtx,
  TableData,
  Sort,
  Filter
} from 'element-plus'

interface Props {
  data?: TableData[]
  height?: string | number
  maxHeight?: string | number
  stripe?: boolean
  border?: boolean
  size?: 'large' | 'default' | 'small'
  fit?: boolean
  showHeader?: boolean
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
  rowKey?: string | ((row: TableData) => string)
  emptyText?: string
  defaultExpandAll?: boolean
  expandRowKeys?: TableData[]
  defaultSort?: Sort
  tooltipEffect?: 'dark' | 'light'
  showSummary?: boolean
  sumText?: string
  summaryMethod?: TableProps['summaryMethod']
  spanMethod?: TableProps['spanMethod']
  selectOnIndeterminate?: boolean
  indent?: number
  lazy?: boolean
  load?: TableProps['load']
  treeProps?: TableProps['treeProps']
  tableLayout?: 'auto' | 'fixed'
  scrollbarAlwaysOn?: boolean
  flexible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  stripe: false,
  border: false,
  size: 'default',
  fit: true,
  showHeader: true,
  highlightCurrentRow: false,
  showSummary: false,
  selectOnIndeterminate: true,
  indent: 16,
  lazy: false,
  tableLayout: 'auto',
  scrollbarAlwaysOn: false,
  flexible: false
})

const emit = defineEmits<{
  (e: 'select', selection: TableData[], row: TableData): void
  (e: 'select-all', selection: TableData[]): void
  (e: 'selection-change', selection: TableData[]): void
  (e: 'cell-mouse-enter', row: TableData, column: TableColumnCtx<TableData>, cell: HTMLElement, event: Event): void
  (e: 'cell-mouse-leave', row: TableData, column: TableColumnCtx<TableData>, cell: HTMLElement, event: Event): void
  (e: 'cell-click', row: TableData, column: TableColumnCtx<TableData>, cell: HTMLElement, event: Event): void
  (e: 'cell-dblclick', row: TableData, column: TableColumnCtx<TableData>, cell: HTMLElement, event: Event): void
  (e: 'row-click', row: TableData, column: TableColumnCtx<TableData>, event: Event): void
  (e: 'row-contextmenu', row: TableData, column: TableColumnCtx<TableData>, event: Event): void
  (e: 'row-dblclick', row: TableData, column: TableColumnCtx<TableData>, event: Event): void
  (e: 'header-click', column: TableColumnCtx<TableData>, event: Event): void
  (e: 'header-contextmenu', column: TableColumnCtx<TableData>, event: Event): void
  (e: 'sort-change', sort: Sort): void
  (e: 'filter-change', filters: Filter): void
  (e: 'current-change', currentRow: TableData | null, oldCurrentRow: TableData | null): void
  (e: 'header-dragend', newWidth: number, oldWidth: number, column: TableColumnCtx<TableData>, event: Event): void
  (e: 'expand-change', row: TableData, expandedRows: TableData[]): void
}>()

const tableRef = ref<InstanceType<typeof ElTable>>()

// 表格操作方法
const clearSelection = () => {
  if (!tableRef.value) return
  tableRef.value.clearSelection()
}

const toggleRowSelection = (row: TableData, selected?: boolean) => {
  if (!tableRef.value) return
  tableRef.value.toggleRowSelection(row, selected)
}

const toggleAllSelection = () => {
  if (!tableRef.value) return
  tableRef.value.toggleAllSelection()
}

const toggleRowExpansion = (row: TableData, expanded?: boolean) => {
  if (!tableRef.value) return
  tableRef.value.toggleRowExpansion(row, expanded)
}

const setCurrentRow = (row?: TableData) => {
  if (!tableRef.value) return
  tableRef.value.setCurrentRow(row)
}

const clearSort = () => {
  if (!tableRef.value) return
  tableRef.value.clearSort()
}

const clearFilter = (columnKeys?: string[]) => {
  if (!tableRef.value) return
  tableRef.value.clearFilter(columnKeys)
}

const doLayout = () => {
  if (!tableRef.value) return
  tableRef.value.doLayout()
}

const sort = (prop: string, order: 'ascending' | 'descending') => {
  if (!tableRef.value) return
  tableRef.value.sort(prop, order)
}

// 暴露方法
defineExpose({
  clearSelection,
  toggleRowSelection,
  toggleAllSelection,
  toggleRowExpansion,
  setCurrentRow,
  clearSort,
  clearFilter,
  doLayout,
  sort,
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