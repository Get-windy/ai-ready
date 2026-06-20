<template>
  <BasicFormPage
    title="合同"
    :fields="fields"
    :api="contractApi"
    redirect-path="/crm/contract"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { contractApi } from '@/api/crm'

const fields: BasicField[] = [
  { name: 'contractNo', label: '合同编号', type: 'input', required: true, span: 12 },
  { name: 'contractName', label: '合同名称', type: 'input', required: true, span: 12 },
  { name: 'customerName', label: '客户名称', type: 'input', required: true, span: 12 },
  { name: 'contractType', label: '合同类型', type: 'select', options: [
    { label: '销售合同', value: 1 },
    { label: '服务合同', value: 2 },
    { label: '框架协议', value: 3 },
  ], span: 12 },
  { name: 'contractAmount', label: '合同金额', type: 'number', precision: 2, required: true, span: 12 },
  { name: 'startDate', label: '开始日期', type: 'date', required: true, span: 12 },
  { name: 'endDate', label: '结束日期', type: 'date', required: true, span: 12 },
  { name: 'salesPersonName', label: '负责人', type: 'input', span: 12 },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '草稿', value: 0 },
    { label: '审批中', value: 1 },
    { label: '已签订', value: 2 },
    { label: '已终止', value: 3 },
  ], span: 12 },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
