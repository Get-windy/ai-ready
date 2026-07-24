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
    <!-- Zone 3: 调价明细表格 -->
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
        <template #diffCell="{ record }">
          <span
            :class="{
              'diff-positive': ((record.newCost ?? 0) - (record.oldCost ?? 0)) > 0,
              'diff-negative': ((record.newCost ?? 0) - (record.oldCost ?? 0)) < 0,
            }"
          >
            {{ ((record.newCost ?? 0) - (record.oldCost ?? 0)).toFixed(2) }}
          </span>
        </template>
      </BillDetailTable>
    </template>

    <!-- Zone 4: 备注/原因说明 -->
    <template #bottom-extra>
      <div class="remark-section">
        <div class="remark-row">
          <span class="remark-label">原因说明</span>
          <a-input
            v-model:value="formData.reasonDesc"
            size="small"
            class="remark-input"
            placeholder="请输入调价原因说明"
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
import { stockCostAdjustApi } from '@/api/erp'

defineOptions({ name: 'StockCostAdjustForm' })

const tableMaxHeight = ref(400)

const ADJUST_TYPE_OPTIONS = [
  { label: '移动加权', value: 1 },
  { label: '全月平均', value: 2 },
  { label: '个别计价', value: 3 },
]

const REASON_TYPE_OPTIONS = [
  { label: '市场波动', value: 1 },
  { label: '供应商调价', value: 2 },
  { label: '汇率变动', value: 3 },
  { label: '其他', value: 4 },
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
  billPrefix: 'CBTZ',
  api: {
    create: stockCostAdjustApi.create,
    update: stockCostAdjustApi.update,
    getById: stockCostAdjustApi.getById,
  },
  redirectPath: '/erp/stock-cost-adjust',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', location: '', unit: '',
    conversionRelation: '', conversionResult: '',
    wholesalePrice: 0, retailPrice: 0,
    currentQuantity: 0, oldCost: 0, newCost: 0, adjustAmount: 0,
    remark: '',
  },
})

// 初始化调价单特有字段
if (!('warehouseId' in formData)) {
  Object.assign(formData, {
    warehouseId: undefined, warehouseName: '',
    handlerId: undefined, handlerName: '',
    date: '',
    adjustType: 1,
    reasonType: 1,
    reasonDesc: '',
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
    row.conversionRelation = p.conversionRelation || ''
    row.wholesalePrice = p.wholesalePrice || 0
    row.retailPrice = p.retailPrice || 0
    row.oldCost = p.purchasePrice || 0
    row.currentQuantity = p.stock ?? 0
  }
}

// BillFormPage 配置
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '成本调价单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '仓库', type: 'select', required: true, placeholder: '请选择仓库', options: (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '调价日期', type: 'date', required: true },
  { key: 'adjustType', label: '调价类型', type: 'select', required: true, options: ADJUST_TYPE_OPTIONS },
  { key: 'reasonType', label: '原因类型', type: 'select', required: true, options: REASON_TYPE_OPTIONS },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '调价项',
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
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 90 },
  { key: 'conversionResult', title: '换算结果', type: 'input', width: 90 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 100, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 100, precision: 2 },
  { key: 'currentQuantity', title: '库存数量', type: 'number', width: 100, precision: 0 },
  { key: 'oldCost', title: '调前成本价', type: 'number', width: 100, precision: 2 },
  { key: 'newCost', title: '调后成本价', type: 'number', width: 100, precision: 2 },
  { key: 'adjustAmount', title: '调整金额', type: 'number', width: 100, precision: 2 },
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
.diff-positive { color: #f5222d; font-weight: 600; }
.diff-negative { color: #52c41a; font-weight: 600; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
</style>
