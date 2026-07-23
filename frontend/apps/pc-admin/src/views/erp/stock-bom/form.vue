<template>
  <div
    class="form-page-container"
    style="height:100%;display:flex;flex-direction:column;"
  >
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="basicInfoFields"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- Zone 3: 物料明细表格 -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="detailColumns"
          :data-source="formData.products"
          :max-height="tableMaxHeight"
          :summary-columns="tableSummaryColumns"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
        >
          <template #actionCell="{ index }">
            <a-space :size="2">
              <a-button
                type="link"
                size="small"
                class="action-add-btn"
                @click="handleInsertProduct(index)"
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
            <a-select
              v-model:value="record.productId"
              placeholder="搜索选择物料"
              show-search
              :filter-option="filterOption"
              style="width:100%"
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
          </template>
        </BillDetailTable>
      </template>

      <!-- Zone 4: 备注 + 单据信息 -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">备注</span>
            <a-input
              v-model:value="formData.remark"
              size="small"
              class="remark-input"
              placeholder="请输入备注"
            />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">状态
            <a-tag :color="statusColor">{{ statusText }}</a-tag>
          </span>
          <span class="doc-info-item">总成本
            <a-tag color="blue">¥{{ totalCost.toFixed(2) }}</a-tag>
          </span>
          <span class="doc-info-item">制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
        </div>
      </template>
    </BillFormPage>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined, CheckOutlined, StopOutlined,
  PlusCircleOutlined, MinusCircleOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stockBomApi } from '@/api/erp'
import type { StockBomPayload } from '@/api/erp'
import { BOM_STATUS } from '@/utils/statusConfig'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'StockBomForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

/**
 * 保存 + 按目标状态推进：
 * status=1 时保存后调用 /enable 端点启用 BOM（后端 create 默认即启用）
 */
async function createWithStatus(data: StockBomPayload & { status?: number }) {
  const res: any = await stockBomApi.create(data)
  const created = res?.data || res
  if (data.status === 1 && created?.id) {
    await stockBomApi.enable(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: StockBomPayload & { status?: number }) {
  const res: any = await stockBomApi.update(id, data)
  if (data.status === 1) {
    await stockBomApi.enable(id)
  }
  return res?.data || res
}

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
} = useBillForm({
  billPrefix: 'BOM',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await stockBomApi.getById(id)
      const data = res?.data || res || {}
      let rawItems: any[] = []
      try {
        const itemsRes: any = await stockBomApi.getItems(id)
        rawItems = itemsRes?.data || []
      } catch { rawItems = [] }
      return {
        ...data,
        orderNo: data.bomNo || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || '',
          specification: d.productSpec || '',
          unit: d.productUnit || '',
          quantity: d.quantity ?? 1,
          unitCost: d.unitCost ?? 0,
          cost: d.cost ?? (d.quantity ?? 0) * (d.unitCost ?? 0),
        })),
      }
    },
  },
  redirectPath: '/erp/stock-bom',
  optionTypes: ['products'],
  fields: [
    { key: 'bomName', label: 'BOM名称', type: 'input', required: true },
    { key: 'productId', label: '成品', type: 'select', required: true },
    { key: 'bomType', label: 'BOM类型', type: 'select', required: true },
    { key: 'outputQuantity', label: '产出数量', type: 'number', required: true },
  ],
  productDefaults: {
    itemCode: '', specification: '', unit: '',
    quantity: 1, unitCost: 0, cost: 0, remark: '',
  },
  transformPayload: (fd, status): StockBomPayload & { status: number } => {
    const validItems = fd.products.filter((p: any) => p.productId != null)
    if (validItems.length === 0) {
      throw new Error('请添加物料明细并选择商品')
    }
    return {
      status,
      bomName: fd.bomName,
      productId: fd.productId,
      bomType: fd.bomType,
      outputQuantity: fd.outputQuantity,
      remark: fd.remark || undefined,
      items: validItems.map((p: any) => ({
        productId: p.productId,
        productCode: p.itemCode || p.productCode,
        productName: p.productName,
        productSpec: p.specification,
        productUnit: p.unit,
        quantity: p.quantity,
        unitCost: p.unitCost,
        remark: p.remark || undefined,
      })),
    }
  },
})

// 初始化BOM特有字段（fields 已预建部分 key，逐个补默认值；编辑模式由 loadDetail 覆盖）
if (formData.bomName === undefined) formData.bomName = ''
if (formData.bomType === undefined) formData.bomType = 1
if (!formData.outputQuantity) formData.outputQuantity = 1
if (formData.remark === undefined) formData.remark = ''
if (formData.status === undefined) formData.status = 1
if (formData.createTime === undefined) formData.createTime = ''

// ── 计算 ──
const totalCost = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitCost || 0), 0))

// ── 状态展示 ──
const currentStatus = computed(() => Number(formData.status ?? 1))
const statusText = computed(() => BOM_STATUS[currentStatus.value]?.text || '启用')
const statusColor = computed(() => BOM_STATUS[currentStatus.value]?.color || 'default')
const isEdit = computed(() => effectiveMode.value === 'edit')

// ── 页眉配置 ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '生产模板（BOM）',
  orderNo: formData.orderNo,
  showAttachment: false,
  actions: [
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    ...(isEdit.value && currentStatus.value === 0
      ? [{ key: 'enable', label: '启用', icon: CheckOutlined }]
      : []),
    ...(isEdit.value && currentStatus.value === 1
      ? [{ key: 'disable', label: '停用', icon: StopOutlined }]
      : []),
  ],
}))

// ── 基本信息字段 ──
const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'orderNo', label: 'BOM编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'bomName', label: 'BOM名称', type: 'input', required: true, inlineLabel: true, width: 300 },
  { key: 'productId', label: '成品', type: 'select', required: true, inlineLabel: true, width: 300, options: optionRefs.products.map((p: any) => ({ label: `${p.code ? p.code + ' - ' : ''}${p.name}`, value: p.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'bomType', label: 'BOM类型', type: 'select', required: true, inlineLabel: true, width: 210, options: [
    { label: '组装BOM', value: 1 },
    { label: '拆分BOM', value: 2 },
    { label: '通用', value: 3 },
  ]},
  { key: 'outputQuantity', label: '产出数量', type: 'number', required: true, inlineLabel: true, width: 160, precision: 2, min: 0 },
])

// ── 摘要 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '物料种类', value: formData.products.filter((p: any) => p.productId != null).length, statusLabel: statusText.value },
  { label: '物料总用量', value: totalQuantity.value },
  { label: '产出数量', value: formData.outputQuantity ?? 1 },
  { label: '合计成本', value: totalCost.value.toFixed(2), divider: true },
])

// ── 页脚 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '合计成本',
  amountValue: `¥${totalCost.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '保存并启用',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ── 明细表格列 ──
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'productName', title: '物料名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'itemCode', title: '物料编码', type: 'input', width: 110 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'quantity', title: '用量', type: 'number', width: 90, precision: 2, min: 0 },
  { key: 'unitCost', title: '单位成本', type: 'number', width: 100, precision: 2, min: 0 },
  { key: 'cost', title: '成本', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'cost', value: totalCost.value.toFixed(2), highlight: true },
])

// ── 事件处理 ──
function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
    row.unitCost = p.purchasePrice || 0
    row.cost = (row.quantity || 0) * (row.unitCost || 0)
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (['quantity', 'unitCost'].includes(fieldKey)) {
    record.cost = (record.quantity || 0) * (record.unitCost || 0)
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/erp/stock-bom')
      break
    case 'enable':
      handleToggleStatus(true)
      break
    case 'disable':
      handleToggleStatus(false)
      break
  }
}

/** 启用/停用 BOM */
function handleToggleStatus(enable: boolean) {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: enable ? '确认启用' : '确认停用',
    content: `确认${enable ? '启用' : '停用'}BOM ${formData.orderNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        if (enable) {
          await stockBomApi.enable(id)
        } else {
          await stockBomApi.disable(id)
        }
        message.success(enable ? 'BOM已启用' : 'BOM已停用')
        router.push('/erp/stock-bom')
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    },
  })
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 3; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
</style>
