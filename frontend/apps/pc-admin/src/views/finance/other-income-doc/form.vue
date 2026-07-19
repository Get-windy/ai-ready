<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '其他收入单' }"
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
  create: (data: any) => request.post('/finance/other-income-doc', data),
  update: (id: number, data: any) => request.put(`/finance/other-income-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/other-income-doc/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api,
  redirectPath: '/finance/other-income-doc',
  fields: [
    { key: 'docNo', label: '单据编号', type: 'input', disabled: true },
    {
      key: 'incomeType', label: '收入类型', type: 'select', required: true,
      options: [
        { label: '利息收入', value: 'INTEREST' },
        { label: '租金收入', value: 'RENT' },
        { label: '罚款收入', value: 'PENALTY' },
        { label: '保险理赔', value: 'INSURANCE' },
        { label: '废品变卖', value: 'SCRAP' },
        { label: '其他', value: 'OTHER' },
      ],
    },
    { key: 'amount', label: '收入金额', type: 'number', precision: 2, required: true },
    { key: 'incomeDate', label: '收入日期', type: 'date', required: true },
    { key: 'partnerName', label: '付款单位', type: 'input' },
    {
      key: 'settlementMethod', label: '结算方式', type: 'select',
      options: [
        { label: '银行转账', value: 'BANK_TRANSFER' },
        { label: '支票', value: 'CHECK' },
        { label: '现金', value: 'CASH' },
        { label: '承兑汇票', value: 'ACCEPTANCE' },
      ],
    },
    { key: 'bankName', label: '开户银行', type: 'input' },
    { key: 'bankAccount', label: '银行账号', type: 'input' },
    { key: 'transactionNo', label: '交易流水号', type: 'input' },
    { key: 'source', label: '收入来源说明', type: 'input', width: 'wide' },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
