<template>
  <ErrorBoundary @reset="handleErrorReset">
    <PageContainer full-height>
      <template #header>
        <div class="form-page-header">
          <div class="form-page-header__left">
            <span class="breadcrumb-text">仓库管理 / 其他出库单</span>
            <h2 class="page-title">其他出库单</h2>
          </div>
          <div class="form-page-header__right">
            <a-space :size="8">
              <a-button size="small" @click="router.push('/erp/stock-out')">
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
                  完成出库
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
            storage-key="stock-out-form-detail-columns"
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
              出库类型 <a-tag>{{ currentStockOutTypeText }}</a-tag>
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

      <!-- ═══ 配置弹窗（齿轮图标触发）：页面配置/录单默认值/打印设置 ═══ -->
      <a-modal
        v-model:open="showFormConfig"
        title="配置"
        :width="760"
        :footer="null"
        destroy-on-close
      >
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="pageConfig" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table
              :columns="pageConfigTableColumns"
              :data-source="pageConfigFields"
              :pagination="false"
              size="small"
              row-key="key"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'visible'">
                  <a-checkbox
                    :checked="record.visible"
                    @change="(e: any) => { pageConfig[record.key].visible = e.target.checked; saveFormConfig() }"
                  />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox
                    :checked="record.enterJump"
                    @change="(e: any) => { pageConfig[record.key].enterJump = e.target.checked; saveFormConfig() }"
                  />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="standard">标准模板</a-select-option>
                  <a-select-option value="simple">简化模板</a-select-option>
                  <a-select-option value="detailed">详细模板</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number v-model:value="formData.printCopies" :precision="0" :min="1" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="纸张大小">
                <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="A4">A4</a-select-option>
                  <a-select-option value="A5">A5</a-select-option>
                  <a-select-option value="B5">B5</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印选项">
                <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">始终使用最后一次打印的模板，打印时不再选择</a-checkbox>
                <a-checkbox v-model:checked="formData.printAfterSubmit" @change="saveFormConfig">记账后立即打印</a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  HistoryOutlined, CheckOutlined, CheckCircleOutlined, CloseCircleOutlined,
  PlusCircleOutlined, MinusCircleOutlined, SettingOutlined,
  ExportOutlined, ClockCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig, SummaryColumnDef } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useUserStore } from '@/stores/user'
import { stockOutApi, userPageConfigApi } from '@/api/erp'

defineOptions({ name: 'WarehouseStockOutForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)
const detailTableRef = ref()

// ── 出库类型字典 ──
const STOCK_OUT_TYPE_MAP: Record<number, string> = {
  1: '领用',
  2: '赠送',
  3: '样品',
  4: '盘亏',
  5: '其他',
}
const STOCK_OUT_TYPE_OPTIONS = Object.entries(STOCK_OUT_TYPE_MAP).map(([k, v]) => ({
  label: v,
  value: Number(k),
}))

// ── 状态字典（简单审批流） ──
const STOCK_OUT_STATUS_MAP: Record<number, { text: string; color: string }> = {
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
  const res: any = await stockOutApi.create({ ...payload, status: 0 })
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await stockOutApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await stockOutApi.update(id, { ...payload, status: 0 })
  if (status === 1) {
    await stockOutApi.submit(id)
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
  billPrefix: 'QTCK',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await stockOutApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || []
      return {
        ...data,
        date: data.stockOutDate || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        stockOutType: data.stockOutType ?? 5,
        warehouseId: data.warehouseId,
        warehouseName: data.warehouseName || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || '',
          barcode: d.barcode || '',
          specification: d.productSpec || d.specification || '',
          location: d.location || '',
          unit: d.productUnit || d.unit || '',
          availableStock: d.availableStock ?? 0,
          batchCode: d.batchCode || '',
          productionDate: d.productionDate || '',
          shelfLife: d.shelfLife || '',
          expiryDate: d.expiryDate || '',
          quantity: d.quantity ?? d.outboundQuantity ?? 0,
          conversionRelation: d.conversionRelation || '',
          pieceQuantity: d.pieceQuantity ?? 0,
          bigPack: d.bigPack ?? 0,
          midPack: d.midPack ?? 0,
          smallPack: d.smallPack ?? 0,
          unitPrice: d.unitPrice || 0,
          amount: (d.quantity ?? 0) * (d.unitPrice || 0),
        })),
      }
    },
  },
  redirectPath: '/erp/stock-out',
  optionTypes: ['warehouses', 'users', 'products', 'partners'],
  fields: [
    { key: 'partnerId', label: '往来单位', type: 'select' },
    { key: 'stockOutType', label: '出库类型', type: 'select', required: true },
    { key: 'warehouseId', label: '出库仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'summary', label: '摘要', type: 'input' },
  ],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', location: '', unit: '',
    availableStock: 0, batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    quantity: 0, conversionRelation: '', pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, amount: 0, remark: '',
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
    stockOutType: fd.stockOutType ?? 5,
    partnerId: fd.partnerId || undefined,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    stockOutDate: fd.date || undefined,
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
      barcode: p.barcode || undefined,
      location: p.location || undefined,
      batchCode: p.batchCode || undefined,
      productionDate: p.productionDate || undefined,
      shelfLife: p.shelfLife || undefined,
      expiryDate: p.expiryDate || undefined,
      quantity: p.quantity,
      conversionRelation: p.conversionRelation || undefined,
      pieceQuantity: p.pieceQuantity || 0,
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
if (formData.stockOutType === undefined) formData.stockOutType = 5
for (const key of ['warehouseName', 'handlerName', 'summary', 'remark', 'createTime']) {
  if (formData[key] === undefined) formData[key] = ''
}
if (formData.status === undefined) formData.status = 0

// ── 计算属性 ──
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => STOCK_OUT_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STOCK_OUT_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)
const currentStockOutTypeText = computed(() => STOCK_OUT_TYPE_MAP[formData.stockOutType] || '其他')

// ── 头部配置 ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '其他出库单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'export', label: '导出', icon: ExportOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ── 基本信息字段（全量可配置，含文档13字段，受页面配置显隐控制） ──
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'partnerId', label: '往来单位', type: 'select', inlineLabel: true, width: 210, searchBtn: '+Q' },
  { key: 'partnerCode', label: '往来编号', type: 'display', inlineLabel: true, width: 130 },
  { key: 'stockOutType', label: '出库类型', type: 'select', required: true, inlineLabel: true, width: 210, options: STOCK_OUT_TYPE_OPTIONS },
  { key: 'warehouseId', label: '出库仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q' },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q' },
  { key: 'deptName', label: '部门', type: 'input', inlineLabel: true, width: 160 },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 420 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'display', inlineLabel: true, width: 170 },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
]
// 在 computed 内响应式绑定下拉选项
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter((f) => pageConfig[f.key]?.visible !== false)
    .map((f) => {
      if (f.key === 'partnerId') return { ...f, options: (optionRefs.partners || []).map((p: any) => ({ label: p.name, value: p.id })), loading: loadingOptions.value }
      if (f.key === 'warehouseId') return { ...f, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), loading: loadingOptions.value }
      if (f.key === 'handlerId') return { ...f, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), loading: loadingOptions.value }
      return f
    })
)

// ── 页面配置弹窗（页面配置/录单默认值/打印设置） ──
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
const DEFAULT_HIDDEN_PAGE_FIELDS: string[] = ['partnerCode', 'deptName', 'summary', 'creatorName', 'createTime', 'printCount']
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = { visible: !DEFAULT_HIDDEN_PAGE_FIELDS.includes(f.key), enterJump: ['select', 'date', 'number', 'input'].includes(f.type) }
}
const pageConfigTableColumns = [
  { title: '字段', key: 'label', width: 160 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 60 },
  { title: '回车跳转', key: 'enterJump', width: 80 },
]
const pageConfigFields = computed(() => ALL_BASIC_INFO_FIELDS.map((f) => ({
  key: f.key, label: f.label, displayName: f.label,
  visible: pageConfig[f.key]?.visible !== false,
  enterJump: !!pageConfig[f.key]?.enterJump,
})))

const FORM_CONFIG_MODULE = 'stock-out-form'
const FORM_CONFIG_PAGE = 'form'
async function loadFormConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach((k) => {
        if (pageConfig[k]) {
          pageConfig[k].visible = parsed.fields[k].visible !== false
          pageConfig[k].enterJump = !!parsed.fields[k].enterJump
        }
      })
    }
    if (parsed.defaults) {
      Object.assign(formData, {
        defaultWarehouseId: parsed.defaults.defaultWarehouseId,
        defaultHandlerId: parsed.defaults.defaultHandlerId,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* API 不可用时保持默认 */ }
}
async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultWarehouseId: formData.defaultWarehouseId,
      defaultHandlerId: formData.defaultHandlerId,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
}
// 录单默认值 / 打印设置 初始化
for (const k of ['defaultWarehouseId', 'defaultHandlerId', 'printTemplate', 'printCopies', 'printPaperSize']) {
  if (formData[k] === undefined) formData[k] = k === 'printTemplate' ? 'standard' : (k === 'printCopies' ? 1 : (k === 'printPaperSize' ? 'A4' : undefined))
}
if (formData.printAlwaysLastTemplate === undefined) formData.printAlwaysLastTemplate = false
if (formData.printAfterSubmit === undefined) formData.printAfterSubmit = false

// ── 摘要面板 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '出库数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '出库金额', value: `¥${totalAmount.value.toFixed(2)}`, divider: true },
])

// ── 页脚配置 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '出库金额',
  amountValue: `¥${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? '提交审批' : '提交审批',
  primaryShortcut: isLocked.value ? undefined : 'Ctrl+Enter',
  saving: saving.value,
}))

// ── 明细表格列（全部45列，默认显20列，非默认列 defaultHidden 供配置显示） ──
const ALL_DETAIL_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 90, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 70, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'itemCode', title: '货号', type: 'input', width: 110 },
  { key: 'barcode', title: '条码', type: 'input', width: 110 },
  { key: 'specification', title: '规格', type: 'input', width: 100, defaultHidden: true },
  { key: 'model', title: '型号', type: 'input', width: 80, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 80, defaultHidden: true },
  { key: 'region', title: '区域', type: 'input', width: 80, defaultHidden: true },
  { key: 'location', title: '货位', type: 'input', width: 90 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'input', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'input', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 90 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, precision: 2 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, precision: 2 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'priceLevel1', title: '价格等级1', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel2', title: '价格等级2', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel3', title: '价格等级3', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel4', title: '价格等级4', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel5', title: '价格等级5', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel6', title: '价格等级6', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel7', title: '价格等级7', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'priceLevel8', title: '价格等级8', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'docExtNum1', title: '单据自定义1(数字)', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'docExtNum2', title: '单据自定义2(数字)', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'docExtNum3', title: '单据自定义3(数字)', type: 'number', width: 130, precision: 2, defaultHidden: true },
  { key: 'docExtText1', title: '单据自定义4(文本)', type: 'input', width: 130, defaultHidden: true },
  { key: 'docExtText2', title: '单据自定义5(文本)', type: 'input', width: 130, defaultHidden: true },
]
// 传全部45列给 BillDetailTable：其内置列配置(个人/全局)按 defaultHidden 默认显示20列，其余可配置开启
const detailColumns: DetailColumnConfig[] = ALL_DETAIL_COLUMNS

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
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.location = p.location || ''
    row.unit = p.unit || ''
    row.availableStock = p.stock ?? 0
    row.batchCode = p.batchCode || ''
    row.conversionRelation = p.conversionRelation || ''
    row.pieceQuantity = p.pieceQuantity ?? 0
    row.bigPack = p.bigPack ?? 0
    row.midPack = p.midPack ?? 0
    row.smallPack = p.smallPack ?? 0
    row.unitPrice = p.salePrice || 0
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
      router.push('/erp/stock-out')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'export': {
      const items = formData.products || []
      if (items.length === 0) { message.warning('当前没有可导出的明细'); return }
      const headers = ['商品名称', '货号', '条码', '规格', '数量', '单价', '金额']
      const rows = items.map((p: any) => [
        p.productName || '', p.itemCode || '', p.barcode || '', p.specification || '',
        p.quantity ?? 0, p.unitPrice ?? 0, p.amount ?? 0,
      ])
      const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `其他出库单_${formData.orderNo || '明细'}.csv`
      a.click()
      URL.revokeObjectURL(url)
      message.success('导出成功')
      break
    }
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
    content: `确认审核通过出库单 ${formData.orderNo} 吗？`,
    okText: '审核通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockOutApi.approve(id)
        message.success('审核成功')
        router.push('/erp/stock-out')
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
    title: '完成出库',
    content: `确认完成出库单 ${formData.orderNo} 的库存出库操作吗？`,
    okText: '确认完成',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockOutApi.complete(id)
        message.success('出库完成')
        router.push('/erp/stock-out')
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
    content: `确认取消出库单 ${formData.orderNo} 吗？取消后不可恢复。`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '再想想',
    onOk: async () => {
      try {
        await stockOutApi.cancel(id)
        message.success('已取消')
        router.push('/erp/stock-out')
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
  loadFormConfig()
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
.config-hint { font-size: 12px; color: #8c8c8c; margin: 0 0 8px; }
</style>
