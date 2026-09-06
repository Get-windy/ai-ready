<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink: 0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 检验项目明细 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            :data-source="formData.inspectionItemList"
            :max-height="tableMaxHeight"
            :storage-key="'quality-inspection-form-columns'"
            @cell-change="handleInspectionCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertInspectionItem(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveInspectionItem(index)">
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddInspectionItem()">
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

        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">备注</span>
              <a-input v-model:value="formData.remark" size="small" class="remark-input" placeholder="请输入检验备注" :disabled="isLocked" />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag></span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- 配置弹窗：页面配置/录单默认值/打印设置 -->
      <a-modal v-model:open="showFormConfig" title="配置" :width="760" :footer="null" destroy-on-close>
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="page" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table :columns="fConfigColumns" :data-source="pageConfigRows" :pagination="false" size="small" row-key="key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'"><a-input v-model:value="record.label" size="small" @blur="savePageConfig" /></template>
                <template v-if="column.key === 'visible'"><a-checkbox :checked="record.visible" @change="(e: any) => { record.visible = e.target.checked; savePageConfig() }" /></template>
                <template v-if="column.key === 'enterJump'"><a-checkbox :checked="record.enterJump" @change="(e: any) => { record.enterJump = e.target.checked; savePageConfig() }" :disabled="!record.visible" /></template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="default" tab="录单默认值">
            <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="默认检验类型">
                <a-select v-model:value="formData.defaultInspectionType" size="small" style="width:100%" :options="INSPECTION_TYPE_OPTIONS" @change="savePageConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="print" tab="打印设置">
            <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="始终使用最后一次模板"><a-switch v-model:checked="printConfig.useLastTemplate" /></a-form-item>
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
import dayjs from 'dayjs'
import { SettingOutlined, HistoryOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BasicInfoField, BillHeaderConfig, BillFooterConfig, SummaryRow } from '@/components/BillFormPage/types'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import { qualityInspectionApi } from '@/api/quality'
import { userPageConfigApi } from '@/api/erp'

const router = useRouter()
const route = useRoute()
const editId = computed(() => route.query.id ? Number(route.query.id) : (route.params.id ? Number(route.params.id) : 0))
const effectiveMode = computed(() => (editId.value ? 'edit' : 'create'))
const saving = ref(false)
const showFormConfig = ref(false)

const formData = reactive<any>({
  id: 0,
  orderNo: '',
  bizType: 'PURCHASE_ORDER',
  bizId: undefined,
  bizNo: '',
  productId: undefined,
  warehouseId: undefined,
  productName: '',
  batchNo: '',
  inspectionType: 'INBOUND',
  defaultInspectionType: 'INBOUND',
  quantity: 0,
  sampleQuantity: 0,
  inspectionResult: 'PENDING',
  inspectorName: '',
  inspectionTime: null as any,
  remark: '',
  status: 0,
  creatorName: '',
  createTime: '',
  inspectionItemList: [] as any[],
})

const printConfig = reactive({ useLastTemplate: false })

// ── 状态 ──
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待检', color: 'warning' },
  1: { text: '已完成', color: 'green' },
  2: { text: '已作废', color: 'red' },
}
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '待检')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// ── header ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: effectiveMode.value === 'edit' ? '质检单' : '新建质检单',
  orderNo: formData.orderNo,
  actions: [
    { key: 'history', label: '历史', icon: HistoryOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ── 基本信息字段 ──
const ALL_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '质检单号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'bizNo', label: '来源单号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'productName', label: '产品名称', type: 'input', inlineLabel: true, width: 210, required: true },
  { key: 'batchNo', label: '批次号', type: 'input', inlineLabel: true, width: 160 },
  { key: 'inspectionType', label: '检验类型', type: 'select', inlineLabel: true, width: 180 },
  { key: 'quantity', label: '检验数量', type: 'number', inlineLabel: true, width: 150, required: true, min: 0 },
  { key: 'sampleQuantity', label: '抽检数量', type: 'number', inlineLabel: true, width: 150, min: 0 },
  { key: 'inspectionResult', label: '检验结果', type: 'select', inlineLabel: true, width: 180 },
  { key: 'inspectorName', label: '检验员', type: 'input', inlineLabel: true, width: 160 },
  { key: 'inspectionTime', label: '检验时间', type: 'date', inlineLabel: true, width: 180, format: 'YYYY-MM-DD' },
  { key: 'remark', label: '备注', type: 'textarea', span: 24 },
]

const fConfigColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]
const pageConfigRows = ref<{ key: string; index: number; name: string; label: string; visible: boolean; enterJump: boolean }[]>([])
function initPageConfigRows() {
  pageConfigRows.value = ALL_FIELDS.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    label: f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump ?? false,
  }))
}
const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>({})
for (const f of ALL_FIELDS) pageConfig[f.key] = { visible: true, enterJump: false }
const configModalTab = ref('page')
const INSPECTION_TYPE_OPTIONS = [{ label: '入库检验', value: 'INBOUND' }, { label: '出库检验', value: 'OUTBOUND' }, { label: '过程检验', value: 'PROCESS' }]
const FORM_CONFIG_MODULE = 'quality-inspection-form'
const FORM_CONFIG_PAGE = 'form'
async function savePageConfig() {
  for (const row of pageConfigRows.value) {
    if (pageConfig[row.key]) {
      pageConfig[row.key].visible = row.visible
      pageConfig[row.key].enterJump = row.enterJump
    }
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify({
      fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
      defaults: { defaultInspectionType: formData.defaultInspectionType },
    }))
  } catch { /* 静默 */ }
}

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => {
      const base: BasicInfoField = { ...f }
      if (f.key === 'inspectionType') base.options = [{ label: '入库检验', value: 'INBOUND' }, { label: '出库检验', value: 'OUTBOUND' }, { label: '过程检验', value: 'PROCESS' }]
      if (f.key === 'inspectionResult') base.options = [{ label: '待检验', value: 'PENDING' }, { label: '合格', value: 'PASS' }, { label: '让步接收', value: 'CONCESSION' }, { label: '不合格', value: 'FAIL' }]
      return base
    })
)

// ── 检验项目明细 ──
const tableMaxHeight = ref(400)
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'name', title: '检验项目', type: 'input', width: 180 },
  { key: 'method', title: '检验方法', type: 'input', width: 150 },
  { key: 'standard', title: '标准值', type: 'input', width: 160 },
  { key: 'actual', title: '实测值', type: 'input', width: 140 },
  { key: 'result', title: '判定', type: 'select', width: 100, options: [{ label: '合格', value: 'PASS' }, { label: '不合格', value: 'FAIL' }] },
]

function handleAddInspectionItem() {
  formData.inspectionItemList.push({ id: Date.now() + Math.random().toString(36).slice(2, 6), name: '', method: '', standard: '', actual: '', result: 'PASS' })
}
function handleRemoveInspectionItem(index: number) {
  formData.inspectionItemList.splice(index, 1)
}
function handleInsertInspectionItem(index: number) {
  handleAddInspectionItem()
  const item = formData.inspectionItemList.pop()
  if (item) formData.inspectionItemList.splice(index + 1, 0, item)
}
function handleInspectionCellChange(_record: any, _fieldKey: string, _value: any) {
  // 预留：实测值/标准值联动判定（如需自动判定可在此扩展）
}

// ── 摘要 / 页脚 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '检验数量', value: Number(formData.quantity ?? 0) },
  { label: '抽检数量', value: Number(formData.sampleQuantity ?? 0) },
])
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '检验数量',
  amountValue: String(formData.quantity ?? 0),
  primaryBtnText: '完成检验',
  draftBtnText: '保存草稿',
}))

// ── 保存 ──
function transformPayload() {
  const fd = formData
  return {
    bizType: fd.bizType || undefined,
    bizId: fd.bizId || undefined,
    bizNo: fd.bizNo || undefined,
    qualityNo: fd.orderNo || undefined,
    productId: fd.productId || undefined,
    warehouseId: fd.warehouseId || undefined,
    productName: fd.productName,
    batchNo: fd.batchNo || undefined,
    inspectionType: fd.inspectionType,
    quantity: Number(fd.quantity ?? 0),
    sampleQuantity: Number(fd.sampleQuantity ?? 0),
    inspectorName: fd.inspectorName || undefined,
    inspectionResult: fd.inspectionResult || 'PENDING',
    remark: fd.remark || undefined,
    inspectionTime: fd.inspectionTime
      ? (dayjs.isDayjs(fd.inspectionTime) ? fd.inspectionTime.format('YYYY-MM-DD HH:mm:ss') : fd.inspectionTime)
      : undefined,
    inspectionItems: JSON.stringify((fd.inspectionItemList || []).filter((i: any) => i.name && i.name.trim()).map((i: any) => ({
      name: i.name.trim(),
      method: i.method || '',
      standard: i.standard || '',
      actual: i.actual || '',
      result: i.result || 'PASS',
    }))),
  }
}

async function saveQuality(status: number) {
  if (!formData.productName) { message.warning('请填写产品名称'); return }
  if (formData.quantity == null || formData.quantity < 0) { message.warning('请填写检验数量'); return }
  saving.value = true
  try {
    const payload = transformPayload()
    const result = payload.inspectionResult === 'PENDING' ? 'PASS' : payload.inspectionResult
    const passQuantity = Number(payload.sampleQuantity || payload.quantity || 0)
    const failQuantity = 0
    let savedId = editId.value
    if (effectiveMode.value === 'edit' && savedId) {
      await qualityInspectionApi.update(savedId, payload)
    } else {
      const res: any = await qualityInspectionApi.create({ ...payload, inspectionResult: 'PENDING' })
      savedId = res?.data?.id ?? res?.id
    }
    if (status === 1 && savedId) {
      await qualityInspectionApi.complete(savedId, { result, passQuantity, failQuantity, remark: payload.remark || '' })
    }
    message.success(status === 0 ? '保存草稿成功' : '检验完成')
    router.push('/quality/inspection/list')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleSaveDraft() { if (!isLocked.value) saveQuality(0) }
function handlePrimarySubmit() { if (!isLocked.value) saveQuality(1) }

function handleAction(actionKey: string) {
  if (actionKey === 'config') showFormConfig.value = true
  else if (actionKey === 'history') router.push('/quality/inspection/list')
}

function handleFieldChange() { /* 预留：检验员等下拉回填 */ }

function formatNow() {
  return dayjs().format('YYYY-MM-DD HH:mm')
}

function parseInspectionItems(json?: string): any[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    if (!Array.isArray(arr)) return []
    return arr.filter((i: any) => i && typeof i === 'object').map((it: any, i: number) => ({
      id: `item-${i}`,
      name: it.name || '',
      method: it.method || '',
      standard: it.standard || '',
      actual: it.actual || '',
      result: it.result || 'PASS',
    }))
  } catch {
    return []
  }
}

async function loadDetail() {
  if (!editId.value) return
  try {
    const res: any = await qualityInspectionApi.get(editId.value)
    const data = res?.data ?? res ?? {}
    Object.assign(formData, {
      id: data.id,
      orderNo: data.qualityNo || '',
      bizNo: data.bizNo || '',
      productName: data.productName || '',
      batchNo: data.batchNo || '',
      inspectionType: data.inspectionType || 'INBOUND',
      quantity: Number(data.quantity ?? 0),
      sampleQuantity: Number(data.sampleQuantity ?? 0),
      inspectionResult: data.inspectionResult || 'PENDING',
      inspectorName: data.inspectorName || '',
      inspectionTime: data.inspectionTime || null,
      remark: data.remark || '',
      status: data.status ?? 0,
      creatorName: data.creatorName || '',
      createTime: data.createTime || '',
      inspectionItemList: parseInspectionItems(data.inspectionItems),
    })
  } catch (e: any) {
    message.error(e?.message || '加载质检单失败')
  }
}

async function loadPageConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach(k => {
        if (pageConfig[k]) {
          pageConfig[k].visible = parsed.fields[k].visible !== false
          pageConfig[k].enterJump = !!parsed.fields[k].enterJump
        }
      })
    }
    if (parsed.defaults?.defaultInspectionType) formData.defaultInspectionType = parsed.defaults.defaultInspectionType
  } catch { /* ignore */ }
}

function handleError(err: any) {
  console.error('[质检单表单] 渲染异常', err)
}

onMounted(async () => {
  await loadPageConfig()
  initPageConfigRows()
  if (effectiveMode.value === 'edit') {
    await loadDetail()
  } else {
    // 从来源单据跳转（采购入库/收货/上架）时预填来源信息，打通质检孤岛
    if (route.query.bizType) formData.bizType = String(route.query.bizType)
    if (route.query.bizId) formData.bizId = Number(route.query.bizId)
    if (route.query.bizNo) formData.bizNo = String(route.query.bizNo)
    if (route.query.productId) formData.productId = Number(route.query.productId)
    if (route.query.productName) formData.productName = String(route.query.productName)
    if (route.query.warehouseId) formData.warehouseId = Number(route.query.warehouseId)
    if (route.query.batchNo) formData.batchNo = String(route.query.batchNo)
    if (route.query.quantity) formData.quantity = Number(route.query.quantity)
    // 新建：预置一行空检验项目明细
    if (formData.inspectionItemList.length === 0) handleAddInspectionItem()
    // 新建：生成质检单号
    try {
      const res: any = await qualityInspectionApi.nextNo()
      formData.orderNo = res?.data ?? res ?? ''
    } catch { /* ignore */ }
  }
})
</script>

<style scoped>
.remark-section { display: flex; padding: 4px 0; }
.config-hint { color: #999; font-size: 12px; margin-bottom: 12px; }
.remark-row { display: flex; align-items: center; flex: 1; }
.remark-label { color: rgba(0,0,0,0.65); font-size: 13px; margin-right: 12px; white-space: nowrap; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding-top: 8px; flex-wrap: wrap; }
.doc-info-item { font-size: 13px; color: rgba(0,0,0,0.65); }
</style>
