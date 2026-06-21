<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '会计凭证' }"
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
import { request } from '@/utils/request'

const api = {
  create: (data: any) => request.post('/finance/voucher', data),
  update: (id: number, data: any) => request.put(`/finance/voucher/${id}`, data),
  getById: (id: number) => request.get(`/finance/voucher/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api,
  redirectPath: '/finance/voucher',
  fields: [
    { key: 'voucherNo', label: '凭证号', type: 'input' },
    { key: 'voucherDate', label: '凭证日期', type: 'date', required: true },
    {
      key: 'voucherType',
      label: '凭证类型',
      type: 'select',
      options: [
        { label: '收款凭证', value: '收款凭证' },
        { label: '付款凭证', value: '付款凭证' },
        { label: '转账凭证', value: '转账凭证' },
      ],
    },
    { key: 'summary', label: '摘要', type: 'input', required: true },
    { key: 'debitAmount', label: '借方金额', type: 'number', precision: 2 },
    { key: 'creditAmount', label: '贷方金额', type: 'number', precision: 2 },
    {
      key: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '草稿', value: '草稿' },
        { label: '已审核', value: '已审核' },
        { label: '已过账', value: '已过账' },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
