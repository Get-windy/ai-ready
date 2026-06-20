<template>
  <BasicFormPage
    title="会计凭证"
    :fields="fields"
    :api="api"
    redirect-path="/finance/voucher"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { request } from '@/utils/request'

const fields: BasicField[] = [
  { name: 'voucherNo', label: '凭证号', type: 'input' },
  { name: 'voucherDate', label: '凭证日期', type: 'date', required: true },
  {
    name: 'voucherType',
    label: '凭证类型',
    type: 'select',
    options: [
      { label: '收款凭证', value: '收款凭证' },
      { label: '付款凭证', value: '付款凭证' },
      { label: '转账凭证', value: '转账凭证' },
    ],
  },
  { name: 'summary', label: '摘要', type: 'input', required: true },
  { name: 'debitAmount', label: '借方金额', type: 'number', precision: 2 },
  { name: 'creditAmount', label: '贷方金额', type: 'number', precision: 2 },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '草稿', value: '草稿' },
      { label: '已审核', value: '已审核' },
      { label: '已过账', value: '已过账' },
    ],
  },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]

const api = {
  create: (data: any) => request.post('/finance/voucher', data),
  update: (id: number, data: any) => request.put(`/finance/voucher/${id}`, data),
  getById: (id: number) => request.get(`/finance/voucher/${id}`),
}
</script>
