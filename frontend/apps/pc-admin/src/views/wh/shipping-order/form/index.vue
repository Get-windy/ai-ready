<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已执行/已完成锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入发货流程，内容不可修改`"
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
        @search="handleSearch"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 发货明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <!-- 扫码复核（PC 端复核入口，复用 shipApi.scanItem） -->
          <div v-if="formData.id" class="scan-row">
            <a-input
              v-model:value="scanCode"
              placeholder="扫码/输入商品编码或序列号"
              style="width: 260px"
              size="small"
              @press-enter="handleScan"
            />
            <a-input-number v-model:value="scanQty" :min="1" :max="99999" size="small" style="width: 100px" placeholder="数量" />
            <a-button type="primary" size="small" :loading="acting" @click="handleScan">复核扫描</a-button>
            <span class="scan-hint">扫码累加已扫描数</span>
          </div>

          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'shipping-order-form-columns'"
            :view-mode="isLocked"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <a-space :size="2">
                <a-button v-if="!empty" type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)"><PlusCircleOutlined /></a-button>
                <a-button v-if="!empty" type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)"><MinusCircleOutlined /></a-button>
                <a-button v-else type="link" size="small" class="action-add-btn" @click="handleAddProduct()"><PlusCircleOutlined /></a-button>
              </a-space>
            </template>
          </BillDetailTable>
        </template>

        <!-- ═══ 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row"><span class="remark-label">单据备注</span><a-input v-model:value="formData.remark" placeholder="备注" :disabled="isLocked" /></div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag></span>
            <span class="doc-info-item">客户 <a-tag color="blue">{{ formData.customerName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">经手人 <a-tag color="blue">{{ formData.assigneeName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
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
                <template v-if="column.key === 'displayName'"><a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" /></template>
                <template v-if="column.key === 'visible'"><a-checkbox :checked="record.visible" @change="(e: any) => handlePageConfigVisibleChange(record.key, e.target.checked)" /></template>
                <template v-if="column.key === 'enterJump'"><a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" /></template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认发货仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.warehouseName||w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认承运商">
                <a-input v-model:value="formData.defaultCarrierName" size="small" style="width:100%" @blur="saveFormConfig" />
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
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  MinusCircleOutlined, PlusCircleOutlined, PrinterOutlined, ClockCircleOutlined, SettingOutlined,
  PlayCircleOutlined, CheckCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { shipApi } from '@/api/wms/ship'
import { locationApi } from '@/api/wms/warehouse'
import { mdCustomerApi } from '@/api/erp/md-customer'
import { pickApi } from '@/api/wms/pick'
import { userPageConfigApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { WMS_STATUS_MAP, genTaskNo } from '../../whTask'

defineOptions({ name: 'WhShippingOrderForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统')
// ═══ 状态映射 ═══
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => WMS_STATUS_MAP[currentStatus.value]?.text || '未知')
const statusColor = computed(() => WMS_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// ═══ 客户/承运商/拣货任务 远程搜索选项 ═══
const customerOptions = ref<{ label: string; value: number; name: string }[]>([])
const customerLoading = ref(false)
const carrierOptions = ref<{ label: string; value: string }[]>([])
const carrierLoading = ref(false)
const pickTaskOptions = ref<{ label: string; value: number }[]>([])
const pickTaskLoading = ref(false)

async function handleCustomerSearch(keyword: string) {
  customerLoading.value = true
  try {
    const list: any = await mdCustomerApi.search(keyword || '', 'customer')
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.partnerName, value: c.id, name: c.partnerName }))
  } catch (e) { customerOptions.value = []; console.warn('[发货单] 客户搜索失败', e) }
  finally { customerLoading.value = false }
}
async function handleCarrierSearch(keyword: string) {
  carrierLoading.value = true
  try {
    const list: any = await mdCustomerApi.search(keyword || '', 'logistics')
    carrierOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.partnerName, value: c.partnerName }))
  } catch (e) { carrierOptions.value = []; console.warn('[发货单] 承运商搜索失败', e) }
  finally { carrierLoading.value = false }
}
async function handlePickTaskSearch(keyword: string) {
  pickTaskLoading.value = true
  try {
    const res: any = await pickApi.taskPage({ keyword: keyword || '', pageNum: 1, pageSize: 20 })
    const list = res?.records || res?.data?.records || (Array.isArray(res) ? res : [])
    pickTaskOptions.value = (Array.isArray(list) ? list : []).map((t: any) => ({ label: `${t.taskNo}${t.customerName ? ' ' + t.customerName : ''}`, value: t.id }))
  } catch (e) { pickTaskOptions.value = []; console.warn('[发货单] 拣货任务搜索失败', e) }
  finally { pickTaskLoading.value = false }
}

function handleSearch(fieldKey: string, keyword: string) {
  if (fieldKey === 'pickTaskId') handlePickTaskSearch(keyword)
  else if (fieldKey === 'customerId') handleCustomerSearch(keyword)
  else if (fieldKey === 'carrierName') handleCarrierSearch(keyword)
}

// ═══ 货位加载（按仓库） ═══
const locationOptions = ref<{ label: string; value: number }[]>([])
const locationLoading = ref(false)
async function loadLocations(warehouseId?: number) {
  if (!warehouseId) { locationOptions.value = []; return }
  locationLoading.value = true
  try {
    const list: any = await locationApi.listByWarehouse(warehouseId)
    locationOptions.value = (Array.isArray(list) ? list : []).map((l: any) => ({ label: `${l.locationCode}${l.locationName ? ' ' + l.locationName : ''}`, value: l.id, code: l.locationCode }))
  } catch (e) { locationOptions.value = [] }
  finally { locationLoading.value = false }
}

// ═══ useBillForm：保存主表 + 明细（整体替换） ═══
async function createWithStatus(data: any) {
  const { status, items, ...payload } = data
  const res: any = await shipApi.save({ ...payload, status: 0 })
  const created = res?.data || res || {}
  if (created?.id && Array.isArray(items) && items.length) {
    await shipApi.saveDetails(created.id, items)
  }
  return created
}
async function updateWithStatus(id: number, data: any) {
  const { status, items, ...payload } = data
  const res: any = await shipApi.update({ ...payload, id })
  if (items !== undefined) await shipApi.saveDetails(id, items)
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
  billPrefix: 'SH',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await shipApi.getById(id)
      const data = res?.data || res || {}
      const details: any = await shipApi.getDetails(id).catch(() => [])
      return {
        ...data,
        orderNo: data.taskNo || '',
        taskNo: data.taskNo || '',
        warehouseId: data.warehouseId,
        warehouseName: data.warehouseName || '',
        pickTaskId: data.pickTaskId ?? undefined,
        sourceOrderId: data.sourceOrderId ?? undefined,
        sourceOrderNo: data.sourceOrderNo || '',
        customerId: data.customerId ?? undefined,
        customerName: data.customerName || '',
        carrierName: data.carrierName || '',
        trackingNo: data.trackingNo || '',
        remark: data.remark || '',
        assigneeName: data.assigneeName || '',
        creatorName: data.creatorName || '',
        createTime: data.createTime || '',
        products: (Array.isArray(details) ? details : []).map((d: any, i: number) => ({
          id: d.id || `detail-${i}`,
          productId: d.productId,
          productCode: d.productCode || '',
          productName: d.productName || '',
          productSpec: d.productSpec || '',
          productUnit: d.productUnit || '',
          quantity: d.expectedQuantity ?? 0,
          locationId: d.locationId ?? undefined,
          locationCode: d.locationCode || '',
          batchNo: d.batchNo || '',
          serialNo: d.serialNo || '',
          status: d.status ?? 0,
        })),
      }
    },
  },
  redirectPath: '/wms/ship',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [
    { key: 'warehouseId', label: '发货仓库', type: 'select', required: true },
  ],
  productDefaults: {
    productCode: '', productName: '', productSpec: '', productUnit: '',
    quantity: 0, locationId: undefined, locationCode: '', batchNo: '', serialNo: '', status: 0,
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.warehouseName || w?.name || ''
      fd.sourceOrderNo = fd.sourceOrderNo || ''
      fd.products.forEach((d: any) => { d.locationId = undefined; d.locationCode = '' })
      loadLocations(fd.warehouseId)
    }
    if (fieldKey === 'pickTaskId') {
      handlePickTaskChange(val, fd)
    }
    if (fieldKey === 'customerId') {
      const c = customerOptions.value.find((x: any) => x.value === val)
      fd.customerName = c?.name || ''
    }
    if (fieldKey === 'carrierName') {
      fd.carrierName = val || ''
    }
  },
  transformPayload: (fd, _status) => ({
    taskNo: fd.orderNo || genTaskNo('SH'),
    sourceOrderNo: fd.sourceOrderNo || '',
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName || '',
    pickTaskId: fd.pickTaskId ?? undefined,
    sourceOrderId: fd.sourceOrderId ?? undefined,
    customerId: fd.customerId ?? undefined,
    customerName: fd.customerName || '',
    carrierName: fd.carrierName || '',
    trackingNo: fd.trackingNo || '',
    remark: fd.remark || '',
    items: (fd.products || []).filter((p: any) => p.productId != null && Number(p.quantity) > 0).map((p: any) => ({
      productId: p.productId,
      productCode: p.productCode || '',
      productName: p.productName || '',
      productSpec: p.productSpec || '',
      productUnit: p.productUnit || '',
      expectedQuantity: Number(p.quantity) || 0,
      locationId: p.locationId ?? undefined,
      locationCode: p.locationCode || '',
      batchNo: p.batchNo || '',
      serialNo: p.serialNo || '',
    })),
  }),
})

// 初始化默认值
if (formData.orderNo === undefined) formData.orderNo = genTaskNo('SH')
if (formData.taskNo === undefined) formData.taskNo = formData.orderNo
for (const key of ['warehouseName', 'sourceOrderNo', 'customerName', 'carrierName', 'trackingNo', 'remark', 'createTime', 'creatorName', 'assigneeName', 'printTemplate', 'printPaperSize', 'defaultWarehouseId', 'defaultCarrierName']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) { if (formData[key] === undefined) formData[key] = 0 }
if (formData.status === undefined) formData.status = 0

// ═══ 拣货任务带出：客户/仓库/来源单号/明细 ═══
async function handlePickTaskChange(val: number, fd: any) {
  if (!val) return
  try {
    const t: any = await pickApi.taskGetById(val)
    if (t) {
      fd.customerId = t.customerId ?? undefined
      fd.customerName = t.customerName || ''
      fd.warehouseId = t.warehouseId ?? undefined
      fd.warehouseName = t.warehouseName || ''
      fd.sourceOrderNo = t.sourceOrderNo || ''
      if (t.sourceOrderId) fd.sourceOrderId = t.sourceOrderId
      if (t.warehouseId) loadLocations(t.warehouseId)
    }
    const list: any = await pickApi.detailList(val)
    fd.products = (Array.isArray(list) ? list : []).map((d: any, i: number) => ({
      id: `p${d.id}`,
      productId: d.productId, productCode: d.productCode || '', productName: d.productName || '',
      productSpec: d.productSpec || '', productUnit: d.productUnit || '',
      quantity: d.expectedQuantity || 0,
      locationId: d.locationId ?? undefined, locationCode: d.locationCode || '',
      batchNo: d.batchNo || '', serialNo: d.serialNo || '', status: 0,
    }))
    message.success(`已从拣货任务带入 ${fd.products.length} 条明细`)
  } catch (e: any) { message.error(e?.message || '带出拣货明细失败') }
}

// ═══ 扫码复核 ═══
const scanCode = ref('')
const scanQty = ref<number>(1)
const acting = ref(false)
async function handleScan() {
  if (!formData.id) { message.warning('请先保存发货单，再扫码复核'); return }
  const code = (scanCode.value || '').trim()
  if (!code) { message.warning('请输入商品编码或序列号'); return }
  const qty = Number(scanQty.value) > 0 ? Number(scanQty.value) : 1
  const target = formData.products.find((d: any) => (d.productCode === code) || (d.serialNo === code) || (d.batchNo === code))
  if (!target) { message.warning('未匹配到发货明细中的商品，请核对编码'); return }
  if (!target.id) { message.warning('该明细尚未保存，请先保存明细'); return }
  acting.value = true
  try {
    await shipApi.scanItem(target.id, qty)
    message.success(`复核扫描成功：${code} × ${qty}`)
    scanCode.value = ''; scanQty.value = 1
    await reloadDetail()
  } catch (e: any) { message.error(e?.message || '复核扫描失败') }
  finally { acting.value = false }
}

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => {
  const actions: any[] = [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ]
  if (currentStatus.value === 0 && formData.id) actions.push({ key: 'start', label: '开始发货', icon: PlayCircleOutlined })
  if (currentStatus.value === 1) actions.push({ key: 'confirm', label: '确认发货', icon: CheckCircleOutlined })
  if (currentStatus.value === 0 || currentStatus.value === 1) actions.push({ key: 'cancel', label: '取消', icon: CloseCircleOutlined })
  return { title: '发货单', orderNo: formData.orderNo, showAttachment: true, actions }
})

// ═══ 基本信息字段 ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '单号', type: 'input', inlineLabel: true, width: 190, disabled: true },
  { key: 'warehouseId', label: '发货仓库', type: 'select', required: true, inlineLabel: true, width: 190, searchBtn: '+Q', loading: true },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', inlineLabel: true, width: 170 },
  { key: 'pickTaskId', label: '拣货任务', type: 'select', inlineLabel: true, width: 200, remoteSearch: true },
  { key: 'customerId', label: '客户', type: 'select', inlineLabel: true, width: 180, remoteSearch: true },
  { key: 'carrierName', label: '承运商', type: 'select', inlineLabel: true, width: 160, remoteSearch: true },
  { key: 'trackingNo', label: '运单号', type: 'input', inlineLabel: true, width: 150 },
  { key: 'remark', label: '备注', type: 'input', inlineLabel: true, width: 240 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 190, disabled: true },
]

const DEFAULT_HIDDEN_FIELDS = ['trackingNo', 'creatorName', 'createTime']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'shipping-order-form'
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
        defaultCarrierName: parsed.defaults.defaultCarrierName,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch (e) { /* 保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultWarehouseId: formData.defaultWarehouseId,
      defaultCarrierName: formData.defaultCarrierName,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try { await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload)) } catch (e) { /* 静默 */ }
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
        : f.key === 'pickTaskId'
          ? pickTaskOptions.value
          : f.key === 'customerId'
            ? customerOptions.value
            : f.key === 'carrierName'
              ? carrierOptions.value
              : (f as any).options,
      loading: f.key === 'warehouseId'
        ? loadingOptions.value
        : f.key === 'pickTaskId'
          ? pickTaskLoading.value
          : f.key === 'customerId'
            ? customerLoading.value
            : f.key === 'carrierName'
              ? carrierLoading.value
              : (f as any).loading,
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

function handlePageConfigVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 摘要 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '应发数量', value: totalQuantity.value, statusLabel: statusText.value },
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
    width: 220, showScanToggle: true },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'productSpec', title: '规格', type: 'input', width: 100 },
  { key: 'productUnit', title: '单位', type: 'input', width: 70 },
  { key: 'quantity', title: '应发数量', type: 'number', width: 110, precision: 2 },
  { key: 'locationId', title: '货位', type: 'input', searchable: true,
    options: (locationOptions.value || []).map((l: any) => ({ label: l.label || String(l.value), value: l.value })),
    width: 150 },
  { key: 'batchNo', title: '批次号', type: 'input', width: 110 },
  { key: 'serialNo', title: '序列号', type: 'input', width: 110 },
])

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ═══ 事件处理 ═══
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
      record.productSpec = p.specification || p.spec || ''
      record.productUnit = p.unit || ''
    }
  }
  if (fieldKey === 'locationId' && value != null) {
    const l = locationOptions.value.find((x: any) => x.value === value)
    if (l) record.locationCode = (l as any).code || ''
  }
}
function handleFieldChange(fieldKey: string, val: any) { baseFieldChange(fieldKey, val) }

async function handlePrimarySubmit() {
  if (isLocked.value) return
  handleSubmit()
}

// ═══ 工作流 ═══
async function reloadDetail() {
  if (!formData.id) return
  try {
    const res: any = await shipApi.getById(formData.id)
    const data = res?.data || res
    if (data) {
      Object.assign(formData, { ...data, orderNo: data.taskNo || formData.orderNo, status: data.status })
    }
  } catch (e) { /* 静默 */ }
}
function currentOperator() {
  return { id: userStore?.userId || userStore?.userInfo?.id || 0, name: currentUserName.value || '系统' }
}
async function handleAction(actionKey: string) {
  const u = currentOperator()
  switch (actionKey) {
    case 'history': router.push('/wms/ship'); break
    case 'config': showFormConfig.value = true; break
    case 'print':
      if (formData.orderNo) window.print()
      else message.warning('请先保存单据后再打印')
      break
    case 'start':
      if (!formData.id) { message.warning('请先保存单据'); return }
      await shipApi.startShip(formData.id, u.id, u.name)
      message.success('已开始发货')
      await reloadDetail()
      break
    case 'confirm':
      if (!formData.id) { message.warning('请先保存单据'); return }
      await shipApi.confirmShip(formData.id)
      message.success('发货已确认')
      await reloadDetail()
      break
    case 'cancel':
      if (!formData.id) { message.warning('请先保存单据'); return }
      await shipApi.cancelShip(formData.id, 'PC端取消发货')
      message.success('发货单已取消')
      await reloadDetail()
      break
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function handleError(err: any) { console.warn('[发货单] ErrorBoundary:', err) }

onMounted(async () => {
  if (effectiveMode.value !== 'edit') {
    formData.orderNo = genTaskNo('SH'); formData.taskNo = formData.orderNo
  }
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
    if (formData.defaultWarehouseId) {
      formData.warehouseId = formData.defaultWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
      if (w) formData.warehouseName = w.warehouseName || w.name || ''
      loadLocations(formData.warehouseId)
    }
    if (formData.defaultCarrierName) formData.carrierName = formData.defaultCarrierName
  }
  nextTick(() => {  loadFormConfig()
})
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.remark-row { display: flex; align-items: center; gap: 10px; }
.remark-label { font-size: 13px; color: #666; white-space: nowrap; }
.doc-info-row { display: flex; flex-wrap: wrap; gap: 8px 20px; font-size: 12px; color: #888; }
.doc-info-item { display: inline-flex; align-items: center; gap: 4px; }
.config-hint { color: #999; font-size: 12px; margin-bottom: 12px; }
.scan-row { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.scan-hint { font-size: 12px; color: #999; }
</style>
