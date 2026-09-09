<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已执行/已完成锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入执行流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 移库明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'move-order-form-columns'"
            :view-mode="isLocked"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct()">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" disabled>
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
            </template>
          </BillDetailTable>
        </template>

        <!-- ═══ 备注额外区 ═══ -->
        <template #bottom-extra>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.assigneeName" class="doc-info-item">执行人 <a-tag color="blue">{{ formData.assigneeName }}</a-tag></span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/录单默认值/打印设置 ═══ -->
      <a-modal v-model:open="showFormConfig" title="配置" :width="760" :footer="null" destroy-on-close>
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="pageConfig" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table :columns="pageConfigTableColumns" :data-source="pageConfigFields" :pagination="false" size="small" row-key="key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'">
                  <a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" />
                </template>
                <template v-if="column.key === 'visible'">
                  <a-checkbox :checked="record.visible" @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)" />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认移库仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.name||w.warehouseName,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认移库类型">
                <a-select v-model:value="formData.defaultMoveType" size="small" style="width:100%" :options="MOVE_TYPE_OPTIONS" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认来源类型">
                <a-select v-model:value="formData.defaultSourceType" size="small" style="width:100%" :options="SOURCE_TYPE_OPTIONS" @change="saveFormConfig" />
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
                <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">始终使用最后一次打印的模板</a-checkbox>
                <a-checkbox v-model:checked="formData.printAfterSubmit" @change="saveFormConfig">保存后立即打印</a-checkbox>
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
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, PrinterOutlined, SettingOutlined,
  PlayCircleOutlined, CheckCircleOutlined, CloseCircleOutlined,
  MinusCircleOutlined, PlusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { moveApi } from '@/api/wms/move'
import { locationApi } from '@/api/wms/warehouse'
import { userPageConfigApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { WMS_STATUS_MAP, MOVE_TYPE_MAP } from '../../whTask'

defineOptions({ name: 'WhMoveOrderForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
// ═══ 字典 ═══
const MOVE_TYPE_OPTIONS = Object.entries(MOVE_TYPE_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))
const SOURCE_TYPE_OPTIONS = [
  { label: '手动创建', value: 0 }, { label: '盘点差异', value: 1 },
  { label: '补货', value: 2 }, { label: '其他', value: 3 },
]
function statusTextOf(s: number) { return WMS_STATUS_MAP[s]?.text || '草稿' }
function statusColorOf(s: number) { return WMS_STATUS_MAP[s]?.color || 'default' }

function currentOperator() {
  return {
    id: userStore?.userId || userStore?.userInfo?.id || 0,
    name: currentUserName.value || '系统',
  }
}

// ═══ 货位加载（按仓库） ═══
const locationOptions = ref<{ label: string; value: number }[]>([])
const locationLoading = ref(false)
async function loadLocationsByWarehouse(warehouseId?: number) {
  if (!warehouseId) { locationOptions.value = []; return }
  locationLoading.value = true
  try {
    const list: any = await locationApi.listByWarehouse(warehouseId)
    locationOptions.value = (Array.isArray(list) ? list : []).map((l: any) => ({ label: `${l.locationCode}${l.locationName ? ' ' + l.locationName : ''}`, value: l.id, code: l.locationCode }))
  } catch { locationOptions.value = [] }
  finally { locationLoading.value = false }
}

// ═══ useBillForm：保存主表 + 明细（整体替换） ═══
async function createWithStatus(data: any) {
  const { status, items, ...payload } = data
  const res: any = await moveApi.save({ ...payload, status: 0 })
  const created = res?.data || res
  if (created?.id && Array.isArray(items) && items.length) {
    await moveApi.saveDetails(created.id, items)
  }
  return created
}
async function updateWithStatus(id: number, data: any) {
  const { status, items, ...payload } = data
  const res: any = await moveApi.update({ ...payload, id })
  if (items !== undefined) await moveApi.saveDetails(id, items)
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
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
} = useBillForm({
  billPrefix: 'MV',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await moveApi.getById(id)
      const data = res?.data || res || {}
      const items: any = await moveApi.getDetails(id).catch(() => [])
      return {
        ...data,
        orderNo: data.taskNo || '',
        moveType: data.moveType ?? 1,
        sourceType: data.sourceType ?? 0,
        warehouseId: data.warehouseId,
        warehouseName: data.warehouseName || '',
        fromLocationId: data.fromLocationId ?? undefined,
        fromLocationCode: data.fromLocationCode || '',
        toLocationId: data.toLocationId ?? undefined,
        toLocationCode: data.toLocationCode || '',
        remark: data.remark || '',
        items: (Array.isArray(items) ? items : []).map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          productCode: d.productCode || '',
          productName: d.productName || '',
          productSpec: d.productSpec || '',
          productUnit: d.productUnit || '',
          quantity: d.quantity ?? 0,
          fromLocationId: d.fromLocationId ?? undefined,
          fromLocationCode: d.fromLocationCode || '',
          toLocationId: d.toLocationId ?? undefined,
          toLocationCode: d.toLocationCode || '',
          batchNo: d.batchNo || '',
          serialNo: d.serialNo || '',
        })),
      }
    },
  },
  redirectPath: '/wms/move',
  codeApiPath: '/wms/move/next-no',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [
    { key: 'warehouseId', label: '移库仓库', type: 'select', required: true },
  ],
  productDefaults: {
    productCode: '', productName: '', productSpec: '', productUnit: '',
    quantity: 0, fromLocationId: undefined, fromLocationCode: '',
    toLocationId: undefined, toLocationCode: '', batchNo: '', serialNo: '', remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.warehouseName || w?.name || ''
      fd.fromLocationId = undefined; fd.fromLocationCode = ''
      fd.toLocationId = undefined; fd.toLocationCode = ''
      loadLocationsByWarehouse(fd.warehouseId)
    }
    if (fieldKey === 'fromLocationId') {
      const l = locationOptions.value.find((x: any) => x.value === val)
      fd.fromLocationCode = l?.code || ''
    }
    if (fieldKey === 'toLocationId') {
      const l = locationOptions.value.find((x: any) => x.value === val)
      fd.toLocationCode = l?.code || ''
    }
  },
  transformPayload: (fd, _status) => ({
    taskNo: fd.orderNo,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    fromLocationId: fd.fromLocationId ?? undefined,
    fromLocationCode: fd.fromLocationCode || '',
    toLocationId: fd.toLocationId ?? undefined,
    toLocationCode: fd.toLocationCode || '',
    moveType: fd.moveType ?? 1,
    sourceType: fd.sourceType ?? 0,
    sourceNo: fd.sourceNo || '',
    remark: fd.remark || '',
    items: (fd.products || []).filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.productCode || '',
      productName: p.productName || '',
      productSpec: p.productSpec || '',
      productUnit: p.productUnit || '',
      quantity: p.quantity,
      fromLocationId: p.fromLocationId ?? undefined,
      fromLocationCode: p.fromLocationCode || '',
      toLocationId: p.toLocationId ?? undefined,
      toLocationCode: p.toLocationCode || '',
      batchNo: p.batchNo || '',
      serialNo: p.serialNo || '',
    })),
  }),
})

// 初始化移库单默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of ['warehouseName', 'fromLocationId', 'fromLocationCode', 'toLocationId', 'toLocationCode', 'sourceNo', 'remark', 'createTime', 'creatorName', 'assigneeName', 'printTemplate', 'printPaperSize', 'defaultWarehouseId', 'defaultMoveType', 'defaultSourceType']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) { if (formData[key] === undefined) formData[key] = 0 }
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) { if (formData[key] === undefined) formData[key] = false }
if (formData.moveType === undefined) formData.moveType = 1
if (formData.sourceType === undefined) formData.sourceType = 0
if (formData.status === undefined) formData.status = 0

// ═══ 状态 / 锁定 ═══
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => statusTextOf(currentStatus.value))
const statusColor = computed(() => statusColorOf(currentStatus.value))
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => {
  const actions: any[] = [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ]
  if (currentStatus.value === 0 && formData.id) actions.push({ key: 'start', label: '开始移库', icon: PlayCircleOutlined })
  if (currentStatus.value === 1) actions.push({ key: 'execute', label: '执行移库', icon: CheckCircleOutlined })
  if (currentStatus.value === 0 || currentStatus.value === 1) actions.push({ key: 'cancel', label: '取消', icon: CloseCircleOutlined })
  return { title: '移库单', orderNo: formData.orderNo, showAttachment: true, actions }
})

// ═══ 基本信息字段 ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'warehouseId', label: '移库仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'fromLocationId', label: '来源货位', type: 'select', inlineLabel: true, width: 180, searchBtn: '+Q', loading: true },
  { key: 'toLocationId', label: '目标货位', type: 'select', inlineLabel: true, width: 180, searchBtn: '+Q', loading: true },
  { key: 'moveType', label: '移库类型', type: 'select', inlineLabel: true, width: 140 },
  { key: 'sourceType', label: '来源类型', type: 'select', inlineLabel: true, width: 140 },
  { key: 'sourceNo', label: '来源单号', type: 'input', inlineLabel: true, width: 180 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 260 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
]

const DEFAULT_HIDDEN_FIELDS = ['sourceNo', 'creatorName', 'createTime']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'move-order-form'
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
        defaultMoveType: parsed.defaults.defaultMoveType,
        defaultSourceType: parsed.defaults.defaultSourceType,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* 保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultWarehouseId: formData.defaultWarehouseId,
      defaultMoveType: formData.defaultMoveType,
      defaultSourceType: formData.defaultSourceType,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try { await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload)) } catch { /* 静默 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'warehouseId'
        ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
        : f.key === 'fromLocationId' || f.key === 'toLocationId'
          ? locationOptions.value
          : f.key === 'moveType'
            ? MOVE_TYPE_OPTIONS
            : f.key === 'sourceType'
              ? SOURCE_TYPE_OPTIONS
              : (f as any).options,
      loading: ['warehouseId', 'fromLocationId', 'toLocationId'].includes(f.key)
        ? (f.key === 'warehouseId' ? loadingOptions.value : locationLoading.value)
        : (f as any).loading,
      searchBtn: ['warehouseId', 'fromLocationId', 'toLocationId'].includes(f.key) ? '+Q' : (f as any).searchBtn,
    }))
)

const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map((f, i) => ({
    key: f.key, index: i + 1, name: f.label, displayName: f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump ?? false,
  }))
)

const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 摘要 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '移库数量', value: totalQuantity.value, statusLabel: statusText.value },
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单数量',
  amountValue: `${totalQuantity.value}`,
  draftBtnText: isLocked.value ? undefined : '保存单据',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? undefined : '保存单据',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 明细表格列 ═══
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: (optionRefs.products || []).map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })),
    width: 220 },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'productSpec', title: '规格', type: 'input', width: 100 },
  { key: 'productUnit', title: '单位', type: 'input', width: 70 },
  { key: 'quantity', title: '移库数量', type: 'number', width: 110, precision: 2 },
  { key: 'fromLocationId', title: '来源货位', type: 'input', searchable: true,
    options: (locationOptions.value || []).map((l: any) => ({ label: l.label || String(l.value), value: l.value })),
    width: 150 },
  { key: 'toLocationId', title: '目标货位', type: 'input', searchable: true,
    options: (locationOptions.value || []).map((l: any) => ({ label: l.label || String(l.value), value: l.value })),
    width: 150 },
  { key: 'batchNo', title: '批次号', type: 'input', width: 110 },
  { key: 'serialNo', title: '序列号', type: 'input', width: 110 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
])

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ═══ 事件处理 ═══
/** 补货移库（moveType=2）按商品+仓库推荐目标货位，回填明细行 toLocationId（货位推荐） */
async function recommendTargetLocation(row?: any) {
  if (Number(formData.moveType ?? 1) !== 2) return
  if (!formData.warehouseId || !row?.productId) return
  try {
    const list: any = await locationApi.recommend({
      warehouseId: formData.warehouseId, productId: row.productId, quantity: row.quantity,
    })
    const arr = Array.isArray(list) ? list : (list?.data || [])
    const target = Array.isArray(arr) ? arr[0] : undefined
    if (target) {
      row.toLocationId = target.id
      row.toLocationCode = target.locationCode || ''
    }
  } catch { /* 静默，可手填目标货位 */ }
}
function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}
function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.productSpec = p.specification || ''
      record.productUnit = p.unit || ''
    }
    recommendTargetLocation(record)
  }
  if (fieldKey === 'fromLocationId' && value != null) {
    const l = locationOptions.value.find((x: any) => x.value === value)
    if (l) record.fromLocationCode = l.code || ''
  }
  if (fieldKey === 'toLocationId' && value != null) {
    const l = locationOptions.value.find((x: any) => x.value === value)
    if (l) record.toLocationCode = l.code || ''
  }
}
function handleFieldChange(fieldKey: string, val: any) { baseFieldChange(fieldKey, val) }
function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

async function handlePrimarySubmit() {
  if (isLocked.value) return
  handleSubmit()
}

// ═══ 工作流 ═══
async function reloadDetail() {
  if (!formData.id) return
  try {
    const res: any = await moveApi.getById(formData.id)
    const data = res?.data || res
    if (data) {
      Object.assign(formData, { ...data, orderNo: data.taskNo || formData.orderNo, status: data.status })
    }
  } catch { /* 静默 */ }
}
async function handleAction(actionKey: string) {
  const u = currentOperator()
  switch (actionKey) {
    case 'history': router.push('/wms/move'); break
    case 'config': showFormConfig.value = true; break
    case 'print':
      if (formData.orderNo) window.print()
      else message.warning('请先保存单据后再打印')
      break
    case 'start':
      if (!formData.id) { message.warning('请先保存单据'); return }
      await moveApi.startMove(formData.id, u.id, u.name)
      message.success('已开始移库')
      await reloadDetail()
      break
    case 'execute':
      if (!formData.id) { message.warning('请先保存单据'); return }
      await moveApi.executeMove(formData.id, u.id, u.name)
      message.success('移库已执行')
      await reloadDetail()
      break
    case 'cancel':
      if (!formData.id) { message.warning('请先保存单据'); return }
      await moveApi.cancelMove(formData.id, 'PC端取消')
      message.success('移库单已取消')
      await reloadDetail()
      break
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function handleError(err: any) { console.warn('[移库单] ErrorBoundary:', err) }

onMounted(async () => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
    if (formData.defaultWarehouseId) {
      formData.warehouseId = formData.defaultWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
      if (w) formData.warehouseName = w.warehouseName || w.name || ''
      loadLocationsByWarehouse(formData.warehouseId)
    }
  }
  if (formData.defaultMoveType) formData.moveType = formData.defaultMoveType
  if (formData.defaultSourceType !== undefined) formData.sourceType = formData.defaultSourceType
  nextTick(() => {  loadFormConfig()
})
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
</style>
