<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert v-if="isLocked" :message="`当前单据状态为「${statusText}」，已进入作业流程，内容不可修改（明细可在待处理状态保存）`" type="warning" show-icon banner style="flex-shrink:0" />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @draft="handleSaveDraftFn"
        @submit="handlePrimarySubmit"
      >
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.details"
            :summary-columns="tableSummaryColumns"
            :storage-key="'receiving-order-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <a-space :size="2">
                <a-button v-if="!empty" type="link" size="small" class="action-add-btn" @click="handleInsertDetail(index)"><PlusCircleOutlined /></a-button>
                <a-button v-if="!empty" type="link" size="small" class="action-del-btn" @click="handleRemoveDetail(index)"><MinusCircleOutlined /></a-button>
                <a-button v-else type="link" size="small" class="action-add-btn" @click="handleAddDetail()"><PlusCircleOutlined /></a-button>
              </a-space>
            </template>
            <template #diffQuantityCell="{ record }">
              <span :style="{ color: (Number(record.receivedQuantity || 0) - Number(record.expectedQuantity || 0)) > 0 ? '#cf1322' : '#3878e8' }">
                {{ formatQty(Number(record.receivedQuantity || 0) - Number(record.expectedQuantity || 0)) }}
              </span>
            </template>
          </BillDetailTable>
        </template>

        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row"><span class="remark-label">单据备注</span><a-input v-model:value="formData.remark" placeholder="备注" :disabled="isLocked" /></div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag></span>
            <span class="doc-info-item">来源类型 <a-tag>{{ sourceTypeText }}</a-tag></span>
            <span class="doc-info-item">经手人 <a-tag color="blue">{{ formData.assigneeName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单人 {{ formData.createBy || currentUserName || '系统' }}</span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- 3Tab 配置弹窗 -->
      <a-modal v-model:open="showFormConfig" title="配置" :width="760" :footer="null" destroy-on-close>
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="pageConfig" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table :columns="pageConfigTableColumns" :data-source="pageConfigFields" :pagination="false" size="small" row-key="key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'"><a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" /></template>
                <template v-if="column.key === 'visible'"><a-checkbox :checked="record.visible" @change="(e: any) => handlePageConfigVisibleChange(record.key, e.target.checked)" /></template>
                <template v-if="column.key === 'enterJump'"><a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" /></template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <p class="config-hint">设置新增单据的默认值</p>
            <a-form layout="vertical" size="small">
              <a-form-item label="默认收货仓库">
                <a-select v-model:value="formData.defaultWarehouseId" allow-clear :options="warehouseOptions" style="width:260px" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认来源类型">
                <a-select v-model:value="formData.defaultSourceType" allow-clear :options="sourceTypeOptions" style="width:260px" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认优先级">
                <a-select v-model:value="formData.defaultPriority" allow-clear :options="priorityOptions" style="width:260px" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="printSettings" tab="打印设置">
            <p class="config-hint">配置打印行为</p>
            <a-form layout="vertical" size="small">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="">默认模板</a-select-option>
                  <a-select-option value="receipt-standard">收货单(标准)</a-select-option>
                  <a-select-option value="receipt-simple">收货单(简版)</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number v-model:value="formData.printCopies" :precision="0" :min="1" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="纸张尺寸">
                <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="A4">A4</a-select-option>
                  <a-select-option value="A5">A5</a-select-option>
                  <a-select-option value="LETTER">Letter</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印选项">
                <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">始终使用最后一次打印的模板，打印时不再选择</a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusCircleOutlined, MinusCircleOutlined, PrinterOutlined, HistoryOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { receiptApi, type WmsReceiptTask, type WmsReceiptDetail } from '@/api/wms/receipt'
import { supplierApi } from '@/api/supplier'
import { locationApi } from '@/api/wms/warehouse'
import { userPageConfigApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { WMS_STATUS_MAP, DETAIL_STATUS_MAP, formatQty, formatTime, genTaskNo } from '../../whTask'

defineOptions({ name: 'WhReceivingOrderForm' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统')
const idParam = Number(route.query.id || route.query.idParam || 0)
const effectiveMode = computed<'create' | 'edit'>(() => (idParam ? 'edit' : 'create'))
const editing = computed(() => effectiveMode.value === 'edit')

// ═══ 状态字典 ═══
const RECEIPT_STATUS_MAP = WMS_STATUS_MAP
const sourceTypeOptions = [
  { label: '采购入库', value: 0 }, { label: '生产入库', value: 1 }, { label: '退货入库', value: 2 },
  { label: '调拨入库', value: 3 }, { label: '其他', value: 4 },
]
const priorityOptions = [
  { label: '普通', value: 0 }, { label: '紧急', value: 1 }, { label: '加急', value: 2 },
]
function statusText(s: number) { return RECEIPT_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return RECEIPT_STATUS_MAP[s]?.color || 'default' }
function sourceTypeText(t: number | undefined) { return sourceTypeOptions.find(o => o.value === t)?.label || '其他' }

function availableActions(status: number) {
  const actions = [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: HistoryOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ]
  if (status === 0) actions.splice(1, 0, { key: 'start', label: '开始收货', icon: undefined as any })
  if (status === 1) actions.splice(1, 0, { key: 'confirm', label: '确认收货', icon: undefined as any })
  if (status === 0 || status === 1) actions.splice(1, 0, { key: 'cancel', label: '取消收货', icon: undefined as any })
  return actions as any[]
}

// ═══ 表单（useBillForm） ═══
function createWithStatus(data: any) {
  const { status, ...payload } = data
  return receiptApi.save({ ...payload, status: 0 })
}
function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  return receiptApi.update({ ...payload, id })
}
async function getById(id: number) {
  const res: any = await receiptApi.getById(id)
  const t: any = res?.data || res
  if (!t) return {}
  return {
    id: t.id, orderNo: t.taskNo, taskNo: t.taskNo, warehouseId: t.warehouseId ?? undefined,
    warehouseName: t.warehouseName || '', sourceType: t.sourceType ?? 0, sourceOrderNo: t.sourceOrderNo || '',
    supplierId: t.supplierId ?? undefined,
    supplierName: t.supplierName || '', expectedTime: t.expectedTime || undefined, priority: t.priority ?? 0,
    remark: t.remark || '', status: t.status ?? 0, assigneeName: t.assigneeName || '', createBy: t.createBy || '',
    createTime: t.createTime || '',
  }
}

// ═══ 供应商主数据选项（待完善：supplierName 自由文本 → 供应商选择器） ═══
const supplierOptions = ref<{ label: string; value: number }[]>([])
async function loadSupplierOptions() {
  try {
    const res: any = await supplierApi.page({ pageNum: 1, pageSize: 500 })
    const list = res?.records || []
    supplierOptions.value = list.map((s: any) => ({ label: s.supplierName, value: s.id }))
  } catch (e) { supplierOptions.value = []; console.warn('[收货单] 供应商选项获取失败', e) }
}

const { formData, loadingOptions, saving, optionRefs, filterOption,
  handleFieldChange: baseFieldChange } = useBillForm({
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById,
  },
  redirectPath: '/wms/receive',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [
    { key: 'warehouseId', label: '收货仓库', type: 'select', required: true },
    { key: 'sourceType', label: '来源类型', type: 'select' },
    { key: 'expectedTime', label: '预计时间', type: 'date' },
    { key: 'priority', label: '优先级', type: 'select' },
  ],
  productDefaults: { productId: undefined, productCode: '', productName: '', productSpec: '', productUnit: '', expectedQuantity: 0, locationId: undefined, locationCode: '', batchNo: '' },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find(x => x.id === val)
      fd.warehouseName = w?.warehouseName || w?.name || ''
      fd.locationId = undefined
      loadLocations(val)
    }
    if (fieldKey === 'supplierId') {
      const s = supplierOptions.value.find(x => x.value === val)
      fd.supplierName = s?.label || ''
    }
  },
  transformPayload: (fd) => ({
    taskNo: fd.taskNo || genTaskNo('RC'),
    sourceType: fd.sourceType ?? 0,
    sourceOrderNo: fd.sourceOrderNo || '',
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName || '',
    supplierId: fd.supplierId ?? undefined,
    supplierName: fd.supplierName || '',
    expectedTime: fd.expectedTime || undefined,
    priority: fd.priority ?? 0,
    remark: fd.remark || '',
    status: 0,
  }),
})

// ═══ 初始化默认值 ═══
if (formData.orderNo === undefined) formData.orderNo = genTaskNo('RC')
if (formData.taskNo === undefined) formData.taskNo = formData.orderNo
if (!formData.sourceType) formData.sourceType = 0
if (formData.priority === undefined) formData.priority = 0
for (const k of ['warehouseName', 'supplierName', 'sourceOrderNo', 'remark']) if (formData[k] === undefined) formData[k] = ''
if (formData.status === undefined) formData.status = 0
if (formData.details === undefined) formData.details = []

const currentStatus = computed(() => Number(formData.status ?? 0))
const isLocked = computed(() => editing.value && currentStatus.value !== 0)

// ═══ 状态化保存（草稿/主按钮均走 save status0） ═══
const buildPayload = () => ({
  taskNo: formData.taskNo || genTaskNo('RC'),
  sourceType: formData.sourceType ?? 0,
  sourceOrderNo: formData.sourceOrderNo || '',
  warehouseId: formData.warehouseId,
  warehouseName: formData.warehouseName || '',
  supplierId: formData.supplierId ?? undefined,
  supplierName: formData.supplierName || '',
  expectedTime: formData.expectedTime || undefined,
  priority: formData.priority ?? 0,
  remark: formData.remark || '',
  status: 0,
})
const doSave = async (isSubmit: boolean) => {
  const payload = buildPayload()
  if (!payload.warehouseId) { message.warning('请选择收货仓库'); return }
  try {
    saving.value = true
    if (editing.value && formData.id) {
      await receiptApi.update({ ...payload, id: formData.id })
      message.success('收货单已更新')
    } else {
      const res: any = await receiptApi.save(payload)
      formData.id = res?.data?.id || res?.id || 0
      message.success('收货单已创建，可录入明细')
    }
    if (isSubmit && formData.id && currentStatus.value === 0) {
      const u = currentUser()
      await receiptApi.startReceipt(formData.id, u.userId, u.userName)
      message.success('收货单已开始')
    }
    await refreshCurrent()
  } catch (e: any) { message.error(e?.message || '保存失败') } finally { saving.value = false }
}
const handleSaveDraftFn = () => doSave(false)
const handlePrimarySubmit = () => doSave(true)

// ═══ 表头字段（字段分组） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '单号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'warehouseId', label: '收货仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'sourceType', label: '来源类型', type: 'select', inlineLabel: true, width: 180 },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'supplierId', label: '供应商', type: 'select', inlineLabel: true, width: 210 },
  { key: 'expectedTime', label: '预计时间', type: 'date', inlineLabel: true, width: 210 },
  { key: 'priority', label: '优先级', type: 'select', inlineLabel: true, width: 140 },
  { key: 'assigneeName', label: '经手人', type: 'display', inlineLabel: true, width: 130 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
]
const DEFAULT_HIDDEN_FIELDS = ['assigneeName', 'createTime']
const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>({})
for (const f of ALL_BASIC_INFO_FIELDS) pageConfig[f.key] = { visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key), enterJump: ['select', 'date', 'number', 'input'].includes(f.type) }

const warehouseOptions = computed(() => (optionRefs.warehouses || []).map(w => ({ label: w.warehouseName || w.name, value: w.id })))
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS.filter(f => pageConfig[f.key]?.visible !== false).map(f => ({
    ...f,
    options: f.key === 'warehouseId' ? warehouseOptions.value
      : f.key === 'sourceType' ? sourceTypeOptions
      : f.key === 'priority' ? priorityOptions
      : f.key === 'supplierId' ? supplierOptions.value
      : (f as any).options,
    loading: (['warehouseId'].includes(f.key)) ? loadingOptions.value : (f as any).loading,
  })))

// ═══ 配置弹窗（页面配置） ═══
const FORM_CONFIG_MODULE = 'receiving-order-form'
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const pageConfigTableColumns: any[] = [
  { title: '字段', dataIndex: 'label', key: 'label', width: 160 },
  { title: '显示名', dataIndex: 'displayName', key: 'displayName', width: 200 },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 80 },
  { title: '回车键跳转', dataIndex: 'enterJump', key: 'enterJump', width: 120 },
]
const pageConfigFields = computed(() => ALL_BASIC_INFO_FIELDS.map(f => ({ key: f.key, label: f.label, displayName: f.label, visible: pageConfig[f.key]?.visible !== false, enterJump: pageConfig[f.key]?.enterJump ?? true })))
function handlePageConfigVisibleChange(key: string, v: boolean) { pageConfig[key].visible = v; saveFormConfig() }
function handlePageConfigEnterJumpChange(key: string, v: boolean) { pageConfig[key].enterJump = v; saveFormConfig() }
async function loadFormConfig() {
  try {
    const res: any = await userPageConfigApi.get(FORM_CONFIG_MODULE, 'form')
    const cfg = res?.data || res
    if (cfg?.fields) for (const k of Object.keys(cfg.fields)) { if (pageConfig[k]) pageConfig[k] = { ...pageConfig[k], ...cfg.fields[k] } }
    if (cfg?.defaults) {
      if (cfg.defaults.warehouseId !== undefined) formData.defaultWarehouseId = cfg.defaults.warehouseId
      if (cfg.defaults.sourceType !== undefined) formData.defaultSourceType = cfg.defaults.sourceType
    }
  } catch (e) { /* 忽略 */ }
}
async function saveFormConfig() {
  const payload = { fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, v])), defaults: { warehouseId: formData.defaultWarehouseId, sourceType: formData.defaultSourceType } }
  try { await userPageConfigApi.save(FORM_CONFIG_MODULE, 'form', payload) } catch (e) { /* 忽略 */ }
}

// ═══ 明细 ═══
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', type: 'rowNo', title: '', fixed: 'left', width: 44 },
  { key: 'action', type: 'action', title: '操作', slotName: 'actionCell', fixed: 'left', width: 60 },
  { key: 'productId', type: 'input', title: '商品名称', width: 220, required: true, showScanToggle: true, searchable: true,
    options: (optionRefs.products || []).map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })) },
  { key: 'productSpec', type: 'input', title: '规格', width: 100, readonly: true },
  { key: 'productUnit', type: 'input', title: '单位', width: 64, readonly: true },
  { key: 'expectedQuantity', type: 'number', title: '应收数量', width: 120, precision: 2, required: true },
  { key: 'locationId', type: 'input', title: '货位', width: 160, searchable: true,
    options: (locationOptions.value || []).map((l: any) => ({ label: l.label || String(l.value), value: l.value })) },
  { key: 'batchNo', type: 'input', title: '批次号', width: 120 },
  { key: 'productionDate', type: 'date', title: '生产日期', width: 150 },
  { key: 'validityDate', type: 'date', title: '有效期', width: 150 },
  { key: 'receivedQuantity', type: 'number', title: '实收数量', width: 110, precision: 2 },
  { key: 'brokenQuantity', type: 'number', title: '破损数量', width: 100, precision: 2, defaultHidden: true },
  // 差异=实收−应收，只读展示（保留 slot 自定义样式；如需原生可改 number readonly）
  { key: 'diffQuantity', type: 'slot', title: '差异', slotName: 'diffQuantityCell', width: 90, readonly: true },
  { key: 'putawayQuantity', type: 'input', title: '已上架', width: 90, readonly: true, defaultHidden: true },
])
const tableSummaryColumns = computed(() => [
  { key: 'expectedQuantity', value: Number(totalQty.value.toFixed(2)), highlight: true },
])

const totalQty = computed(() => (formData.details || []).reduce((s: number, d: any) => s + (Number(d.expectedQuantity) || 0), 0))

const locationOptions = ref<{ label: string; value: number }[]>([])
async function loadLocations(warehouseId: number) {
  locationOptions.value = []
  try {
    const list: any = await locationApi.listByWarehouse(warehouseId)
    locationOptions.value = (Array.isArray(list) ? list : []).map((l: any) => ({ label: `${l.locationCode}${l.locationName ? ' ' + l.locationName : ''}`, value: l.id }))
  } catch (e) { locationOptions.value = []; console.warn('货位获取失败', e) }
}

let rowSeq = 0
function newDetail(): any { return { _key: `n${++rowSeq}`, productId: undefined, productCode: '', productName: '', productSpec: '', productUnit: '', expectedQuantity: undefined, receivedQuantity: undefined, brokenQuantity: undefined, locationId: undefined, locationCode: '', batchNo: '', productionDate: undefined, validityDate: undefined } }
function handleAddDetail() { formData.details.push(newDetail()) }
function handleInsertDetail(index: number) { formData.details.splice(index + 1, 0, newDetail()) }
function handleRemoveDetail(index: number) { formData.details.splice(index, 1) }
function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productId = p.id; record.productCode = p.code || ''; record.productName = p.name || ''; record.productSpec = p.specification || p.spec || ''; record.productUnit = p.unit || ''
    }
  }
  if (fieldKey === 'locationId' && value != null) {
    const l = locationOptions.value.find((x: any) => x.value === value)
    if (l) record.locationCode = (l as any).label?.split(' ')[0] || ''
  }
}

// ═══ 明细加载/保存（与主表分离） ═══
async function loadDetails(taskId: number) {
  try {
    const list: any = await receiptApi.getDetails(taskId)
    formData.details = (Array.isArray(list) ? list : []).map((d: any) => ({ ...d, _key: `s${d.id}` }))
  } catch (e) { formData.details = []
    console.warn('[收货单] 明细获取失败', e) }
}
async function saveDetails() {
  if (!formData.id) { message.warning('请先保存单据'); return }
  if (formData.details.some((r: any) => !r.productId)) { message.warning('明细行请选择商品'); return }
  if (formData.details.some((r: any) => !(Number(r.expectedQuantity) > 0))) { message.warning('明细行请填写大于0的应收数量'); return }
  try {
    const payload: Partial<WmsReceiptDetail>[] = formData.details.map((d: any) => ({
      productId: d.productId, productCode: d.productCode, productName: d.productName,
      productSpec: d.productSpec, productUnit: d.productUnit, expectedQuantity: Number(d.expectedQuantity),
      receivedQuantity: d.receivedQuantity != null ? Number(d.receivedQuantity) : undefined,
      brokenQuantity: d.brokenQuantity != null ? Number(d.brokenQuantity) : undefined,
      locationId: d.locationId, locationCode: d.locationCode, batchNo: d.batchNo || '',
      productionDate: d.productionDate || undefined, validityDate: d.validityDate || undefined,
    }))
    await receiptApi.saveDetails(formData.id, payload)
    message.success('收货明细已保存')
    await loadDetails(formData.id)
  } catch (e: any) { message.error(e?.message || '明细保存失败') }
}

// ═══ header / summary / footer ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '收货单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: availableActions(currentStatus.value).map(a => ({ key: a.key, label: a.label, icon: a.icon })),
}))
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '应收数量', value: totalQty.value, statusLabel: statusText(currentStatus.value) },
  { label: '商品行数', value: (formData.details || []).filter((d: any) => d.productId != null).length, divider: true },
])
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '应收数量',
  amountValue: totalQty.value.toString(),
  amountHighlight: true,
  draftBtnText: editing.value && currentStatus.value !== 0 ? undefined : '保存草稿',
  draftShortcut: editing.value && currentStatus.value !== 0 ? undefined : 'Ctrl+S',
  primaryBtnText: editing.value && currentStatus.value !== 0 ? '保存修改' : '保存单据',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 动作分发 ═══
function currentUser() { return { userId: userStore.userId || 0, userName: currentUserName.value } }
async function doAction(fn: () => Promise<any>, ok: string) {
  try { await fn(); message.success(ok); await refreshCurrent() } catch (e: any) { message.error(e?.message || '操作失败') }
}
function handleAction(key: string) {
  const u = currentUser()
  switch (key) {
    case 'config': showFormConfig.value = true; break
    case 'print': message.info('打印功能待接入打印模板'); break
    case 'history': router.push('/wms/receive'); break
    case 'start': doAction(() => receiptApi.startReceipt(formData.id, u.userId, u.userName), '已开始收货'); break
    case 'confirm': doAction(() => receiptApi.confirmReceipt(formData.id, u.userId, u.userName), '收货已确认'); break
    case 'cancel': doAction(() => receiptApi.cancelReceipt(formData.id, 'PC端取消收货'), '收货单已取消'); break
  }
}
function handleFieldChange(k: string, v: any) { baseFieldChange(k, v) }

async function refreshCurrent() {
  if (!formData.id) return
  try {
    const t: any = await receiptApi.getById(formData.id)
    const d = t?.data || t
    if (d) {
      if (d.taskNo != null) formData.orderNo = d.taskNo
      if (d.status != null) formData.status = d.status
      if (d.assigneeName != null) formData.assigneeName = d.assigneeName
    }
  } catch (e) { console.warn('刷新失败', e) }
}

// ═══ 其他 ═══
function computeMaxHeight() {
}
function handleError(e: any) { console.error(e) }
function formatNow() { return new Date().toISOString().slice(0, 19).replace('T', ' ') }

onMounted(async () => {
  computeMaxHeight()
  await loadFormConfig()
  if (!editing.value) {
    try {
      const noRes: any = await receiptApi.nextNo()
      const no = noRes?.data ?? noRes
      if (no) { formData.orderNo = no; formData.taskNo = no }
    } catch (e) { /* 忽略，沿用前端默认单号 */ }
  }
  loadSupplierOptions()
  loadLocations(formData.warehouseId)
  if (editing.value && idParam) { await loadDetails(idParam) }
  if (!editing.value && formData.details?.length === 0) { formData.details = Array.from({ length: 20 }, () => newDetail()) }
})
</script>

<style scoped>
.gap-alert { margin-bottom: 16px; }
.remark-section { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.remark-row { display: flex; align-items: center; gap: 10px; }
.remark-label { font-size: 13px; color: #666; white-space: nowrap; }
.doc-info-row { display: flex; flex-wrap: wrap; gap: 8px 20px; font-size: 12px; color: #888; }
.doc-info-item { display: inline-flex; align-items: center; gap: 4px; }
.config-hint { color: #999; font-size: 12px; margin-bottom: 12px; }
</style>
