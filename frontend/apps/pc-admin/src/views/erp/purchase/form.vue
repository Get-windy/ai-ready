<template>
  <OrderFormPage
    title="采购订单"
    bill-prefix="PO"
    :header-fields="headerFields"
    :detail-columns="detailColumns"
    :tabs="tabs"
    :summary-items="summaryItems"
    :api="purchaseOrderApi"
    redirect-path="/purchase/order"
  />
</template>

<script setup lang="ts">
import OrderFormPage from '@/components/OrderFormPage/index.vue'
import { purchaseOrderApi } from '@/api/erp'
import type { HeaderField, DetailColumn, TabConfig, SummaryItem } from '@/components/OrderFormPage/types'

const headerFields: HeaderField[] = [
  { name: 'supplierId', label: '供应商', type: 'select', optionsRef: 'suppliers', required: true },
  { name: 'warehouseId', label: '入库仓库', type: 'select', optionsRef: 'warehouses', required: true },
  { name: 'buyerId', label: '采购员', type: 'select', optionsRef: 'users', required: true },
  { name: 'date', label: '单据日期', type: 'date', required: true },
  { name: 'purchaseType', label: '采购类型', type: 'select', options: [
    { label: '正常采购', value: 1 },
    { label: '紧急采购', value: 2 },
    { label: '样品采购', value: 3 },
  ]},
  { name: 'deliveryDate', label: '交货日期', type: 'date' },
  { name: 'contactName', label: '联系人', type: 'input', placeholder: '供应商联系人' },
  { name: 'contactPhone', label: '联系电话', type: 'input', placeholder: '供应商联系电话' },
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
  {
    key: 'logistics',
    tab: '交货信息',
    fields: [
      { name: 'deliveryAddress', label: '交货地址', type: 'input', span: 16 },
      { name: 'logisticsCompany', label: '物流公司', type: 'input' },
      { name: 'freight', label: '运费', type: 'number', precision: 2 },
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
