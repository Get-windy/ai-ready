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
    <!-- Zone 3: 报溢明细表格 -->
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
import { stockOverflowApi } from '@/api/erp'

defineOptions({ name: 'StockOverflowForm' })

const tableMaxHeight = ref(400)

const SOURCE_TYPE_OPTIONS = [
  { label: '盘点发现', value: 1 },
  { label: '账外物资发现', value: 2 },
  { label: '其他', value: 3 },
]

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
  billPrefix: 'BY',
  api: {
    create: stockOverflowApi.create,
    update: stockOverflowApi.update,
    getById: stockOverflowApi.getById,
  },
  redirectPath: '/erp/stock-overflow',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', location: '', unit: '',
    batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    quantity: 0, conversionRelation: '', overflowPrice: 0, overflowAmount: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    remark: '',
  },
})

// 初始化报溢单特有字段
if (!('warehouseId' in formData)) {
  Object.assign(formData, {
    warehouseId: undefined, warehouseName: '',
    handlerId: undefined, handlerName: '',
    date: '',
    sourceType: 1,
    remark: '',
  })
}

function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.location = p.location || ''
    row.unit = p.unit || ''
    row.batchCode = p.batchCode || ''
    row.conversionRelation = p.conversionRelation || ''
    row.bigPack = p.bigPack ?? 0
    row.midPack = p.midPack ?? 0
    row.smallPack = p.smallPack ?? 0
    row.overflowPrice = p.purchasePrice || 0
    row.overflowAmount = (row.quantity || 0) * (row.overflowPrice || 0)
  }
}

// BillFormPage 配置
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '报溢单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, placeholder: '请选择入库仓库', options: (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '单据日期', type: 'date', required: true },
  { key: 'sourceType', label: '报溢来源', type: 'select', required: true, options: SOURCE_TYPE_OPTIONS },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '报溢项',
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
  { key: 'barcode', title: '条码', type: 'input', width: 110 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'location', title: '货位', type: 'input', width: 90 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'input', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'input', width: 110 },
  { key: 'quantity', title: '报溢数量', type: 'number', width: 100, precision: 0 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 90 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'overflowPrice', title: '报溢单价', type: 'number', width: 100, precision: 2 },
  { key: 'overflowAmount', title: '报溢金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, precision: 2 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, precision: 2 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, precision: 2 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
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
