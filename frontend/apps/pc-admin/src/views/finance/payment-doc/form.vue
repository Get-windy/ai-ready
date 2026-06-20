<template>
  <BasicFormPage
    title="付款单"
    :fields="fields"
    :api="api"
    redirect-path="/finance/payment-doc"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { request } from '@/utils/request'

const fields: BasicField[] = [
  { name: 'paymentNo', label: '付款单号', type: 'input' },
  { name: 'supplierName', label: '供应商名称', type: 'input', required: true },
  { name: 'paymentDate', label: '付款日期', type: 'date', required: true },
  { name: 'amount', label: '付款金额', type: 'number', precision: 2, required: true },
  {
    name: 'paymentMethod',
    label: '付款方式',
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
      { label: '待审批', value: '待审批' },
      { label: '已审批', value: '已审批' },
      { label: '已付款', value: '已付款' },
    ],
  },
]

const api = {
  create: (data: any) => request.post('/finance/payment-doc', data),
  update: (id: number, data: any) => request.put(`/finance/payment-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/payment-doc/${id}`),
}
</script>
