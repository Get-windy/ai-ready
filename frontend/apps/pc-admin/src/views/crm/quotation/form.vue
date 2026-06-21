<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '报价单' }"
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
import { quotationApi } from '@/api/erp'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: quotationApi.create, update: quotationApi.update, getById: quotationApi.getById },
  redirectPath: '/crm/quotation',
  fields: [
    { key: 'quotationNo', label: '报价单号', type: 'input', required: true },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'quotationDate', label: '报价日期', type: 'date', required: true },
    { key: 'validDate', label: '有效期至', type: 'date' },
    { key: 'totalAmount', label: '报价金额', type: 'number', precision: 2, required: true },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '草稿', value: 0 },
        { label: '已发送', value: 1 },
        { label: '已接受', value: 2 },
        { label: '已拒绝', value: 3 },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
