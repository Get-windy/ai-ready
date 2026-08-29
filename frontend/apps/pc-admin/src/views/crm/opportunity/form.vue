<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '商机' }"
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
import { opportunityApi } from '@/api/crm'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: opportunityApi.create, update: opportunityApi.update, getById: opportunityApi.getById },
  redirectPath: '/crm/opportunity',
  fields: [
    { key: 'opportunityCode', label: '商机编号', type: 'input', disabled: true },
    { key: 'opportunityName', label: '商机名称', type: 'input', required: true },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'leadId', label: '来源线索ID', type: 'input' },
    {
      key: 'opportunityStage', label: '阶段', type: 'select',
      options: [
        { label: '初步接触', value: 1 },
        { label: '需求确认', value: 2 },
        { label: '方案报价', value: 3 },
        { label: '商务谈判', value: 4 },
        { label: '赢单', value: 5 },
        { label: '输单', value: 6 },
      ],
    },
    { key: 'estimatedAmount', label: '预计金额', type: 'number', precision: 2 },
    { key: 'actualAmount', label: '实际金额', type: 'number', precision: 2 },
    { key: 'probability', label: '赢单概率(%)', type: 'number', precision: 0, min: 0, max: 100 },
    {
      key: 'opportunityType', label: '商机类型', type: 'select',
      options: [
        { label: '新客户', value: 1 },
        { label: '老客户增购', value: 2 },
        { label: '续约', value: 3 },
      ],
    },
    {
      key: 'opportunitySource', label: '商机来源', type: 'select',
      options: [
        { label: '线索转化', value: 1 },
        { label: '客户主动', value: 2 },
        { label: '销售开发', value: 3 },
      ],
    },
    { key: 'productInterest', label: '意向产品', type: 'input', width: 'wide' },
    { key: 'requirement', label: '需求描述', type: 'textarea', width: 'wide' },
    { key: 'competitor', label: '竞争对手', type: 'input' },
    { key: 'winReason', label: '赢单原因', type: 'textarea', width: 'wide' },
    { key: 'loseReason', label: '输单原因', type: 'textarea', width: 'wide' },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '进行中', value: 1 },
        { label: '已赢单', value: 2 },
        { label: '已输单', value: 3 },
        { label: '已关闭', value: 4 },
      ],
    },
  ],
})
</script>
