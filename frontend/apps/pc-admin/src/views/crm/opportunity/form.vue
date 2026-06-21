<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '商机' }"
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
import { opportunityApi } from '@/api/crm'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: opportunityApi.create, update: opportunityApi.update, getById: opportunityApi.getById },
  redirectPath: '/crm/opportunity',
  fields: [
    { key: 'name', label: '商机名称', type: 'input', required: true },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    {
      key: 'stage', label: '阶段', type: 'select',
      options: [
        { label: '初步接触', value: 1 },
        { label: '需求确认', value: 2 },
        { label: '方案报价', value: 3 },
        { label: '商务谈判', value: 4 },
        { label: '赢单', value: 5 },
        { label: '输单', value: 6 },
      ],
    },
    { key: 'expectedAmount', label: '预计金额', type: 'number', precision: 2 },
    { key: 'winProbability', label: '赢单概率(%)', type: 'number', precision: 0, min: 0, max: 100 },
    {
      key: 'priority', label: '优先级', type: 'select',
      options: [
        { label: '高', value: 'high' },
        { label: '中', value: 'medium' },
        { label: '低', value: 'low' },
      ],
    },
    { key: 'expectedCloseDate', label: '预计成交日期', type: 'date' },
    { key: 'ownerName', label: '负责人', type: 'input' },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
