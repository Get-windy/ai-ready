<template>
  <BasicFormPage
    title="发票"
    :fields="fields"
    :api="invoiceApi"
    redirect-path="/crm/invoice"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { invoiceApi } from '@/api/crm'

const fields: BasicField[] = [
  { name: 'invoiceNo', label: '发票号码', type: 'input', span: 12 },
  { name: 'invoiceType', label: '发票类型', type: 'select', options: [
    { label: '增值税专用发票', value: '增值税专用发票' },
    { label: '增值税普通发票', value: '增值税普通发票' },
    { label: '电子发票', value: '电子发票' },
  ], span: 12 },
  { name: 'customerName', label: '客户名称', type: 'input', required: true, span: 12 },
  { name: 'invoiceDate', label: '开票日期', type: 'date', required: true, span: 12 },
  { name: 'amount', label: '金额', type: 'number', precision: 2, required: true, span: 8 },
  { name: 'taxAmount', label: '税额', type: 'number', precision: 2, span: 8 },
  { name: 'totalAmount', label: '价税合计', type: 'number', precision: 2, span: 8 },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '待开票', value: 'pending' },
    { label: '已开票', value: 'issued' },
    { label: '已作废', value: 'voided' },
  ], span: 12 },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
