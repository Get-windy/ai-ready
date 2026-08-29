<template>
  <BillFormPage
    v-model="formData"
    :header="{ title: '客户' }"
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
import { customerApi } from '@/api/customer'

const { fields, formData, saving, handleSave, handleSubmit } = useBasicForm({
  api: { create: customerApi.create, update: customerApi.update, getById: customerApi.getById },
  redirectPath: '/crm/customer',
  fields: [
    { key: 'customerCode', label: '客户编码', type: 'input', required: true },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'shortName', label: '客户简称', type: 'input' },
    {
      key: 'customerType', label: '客户类型', type: 'select',
      options: [
        { label: '经销商', value: 1 },
        { label: '零售商', value: 2 },
        { label: '餐饮店', value: 3 },
        { label: '其他', value: 4 },
      ],
    },
    {
      key: 'customerSource', label: '客户来源', type: 'select',
      options: [
        { label: '自主开发', value: 1 },
        { label: '转介绍', value: 2 },
        { label: '网络推广', value: 3 },
        { label: '展会', value: 4 },
        { label: '其他', value: 5 },
      ],
    },
    {
      key: 'industryType', label: '所属行业', type: 'select',
      options: [
        { label: '食品加工', value: 1 },
        { label: '餐饮服务', value: 2 },
        { label: '批发零售', value: 3 },
        { label: '其他', value: 4 },
      ],
    },
    { key: 'province', label: '省份', type: 'input' },
    { key: 'city', label: '城市', type: 'input' },
    { key: 'district', label: '区县', type: 'input' },
    { key: 'address', label: '详细地址', type: 'input', width: 'wide' },
    { key: 'phone', label: '联系电话', type: 'input' },
    { key: 'fax', label: '传真', type: 'input' },
    { key: 'email', label: '邮箱', type: 'input' },
    { key: 'website', label: '网址', type: 'input' },
    { key: 'legalPerson', label: '法人代表', type: 'input' },
    { key: 'businessContact', label: '业务联系人', type: 'input' },
    { key: 'businessContactPhone', label: '业务联系人电话', type: 'input' },
    { key: 'financeContact', label: '财务联系人', type: 'input' },
    { key: 'financeContactPhone', label: '财务联系人电话', type: 'input' },
    { key: 'taxNumber', label: '税号', type: 'input' },
    { key: 'bankName', label: '开户行', type: 'input' },
    { key: 'bankAccount', label: '银行账号', type: 'input' },
    {
      key: 'customerLevel', label: '客户等级', type: 'select',
      options: [
        { label: 'VIP', value: 1 },
        { label: 'A级', value: 2 },
        { label: 'B级', value: 3 },
        { label: 'C级', value: 4 },
      ],
    },
    { key: 'creditLimit', label: '信用额度', type: 'number' },
    { key: 'currentDebt', label: '当前欠款', type: 'number', disabled: true },
    {
      key: 'settlementType', label: '结算方式', type: 'select',
      options: [
        { label: '现结', value: 1 },
        { label: '月结', value: 2 },
        { label: '货到付款', value: 3 },
        { label: '预付款', value: 4 },
      ],
    },
    { key: 'settlementDays', label: '结算账期(天)', type: 'number' },
    {
      key: 'status', label: '状态', type: 'select',
      options: [
        { label: '正常', value: 1 },
        { label: '停用', value: 0 },
      ],
    },
  ],
})
</script>
