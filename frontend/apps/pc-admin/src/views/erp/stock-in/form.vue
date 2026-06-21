<template>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :tabs="tabsConfig"
    :summary="summaryConfig"
    :footer="footerConfig"
    @action="handleAction"
    @field-change="handleFieldChange"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <!-- ═══ Zone 3: 商品明细表格 ═══ -->
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
            <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
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
      <!-- 合计行 -->
      <div class="table-summary-row">
        <span class="summary-label">合计</span>
        <span></span><span></span>
        <span class="summary-red">{{ totalQuantity }}</span>
        <span></span><span></span>
        <span class="summary-red">¥{{ totalAmount.toFixed(2) }}</span>
        <span></span><span></span>
        <span class="summary-red">¥{{ totalWithTax.toFixed(2) }}</span>
      </div>
    </template>
    <!-- ═══ 备注 ═══ -->
    <template #bottom-extra>
      <div class="remark-section">
        <a-input v-model:value="formData.remark" placeholder="备注" size="small" />
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stockInApi } from '@/api/erp'
import optionsApi from '@/api/options'

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
  totalWithTax,
} = useBillForm({
  billPrefix: 'IN',
  api: stockInApi,
  redirectPath: '/erp/stock-in',
  fields: [
    { key: 'supplierId', label: '供应商', type: 'select', required: true, optionsRef: 'suppliers' },
    { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, optionsRef: 'warehouses' },
    { key: 'handlerId', label: '经手人', type: 'select', required: true, optionsRef: 'users' },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'stockInType', label: '入库类型', type: 'select' },
    { key: 'sourceOrderNo', label: '来源单号', type: 'input' },
    { key: 'paymentMethod', label: '付款方式', type: 'select' },
    { key: 'paymentAccount', label: '付款账户', type: 'input' },
    { key: 'remark', label: '备注', type: 'input' },
  ],
})

// 加载供应商选项（useBillForm 默认不加载 suppliers）
onMounted(async () => {
  const suppliers = await optionsApi.getSuppliers().catch(() => [])
  optionRefs.suppliers = suppliers || []
})

const isViewMode = computed(() => effectiveMode.value === 'view')

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '入库单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'supplierId', label: '供应商', type: 'select', required: true, options: optionRefs.suppliers.map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '单据日期', type: 'date', required: true },
  { key: 'stockInType', label: '入库类型', type: 'select', options: [
    { label: '采购入库', value: 1 },
    { label: '退货入库', value: 2 },
    { label: '调拨入库', value: 3 },
    { label: '其他入库', value: 4 },
  ]},
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '关联采购订单号' },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  {
    key: 'payment',
    tab: '付款信息',
    fields: [
      { key: 'paymentMethod', label: '付款方式', type: 'select', options: [
        { label: '现金', value: 1 },
        { label: '银行转账', value: 2 },
        { label: '支票', value: 3 },
        { label: '月结', value: 4 },
      ]},
      { key: 'paymentAccount', label: '付款账户', type: 'input' },
    ],
  },
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '总数量', value: totalQuantity.value },
  { label: '入库金额', value: `¥${totalAmount.value.toFixed(2)}` },
  { label: '价税合计', value: `¥${totalWithTax.value.toFixed(2)}`, divider: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '入库金额',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列（无税列）
// ═══════════════════════════════════════

const tableColumns = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productCell' },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'batchNo', title: '批次号', width: 120 },
  { field: 'remark', title: '备注', width: 150 },
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

.table-summary-row {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.summary-label {
  font-weight: 700;
  color: #262626;
  min-width: 60px;
}

.summary-red {
  color: #ff4d4f;
  font-weight: 700;
  min-width: 50px;
  text-align: right;
}

.amount-text {
  font-weight: 500;
}

.remark-section {
  padding: 4px 0;
}
</style>
