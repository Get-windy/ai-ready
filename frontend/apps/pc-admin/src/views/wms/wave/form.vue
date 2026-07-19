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
        :data-source="formData.products"
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
import { pickApi } from '@/api/wms/pick'

const {
  formData, loadingOptions, saving, optionRefs, filterOption,
  handleAddProduct, handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft, handleSubmit, totalQuantity,
} = useBillForm({
  billPrefix: 'WV',
  api: {
    create: (data) => pickApi.waveSave(data),
    update: (_id, data) => pickApi.waveUpdate(data),
    getById: pickApi.waveGetById,
  },
  redirectPath: '/wms/wave',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: { orderNo: '', customerName: '', productCount: 0 },
  transformPayload: (fd, status) => ({
    warehouseId: fd.warehouseId,
    waveType: fd.waveType,
    operatorName: fd.handlerName,
    remark: fd.remark,
    status,
    orders: fd.products.map((p: any) => ({
      orderNo: p.orderNo,
      customerName: p.customerName,
      productCount: p.productCount,
      totalQuantity: p.totalQuantity,
      remark: p.remark,
    })),
  }),
})

if (!('warehouseId' in formData)) {Object.assign(formData, {
  warehouseId: undefined, handlerId: undefined, handlerName: '',
  date: '', waveType: 1, remark: '',
})}

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '波次单', orderNo: formData.orderNo, showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '仓库', type: 'select', required: true, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '操作员', type: 'select', required: true, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '波次日期', type: 'date', required: true },
  { key: 'waveType', label: '波次类型', type: 'select', options: [{ label: '普通波次', value: 1 }, { label: '紧急波次', value: 2 }] },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单数量', amountValue: `${totalQuantity.value}`,
  draftBtnText: '保存草稿', draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交', primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'orderNo', title: '订单号', type: 'input', width: 150 },
  { key: 'customerName', title: '客户', type: 'input', width: 150 },
  { key: 'productCount', title: '商品数', type: 'number', width: 100, precision: 0 },
  { key: 'totalQuantity', title: '总数量', type: 'number', width: 100, precision: 0 },
  { key: 'remark', title: '备注', type: 'input', width: 200 },
]

function handleCellChange(_r: any, _k: string, _v: any) {}
function handleAction(_k: string) {}
function handleFieldChange(k: string, v: any) { baseFieldChange(k, v) }

onMounted(() => {
  if (formData.products.length === 0) { for (let i = 0; i < 3; i++) handleAddProduct() }
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
</style>
