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
    { key: 'name', label: '线索名称', type: 'input', required: true },
    { key: 'companyName', label: '公司名称', type: 'input', required: true },
    { key: 'contactName', label: '联系人', type: 'input' },
    { key: 'phone', label: '联系电话', type: 'input' },
    { key: 'email', label: '邮箱', type: 'input' },
    {
      key: 'source', label: '来源', type: 'select',
      options: [
        { label: '网络推广', value: '网络推广' },
        { label: '客户介绍', value: '客户介绍' },
        { label: '电话营销', value: '电话营销' },
        { label: '展会', value: '展会' },
        { label: '其他', value: '其他' },
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
    {
      key: 'status', label: '状态', type: 'select',
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
