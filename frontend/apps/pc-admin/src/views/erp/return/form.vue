<template>
  <OrderFormPage
    title="退货单"
    bill-prefix="RT"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :tabs="tabs"
    :summary-items="summaryItems"
    :api="returnOrderApi"
    redirect-path="/erp/return"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import { returnOrderApi } from '@/api/erp'
import type { HeaderField, DetailColumn, TabConfig, SummaryItem } from '@/components/OrderFormPage/types'

const headerFields: HeaderField[] = [
  { name: 'customerId', label: '客户', type: 'select', optionsRef: 'customers', required: true },
  { name: 'warehouseId', label: '退货仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '经手人', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '单据日期', type: 'date', required: true },
  { name: 'returnReason', label: '退货原因', type: 'select', options: [
    { label: '质量问题', value: 1 },
    { label: '错发货物', value: 2 },
    { label: '客户拒收', value: 3 },
    { label: '其他原因', value: 4 },
  ]},
  { name: 'returnNo', label: '原单号', type: 'input', placeholder: '请输入原销售单号' },
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'remark', title: '备注', width: 200 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]

const tabs: TabConfig[] = [
  {
    key: 'refund',
    tab: '退款信息',
    fields: [
      { name: 'refundMethod', label: '退款方式', type: 'select', options: [
        { label: '原路退回', value: 1 },
        { label: '现金退款', value: 2 },
        { label: '冲抵货款', value: 3 },
      ]},
      { name: 'refundAmount', label: '退款金额', type: 'number', precision: 2 },
      { name: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
]

const summaryItems: SummaryItem[] = [
  { label: '总数量', valueKey: 'totalQuantity', format: (v) => String(v) },
  { label: '退货金额', valueKey: 'totalAmount', highlight: true, format: (v) => `¥${v.toFixed(2)}` },
]
</script>
