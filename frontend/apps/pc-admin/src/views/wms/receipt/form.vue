<template>
  <OrderFormPage
    title="收货单"
    bill-prefix="RC"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :api="wmsReceiptApi"
    :show-tax="false"
    redirect-path="/wms/receipt"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import request from '@/utils/request'
import type { HeaderField, DetailColumn } from '@/components/OrderFormPage/types'

const wmsReceiptApi = {
  create(data: any) { return request.post('/wms/receipt', data) },
  update(id: number, data: any) { return request.put(`/wms/receipt/${id}`, data) },
  getById(id: number) { return request.get(`/wms/receipt/${id}`) },
}

const headerFields: HeaderField[] = [
  { name: 'warehouseId', label: '收货仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '操作员', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '收货日期', type: 'date', required: true },
  { name: 'receiptType', label: '收货类型', type: 'select', options: [
    { label: '采购收货', value: 1 },
    { label: '调拨收货', value: 2 },
    { label: '退货收货', value: 3 },
  ]},
  { name: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '关联订单号' },
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'locationCode', title: '库位', width: 100 },
  { field: 'batchNo', title: '批次号', width: 120 },
  { field: 'remark', title: '备注', width: 150 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]
</script>
