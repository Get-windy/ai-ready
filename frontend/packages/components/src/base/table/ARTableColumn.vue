<template>
  <el-table-column
    :type="type"
    :index="index"
    :column-key="columnKey"
    :label="label"
    :prop="prop"
    :width="width"
    :min-width="minWidth"
    :fixed="fixed"
    :render-header="renderHeader"
    :sortable="sortable"
    :sort-method="sortMethod"
    :sort-by="sortBy"
    :sort-orders="sortOrders"
    :resizable="resizable"
    :formatter="formatter"
    :show-overflow-tooltip="showOverflowTooltip"
    :align="align"
    :header-align="headerAlign"
    :class-name="className"
    :label-class-name="labelClassName"
    :selectable="selectable"
    :reserve-selection="reserveSelection"
    :filters="filters"
    :filter-placement="filterPlacement"
    :filter-multiple="filterMultiple"
    :filter-method="filterMethod"
    :filtered-value="filteredValue"
  >
    <template v-if="$slots.default" #default="scope">
      <slot v-bind="scope" />
    </template>
    
    <template v-if="$slots.header" #header="scope">
      <slot name="header" v-bind="scope" />
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
import type { TableColumnCtx } from 'element-plus'
import type { VNode } from 'vue'

interface Props {
  type?: 'selection' | 'index' | 'expand'
  index?: number | ((index: number) => number)
  columnKey?: string
  label?: string
  prop?: string
  width?: string | number
  minWidth?: string | number
  fixed?: boolean | 'left' | 'right'
  renderHeader?: (data: { column: TableColumnCtx; $index: number }) => VNode
  sortable?: boolean | 'custom'
  sortMethod?: (a: any, b: any) => number
  sortBy?: string | string[] | ((row: any, index: number) => string)
  sortOrders?: ('ascending' | 'descending' | null)[]
  resizable?: boolean
  formatter?: (row: any, column: TableColumnCtx, cellValue: any, index: number) => any
  showOverflowTooltip?: boolean
  align?: 'left' | 'center' | 'right'
  headerAlign?: 'left' | 'center' | 'right'
  className?: string
  labelClassName?: string
  selectable?: (row: any, index: number) => boolean
  reserveSelection?: boolean
  filters?: { text: string; value: any }[]
  filterPlacement?: string
  filterMultiple?: boolean
  filterMethod?: (value: any, row: any, column: TableColumnCtx) => boolean
  filteredValue?: any[]
}

const props = withDefaults(defineProps<Props>(), {
  sortable: false,
  resizable: true,
  showOverflowTooltip: false,
  align: 'left',
  headerAlign: 'left',
  reserveSelection: false,
  filterMultiple: true
})
</script>

<style lang="scss" scoped>
.ar-table-column {
  &__header {
    font-weight: 600;
    color: var(--ar-text-color-primary, #303133);
    
    &--sortable {
      cursor: pointer;
      
      &:hover {
        color: var(--ar-color-primary, #409eff);
      }
    }
  }
  
  &__cell {
    &--center {
      text-align: center;
    }
    
    &--right {
      text-align: right;
    }
  }
}
</style>