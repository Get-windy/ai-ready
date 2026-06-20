<template>
  <OrderFormPage
    title="波次单"
    bill-prefix="WV"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :api="wmsWaveApi"
    :show-tax="false"
    redirect-path="/wms/wave"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import request from '@/utils/request'
import type { HeaderField, DetailColumn } from '@/components/OrderFormPage/types'

const wmsWaveApi = {
  create(data: any) { return request.post('/wms/wave', data) },
  update(id: number, data: any) { return request.put(`/wms/wave/${id}`, data) },
  getById(id: number) { return request.get(`/wms/wave/${id}`) },
}

const headerFields: HeaderField[] = [
  { name: 'warehouseId', label: '仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '操作员', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '波次日期', type: 'date', required: true },
  { name: 'waveType', label: '波次类型', type: 'select', options: [
    { label: '普通波次', value: 1 },
    { label: '紧急波次', value: 2 },
  ]},
]

const detailColumns: DetailColumn[] = [
  { field: 'orderNo', title: '订单号', width: 150 },
  { field: 'customerName', title: '客户', width: 150 },
  { field: 'productCount', title: '商品数', width: 100 },
  { field: 'totalQuantity', title: '总数量', width: 100 },
  { field: 'remark', title: '备注', width: 200 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]
</script>
