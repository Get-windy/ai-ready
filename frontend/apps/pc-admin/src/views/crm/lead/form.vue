<template>
  <BasicFormPage
    title="线索"
    :fields="fields"
    :api="leadApi"
    redirect-path="/crm/lead"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import { leadApi } from '@/api/crm'

const fields: BasicField[] = [
  { name: 'name', label: '线索名称', type: 'input', required: true, span: 12 },
  { name: 'companyName', label: '公司名称', type: 'input', required: true, span: 12 },
  { name: 'contactName', label: '联系人', type: 'input', span: 12 },
  { name: 'phone', label: '联系电话', type: 'input', span: 12 },
  { name: 'email', label: '邮箱', type: 'input', span: 12 },
  { name: 'source', label: '来源', type: 'select', options: [
    { label: '网络推广', value: '网络推广' },
    { label: '客户介绍', value: '客户介绍' },
    { label: '电话营销', value: '电话营销' },
    { label: '展会', value: '展会' },
    { label: '其他', value: '其他' },
  ], span: 12 },
  { name: 'leadLevel', label: '线索等级', type: 'select', options: [
    { label: '高', value: 1 },
    { label: '中', value: 2 },
    { label: '低', value: 3 },
  ], span: 12 },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '新建', value: 0 },
    { label: '跟进中', value: 1 },
    { label: '已转化', value: 2 },
    { label: '已关闭', value: 3 },
  ], span: 12 },
  { name: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
