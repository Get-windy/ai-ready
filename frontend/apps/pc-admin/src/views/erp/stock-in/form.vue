<template>
  <OrderFormPage
    title="入库单"
    bill-prefix="IN"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :tabs="tabs"
    :summary-items="summaryItems"
    :api="stockInApi"
    redirect-path="/erp/stock-in"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import { stockInApi } from '@/api/erp'
import type { HeaderField, DetailColumn, TabConfig, SummaryItem } from '@/components/OrderFormPage/types'

const headerFields: HeaderField[] = [
  { name: 'supplierId', label: '供应商', type: 'select', optionsRef: 'suppliers', required: true },
  { name: 'warehouseId', label: '入库仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '经手人', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '单据日期', type: 'date', required: true },
  { name: 'stockInType', label: '入库类型', type: 'select', options: [
    { label: '采购入库', value: 1 },
    { label: '退货入库', value: 2 },
    { label: '调拨入库', value: 3 },
    { label: '其他入库', value: 4 },
  ]},
  { name: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '关联采购订单号' },
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'batchNo', title: '批次号', width: 120 },
  { field: 'remark', title: '备注', width: 150 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]

const tabs: TabConfig[] = [
  {
    key: 'payment',
    tab: '付款信息',
    fields: [
      { name: 'paymentMethod', label: '付款方式', type: 'select', options: [
        { label: '现金', value: 1 },
        { label: '银行转账', value: 2 },
        { label: '支票', value: 3 },
        { label: '月结', value: 4 },
      ]},
      { name: 'paymentAccount', label: '付款账户', type: 'input' },
      { name: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
]

const summaryItems: SummaryItem[] = [
  { label: '总数量', valueKey: 'totalQuantity', format: (v) => String(v) },
  { label: '入库金额', valueKey: 'totalAmount', format: (v) => `¥${v.toFixed(2)}` },
  { label: '价税合计', valueKey: 'totalWithTax', highlight: true, format: (v) => `¥${v.toFixed(2)}` },
]
</script>
