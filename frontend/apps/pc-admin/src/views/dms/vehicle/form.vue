<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '车辆' }"
    :basic-info-fields="fields"
    :show-bottom-panel="false"
  >
    <template #footer>
      <div class="footer-right">
        <a-button size="large" :loading="saving" @click="handleSave">
          保存<span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button type="primary" size="large" :loading="saving" @click="handleSubmit">
          提交<span class="shortcut-hint">Ctrl+Enter</span>
        </a-button>
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import BillFormPage from '@/components/BillFormPage/index.vue'
import { useBasicForm } from '@/components/BillFormPage/useBasicForm'
import request from '@/utils/request'

const vehicleApi = {
  create: (data: any) => request.post('/dms/vehicle', data),
  update: (id: number, data: any) => request.put(`/dms/vehicle/${id}`, data),
  getById: (id: number) => request.get(`/dms/vehicle/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: vehicleApi,
  redirectPath: '/dms/vehicle',
  fields: [
    { key: 'plateNo', label: '车牌号', type: 'input', required: true },
    {
      key: 'vehicleType', label: '车辆类型', type: 'select',
      options: [
        { label: '电动车', value: '电动车' },
        { label: '摩托车', value: '摩托车' },
        { label: '汽车', value: '汽车' },
        { label: '货车', value: '货车' },
      ],
    },
    { key: 'brand', label: '品牌', type: 'input' },
    { key: 'model', label: '型号', type: 'input' },
    { key: 'purchaseDate', label: '购买日期', type: 'date' },
    { key: 'insuranceExpiry', label: '保险到期日', type: 'date' },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '正常', value: '正常' },
        { label: '维修', value: '维修' },
        { label: '报废', value: '报废' },
      ],
    },
    { key: 'riderName', label: '骑手姓名', type: 'input' },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
