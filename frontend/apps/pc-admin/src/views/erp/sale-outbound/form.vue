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
        <template #unitPriceCell="{ record }">
          <a-input-number v-if="!isViewMode" v-model:value="record.unitPrice" :min="0" :precision="2" style="width:100%" size="small" />
          <span v-else>{{ record.unitPrice?.toFixed(2) }}</span>
        </template>
        <template #amountCell="{ record }">
          <span class="amount-text">{{ ((record.quantity || 0) * (record.unitPrice || 0)).toFixed(2) }}</span>
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
import { outboundApi } from '@/api/erp'

const {
  formData, loadingOptions, saving, optionRefs, filterOption, effectiveMode,
  handleAddProduct, handleRemoveProduct, handleProductChange,
  handleSaveDraft, handleSubmit, totalQuantity, totalAmount, totalWithTaxFormatted,
} = useBillForm({
  billPrefix: 'XSCK',
  api: outboundApi,
  redirectPath: '/erp/sale-outbound',
  fields: [
    { key: 'customerId', label: '客户', type: 'select', required: true },
    { key: 'warehouseId', label: '出库仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '出库日期', type: 'date', required: true },
    { key: 'outboundType', label: '出库类型', type: 'select' },
    { key: 'sourceOrderNo', label: '来源单号', type: 'input' },
  ],
})

const isViewMode = computed(() => effectiveMode.value === 'view')

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售出库单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'customerId', label: '客户', type: 'select', required: true,
    options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '出库仓库', type: 'select', required: true,
    options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true,
    options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '出库日期', type: 'date', required: true },
  { key: 'outboundType', label: '出库类型', type: 'select',
    options: [{ label: '销售出库', value: 1 }, { label: '调拨出库', value: 2 }, { label: '其他出库', value: 3 }] },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '关联销售订单号' },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: totalWithTaxFormatted.value,
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
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'remark', title: '备注', width: 150 },
  { field: 'action', title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]

function handleAction(_key: string) {}
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
.amount-text {
  font-weight: 600;
  color: #262626;
}
</style>
