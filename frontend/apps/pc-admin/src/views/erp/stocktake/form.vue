<template>
  <OrderFormPage
    title="盘点单"
    bill-prefix="PC"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :api="stocktakeOrderApi"
    :show-tax="false"
    redirect-path="/erp/stocktake"
  >
    <!-- 自定义账面数量列 -->
    <template #bookQtyCell="{ record }">
      <span>{{ record.bookQuantity ?? '-' }}</span>
    </template>
    <!-- 自定义实盘数量列 -->
    <template #actualQtyCell="{ record }">
      <a-input-number v-model:value="record.actualQuantity" :min="0" :precision="2" style="width: 100%" size="small" />
    </template>
    <!-- 自定义差异列 -->
    <template #diffQtyCell="{ record }">
      <span :class="{ 'diff-positive': (record.actualQuantity ?? 0) - (record.bookQuantity ?? 0) > 0, 'diff-negative': (record.actualQuantity ?? 0) - (record.bookQuantity ?? 0) < 0 }">
        {{ ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)).toFixed(2) }}
      </span>
    </template>
  </OrderFormPage>
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import { stocktakeOrderApi } from '@/api/erp'
import type { HeaderField, DetailColumn } from '@/components/OrderFormPage/types'

const headerFields: HeaderField[] = [
  { name: 'warehouseId', label: '盘点仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '盘点人', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '盘点日期', type: 'date', required: true },
  { name: 'stocktakeType', label: '盘点类型', type: 'select', options: [
    { label: '全面盘点', value: 1 },
    { label: '抽盘', value: 2 },
    { label: '动态盘点', value: 3 },
  ]},
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'bookQuantity', title: '账面数量', width: 100, slotName: 'bookQtyCell' },
  { field: 'actualQuantity', title: '实盘数量', width: 120, slotName: 'actualQtyCell' },
  { field: 'diffQuantity', title: '差异', width: 100, slotName: 'diffQtyCell' },
  { field: 'remark', title: '备注', width: 200 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]
</script>

<style scoped>
.diff-positive {
  color: #52c41a;
  font-weight: 600;
}
.diff-negative {
  color: #f5222d;
  font-weight: 600;
}
</style>
