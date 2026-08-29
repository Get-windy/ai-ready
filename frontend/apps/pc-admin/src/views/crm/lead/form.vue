<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '线索' }"
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
import { leadApi } from '@/api/crm'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: leadApi.create, update: leadApi.update, getById: leadApi.getById },
  redirectPath: '/crm/lead',
  fields: [
    { key: 'leadCode', label: '线索编号', type: 'input', disabled: true },
    { key: 'leadName', label: '线索名称', type: 'input', required: true },
    { key: 'companyName', label: '公司名称', type: 'input', required: true },
    { key: 'contactName', label: '联系人', type: 'input' },
    { key: 'contactPhone', label: '联系电话', type: 'input' },
    { key: 'contactEmail', label: '联系邮箱', type: 'input' },
    {
      key: 'industryType', label: '所属行业', type: 'select',
      options: [
        { label: '食品加工', value: 1 },
        { label: '餐饮服务', value: 2 },
        { label: '批发零售', value: 3 },
        { label: '其他', value: 4 },
      ],
    },
    {
      key: 'leadSource', label: '线索来源', type: 'select',
      options: [
        { label: '网络推广', value: 1 },
        { label: '客户介绍', value: 2 },
        { label: '电话营销', value: 3 },
        { label: '展会', value: 4 },
        { label: '其他', value: 5 },
      ],
    },
    {
      key: 'leadLevel', label: '线索等级', type: 'select',
      options: [
        { label: '高', value: 1 },
        { label: '中', value: 2 },
        { label: '低', value: 3 },
      ],
    },
    { key: 'estimatedAmount', label: '预计金额', type: 'number' },
    { key: 'province', label: '省份', type: 'input' },
    { key: 'city', label: '城市', type: 'input' },
    { key: 'address', label: '详细地址', type: 'input', width: 'wide' },
    { key: 'requirement', label: '需求描述', type: 'textarea', width: 'wide' },
    { key: 'salesPersonName', label: '负责销售', type: 'input' },
    {
      key: 'leadStatus', label: '状态', type: 'select',
      options: [
        { label: '新建', value: 0 },
        { label: '跟进中', value: 1 },
        { label: '已转化', value: 2 },
        { label: '已关闭', value: 3 },
      ],
    },
    { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
