<template>
  <BasicFormPage
    title="骑手"
    :fields="fields"
    :api="riderApi"
    redirect-path="/dms/rider"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import request from '@/utils/request'

const riderApi = {
  create: (data: any) => request.post('/dms/rider', data),
  update: (id: number, data: any) => request.put(`/dms/rider/${id}`, data),
  getById: (id: number) => request.get(`/dms/rider/${id}`),
}

const fields: BasicField[] = [
  {
    name: 'riderName',
    label: '骑手姓名',
    type: 'input',
    required: true,
  },
  {
    name: 'phone',
    label: '联系电话',
    type: 'input',
    required: true,
  },
  {
    name: 'idCard',
    label: '身份证号',
    type: 'input',
  },
  {
    name: 'emergencyContact',
    label: '紧急联系人',
    type: 'input',
  },
  {
    name: 'emergencyPhone',
    label: '紧急联系电话',
    type: 'input',
  },
  {
    name: 'vehicleType',
    label: '车辆类型',
    type: 'select',
    options: [
      { label: '电动车', value: '电动车' },
      { label: '摩托车', value: '摩托车' },
      { label: '汽车', value: '汽车' },
    ],
  },
  {
    name: 'entryDate',
    label: '入职日期',
    type: 'date',
  },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '在职', value: '在职' },
      { label: '离职', value: '离职' },
      { label: '试用期', value: '试用期' },
    ],
  },
  {
    name: 'address',
    label: '地址',
    type: 'textarea',
    span: 24,
  },
]
</script>
