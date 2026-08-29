<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '合同' }"
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
import { contractApi } from '@/api/crm'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: contractApi.create, update: contractApi.update, getById: contractApi.getById },
  redirectPath: '/crm/contract',
  fields: [
    { key: 'contractNo', label: '合同编号', type: 'input', required: true },
    { key: 'contractName', label: '合同名称', type: 'input', required: true },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'opportunityName', label: '关联商机', type: 'input' },
    { key: 'contactName', label: '联系人', type: 'input' },
    {
      key: 'contractType', label: '合同类型', type: 'select',
      options: [
        { label: '销售合同', value: 1 },
        { label: '服务合同', value: 2 },
        { label: '框架协议', value: 3 },
      ],
    },
    { key: 'signDate', label: '签订日期', type: 'date' },
    { key: 'contractAmount', label: '合同金额', type: 'number', precision: 2, required: true },
    { key: 'paidAmount', label: '已收金额', type: 'number', precision: 2 },
    { key: 'pendingAmount', label: '待收金额', type: 'number', precision: 2, disabled: true },
    {
      key: 'currency', label: '币种', type: 'select',
      options: [
        { label: '人民币', value: 'CNY' },
        { label: '美元', value: 'USD' },
      ],
    },
    {
      key: 'paymentMethod', label: '结算方式', type: 'select',
      options: [
        { label: '一次性付款', value: 1 },
        { label: '分期付款', value: 2 },
        { label: '货到付款', value: 3 },
      ],
    },
    { key: 'paymentTerms', label: '付款条款', type: 'textarea', width: 'wide' },
    { key: 'deliveryTerms', label: '交付条款', type: 'textarea', width: 'wide' },
    { key: 'warrantyTerms', label: '质保条款', type: 'textarea', width: 'wide' },
    { key: 'serviceTerms', label: '服务条款', type: 'textarea', width: 'wide' },
    { key: 'startDate', label: '开始日期', type: 'date', required: true },
    { key: 'endDate', label: '结束日期', type: 'date', required: true },
    { key: 'salesPersonName', label: '负责人', type: 'input' },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '草稿', value: 0 },
        { label: '审批中', value: 1 },
        { label: '已签订', value: 2 },
        { label: '已终止', value: 3 },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
