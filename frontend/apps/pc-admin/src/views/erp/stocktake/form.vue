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
    <!-- ═══ Zone 3: 盘点明细表格 ═══ -->
    <template #detail-table>
      <div class="table-toolbar">
        <span class="section-title">盘点明细</span>
        <a-button v-if="!isViewMode" type="primary" size="small" @click="addStocktakeRow">
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
            <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
          </a-select>
          <span v-else>{{ record.productName }}</span>
        </template>
        <template #bookQtyCell="{ record }">
          <span>{{ record.bookQuantity ?? '-' }}</span>
        </template>
        <template #actualQtyCell="{ record }">
          <a-input-number
            v-if="!isViewMode"
            v-model:value="record.actualQuantity"
            :min="0"
            :precision="2"
            style="width:100%"
            size="small"
          />
          <span v-else>{{ record.actualQuantity }}</span>
        </template>
        <template #diffQtyCell="{ record }">
          <span :class="{
            'diff-positive': ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)) > 0,
            'diff-negative': ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)) < 0,
          }">
            {{ ((record.actualQuantity ?? 0) - (record.bookQuantity ?? 0)).toFixed(2) }}
          </span>
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
import { computed, onMounted } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import type { BillHeaderConfig, BasicInfoField, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stocktakeOrderApi } from '@/api/erp'

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleRemoveProduct,
  handleProductChange,
  handleSaveDraft,
  handleSubmit,
} = useBillForm({
  billPrefix: 'PC',
  api: stocktakeOrderApi,
  redirectPath: '/erp/stocktake',
  fields: [
    { key: 'warehouseId', label: '盘点仓库', type: 'select', required: true, optionsRef: 'warehouses' },
    { key: 'handlerId', label: '盘点人', type: 'select', required: true, optionsRef: 'users' },
    { key: 'date', label: '盘点日期', type: 'date', required: true },
    { key: 'stocktakeType', label: '盘点类型', type: 'select' },
  ],
})

const isViewMode = computed(() => effectiveMode.value === 'view')

// ═══════════════════════════════════════
// 盘点专用：添加行（无价格/税率字段）
// ═══════════════════════════════════════

function addStocktakeRow() {
  formData.products.push({
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    productCode: '',
    productName: '',
    specification: '',
    unit: '',
    bookQuantity: 0,
    actualQuantity: 0,
    diffQuantity: 0,
    remark: '',
  })
}

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '盘点单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'warehouseId', label: '盘点仓库', type: 'select', required: true, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '盘点人', type: 'select', required: true, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '盘点日期', type: 'date', required: true },
  { key: 'stocktakeType', label: '盘点类型', type: 'select', options: [
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

// ═══════════════════════════════════════
// 明细表格列（无税列，有账面/实盘/差异列）
// ═══════════════════════════════════════

const tableColumns = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'bookQuantity', title: '账面数量', width: 100, slotName: 'bookQtyCell' },
  { field: 'actualQuantity', title: '实盘数量', width: 120, slotName: 'actualQtyCell' },
  { field: 'diffQuantity', title: '差异', width: 100, slotName: 'diffQtyCell' },
  { field: 'remark', title: '备注', width: 200 },
  { field: 'action', title: '操作', width: 80, fixed: 'right', slotName: 'action' },
]

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

function handleAction(_actionKey: string) {
  // 头部操作按钮（可扩展）
}

function handleFieldChange(_fieldKey: string, _val: any) {
  // 字段联动（可扩展）
}
</script>

<style scoped>
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.section-title {
  font-weight: 600;
  font-size: 13px;
  color: #262626;
}

.diff-positive {
  color: #52c41a;
  font-weight: 600;
}

.diff-negative {
  color: #f5222d;
  font-weight: 600;
}
</style>
