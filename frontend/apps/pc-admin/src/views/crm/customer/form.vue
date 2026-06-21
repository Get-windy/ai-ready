<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '客户' }"
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
import { customerApi } from '@/api/customer'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: customerApi.create, update: customerApi.update, getById: customerApi.getById },
  redirectPath: '/crm/customer',
  fields: [
    { key: 'name', label: '客户名称', type: 'input', required: true },
    { key: 'code', label: '客户编码', type: 'input', required: true },
    { key: 'contactPerson', label: '联系人', type: 'input' },
    { key: 'phone', label: '联系电话', type: 'input' },
    { key: 'email', label: '邮箱', type: 'input' },
    {
      key: 'industry', label: '行业', type: 'select',
      options: [
        { label: '制造业', value: '制造业' },
        { label: '批发零售', value: '批发零售' },
        { label: '服务业', value: '服务业' },
        { label: '其他', value: '其他' },
      ],
    },
    {
      key: 'level', label: '客户等级', type: 'select',
      options: [
        { label: 'VIP', value: 1 },
        { label: 'A级', value: 2 },
        { label: 'B级', value: 3 },
        { label: 'C级', value: 4 },
      ],
    },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '正常', value: 1 },
        { label: '停用', value: 0 },
      ],
    },
    { key: 'address', label: '地址', type: 'input', width: 'wide' },
    { key: 'description', label: '备注', type: 'textarea', width: 'wide' },
  ],
})
</script>
