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
        <template #bookQtyCell="{ record }">
          <span>{{ record.bookQuantity ?? '-' }}</span>
        </template>
        <template #diffCell="{ record }">
          <span :class="diffClass(record)">
            {{ ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)).toFixed(2) }}
          </span>
        </template>
      </BillDetailTable>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { checkApi } from '@/api/wms/check'

const {
  formData, loadingOptions, saving, optionRefs,
  handleAddProduct, handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft, handleSubmit, totalQuantity,
} = useBillForm({
  billPrefix: 'WC',
  api: {
    create: (data) => checkApi.save(data),
    update: (_id, data) => checkApi.update(data),
    getById: checkApi.getById,
  },
  redirectPath: '/wms/check',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: { bookQuantity: 0, actualQuantity: 0, locationCode: '' },
  transformPayload: (fd, status) => ({
    warehouseId: fd.warehouseId,
    checkType: fd.checkType,
    operatorName: fd.handlerName,
    remark: fd.remark,
    status,
    results: fd.products.map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      locationCode: p.locationCode,
      batchNo: p.batchNo,
      bookQty: p.bookQuantity || 0,
      actualQty: p.actualQuantity || 0,
      diffQty: (p.actualQuantity || 0) - (p.bookQuantity || 0),
      remark: p.remark,
    })),
  }),
})

if (!('warehouseId' in formData)) {Object.assign(formData, {
  warehouseId: undefined, handlerId: undefined, handlerName: '',
  date: '', checkType: 1, remark: '',
})}

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '盘点单', orderNo: formData.orderNo, showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '仓库', type: 'select', required: true, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '盘点人', type: 'select', required: true, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '盘点日期', type: 'date', required: true },
  { key: 'checkType', label: '盘点类型', type: 'select', options: [{ label: '全面盘点', value: 1 }, { label: '抽盘', value: 2 }] },
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
  { key: 'locationCode', title: '库位', type: 'input', width: 100 },
  { key: 'bookQuantity', title: '账面数量', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'actualQuantity', title: '实盘数量', type: 'number', width: 100, precision: 2 },
  { key: 'diffQuantity', title: '差异', type: 'slot', slotName: 'diffCell', width: 100 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
])

function diffClass(record: any) {
  const diff = (record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)
  return diff > 0 ? 'diff-positive' : diff < 0 ? 'diff-negative' : ''
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId') {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productName = p.name || ''
      record.productCode = p.code || ''
      record.itemCode = p.code || ''
      record.specification = p.specification || ''
      record.unit = p.unit || ''
      record.bookQuantity = p.stockQuantity || 0
      record.unitPrice = p.salePrice || p.price || 0
    }
  }
}
function handleAction(_k: string) {}
function handleFieldChange(k: string, v: any) { baseFieldChange(k, v) }

onMounted(() => {
  if (formData.products.length === 0) { for (let i = 0; i < 3; i++) handleAddProduct() }
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.diff-positive { color: #52c41a; font-weight: 600; }
.diff-negative { color: #f5222d; font-weight: 600; }
</style>
