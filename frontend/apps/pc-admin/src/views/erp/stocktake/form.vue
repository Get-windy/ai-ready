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
    <!-- Zone 3: 盘点明细表格 -->
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
              @click="addStocktakeRow"
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
        <template #diffCell="{ record }">
          <span
            :class="{
              'diff-positive': ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)) > 0,
              'diff-negative': ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)) < 0,
            }"
          >
            {{ ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)).toFixed(2) }}
          </span>
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
import { stockCheckApi } from '@/api/erp'

defineOptions({ name: 'StockTakeForm' })

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
} = useBillForm({
  billPrefix: 'PC',
  api: {
    create: stockCheckApi.create,
    update: stockCheckApi.update,
    getById: stockCheckApi.getById,
  },
  redirectPath: '/erp/stocktake',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', specification: '', unit: '',
    bookQuantity: 0, actualQuantity: 0,
  },
})

// 初始化盘点单特有字段
if (!('warehouseId' in formData)) {Object.assign(formData, {
  warehouseId: undefined, warehouseName: '',
  handlerId: undefined, handlerName: '',
  date: '', checkType: 1,
  remark: '',
})}

// 盘点专用：添加行
function addStocktakeRow() {
  formData.products.push({
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    itemCode: '',
    productName: '',
    specification: '',
    unit: '',
    bookQuantity: 0,
    actualQuantity: 0,
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
  title: '盘点单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '盘点仓库', type: 'select', required: true, placeholder: '请选择仓库', options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '盘点人', type: 'select', required: true, placeholder: '请选择盘点人', options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '盘点日期', type: 'date', required: true },
  { key: 'checkType', label: '盘点类型', type: 'select', options: [
    { label: '全面盘点', value: 1 },
    { label: '抽盘', value: 2 },
    { label: '动态盘点', value: 3 },
  ]},
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '盘点项',
  amountValue: `${formData.products.length} 项`,
  amountHighlight: false,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// 明细表格列配置（无价格/税率，有账面/实盘/差异列）
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'slot', slotName: 'productCell', width: 200 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'bookQuantity', title: '账面数量', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'actualQuantity', title: '实盘数量', type: 'number', width: 100, precision: 2 },
  { key: 'diffQuantity', title: '差异', type: 'slot', slotName: 'diffCell', width: 100 },
  { key: 'remark', title: '备注', type: 'input', width: 200 },
]

// 事件处理

function handleCellChange(_record: any, _fieldKey: string, _value: any) {}

function handleAction(_actionKey: string) {}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

onMounted(() => {
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) addStocktakeRow()
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
.diff-positive { color: #52c41a; font-weight: 600; }
.diff-negative { color: #f5222d; font-weight: 600; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
</style>
