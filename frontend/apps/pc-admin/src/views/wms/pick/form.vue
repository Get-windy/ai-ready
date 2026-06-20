<template>
  <OrderFormPage
    title="拣货单"
    bill-prefix="PK"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :api="wmsPickApi"
    :show-tax="false"
    redirect-path="/wms/pick"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import request from '@/utils/request'
import type { HeaderField, DetailColumn } from '@/components/OrderFormPage/types'

const wmsPickApi = {
  create(data: any) { return request.post('/wms/pick', data) },
  update(id: number, data: any) { return request.put(`/wms/pick/${id}`, data) },
  getById(id: number) { return request.get(`/wms/pick/${id}`) },
}

const headerFields: HeaderField[] = [
  { name: 'warehouseId', label: '仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '拣货员', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '拣货日期', type: 'date', required: true },
  { name: 'pickType', label: '拣货方式', type: 'select', options: [
    { label: '按单拣货', value: 1 },
    { label: '批量拣货', value: 2 },
  ]},
  { name: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '出库单号' },
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'locationCode', title: '拣货库位', width: 120 },
  { field: 'batchNo', title: '批次号', width: 120 },
  { field: 'remark', title: '备注', width: 150 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]
</script>
