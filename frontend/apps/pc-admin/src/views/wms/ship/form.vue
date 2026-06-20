<template>
  <OrderFormPage
    title="发货单"
    bill-prefix="WS"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :api="wmsShipApi"
    :show-tax="false"
    redirect-path="/wms/ship"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import request from '@/utils/request'
import type { HeaderField, DetailColumn } from '@/components/OrderFormPage/types'

const wmsShipApi = {
  create(data: any) { return request.post('/wms/ship', data) },
  update(id: number, data: any) { return request.put(`/wms/ship/${id}`, data) },
  getById(id: number) { return request.get(`/wms/ship/${id}`) },
}

const headerFields: HeaderField[] = [
  { name: 'warehouseId', label: '发货仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '操作员', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '发货日期', type: 'date', required: true },
  { name: 'shipType', label: '发货类型', type: 'select', options: [
    { label: '销售发货', value: 1 },
    { label: '调拨发货', value: 2 },
  ]},
  { name: 'receiverName', label: '收货人', type: 'input' },
  { name: 'receiverPhone', label: '联系电话', type: 'input' },
  { name: 'receiverAddress', label: '收货地址', type: 'input' },
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
