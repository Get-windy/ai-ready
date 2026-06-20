<template>
  <OrderFormPage
    title="移库单"
    bill-prefix="MV"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :api="wmsMoveApi"
    :show-tax="false"
    redirect-path="/wms/move"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import request from '@/utils/request'
import type { HeaderField, DetailColumn } from '@/components/OrderFormPage/types'

const wmsMoveApi = {
  create(data: any) { return request.post('/wms/move', data) },
  update(id: number, data: any) { return request.put(`/wms/move/${id}`, data) },
  getById(id: number) { return request.get(`/wms/move/${id}`) },
}

const headerFields: HeaderField[] = [
  { name: 'warehouseId', label: '仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '操作员', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '移库日期', type: 'date', required: true },
  { name: 'fromLocation', label: '源库位', type: 'input', required: true, placeholder: '原库位编码' },
  { name: 'toLocation', label: '目标库位', type: 'input', required: true, placeholder: '目标库位编码' },
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'batchNo', title: '批次号', width: 120 },
  { field: 'remark', title: '备注', width: 200 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]
</script>
