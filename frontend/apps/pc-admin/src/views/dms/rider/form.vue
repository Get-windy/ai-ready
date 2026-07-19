<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '骑手' }"
    :basic-info-fields="fields"
    :show-bottom-panel="false"
  >
    <template #footer>
      <div class="footer-right">
        <a-button
          size="large"
          :loading="saving"
          @click="handleSave"
        >
          保存<span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button
          type="primary"
          size="large"
          :loading="saving"
          @click="handleSubmit"
        >
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

const riderApi = {
  create: (data: any) => request.post('/dms/rider', data),
  update: (id: number, data: any) => request.put(`/dms/rider/${id}`, data),
  getById: (id: number) => request.get(`/dms/rider/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: riderApi,
  redirectPath: '/dms/rider',
  fields: [
    { key: 'riderName', label: '骑手姓名', type: 'input', required: true },
    { key: 'phone', label: '联系电话', type: 'input', required: true },
    { key: 'idCard', label: '身份证号', type: 'input' },
    { key: 'emergencyContact', label: '紧急联系人', type: 'input' },
    { key: 'emergencyPhone', label: '紧急联系电话', type: 'input' },
    {
      key: 'vehicleType', label: '车辆类型', type: 'select',
      options: [
        { label: '电动车', value: '电动车' },
        { label: '摩托车', value: '摩托车' },
        { label: '汽车', value: '汽车' },
      ],
    },
    { key: 'entryDate', label: '入职日期', type: 'date' },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '在职', value: '在职' },
        { label: '离职', value: '离职' },
        { label: '试用期', value: '试用期' },
      ],
    },
    { key: 'address', label: '地址', type: 'textarea', width: 'wide' },
  ],
})
</script>
