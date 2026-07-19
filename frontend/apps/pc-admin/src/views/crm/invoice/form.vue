<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '发票' }"
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
import { invoiceApi } from '@/api/crm'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: invoiceApi.create, getById: invoiceApi.getById },
  redirectPath: '/crm/invoice',
  fields: [
    { key: 'invoiceNo', label: '发票号码', type: 'input' },
    {
      key: 'invoiceType', label: '发票类型', type: 'select',
      options: [
        { label: '增值税专用发票', value: '增值税专用发票' },
        { label: '增值税普通发票', value: '增值税普通发票' },
        { label: '电子发票', value: '电子发票' },
      ],
    },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'invoiceDate', label: '开票日期', type: 'date', required: true },
    { key: 'amount', label: '金额', type: 'number', precision: 2, required: true },
    { key: 'taxAmount', label: '税额', type: 'number', precision: 2 },
    { key: 'totalAmount', label: '价税合计', type: 'number', precision: 2 },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '待开票', value: 'pending' },
        { label: '已开票', value: 'issued' },
        { label: '已作废', value: 'voided' },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
