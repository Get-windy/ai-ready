<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '预付款单' }"
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
import { prePaymentApi } from '@/api/finance/index'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: {
    create: (data) => prePaymentApi.create(data),
    getById: (id) => prePaymentApi.getById(id),
  },
  redirectPath: '/finance/advance-payment',
  fields: [
    { key: 'prePaymentNo', label: '预付款单号', type: 'input' },
    { key: 'supplierName', label: '供应商名称', type: 'input', required: true },
    { key: 'paymentDate', label: '预付日期', type: 'date', required: true },
    { key: 'amount', label: '预付金额', type: 'number', precision: 2, required: true },
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
        { label: '待核销', value: '待核销' },
        { label: '已核销', value: '已核销' },
        { label: '已退回', value: '已退回' },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
