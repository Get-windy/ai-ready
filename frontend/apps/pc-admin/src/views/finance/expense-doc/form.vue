<template>
  <BasicFormPage
    title="费用单"
    :fields="fields"
    :api="api"
    redirect-path="/finance/expense-doc"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { request } from '@/utils/request'

const fields: BasicField[] = [
  { name: 'expenseNo', label: '费用单号', type: 'input' },
  {
    name: 'department',
    label: '部门',
    type: 'select',
    options: [
      { label: '销售部', value: '销售部' },
      { label: '采购部', value: '采购部' },
      { label: '仓储部', value: '仓储部' },
      { label: '财务部', value: '财务部' },
      { label: '行政部', value: '行政部' },
    ],
  },
  { name: 'expenseDate', label: '费用日期', type: 'date', required: true },
  { name: 'amount', label: '费用金额', type: 'number', precision: 2, required: true },
  {
    name: 'expenseType',
    label: '费用类型',
    type: 'select',
    options: [
      { label: '办公费', value: '办公费' },
      { label: '差旅费', value: '差旅费' },
      { label: '运输费', value: '运输费' },
      { label: '仓储费', value: '仓储费' },
      { label: '其他', value: '其他' },
    ],
  },
  { name: 'applicantName', label: '申请人', type: 'input', required: true },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '待审批', value: '待审批' },
      { label: '已审批', value: '已审批' },
      { label: '已报销', value: '已报销' },
    ],
  },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]

const api = {
  create: (data: any) => request.post('/finance/expense-doc', data),
  update: (id: number, data: any) => request.put(`/finance/expense-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/expense-doc/${id}`),
}
</script>
