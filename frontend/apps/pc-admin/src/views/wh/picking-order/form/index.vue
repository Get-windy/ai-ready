<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 状态锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，进入拣货流程后内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 拣货明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'picking-order-form-columns'"
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

        <!-- ═══ 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">备注</span>
              <a-input
                v-model:value="formData.remark"
                size="small"
                class="remark-input"
                placeholder="请输入备注"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              经手人 <a-tag color="blue">{{ formData.assigneeName || currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/录单默认值/打印设置 ═══ -->
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
                <template v-if="column.key === 'displayName'">
                  <a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" />
                </template>
                <template v-if="column.key === 'visible'">
                  <a-checkbox
                    :checked="record.visible"
                    @change="(e: any) => handleConfigFieldChange(record.key, 'visible', e.target.checked)"
                  />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.warehouseName||w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认来源类型">
                <a-select v-model:value="formData.defaultSourceType" size="small" style="width:100%" :options="PICK_SOURCE_TYPE_OPTIONS" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认优先级">
                <a-select v-model:value="formData.defaultPriority" size="small" style="width:100%" :options="priorityOptions" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="">默认模板</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number v-model:value="formData.printCopies" size="small" :min="1" style="width:100%" @change="saveFormConfig" />
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
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusCircleOutlined, MinusCircleOutlined, PrinterOutlined, ImportOutlined,
  DownloadOutlined, ClockCircleOutlined, SettingOutlined, PlayCircleOutlined,
  CarryOutOutlined, CloseCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { BillHeaderConfig, BasicInfoField } from '@/components/BillFormPage/types'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { pickApi, type WmsPickTask, type WmsPickDetail } from '@/api/wms/pick'
import { userPageConfigApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { PICK_STATUS_MAP, PICK_SOURCE_TYPE_OPTIONS, pickSourceTypeText, formatQty } from '../../whTask'

defineOptions({ name: 'WhPickingOrderForm' })

const router = useRouter()
const userStore = useUserStore()

// ═══ 状态映射 ═══
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => PICK_STATUS_MAP[currentStatus.value]?.text || '未知')
const statusColor = computed(() => PICK_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

const priorityOptions = [
  { label: '普通', value: 0 },
  { label: '紧急', value: 1 },
  { label: '加急', value: 2 },
]

// ═══ useBillForm ═══
const {
  formData, loadingOptions, optionRefs, filterOption, effectiveMode,
  handleAddProduct, handleRemoveProduct, handleProductChange, handleFieldChange,
  handleSaveDraft, totalQuantity,
} = useBillForm({
  billPrefix: 'PK',
  codeApiPath: '/wms/pick/next-no',
  redirectPath: '/wms/pick',
  optionTypes: ['warehouses', 'users', 'products'],
  api: {
    create: async (data: any) => {
      const { items, ...task } = data
      const res: any = await pickApi.taskSave({ status: 0, ...task })
      const id = res?.data?.id || res?.id
      if (id) await pickApi.saveDetails(id, items || [])
      return res
    },
    update: async (id: number, data: any) => {
      const { items, ...task } = data
      const res = await pickApi.taskUpdate({ id, status: 0, ...task })
      if (items) await pickApi.saveDetails(id, items)
      return res
    },
    getById: async (id: number) => {
      const task: any = await pickApi.taskGetById(id)
      const t = (task as any)?.data ?? task
      const details: any = await pickApi.detailList(id)
      t.items = Array.isArray(details) ? details : []
      return t
    },
  },
  fields: [
    { key: 'warehouseId', label: '拣货仓库', type: 'select', required: true, optionsRef: 'warehouses' },
    { key: 'sourceType', label: '来源类型', type: 'select' },
    { key: 'customerName', label: '客户', type: 'input' },
  ],
  productDefaults: {
    productCode: '', productName: '', productSpec: '', productUnit: '',
    locationCode: '', batchNo: '', serialNo: '', expectedQuantity: 0,
    pickedQuantity: 0, shortageQuantity: 0, status: 0,
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.warehouseName || w?.name || ''
    }
  },
  transformPayload: (fd: any) => ({
    taskNo: fd.orderNo,
    sourceType: Number(fd.sourceType ?? 0),
    sourceOrderNo: fd.sourceOrderNo || undefined,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    customerName: fd.customerName || undefined,
    expectShipTime: fd.expectShipTime || undefined,
    priority: Number(fd.priority ?? 0),
    remark: fd.remark || undefined,
    totalQuantity: totalQuantity.value,
    items: (fd.products || []).filter((p: any) => p.productId != null && Number(p.expectedQuantity) > 0).map((p: any) => ({
      productId: p.productId,
      productCode: p.productCode || '',
      productName: p.productName || '',
      productSpec: p.productSpec || '',
      productUnit: p.productUnit || '',
      locationId: p.locationId,
      locationCode: p.locationCode || '',
      expectedQuantity: Number(p.expectedQuantity) || 0,
      pickedQuantity: Number(p.pickedQuantity) || 0,
      shortageQuantity: Number(p.shortageQuantity) || 0,
      batchNo: p.batchNo || '',
      serialNo: p.serialNo || '',
      status: Number(p.status ?? 0),
    }) as WmsPickDetail[]),
  }),
})

// 初始化默认值
for (const key of [
  'sourceOrderNo', 'customerName', 'warehouseName', 'remark', 'expectShipTime', 'createTime', 'creatorName',
  'printCount', 'printTemplate', 'assigneeName', 'assigneeId',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
if (formData.status === undefined) formData.status = 0
if (formData.sourceType === undefined) formData.sourceType = 0
if (formData.priority === undefined) formData.priority = 0
if (formData.orderNo === undefined) formData.orderNo = ''

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '拣货单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    ...(currentStatus.value === 0 ? [{ key: 'start', label: '开始拣货', icon: PlayCircleOutlined }] : []),
    ...(currentStatus.value === 1 ? [{ key: 'complete', label: '完成拣货', icon: CarryOutOutlined }] : []),
    ...(currentStatus.value === 0 || currentStatus.value === 1 ? [{ key: 'cancel', label: '取消', icon: CloseCircleOutlined }] : []),
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'export', label: '导出', icon: DownloadOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段 ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '单号', type: 'input', inlineLabel: true, width: 190, disabled: true },
  { key: 'warehouseId', label: '拣货仓库', type: 'select', required: true, inlineLabel: true, width: 190, searchBtn: '+Q', loading: true },
  { key: 'sourceType', label: '来源类型', type: 'select', inlineLabel: true, width: 150 },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', inlineLabel: true, width: 180 },
  { key: 'customerName', label: '客户', type: 'input', inlineLabel: true, width: 180 },
  { key: 'expectShipTime', label: '期望发货', type: 'date', inlineLabel: true, width: 190 },
  { key: 'priority', label: '优先级', type: 'select', inlineLabel: true, width: 120 },
  { key: 'remark', label: '备注', type: 'input', inlineLabel: true, width: 240 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 190, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
]

const DEFAULT_HIDDEN_FIELDS = ['remark', 'creatorName', 'createTime', 'printCount']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

function fieldOptions(key: string): any[] | undefined {
  if (key === 'warehouseId') return (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name || '', value: w.id }))
  if (key === 'sourceType') return PICK_SOURCE_TYPE_OPTIONS
  if (key === 'priority') return priorityOptions
  return undefined
}

const basicInfoFields = computed(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: fieldOptions(f.key),
      loading: f.key === 'warehouseId' ? true : !!f.loading,
      disabled: f.key === 'warehouseId' && isLocked.value ? true : f.disabled,
    }))
)

// 明细列（选择商品/规格/单位/货位/应拣/批次/序列号）
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'productId', title: '商品', type: 'input', searchable: true,
    options: (optionRefs.products || []).map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })),
    width: 230 },
  { key: 'productCode', title: '商品编码', type: 'input', width: 110 },
  { key: 'productSpec', title: '规格', type: 'input', width: 100 },
  { key: 'productUnit', title: '单位', type: 'input', width: 70 },
  { key: 'locationCode', title: '拣货货位', type: 'input', width: 120 },
  { key: 'expectedQuantity', title: '应拣数量', type: 'number', width: 110, precision: 2 },
  { key: 'pickedQuantity', title: '已拣数量', type: 'number', width: 100, precision: 2, readonly: true, defaultHidden: true },
  { key: 'shortageQuantity', title: '缺货数量', type: 'number', width: 100, precision: 2, readonly: true, defaultHidden: true },
  { key: 'batchNo', title: '批次号', type: 'input', width: 120 },
  { key: 'serialNo', title: '序列号', type: 'input', width: 120, defaultHidden: true },
  { key: 'status', title: '状态', type: 'input', width: 90, readonly: true, defaultHidden: true },
])

const tableSummaryColumns = computed(() => [
  { key: 'expectedQuantity', value: totalQuantity.value, highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ═══ 配置弹窗 ═══
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const pageConfigTableColumns = [
  { title: '字段', key: 'displayName', width: 200 },
  { title: '显示', key: 'visible', width: 80 },
  { title: '回车跳转', key: 'enterJump', width: 90 },
]
const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map(f => ({
    key: f.key,
    displayName: pageConfig[f.key]?.displayName || f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: !!pageConfig[f.key]?.enterJump,
  }))
)

const FORM_CONFIG_MODULE = 'picking-order-form'
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
      const nd: Record<string, any> = {}
      if (parsed.defaults.defaultWarehouseId) nd.defaultWarehouseId = parsed.defaults.defaultWarehouseId
      if (parsed.defaults.defaultSourceType !== undefined) nd.defaultSourceType = parsed.defaults.defaultSourceType
      if (parsed.defaults.defaultPriority !== undefined) nd.defaultPriority = parsed.defaults.defaultPriority
      if (parsed.defaults.printTemplate) nd.printTemplate = parsed.defaults.printTemplate
      if (parsed.defaults.printCopies) nd.printCopies = parsed.defaults.printCopies
      Object.assign(formData, nd)
    }
  } catch { /* 保持默认 */ }
}

function handleConfigFieldChange(key: string, fieldKey: string, _val: boolean) {
  // visible 通过 table bodyCell 直接改 record.visible，保存后持久化
  if (pageConfig[key]) pageConfig[key].visible = pageConfigFields.value.find(f => f.key === key)?.visible !== false
  saveFormConfig()
}

async function saveFormConfig() {
  const defaults: Record<string, any> = {}
  if (formData.defaultWarehouseId) defaults.defaultWarehouseId = formData.defaultWarehouseId
  if (formData.defaultSourceType !== undefined) defaults.defaultSourceType = formData.defaultSourceType
  if (formData.defaultPriority !== undefined) defaults.defaultPriority = formData.defaultPriority
  if (formData.printTemplate) defaults.printTemplate = formData.printTemplate
  if (formData.printCopies) defaults.printCopies = formData.printCopies
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify({
      fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
      defaults,
    }))
  } catch { /* 静默 */ }
}

// ═══ 事件处理 ═══
function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.productSpec = p.specification || p.spec || ''
      record.productUnit = p.unit || ''
    }
  }
  if (fieldKey === 'locationCode') {
    const loc = listLocations().find((l: any) => l.locationCode === value || String(l.id) === String(value))
    record.locationId = loc?.id || undefined
  }
}

function listLocations(): any[] {
  return (formData as any)._locations || []
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

function handlePrimarySubmit() {
  if (isLocked.value) return
  handleSaveDraft()
}

function formatQtyOrDash(val: number) { return formatQty(val) }

async function handleStart() {
  const u = { id: userStore?.userId || 0, name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统' }
  try {
    await pickApi.taskStart(formData.id, u.id, u.name)
    message.success('已开始拣货')
    reloadDetail(formData.id!)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}
async function handleComplete() {
  try {
    await pickApi.taskComplete(formData.id)
    message.success('拣货已完成')
    reloadDetail(formData.id!)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}
async function handleCancel() {
  try {
    await pickApi.taskCancel(formData.id, 'PC端取消拣货')
    message.success('拣货单已取消')
    reloadDetail(formData.id!)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/wms/pick')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      window.print()
      break
    case 'import':
      handleImport()
      break
    case 'export':
      handleExport()
      break
    case 'start':
      await handleStart()
      break
    case 'complete':
      await handleComplete()
      break
    case 'cancel':
      await handleCancel()
      break
  }
}

function handleImport() {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.accept = '.csv,.txt'
  fileInput.onchange = () => {
    const file = fileInput.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (e: any) => {
      const text = String(e.target?.result || '')
      const lines = text.split(/\r?\n/).map((l: string) => l.trim()).filter(Boolean)
      if (lines.length <= 1) { message.warning('未解析到有效明细数据'); return }
      const header = lines[0].split(/[,\t]/).map((h: string) => h.trim())
      const nameIdx = header.findIndex((h) => h.includes('商品'))
      const qtyIdx = header.findIndex((h) => h.includes('数量'))
      if (nameIdx < 0) { message.warning('导入文件需包含「商品」列'); return }
      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(/[,\t]/).map((c: string) => c.trim())
        const row: any = {
          id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
          productId: undefined,
          productName: cols[nameIdx] || '',
          expectedQuantity: qtyIdx >= 0 ? Number(cols[qtyIdx] || 0) : 1,
        }
        formData.products.push({ ...row, ...productDefaultsForImport() })
      }
      message.success(`已导入 ${lines.length - 1} 行`)
    }
    reader.readAsText(file)
  }
  fileInput.click()
}
function productDefaultsForImport() {
  return { productCode: '', productSpec: '', productUnit: '', locationCode: '', batchNo: '', serialNo: '', pickedQuantity: 0, shortageQuantity: 0, status: 0 }
}
function handleExport() {
  if (!formData.products.length) { message.warning('没有可导出的明细'); return }
  const headers = ['商品名称', '商品编码', '规格', '单位', '货位', '应拣数量', '批次号']
  const rows = formData.products.filter((p: any) => p.productName).map((p: any) => [
    p.productName, p.productCode || '', p.productSpec || '', p.productUnit || '', p.locationCode || '',
    p.expectedQuantity || 0, p.batchNo || '',
  ])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `拣货单明细_${formData.orderNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

async function reloadDetail(id: number) {
  try {
    const task: any = await pickApi.taskGetById(id)
    const t = (task as any)?.data ?? task
    if (t) {
      Object.assign(formData, { ...t, status: t.status })
    }
  } catch { /* silent */ }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统')

function handleError(err: any) { console.warn('[拣货单] ErrorBoundary:', err) }

// ═══ 生命周期 ═══
onMounted(async () => {
  await loadFormConfig()
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
    if (formData.defaultWarehouseId) {
      formData.warehouseId = formData.defaultWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
      if (w) formData.warehouseName = w.warehouseName || w.name || ''
    }
    if (formData.defaultSourceType !== undefined) formData.sourceType = formData.defaultSourceType
    if (formData.defaultPriority !== undefined) formData.priority = formData.defaultPriority
  }
})
</script>

<style scoped>
.config-hint { color: #999; font-size: 12px; margin-bottom: 12px; }
.remark-section { display: flex; flex-direction: column; gap: 8px; padding-top: 8px; }
.remark-row { display: flex; align-items: center; gap: 8px; }
.remark-label { width: 60px; color: rgba(0,0,0,.65); font-size: 13px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 12px; padding-top: 12px; border-top: 1px dashed #f0f0f0; }
.doc-info-item { color: rgba(0,0,0,.65); font-size: 13px; }
</style>
