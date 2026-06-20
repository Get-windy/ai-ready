<template>
  <BasicFormPage
    title="收款单"
    :fields="fields"
    :api="api"
    redirect-path="/finance/receipt-doc"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { request } from '@/utils/request'

const fields: BasicField[] = [
  { name: 'receiptNo', label: '收款单号', type: 'input' },
  { name: 'customerName', label: '客户名称', type: 'input', required: true },
  { name: 'receiptDate', label: '收款日期', type: 'date', required: true },
  { name: 'amount', label: '收款金额', type: 'number', precision: 2, required: true },
  {
    name: 'receiptMethod',
    label: '收款方式',
    type: 'select',
    options: [
      { label: '银行转账', value: '银行转账' },
      { label: '支票', value: '支票' },
      { label: '现金', value: '现金' },
      { label: '电汇', value: '电汇' },
    ],
  },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '待确认', value: '待确认' },
      { label: '已确认', value: '已确认' },
      { label: '已入账', value: '已入账' },
    ],
  },
]

const api = {
  create: (data: any) => request.post('/finance/receipt-doc', data),
  update: (id: number, data: any) => request.put(`/finance/receipt-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/receipt-doc/${id}`),
}
</script>
