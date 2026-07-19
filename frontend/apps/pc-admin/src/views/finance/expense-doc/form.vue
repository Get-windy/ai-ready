<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '费用单' }"
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
  create: (data: any) => request.post('/finance/expense-doc', data),
  update: (id: number, data: any) => request.put(`/finance/expense-doc/${id}`, data),
  getById: (id: number) => request.get(`/finance/expense-doc/${id}`),
}

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api,
  redirectPath: '/finance/expense-doc',
  fields: [
    { key: 'expenseNo', label: '费用单号', type: 'input' },
    {
      key: 'department',
      label: '部门',
      type: 'select',
      options: [
        { label: '销售部', value: '销售部' },
        { label: '采购部', value: '采购部' },
        { label: '仓储部', value: '仓储部' },
        { label: '财务部', value: '财务部' },
        { label: '行政部', value: '行政部' },
      ],
    },
    { key: 'expenseDate', label: '费用日期', type: 'date', required: true },
    { key: 'amount', label: '费用金额', type: 'number', precision: 2, required: true },
    {
      key: 'expenseType',
      label: '费用类型',
      type: 'select',
      options: [
        { label: '办公费', value: '办公费' },
        { label: '差旅费', value: '差旅费' },
        { label: '运输费', value: '运输费' },
        { label: '仓储费', value: '仓储费' },
        { label: '其他', value: '其他' },
      ],
    },
    { key: 'applicantName', label: '申请人', type: 'input', required: true },
    {
      key: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '待审批', value: '待审批' },
        { label: '已审批', value: '已审批' },
        { label: '已报销', value: '已报销' },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
