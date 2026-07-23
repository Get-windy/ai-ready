<template>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :footer="footerConfig"
    :show-bottom-panel="false"
    @action="handleAction"
    @field-change="handleFieldChange"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <!-- Zone 3: 调拨明细表格 -->
    <template #detail-table>
      <BillDetailTable
        :columns="detailColumns"
        :data-source="formData.products"
        :max-height="tableMaxHeight"
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
        <template #productCell="{ record, index }">
          <div class="product-cell">
            <a-select
              v-model:value="record.productId"
              placeholder="请选择商品"
              show-search
              :filter-option="filterOption"
              style="flex:1"
              :loading="loadingOptions"
              size="small"
              @change="(val: number) => handleProductChange(val, index)"
            >
              <a-select-option
                v-for="p in optionRefs.products"
                :key="p.id"
                :value="p.id"
              >
                {{ p.name }}
              </a-select-option>
            </a-select>
          </div>
        </template>
      </BillDetailTable>
    </template>

    <!-- Zone 4: 备注 -->
    <template #bottom-extra>
      <div class="remark-section">
        <div class="remark-row">
          <span class="remark-label">备注</span>
          <a-input
            v-model:value="formData.remark"
            size="small"
            class="remark-input"
          />
        </div>
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick, defineOptions } from 'vue'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stockTransferApi } from '@/api/erp'

defineOptions({ name: 'StockTransferForm' })

const tableMaxHeight = ref(400)

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  handleAddProduct,
} = useBillForm({
  billPrefix: 'DB',
  api: {
    create: stockTransferApi.create,
    update: (id: number, data: any) => stockTransferApi.create(data),
    getById: stockTransferApi.getById,
  },
  redirectPath: '/erp/stock-transfer',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', specification: '', unit: '',
    quantity: 0,
  },
})

// 初始化调拨单特有字段
if (!('fromWarehouseId' in formData)) {
  Object.assign(formData, {
    fromWarehouseId: undefined, fromWarehouseName: '',
    toWarehouseId: undefined, toWarehouseName: '',
    handlerId: undefined, handlerName: '',
    date: '',
    remark: '',
  })
}

function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
  }
}

// BillFormPage 配置
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '调拨单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const fromWarehouseOptions = computed(() =>
  (optionRefs.warehouses || []).map((w: any) => ({
    label: w.name, value: w.id,
  }))
)

const toWarehouseOptions = computed(() =>
  (optionRefs.warehouses || []).map((w: any) => ({
    label: w.name, value: w.id,
  }))
)

// 调拨单：调出仓库 + 调入仓库（用两个下拉字段）
const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'fromWarehouseId', label: '调出仓库', type: 'select', required: true, placeholder: '请选择调出仓库', options: fromWarehouseOptions.value, searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'toWarehouseId', label: '调入仓库', type: 'select', required: true, placeholder: '请选择调入仓库', options: toWarehouseOptions.value, searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '调拨日期', type: 'date', required: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '调拨项',
  amountValue: `${formData.products.length} 项`,
  amountHighlight: false,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// 明细列配置
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'slot', slotName: 'productCell', width: 200 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'quantity', title: '调拨数量', type: 'number', width: 100, precision: 0 },
]

function handleCellChange(_record: any, _fieldKey: string, _value: any) {}
function handleAction(_actionKey: string) {}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

onMounted(() => {
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.product-cell { display: flex; align-items: center; gap: 4px; }
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
</style>
