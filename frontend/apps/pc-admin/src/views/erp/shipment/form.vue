<template>
  <OrderFormPage
    title="发货单"
    bill-prefix="SH"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :tabs="tabs"
    :summary-items="summaryItems"
    :api="shipmentApi"
    redirect-path="/erp/shipment"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import { shipmentApi } from '@/api/erp'
import type { HeaderField, DetailColumn, TabConfig, SummaryItem } from '@/components/OrderFormPage/types'

const headerFields: HeaderField[] = [
  { name: 'customerId', label: '客户', type: 'select', optionsRef: 'customers', required: true },
  { name: 'warehouseId', label: '发货仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'handlerId', label: '经手人', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '单据日期', type: 'date', required: true },
  { name: 'shipmentType', label: '发货类型', type: 'select', options: [
    { label: '销售发货', value: 1 },
    { label: '调拨发货', value: 2 },
    { label: '其他发货', value: 3 },
  ]},
  { name: 'receiverName', label: '收货人', type: 'input', placeholder: '请输入收货人' },
  { name: 'receiverPhone', label: '联系电话', type: 'input', placeholder: '请输入联系电话' },
  { name: 'receiverAddress', label: '收货地址', type: 'input', placeholder: '请输入收货地址' },
]

const detailColumns: DetailColumn[] = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'taxRate', title: '税率%', width: 80, slotName: 'taxRateCell' },
  { field: 'taxAmount', title: '税额', width: 100, slotName: 'taxAmountCell' },
  { field: 'totalAmount', title: '价税合计', width: 120, slotName: 'totalAmountCell' },
  { field: 'remark', title: '备注', width: 150 },
  { type: 'action' as any, title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]

const tabs: TabConfig[] = [
  {
    key: 'logistics',
    tab: '物流信息',
    fields: [
      { name: 'logisticsCompany', label: '物流公司', type: 'input' },
      { name: 'logisticsNo', label: '物流单号', type: 'input' },
      { name: 'shippingFee', label: '运费', type: 'number', precision: 2 },
    ],
  },
  {
    key: 'remark',
    tab: '备注',
    fields: [
      { name: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
]

const summaryItems: SummaryItem[] = [
  { label: '总数量', valueKey: 'totalQuantity', format: (v) => String(v) },
  { label: '商品金额', valueKey: 'totalAmount', format: (v) => `¥${v.toFixed(2)}` },
  { label: '税额', valueKey: 'totalTaxAmount', format: (v) => `¥${v.toFixed(2)}` },
  { label: '价税合计', valueKey: 'totalWithTax', highlight: true, format: (v) => `¥${v.toFixed(2)}` },
]
</script>
