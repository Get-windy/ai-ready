<template>
  <BasicFormPage
    title="报价单"
    :fields="fields"
    :api="quotationApi"
    redirect-path="/crm/quotation"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { quotationApi } from '@/api/erp'

const fields: BasicField[] = [
  { name: 'quotationNo', label: '报价单号', type: 'input', required: true, span: 12 },
  { name: 'customerName', label: '客户名称', type: 'input', required: true, span: 12 },
  { name: 'quotationDate', label: '报价日期', type: 'date', required: true, span: 12 },
  { name: 'validDate', label: '有效期至', type: 'date', span: 12 },
  { name: 'totalAmount', label: '报价金额', type: 'number', precision: 2, required: true, span: 12 },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '草稿', value: 0 },
    { label: '已发送', value: 1 },
    { label: '已接受', value: 2 },
    { label: '已拒绝', value: 3 },
  ], span: 12 },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
