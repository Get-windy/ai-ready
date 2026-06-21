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
        <span class="section-title">商品明细</span>
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
        <template #productCell="{ record, index }">
          <a-select
            v-if="!isViewMode"
            v-model:value="record.productId"
            placeholder="请选择商品"
            show-search
            :filter-option="filterOption"
            style="width:100%"
            :loading="loadingOptions"
            size="small"
            @change="(val: number) => handleProductChange(val, index)"
          >
            <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">
              {{ p.name }}
            </a-select-option>
          </a-select>
          <span v-else>{{ record.productName }}</span>
        </template>
        <template #quantityCell="{ record }">
          <a-input-number v-if="!isViewMode" v-model:value="record.quantity" :min="0" :precision="2" style="width:100%" size="small" />
          <span v-else>{{ record.quantity }}</span>
        </template>
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
const wmsPickApi = {
  create(data: any) { return request.post('/wms/pick', data) },
  update(id: number, data: any) { return request.put(`/wms/pick/${id}`, data) },
  getById(id: number) { return request.get(`/wms/pick/${id}`) },
}

const {
  formData, loadingOptions, saving, optionRefs, filterOption, effectiveMode,
  handleAddProduct, handleRemoveProduct, handleProductChange,
  handleSaveDraft, handleSubmit, totalQuantity, totalWithTaxFormatted,
} = useBillForm({
  billPrefix: 'PK',
  api: wmsPickApi,
  redirectPath: '/wms/pick',
  fields: [
    { key: 'warehouseId', label: '仓库', type: 'select', required: true, optionsRef: 'warehouses' },
    { key: 'handlerId', label: '拣货员', type: 'select', required: true, optionsRef: 'users' },
    { key: 'date', label: '拣货日期', type: 'date', required: true },
    { key: 'pickType', label: '拣货方式', type: 'select' },
    { key: 'sourceOrderNo', label: '来源单号', type: 'input' },
  ],
})

const isViewMode = computed(() => effectiveMode.value === 'view')

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '拣货单',
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
    key: 'handlerId', label: '拣货员', type: 'select', required: true,
    options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  { key: 'date', label: '拣货日期', type: 'date', required: true },
  {
    key: 'pickType', label: '拣货方式', type: 'select',
    options: [
      { label: '按单拣货', value: 1 },
      { label: '批量拣货', value: 2 },
    ],
  },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '出库单号' },
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
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'locationCode', title: '拣货库位', width: 120 },
  { field: 'batchNo', title: '批次号', width: 120 },
  { field: 'remark', title: '备注', width: 150 },
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
