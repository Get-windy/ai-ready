<template>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :show-bottom-panel="false"
    :footer="footerConfig"
    @action="handleAction"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <template #detail-table>
      <div class="table-toolbar">
        <span class="section-title">订单明细</span>
        <a-button v-if="!isViewMode" type="primary" size="small" @click="handleAddProduct">
          <template #icon><PlusOutlined /></template>
          添加
        </a-button>
      </div>
      <VxeTableList
        :columns="tableColumns"
        :data-source="formData.products"
        :pagination="false as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #action="{ index }">
          <a-button v-if="!isViewMode" type="link" danger size="small" @click="handleRemoveProduct(index)">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </template>
      </VxeTableList>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import type { BillHeaderConfig, BasicInfoField, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'

// API
const wmsWaveApi = {
  create(data: any) { return request.post('/wms/wave', data) },
  update(id: number, data: any) { return request.put(`/wms/wave/${id}`, data) },
  getById(id: number) { return request.get(`/wms/wave/${id}`) },
}

const {
  formData, loadingOptions, saving, optionRefs, filterOption, effectiveMode,
  handleAddProduct, handleRemoveProduct, handleProductChange,
  handleSaveDraft, handleSubmit, totalQuantity, totalWithTaxFormatted,
} = useBillForm({
  billPrefix: 'WV',
  api: wmsWaveApi,
  redirectPath: '/wms/wave',
  fields: [
    { key: 'warehouseId', label: '仓库', type: 'select', required: true, optionsRef: 'warehouses' },
    { key: 'handlerId', label: '操作员', type: 'select', required: true, optionsRef: 'users' },
    { key: 'date', label: '波次日期', type: 'date', required: true },
    { key: 'waveType', label: '波次类型', type: 'select' },
  ],
})

const isViewMode = computed(() => effectiveMode.value === 'view')

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '波次单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  {
    key: 'warehouseId', label: '仓库', type: 'select', required: true,
    options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  {
    key: 'handlerId', label: '操作员', type: 'select', required: true,
    options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  { key: 'date', label: '波次日期', type: 'date', required: true },
  {
    key: 'waveType', label: '波次类型', type: 'select',
    options: [
      { label: '普通波次', value: 1 },
      { label: '紧急波次', value: 2 },
    ],
  },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单数量',
  amountValue: `${totalQuantity.value}`,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

const tableColumns = [
  { field: 'orderNo', title: '订单号', width: 150 },
  { field: 'customerName', title: '客户', width: 150 },
  { field: 'productCount', title: '商品数', width: 100 },
  { field: 'totalQuantity', title: '总数量', width: 100 },
  { field: 'remark', title: '备注', width: 200 },
  { field: 'action', title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]

function handleAction(key: string) {
  // handle actions
}
</script>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
</style>
