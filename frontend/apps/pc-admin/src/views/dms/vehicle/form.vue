<template>
  <BasicFormPage
    title="车辆"
    :fields="fields"
    :api="vehicleApi"
    redirect-path="/dms/vehicle"
  />
</template>

<script setup lang="ts">
import BasicFormPage from '@/components/BasicFormPage/index.vue'
import type { BasicField } from '@/components/BasicFormPage/index.vue'
import request from '@/utils/request'

const vehicleApi = {
  create: (data: any) => request.post('/dms/vehicle', data),
  update: (id: number, data: any) => request.put(`/dms/vehicle/${id}`, data),
  getById: (id: number) => request.get(`/dms/vehicle/${id}`),
}

const fields: BasicField[] = [
  {
    name: 'plateNo',
    label: '车牌号',
    type: 'input',
    required: true,
  },
  {
    name: 'vehicleType',
    label: '车辆类型',
    type: 'select',
    options: [
      { label: '电动车', value: '电动车' },
      { label: '摩托车', value: '摩托车' },
      { label: '汽车', value: '汽车' },
      { label: '货车', value: '货车' },
    ],
  },
  {
    name: 'brand',
    label: '品牌',
    type: 'input',
  },
  {
    name: 'model',
    label: '型号',
    type: 'input',
  },
  {
    name: 'purchaseDate',
    label: '购买日期',
    type: 'date',
  },
  {
    name: 'insuranceExpiry',
    label: '保险到期日',
    type: 'date',
  },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '正常', value: '正常' },
      { label: '维修', value: '维修' },
      { label: '报废', value: '报废' },
    ],
  },
  {
    name: 'riderName',
    label: '骑手姓名',
    type: 'input',
  },
  {
    name: 'remark',
    label: '备注',
    type: 'textarea',
    span: 24,
  },
]
</script>
