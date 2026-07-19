<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '收款单' }"
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
import { request } from '@/utils/request'

const api = {
  create: (data: any) => request.post('/finance/receipt-doc', data),
  update: (id: number, data: any) => request.put(`/finance/receipt-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/receipt-doc/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api,
  redirectPath: '/finance/receipt-doc',
  fields: [
    { key: 'receiptNo', label: '收款单号', type: 'input' },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'receiptDate', label: '收款日期', type: 'date', required: true },
    { key: 'amount', label: '收款金额', type: 'number', precision: 2, required: true },
    {
      key: 'receiptMethod',
      label: '收款方式',
      type: 'select',
      options: [
        { label: '银行转账', value: '银行转账' },
        { label: '支票', value: '支票' },
        { label: '现金', value: '现金' },
        { label: '电汇', value: '电汇' },
      ],
    },
    {
      key: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '待确认', value: '待确认' },
        { label: '已确认', value: '已确认' },
        { label: '已入账', value: '已入账' },
      ],
    },
  ],
})
</script>
