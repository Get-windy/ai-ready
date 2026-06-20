<template>
  <BasicFormPage
    title="客户"
    :fields="fields"
    :api="customerApi"
    redirect-path="/crm/customer"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { customerApi } from '@/api/customer'

const fields: BasicField[] = [
  { name: 'name', label: '客户名称', type: 'input', required: true, span: 12 },
  { name: 'code', label: '客户编码', type: 'input', required: true, span: 12 },
  { name: 'contactPerson', label: '联系人', type: 'input', span: 12 },
  { name: 'phone', label: '联系电话', type: 'input', span: 12 },
  { name: 'email', label: '邮箱', type: 'input', span: 12 },
  { name: 'industry', label: '行业', type: 'select', options: [
    { label: '制造业', value: '制造业' },
    { label: '批发零售', value: '批发零售' },
    { label: '服务业', value: '服务业' },
    { label: '其他', value: '其他' },
  ], span: 12 },
  { name: 'level', label: '客户等级', type: 'select', options: [
    { label: 'VIP', value: 1 },
    { label: 'A级', value: 2 },
    { label: 'B级', value: 3 },
    { label: 'C级', value: 4 },
  ], span: 12 },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '正常', value: 1 },
    { label: '停用', value: 0 },
  ], span: 12 },
  { name: 'address', label: '地址', type: 'input', span: 24 },
  { name: 'description', label: '备注', type: 'textarea', span: 24 },
]
</script>
