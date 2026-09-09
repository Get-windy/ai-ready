<template>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :show-bottom-panel="false"
    :footer="footerConfig"
    @action="handleAction"
    @field-change="handleFieldChange"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <template #detail-table>
      <BillDetailTable
        :columns="detailColumns"
        v-model:data-source="formData.products"
        :max-height="400"
        @cell-change="handleCellChange"
      >
        <template #actionCell="{ index }">
          <a-space :size="2">
            <a-button
              type="link"
              size="small"
              class="action-add-btn"
              @click="handleAddProduct"
            >
              <PlusCircleOutlined />
            </a-button>
            <a-button
              type="link"
              size="small"
              class="action-del-btn"
              @click="handleRemoveProduct(index)"
            >
              <MinusCircleOutlined />
            </a-button>
          </a-space>
        </template>
      </BillDetailTable>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { receiptApi } from '@/api/wms/receipt'

const {
  formData, loadingOptions, saving, optionRefs,
  handleAddProduct, handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft, handleSubmit, totalQuantity,
} = useBillForm({
  billPrefix: 'RC',
  api: {
    create: (data) => receiptApi.save(data),
    update: (_id, data) => receiptApi.update(data),
    getById: receiptApi.getById,
  },
  redirectPath: '/wms/receipt',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: { quantity: 0, batchNo: '', locationCode: '' },
  transformPayload: (fd, status) => ({
    warehouseId: fd.warehouseId,
    sourceType: fd.receiptType,
    sourceNo: fd.sourceOrderNo || '',
    operatorName: fd.handlerName,
    remark: fd.remark,
    status,
    details: fd.products.map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      expectedQty: p.quantity,
      locationCode: p.locationCode,
      batchNo: p.batchNo,
      productionDate: p.productionDate || null,
      expiryDate: p.expiryDate || null,
      remark: p.remark,
    })),
  }),
})

if (!('warehouseId' in formData)) {Object.assign(formData, {
  warehouseId: undefined, receiptType: 1,
  handlerId: undefined, handlerName: '',
  date: '', sourceOrderNo: '', remark: '',
})}

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '收货单', orderNo: formData.orderNo, showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '收货仓库', type: 'select', required: true, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '操作员', type: 'select', required: true, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '收货日期', type: 'date', required: true },
  { key: 'receiptType', label: '收货类型', type: 'select', options: [{ label: '采购收货', value: 1 }, { label: '调拨收货', value: 2 }, { label: '退货收货', value: 3 }] },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '关联订单号' },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单数量', amountValue: `${totalQuantity.value}`,
  draftBtnText: '保存草稿', draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交', primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'select', width: 200, options: optionRefs.products.map((p: any) => ({ value: p.id, label: p.name })) },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'locationCode', title: '库位', type: 'input', width: 100 },
  { key: 'batchNo', title: '批次号', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
])

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId') {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productName = p.name || ''
      record.productCode = p.code || ''
      record.itemCode = p.code || ''
      record.specification = p.specification || ''
      record.unit = p.unit || ''
      record.unitPrice = p.salePrice || p.price || 0
    }
  }
}
function handleAction(_k: string) {}
function handleFieldChange(k: string, v: any) { baseFieldChange(k, v) }

onMounted(() => {
  if (formData.products.length === 0) { for (let i = 0; i < 3; i++) handleAddProduct() }
  nextTick(() => { /* table height */ })
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
</style>
