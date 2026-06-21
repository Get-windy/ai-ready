<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '应收应付调整' }"
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
import { offsetApi } from '@/api/finance/index'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: {
    create: (data) => offsetApi.create(data),
    getById: (id) => offsetApi.getById(id),
  },
  redirectPath: '/finance/ar-ap-adjust',
  fields: [
    { key: 'offsetNo', label: '调整单号', type: 'input' },
    { key: 'partyName', label: '往来单位', type: 'input', required: true },
    {
      key: 'partyType',
      label: '单位类型',
      type: 'select',
      options: [
        { label: '客户', value: 'CUSTOMER' },
        { label: '供应商', value: 'SUPPLIER' },
      ],
    },
    { key: 'offsetDate', label: '调整日期', type: 'date', required: true },
    { key: 'offsetAmount', label: '调整金额', type: 'number', precision: 2, required: true },
    {
      key: 'direction',
      label: '调整方向',
      type: 'select',
      options: [
        { label: '应收→应付', value: 'RECEIVABLE_TO_PAYABLE' },
        { label: '应付→应收', value: 'PAYABLE_TO_RECEIVABLE' },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
