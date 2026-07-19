<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '付款单' }"
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
  create: (data: any) => request.post('/finance/payment-doc', data),
  update: (id: number, data: any) => request.put(`/finance/payment-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/payment-doc/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api,
  redirectPath: '/finance/payment-doc',
  fields: [
    { key: 'paymentNo', label: '付款单号', type: 'input' },
    { key: 'supplierName', label: '供应商名称', type: 'input', required: true },
    { key: 'paymentDate', label: '付款日期', type: 'date', required: true },
    { key: 'amount', label: '付款金额', type: 'number', precision: 2, required: true },
    {
      key: 'paymentMethod',
      label: '付款方式',
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
        { label: '待审批', value: '待审批' },
        { label: '已审批', value: '已审批' },
        { label: '已付款', value: '已付款' },
      ],
    },
  ],
})
</script>
