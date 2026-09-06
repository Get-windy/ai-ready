<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert v-if="isLocked" :message="`当前单据状态为「${statusText}」，已进入作业流程，内容不可修改`" type="warning" show-icon banner style="flex-shrink:0" />

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
            :data-source="formData.details"
            :max-height="tableMaxHeight"
            :summary-columns="tableSummaryColumns"
            :storage-key="'putaway-order-form-columns'"
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
          </BillDetailTable>
        </template>

        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row"><span class="remark-label">单据备注</span><a-input v-model:value="formData.remark" placeholder="备注" :disabled="isLocked" /></div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor(currentStatus)">{{ statusText(currentStatus) }}</a-tag></span>
            <span class="doc-info-item">来源类型 <a-tag>{{ sourceTypeText(formData.sourceType) }}</a-tag></span>
            <span class="doc-info-item">经手人 <a-tag color="blue">{{ formData.assigneeName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          </div>
        </template>
      </BillFormPage>

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
              <a-form-item label="默认上架仓库"><a-select v-model:value="formData.defaultWarehouseId" allow-clear :options="warehouseOptions" style="width:260px" @change="saveFormConfig" /></a-form-item>
              <a-form-item label="默认来源类型"><a-select v-model:value="formData.defaultSourceType" allow-clear :options="sourceTypeOptions" style="width:260px" @change="saveFormConfig" /></a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="printSettings" tab="打印设置">
            <p class="config-hint">配置打印行为</p>
            <a-form layout="vertical" size="small">
              <a-form-item label="始终使用上次打印模板"><a-switch v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig" /></a-form-item>
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
import { putawayApi } from '@/api/wms/putaway'
import { locationApi } from '@/api/wms/warehouse'
import { userPageConfigApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { WMS_STATUS_MAP, genTaskNo } from '../../whTask'

defineOptions({ name: 'WhPutawayOrderForm' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统')
const idParam = Number(route.query.id || 0)
const editing = computed(() => !!idParam)

const sourceTypeOptions = [
  { label: '收货上架', value: 0 }, { label: '退货上架', value: 1 }, { label: '调拨上架', value: 2 }, { label: '其他', value: 3 },
]
function statusText(s: number) { return WMS_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return WMS_STATUS_MAP[s]?.color || 'default' }
function sourceTypeText(t: number | undefined) { return sourceTypeOptions.find(o => o.value === t)?.label || '其他' }

function availableActions(status: number) {
  const actions = [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'recommend', label: '推荐货位', icon: undefined as any },
    { key: 'history', label: '历史', icon: HistoryOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ]
  if (status === 0) actions.splice(1, 0, { key: 'start', label: '开始上架', icon: undefined as any })
  if (status === 1) actions.splice(1, 0, { key: 'confirm', label: '确认上架', icon: undefined as any })
  if (status === 0 || status === 1) actions.splice(1, 0, { key: 'cancel', label: '取消', icon: undefined as any })
  return actions as any[]
}

async function getById(id: number) {
  const res: any = await putawayApi.getById(id)
  const t: any = res?.data || res
  if (!t) return {}
  return {
    id: t.id, orderNo: t.taskNo, taskNo: t.taskNo, warehouseId: t.warehouseId ?? undefined,
    warehouseName: t.warehouseName || '', sourceType: t.sourceType ?? 0, sourceId: t.sourceId ?? undefined,
    sourceOrderNo: t.sourceOrderNo || '', remark: t.remark || '', status: t.status ?? 0, assigneeName: t.assigneeName || '', createTime: t.createTime || '',
  }
}

const { formData, loadingOptions, saving, optionRefs, filterOption,
  handleFieldChange: baseFieldChange } = useBillForm({
  api: { create: (d: any) => putawayApi.save({ ...d, status: 0 }), update: (id: number, d: any) => putawayApi.update({ ...d, id }), getById },
  redirectPath: '/wms/putaway',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [{ key: 'warehouseId', label: '上架仓库', type: 'select', required: true }],
  productDefaults: { productId: undefined, productCode: '', productName: '', productSpec: '', productUnit: '', quantity: 0, fromLocationId: undefined, fromLocationCode: '', toLocationId: undefined, toLocationCode: '', batchNo: '', productionDate: undefined, validityDate: undefined },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') { const w = optionRefs.warehouses.find(x => x.id === val); fd.warehouseName = w?.warehouseName || w?.name || ''; loadLocations(val); formData.details.forEach((d: any) => { d.fromLocationId = undefined; d.toLocationId = undefined }) }
  },
})

if (formData.orderNo === undefined) formData.orderNo = genTaskNo('PA')
if (formData.taskNo === undefined) formData.taskNo = formData.orderNo
if (!formData.sourceType) formData.sourceType = 0
if (formData.sourceOrderNo === undefined) formData.sourceOrderNo = ''
for (const k of ['warehouseName', 'remark']) if (formData[k] === undefined) formData[k] = ''
if (formData.status === undefined) formData.status = 0
if (formData.details === undefined) formData.details = []

const currentStatus = computed(() => Number(formData.status ?? 0))
const isLocked = computed(() => editing.value && currentStatus.value !== 0)

const buildPayload = () => ({ taskNo: formData.taskNo || genTaskNo('PA'), sourceType: formData.sourceType ?? 0, sourceId: formData.sourceId, sourceOrderNo: formData.sourceOrderNo || '', warehouseId: formData.warehouseId, warehouseName: formData.warehouseName || '', remark: formData.remark || '', status: 0 })
const doSave = async () => {
  const payload = buildPayload()
  if (!payload.warehouseId) { message.warning('请选择上架仓库'); return }
  try {
    saving.value = true
    if (editing.value && formData.id) { await putawayApi.update({ ...payload, id: formData.id }); message.success('上架单已更新') }
    else { const res: any = await putawayApi.save(payload); formData.id = res?.data?.id || res?.id || 0; message.success('上架单已创建，可录入明细') }
    await refreshCurrent()
  } catch (e: any) { message.error(e?.message || '保存失败') } finally { saving.value = false }
}
const handleSaveDraftFn = () => doSave()
const handlePrimarySubmit = () => doSave()

const warehouseOptions = computed(() => (optionRefs.warehouses || []).map(w => ({ label: w.warehouseName || w.name, value: w.id })))

const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '单号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'warehouseId', label: '上架仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'sourceType', label: '来源类型', type: 'select', inlineLabel: true, width: 180 },
  { key: 'sourceId', label: '来源单据ID', type: 'number', inlineLabel: true, width: 140 },
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', inlineLabel: true, width: 160 },
  { key: 'assigneeName', label: '经手人', type: 'display', inlineLabel: true, width: 130 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
]
const DEFAULT_HIDDEN_FIELDS = ['assigneeName', 'createTime', 'sourceId']
const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>({})
for (const f of ALL_BASIC_INFO_FIELDS) pageConfig[f.key] = { visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key), enterJump: ['select', 'date', 'number', 'input'].includes(f.type) }
const basicInfoFields = computed<BasicInfoField[]>(() => ALL_BASIC_INFO_FIELDS.filter(f => pageConfig[f.key]?.visible !== false).map(f => ({ ...f, options: f.key === 'warehouseId' ? warehouseOptions.value : f.key === 'sourceType' ? sourceTypeOptions : (f as any).options, loading: f.key === 'warehouseId' ? loadingOptions.value : (f as any).loading })))

const FORM_CONFIG_MODULE = 'putaway-order-form'
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const pageConfigTableColumns: any[] = [{ title: '字段', dataIndex: 'label', key: 'label', width: 160 }, { title: '显示名', dataIndex: 'displayName', key: 'displayName', width: 200 }, { title: '显示', dataIndex: 'visible', key: 'visible', width: 80 }, { title: '回车键跳转', dataIndex: 'enterJump', key: 'enterJump', width: 120 }]
const pageConfigFields = computed(() => ALL_BASIC_INFO_FIELDS.map(f => ({ key: f.key, label: f.label, displayName: f.label, visible: pageConfig[f.key]?.visible !== false, enterJump: pageConfig[f.key]?.enterJump ?? true })))
function handlePageConfigVisibleChange(key: string, v: boolean) { pageConfig[key].visible = v; saveFormConfig() }
function handlePageConfigEnterJumpChange(key: string, v: boolean) { pageConfig[key].enterJump = v; saveFormConfig() }
async function loadFormConfig() { try { const raw: any = await userPageConfigApi.get(FORM_CONFIG_MODULE, 'form'); if (!raw) return; const cfg = typeof raw === 'string' ? JSON.parse(raw) : raw; if (cfg?.fields) for (const k of Object.keys(cfg.fields)) if (pageConfig[k]) pageConfig[k] = { ...pageConfig[k], ...cfg.fields[k] } } catch (e) {} }
async function saveFormConfig() { try { await userPageConfigApi.save(FORM_CONFIG_MODULE, 'form', JSON.stringify({ fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, v])) })) } catch (e) {} }

const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', type: 'rowNo', title: '', fixed: 'left', width: 44 },
  { key: 'action', type: 'action', title: '操作', slotName: 'actionCell', fixed: 'left', width: 60 },
  { key: 'productId', type: 'input', title: '商品名称', width: 220, required: true, showScanToggle: true, searchable: true,
    options: (optionRefs.products || []).map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })) },
  { key: 'productSpec', type: 'input', title: '规格', width: 100, readonly: true },
  { key: 'productUnit', type: 'input', title: '单位', width: 64, readonly: true },
  { key: 'quantity', type: 'number', title: '上架数量', width: 120, precision: 2, required: true },
  { key: 'fromLocationId', type: 'input', title: '来源货位', width: 160, searchable: true,
    options: (locationOptions.value || []).map((l: any) => ({ label: l.label || String(l.value), value: l.value })) },
  { key: 'toLocationId', type: 'input', title: '目标货位', width: 160, searchable: true,
    options: (locationOptions.value || []).map((l: any) => ({ label: l.label || String(l.value), value: l.value })) },
  { key: 'batchNo', type: 'input', title: '批次号', width: 120 },
  { key: 'productionDate', type: 'date', title: '生产日期', width: 130 },
  { key: 'validityDate', type: 'date', title: '有效期至', width: 130 },
])
const tableSummaryColumns = computed(() => [{ key: 'quantity', value: Number(totalQty.value.toFixed(2)), highlight: true }])
const totalQty = computed(() => (formData.details || []).reduce((s: number, d: any) => s + (Number(d.quantity) || 0), 0))

const locationOptions = ref<{ label: string; value: number }[]>([])
async function loadLocations(warehouseId: number) { locationOptions.value = []; if (!warehouseId) return; try { const list: any = await locationApi.listByWarehouse(warehouseId); locationOptions.value = (Array.isArray(list) ? list : []).map((l: any) => ({ label: `${l.locationCode}${l.locationName ? ' ' + l.locationName : ''}`, value: l.id })) } catch (e) { locationOptions.value = [] } }

let rowSeq = 0
function newDetail(): any { return { _key: `n${++rowSeq}`, productId: undefined, productCode: '', productName: '', productSpec: '', productUnit: '', quantity: undefined, fromLocationId: undefined, fromLocationCode: '', toLocationId: undefined, toLocationCode: '', batchNo: '', productionDate: undefined, validityDate: undefined } }
function handleAddDetail() { formData.details.push(newDetail()) }
function handleInsertDetail(index: number) { formData.details.splice(index + 1, 0, newDetail()) }
function handleRemoveDetail(index: number) { formData.details.splice(index, 1) }
function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) { record.productId = p.id; record.productCode = p.code || ''; record.productName = p.name || ''; record.productSpec = p.specification || p.spec || ''; record.productUnit = p.unit || '' }
  }
  if ((fieldKey === 'fromLocationId' || fieldKey === 'toLocationId') && value != null) {
    const l = locationOptions.value.find((x: any) => x.value === value)
    const code = l?.label?.split(' ')[0] || ''
    if (fieldKey === 'fromLocationId') record.fromLocationCode = code
    else record.toLocationCode = code
  }
}

async function loadDetails(taskId: number) { try { const list: any = await putawayApi.getDetails(taskId); formData.details = (Array.isArray(list) ? list : []).map((d: any) => ({ ...d, _key: `s${d.id}` })) } catch (e) { formData.details = [] } }
async function saveDetails() {
  if (!formData.id) { message.warning('请先保存单据'); return }
  if (formData.details.some((r: any) => !r.productId)) { message.warning('明细行请选择商品'); return }
  if (formData.details.some((r: any) => !(Number(r.quantity) > 0))) { message.warning('明细行请填写大于0的上架数量'); return }
  try {
    const payload = formData.details.map((d: any) => ({ productId: d.productId, productCode: d.productCode, productName: d.productName, productSpec: d.productSpec, productUnit: d.productUnit, quantity: Number(d.quantity), fromLocationId: d.fromLocationId, fromLocationCode: d.fromLocationCode, toLocationId: d.toLocationId, toLocationCode: d.toLocationCode, batchNo: d.batchNo || '', productionDate: d.productionDate || undefined, validityDate: d.validityDate || undefined }))
    await putawayApi.saveDetails(formData.id, payload)
    message.success('上架明细已保存'); await loadDetails(formData.id)
  } catch (e: any) { message.error(e?.message || '明细保存失败') }
}

const headerConfig = computed<BillHeaderConfig>(() => ({ title: '上架单', orderNo: formData.orderNo, showAttachment: true, actions: availableActions(currentStatus.value).map(a => ({ key: a.key, label: a.label, icon: a.icon })) }))
const summaryConfig = computed<SummaryRow[]>(() => [{ label: '上架数量', value: totalQty.value, statusLabel: statusText(currentStatus.value) }, { label: '商品行数', value: (formData.details || []).filter((d: any) => d.productId != null).length, divider: true }])
const footerConfig = computed<BillFooterConfig>(() => ({ amountLabel: '上架数量', amountValue: totalQty.value.toString(), amountHighlight: true, draftBtnText: editing.value && currentStatus.value !== 0 ? undefined : '保存草稿', draftShortcut: editing.value && currentStatus.value !== 0 ? undefined : 'Ctrl+S', primaryBtnText: editing.value && currentStatus.value !== 0 ? '保存修改' : '保存单据', primaryShortcut: 'Ctrl+Enter', saving: saving.value }))

function currentUser() { return { userId: userStore.userId || 0, userName: currentUserName.value } }
async function doAction(fn: () => Promise<any>, ok: string) { try { await fn(); message.success(ok); await refreshCurrent() } catch (e: any) { message.error(e?.message || '操作失败') } }
function handleAction(key: string) {
  const u = currentUser()
  switch (key) {
    case 'config': showFormConfig.value = true; break
    case 'print': message.info('打印功能待接入打印模板'); break
    case 'history': router.push('/wms/putaway'); break
    case 'start': doAction(() => putawayApi.startPutaway(formData.id, u.userId, u.userName), '已开始上架'); break
    case 'confirm': doAction(() => putawayApi.confirmPutaway(formData.id, u.userId, u.userName), '上架已确认'); break
    case 'cancel': doAction(() => putawayApi.cancelPutaway(formData.id, 'PC端取消上架'), '上架单已取消'); break
    case 'recommend': doRecommendLocation(); break
  }
}
async function doRecommendLocation() {
  if (!formData.warehouseId) { message.warning('请先选择上架仓库'); return }
  let cnt = 0
  for (const d of (formData.details || [])) {
    if (!d.productId || d.toLocationId) continue
    try {
      const list: any = await locationApi.recommend({ warehouseId: formData.warehouseId, productId: d.productId, quantity: d.quantity })
      const first = Array.isArray(list) ? list[0] : (list?.data && list.data[0]) || list?.[0]
      if (first && first.id) { d.toLocationId = first.id; d.toLocationCode = first.locationCode || ''; cnt++ }
    } catch (e) {}
  }
  if (cnt > 0) message.success(`已为 ${cnt} 行分配推荐货位`); else message.info('没有可推荐的货位，请手动选择')
}

function handleFieldChange(k: string, v: any) { baseFieldChange(k, v) }
async function refreshCurrent() { if (!formData.id) return; try { const t: any = await putawayApi.getById(formData.id); const d = t?.data || t; if (d) { if (d.taskNo != null) formData.orderNo = d.taskNo; if (d.status != null) formData.status = d.status } } catch (e) {} }

const tableMaxHeight = ref<number>(0)
function computeMaxHeight() { tableMaxHeight.value = Math.max(320, window.innerHeight - 420) }
function handleError(e: any) { console.error(e) }
function formatNow() { return new Date().toISOString().slice(0, 19).replace('T', ' ') }

onMounted(async () => { computeMaxHeight(); await loadFormConfig(); loadLocations(formData.warehouseId); if (editing.value && idParam) { await loadDetails(idParam) } if (!editing.value && formData.details?.length === 0) { formData.details = Array.from({ length: 20 }, () => newDetail()) } })
</script>

<style scoped>
.remark-section { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.remark-row { display: flex; align-items: center; gap: 10px; }
.remark-label { font-size: 13px; color: #666; white-space: nowrap; }
.doc-info-row { display: flex; flex-wrap: wrap; gap: 8px 20px; font-size: 12px; color: #888; }
.doc-info-item { display: inline-flex; align-items: center; gap: 4px; }
.config-hint { color: #999; font-size: 12px; margin-bottom: 12px; }
</style>
