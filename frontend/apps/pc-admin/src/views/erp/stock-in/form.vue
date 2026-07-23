<template>
  <ErrorBoundary @reset="handleErrorReset">
    <PageContainer full-height>
      <template #header>
        <div class="form-page-header">
          <div class="form-page-header__left">
            <span class="breadcrumb-text">仓库管理 / 其他入库单</span>
            <h2 class="page-title">其他入库单</h2>
          </div>
          <div class="form-page-header__right">
            <a-space :size="8">
              <a-button size="small" @click="router.push('/erp/stock-in')">
                <template #icon><HistoryOutlined /></template>
                返回列表
              </a-button>
              <template v-if="effectiveMode === 'edit'">
                <a-button
                  v-if="currentStatus === 1"
                  type="primary"
                  size="small"
                  @click="handleApprove"
                >
                  <template #icon><CheckOutlined /></template>
                  审核通过
                </a-button>
                <a-button
                  v-if="currentStatus === 2"
                  type="primary"
                  size="small"
                  style="background:#52c41a;border-color:#52c41a;"
                  @click="handleComplete"
                >
                  <template #icon><CheckCircleOutlined /></template>
                  完成入库
                </a-button>
                <a-button
                  v-if="currentStatus >= 0 && currentStatus < 3"
                  size="small"
                  danger
                  @click="handleCancel"
                >
                  <template #icon><CloseCircleOutlined /></template>
                  取消
                </a-button>
              </template>
            </a-space>
          </div>
        </div>
      </template>

      <!-- 锁定时警告 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入审批/执行流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0;"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        :mode="pageMode"
        @action="handleAction"
        @field-change="handleFieldChange"
        @draft="handleSaveDraft"
        @submit="handleSubmit"
      >
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            ref="detailTableRef"
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
                placeholder="搜索选择商品"
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

        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">单据备注</span>
              <a-input
                v-model:value="formData.remark"
                size="small"
                class="remark-input"
                placeholder="请输入单据备注"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              入库类型 <a-tag>{{ currentStockInTypeText }}</a-tag>
            </span>
            <span class="doc-info-item">
              制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">
              制单时间 {{ formData.createTime || formatNow() }}
            </span>
          </div>
        </template>
      </BillFormPage>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  HistoryOutlined, CheckOutlined, CheckCircleOutlined, CloseCircleOutlined,
  PlusCircleOutlined, MinusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig, SummaryColumnDef } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useUserStore } from '@/stores/user'
import { warehouseStockInApi } from '@/api/erp'

defineOptions({ name: 'WarehouseStockInForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)
const detailTableRef = ref()

// ── 入库类型字典 ──
const STOCK_IN_TYPE_MAP: Record<number, string> = {
  1: '盘盈',
  2: '获赠',
  3: '退货入库',
  4: '其他',
}
const STOCK_IN_TYPE_OPTIONS = Object.entries(STOCK_IN_TYPE_MAP).map(([k, v]) => ({
  label: v,
  value: Number(k),
}))

// ── 状态字典（简单审批流） ──
const STOCK_IN_STATUS_MAP: Record<number, { text: string; color: string }> = {
  [-1]: { text: '已取消', color: 'error' },
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'processing' },
  3: { text: '已完成', color: 'success' },
}

// ── 页面模式 ──
const route = useRouter().currentRoute
const pageMode = computed<'create' | 'edit' | 'view'>(() => {
  const id = route.value.params.id || route.value.query.id
  return id ? 'edit' : 'create'
})

// ── API 包装：保存+提交两步合一 ──
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await warehouseStockInApi.create({ ...payload, status: 0 })
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await warehouseStockInApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await warehouseStockInApi.update(id, { ...payload, status: 0 })
  if (status === 1) {
    await warehouseStockInApi.submit(id)
  }
  return res?.data || res
}

// ── useBillForm ──
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
  totalAmount,
} = useBillForm({
  billPrefix: 'QTRK',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await warehouseStockInApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || []
      return {
        ...data,
        date: data.stockInDate || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        stockInType: data.stockInType ?? 4,
        warehouseId: data.warehouseId,
        warehouseName: data.warehouseName || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || '',
          specification: d.productSpec || d.specification || '',
          unit: d.productUnit || d.unit || '',
          quantity: d.quantity ?? d.inboundQuantity ?? 0,
          unitPrice: d.unitPrice || 0,
          amount: (d.quantity ?? 0) * (d.unitPrice || 0),
        })),
      }
    },
  },
  redirectPath: '/erp/stock-in',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [
    { key: 'stockInType', label: '入库类型', type: 'select', required: true },
    { key: 'warehouseId', label: '入库仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'summary', label: '摘要', type: 'input' },
  ],
  productDefaults: {
    itemCode: '', specification: '', unit: '',
    quantity: 0, unitPrice: 0, amount: 0, remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.name || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
  },
  transformPayload: (fd, status) => ({
    status,
    stockInType: fd.stockInType ?? 4,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    stockInDate: fd.date || undefined,
    summary: fd.summary || undefined,
    remark: fd.remark || undefined,
    totalQuantity: totalQuantity.value,
    totalAmount: totalAmount.value,
    items: (fd.products || []).filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      quantity: p.quantity,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
if (formData.stockInType === undefined) formData.stockInType = 4
for (const key of ['warehouseName', 'handlerName', 'summary', 'remark', 'createTime']) {
  if (formData[key] === undefined) formData[key] = ''
}
if (formData.status === undefined) formData.status = 0

// ── 计算属性 ──
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => STOCK_IN_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STOCK_IN_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)
const currentStockInTypeText = computed(() => STOCK_IN_TYPE_MAP[formData.stockInType] || '其他')

// ── 头部配置 ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '其他入库单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [],
}))

// ── 基本信息字段 ──
const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'stockInType', label: '入库类型', type: 'select', required: true, inlineLabel: true, width: 210,
    options: STOCK_IN_TYPE_OPTIONS },
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, inlineLabel: true, width: 210,
    options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210,
    options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 420 },
])

// ── 摘要面板 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '入库数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '入库金额', value: `¥${totalAmount.value.toFixed(2)}`, divider: true },
])

// ── 页脚配置 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '入库金额',
  amountValue: `¥${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? '提交审批' : '提交审批',
  primaryShortcut: isLocked.value ? undefined : 'Ctrl+Enter',
  saving: saving.value,
}))

// ── 明细表格列 ──
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'itemCode', title: '货号', type: 'input', width: 110 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: Number(totalAmount.value.toFixed(2)), highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ── 事件处理 ──
function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
    row.unitPrice = p.purchasePrice || p.salePrice || 0
    row.amount = (row.quantity || 0) * (row.unitPrice || 0)
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (['quantity', 'unitPrice'].includes(fieldKey)) {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/erp/stock-in')
      break
  }
}

function handleErrorReset() {
  // 错误恢复后重新加载
}

// ── 审批流操作 ──
function handleApprove() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '审核确认',
    content: `确认审核通过入库单 ${formData.orderNo} 吗？`,
    okText: '审核通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await warehouseStockInApi.approve(id)
        message.success('审核成功')
        router.push('/erp/stock-in')
      } catch (err: any) {
        message.error(err?.message || '审核失败')
      }
    },
  })
}

function handleComplete() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '完成入库',
    content: `确认完成入库单 ${formData.orderNo} 的库存入库操作吗？`,
    okText: '确认完成',
    cancelText: '取消',
    onOk: async () => {
      try {
        await warehouseStockInApi.complete(id)
        message.success('入库完成')
        router.push('/erp/stock-in')
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    },
  })
}

function handleCancel() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '取消单据',
    content: `确认取消入库单 ${formData.orderNo} 吗？取消后不可恢复。`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '再想想',
    onOk: async () => {
      try {
        await warehouseStockInApi.cancel(id)
        message.success('已取消')
        router.push('/erp/stock-in')
      } catch (err: any) {
        message.error(err?.message || '取消失败')
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
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.form-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.form-page-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.breadcrumb-text {
  font-size: 12px;
  color: #999;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.form-page-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
</style>
