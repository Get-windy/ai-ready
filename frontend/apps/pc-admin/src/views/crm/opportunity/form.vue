<template>
  <BasicFormPage
    title="商机"
    :fields="fields"
    :api="opportunityApi"
    redirect-path="/crm/opportunity"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { opportunityApi } from '@/api/crm'

const fields: BasicField[] = [
  { name: 'name', label: '商机名称', type: 'input', required: true, span: 12 },
  { name: 'customerName', label: '客户名称', type: 'input', required: true, span: 12 },
  { name: 'stage', label: '阶段', type: 'select', options: [
    { label: '初步接触', value: 1 },
    { label: '需求确认', value: 2 },
    { label: '方案报价', value: 3 },
    { label: '商务谈判', value: 4 },
    { label: '赢单', value: 5 },
    { label: '输单', value: 6 },
  ], span: 12 },
  { name: 'expectedAmount', label: '预计金额', type: 'number', precision: 2, span: 12 },
  { name: 'winProbability', label: '赢单概率(%)', type: 'number', precision: 0, min: 0, max: 100, span: 12 },
  { name: 'priority', label: '优先级', type: 'select', options: [
    { label: '高', value: 'high' },
    { label: '中', value: 'medium' },
    { label: '低', value: 'low' },
  ], span: 12 },
  { name: 'expectedCloseDate', label: '预计成交日期', type: 'date', span: 12 },
  { name: 'ownerName', label: '负责人', type: 'input', span: 12 },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
