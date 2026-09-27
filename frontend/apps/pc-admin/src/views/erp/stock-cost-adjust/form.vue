<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 非草稿状态锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入审批/记账流程，内容不可修改`"
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
        <!-- ═══ 明细表格 ═══ -->
        <template #detail-table>
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'stock-cost-adjust-form-columns'"
            @cell-change="handleCellChange"
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
              <span class="remark-label">单据备注</span>
              <a-input
                v-model:value="formData.remark"
                size="small"
                class="remark-input"
                placeholder="请输入单据备注"
                :disabled="isLocked"
              />
            </div>
            <div class="remark-row">
              <span class="remark-label">原因说明</span>
              <a-input
                v-model:value="formData.reasonDesc"
                size="small"
                class="remark-input"
                placeholder="请输入调价原因说明"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              经手人 <a-tag color="blue">{{ formData.handlerName || currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.bookkeepingTime" class="doc-info-item">记账时间 {{ formData.bookkeepingTime }}</span>
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
          <!-- Tab 1: 页面配置 -->
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
                    @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)"
                  />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <!-- Tab 2: 录单默认值 -->
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认调价仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.name||w.warehouseName,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认调价类型">
                <a-select v-model:value="formData.defaultAdjustType" size="small" style="width:100%" :options="ADJUST_TYPE_OPTIONS" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <!-- Tab 3: 打印设置 -->
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
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="stock-cost-adjust"
    :document-id="printData.id"
    :print-data="printData"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  PrinterOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  ImportOutlined,
  DownloadOutlined,
  AuditOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stockCostAdjustApi, userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'StockCostAdjustForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)
// ════════════════════════════════════════════
// 调价类型/原因类型字典
// ════════════════════════════════════════════

const ADJUST_TYPE_MAP: Record<number, string> = { 1: '移动加权', 2: '全月平均', 3: '个别计价' }
const ADJUST_TYPE_OPTIONS = Object.entries(ADJUST_TYPE_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const REASON_TYPE_MAP: Record<number, string> = { 1: '市场波动', 2: '供应商调价', 3: '汇率变动', 4: '其他' }
const REASON_TYPE_OPTIONS = Object.entries(REASON_TYPE_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))

// ════════════════════════════════════════════
// 状态枚举
// ════════════════════════════════════════════

const COST_ADJUST_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'blue' },
  3: { text: '已记账', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' },
}

function getAvailableActions(status: number) {
  switch (status) {
    case 0: return [{ key: 'submit', label: '提交审批', icon: AuditOutlined }]
    case 1: return [
      { key: 'approve', label: '审核通过', icon: CheckOutlined },
      { key: 'reject', label: '驳回', icon: CloseCircleOutlined },
    ]
    case 2: return [{ key: 'complete', label: '记账执行', icon: CheckOutlined }]
    default: return []
  }
}

// ════════════════════════════════════════════
// useBillForm composable
// ════════════════════════════════════════════

async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await stockCostAdjustApi.create({ ...payload, status: 0 })
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await stockCostAdjustApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await stockCostAdjustApi.update(id, { ...payload, status: 0 })
  if (status === 1) {
    await stockCostAdjustApi.submit(id)
  }
  return res?.data || res
}

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
} = useBillForm({
  billPrefix: 'CBTJD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await stockCostAdjustApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || data.details || []
      return {
        ...data,
        orderNo: data.adjustNo || '',
        date: data.adjustDate || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        warehouseId: data.warehouseId,
        warehouseName: data.warehouseName || '',
        deptId: data.deptId || undefined,
        deptName: data.deptName || '',
        adjustType: data.adjustType ?? 1,
        reasonType: Number(data.reasonType) || 1,
        reasonDesc: data.reasonDesc || '',
        summary: data.summary || '',
        remark: data.remark || '',
        products: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || d.itemCode || '',
          productCode: d.productCode || '',
          productName: d.productName || '',
          barcode: d.barcode || '',
          specification: d.productSpec || d.specification || '',
          model: d.model || '',
          origin: d.origin || '',
          brand: d.brand || '',
          taste: d.taste || '',
          region: d.region || '',
          location: d.location || '',
          unit: d.productUnit || d.unit || '',
          conversionRelation: d.conversionRelation || '',
          conversionResult: d.conversionResult || '',
          wholesalePrice: d.wholesalePrice ?? 0,
          retailPrice: d.retailPrice ?? 0,
          currentQuantity: d.currentQuantity ?? 0,
          oldCost: d.oldCost ?? 0,
          oldAmount: d.oldAmount ?? 0,
          newCost: d.newCost ?? 0,
          newAmount: d.newAmount ?? 0,
          diffAmount: d.diffAmount ?? 0,
          itemExtNum1: d.itemExtNum1 ?? 0,
          itemExtNum2: d.itemExtNum2 ?? 0,
          itemExtNum3: d.itemExtNum3 ?? 0,
          itemExtText1: d.itemExtText1 || '',
          itemExtText2: d.itemExtText2 || '',
          image: d.image || '',
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/erp/stock-cost-adjust',
  codeApiPath: '/erp/stock/cost-adjust/next-no',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [
    { key: 'warehouseId', label: '调价仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'deptId', label: '部门', type: 'select' },
  ],
  productDefaults: {
    itemCode: '', productCode: '', productName: '', barcode: '', specification: '',
    model: '', origin: '', brand: '', taste: '', region: '', location: '', unit: '',
    conversionRelation: '', conversionResult: '', wholesalePrice: 0, retailPrice: 0,
    currentQuantity: 0, oldCost: 0, oldAmount: 0, newCost: 0, newAmount: 0, diffAmount: 0,
    itemExtNum1: 0, itemExtNum2: 0, itemExtNum3: 0, itemExtText1: '', itemExtText2: '',
    image: '', remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.warehouseName || w?.name || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.deptName = d?.name || ''
    }
  },
  transformPayload: (fd, status) => ({
    status,
    adjustType: fd.adjustType ?? 1,
    adjustDate: fd.date || undefined,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    deptId: fd.deptId || undefined,
    deptName: fd.deptName || undefined,
    reasonType: String(fd.reasonType ?? 1),
    reasonDesc: fd.reasonDesc || undefined,
    summary: fd.summary || undefined,
    remark: fd.remark || undefined,
    totalAdjustAmount: totalAdjustAmount.value,
    items: (fd.products || []).filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      barcode: p.barcode || undefined,
      model: p.model || undefined,
      origin: p.origin || undefined,
      brand: p.brand || undefined,
      taste: p.taste || undefined,
      location: p.location || undefined,
      conversionRelation: p.conversionRelation || undefined,
      conversionResult: p.conversionResult || undefined,
      wholesalePrice: p.wholesalePrice ?? 0,
      retailPrice: p.retailPrice ?? 0,
      currentQuantity: p.currentQuantity ?? 0,
      oldCost: p.oldCost ?? 0,
      oldAmount: p.oldAmount ?? 0,
      newCost: p.newCost ?? 0,
      newAmount: p.newAmount ?? 0,
      diffAmount: p.diffAmount ?? 0,
      itemExtNum1: p.itemExtNum1 ?? 0,
      itemExtNum2: p.itemExtNum2 ?? 0,
      itemExtNum3: p.itemExtNum3 ?? 0,
      itemExtText1: p.itemExtText1 || undefined,
      itemExtText2: p.itemExtText2 || undefined,
      image: p.image || undefined,
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化成本调价单特有字段
if (formData.adjustType === undefined) formData.adjustType = 1
if (formData.reasonType === undefined) formData.reasonType = 1
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of ['warehouseName', 'handlerName', 'deptName', 'summary', 'reasonDesc', 'remark', 'createTime', 'creatorName', 'bookkeepingTime', 'printCount', 'defaultWarehouseId', 'defaultHandlerId', 'printTemplate', 'printPaperSize']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['defaultAdjustType', 'printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = key === 'defaultAdjustType' ? 1 : false
}
if (formData.status === undefined) formData.status = 0

// ════════════════════════════════════════════
// 计算属性
// ════════════════════════════════════════════

const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => COST_ADJUST_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => COST_ADJUST_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)
const availableActions = computed(() => getAvailableActions(currentStatus.value))

// 合计金额 = 各行调整金额之和
const totalAdjustAmount = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (((p.newCost || 0) - (p.oldCost || 0)) * (p.currentQuantity || 0)), 0)
)

// ════════════════════════════════════════════
// 页眉配置
// ════════════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '成本调价单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'export', label: '导出', icon: DownloadOutlined },
    ...availableActions.value.map((act: any) => ({ key: act.key, label: act.label, icon: act.icon })),
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ════════════════════════════════════════════
// 基本信息字段（页面对标文档11字段 + 调价类型/原因类型）
// ════════════════════════════════════════════

const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'warehouseId', label: '调价仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'adjustType', label: '调价类型', type: 'select', inlineLabel: true, width: 160 },
  { key: 'reasonType', label: '原因类型', type: 'select', inlineLabel: true, width: 160 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'totalAdjustAmount', label: '合计金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
]

// 默认隐藏非核心字段，避免与底部单据信息区重复显示
const DEFAULT_HIDDEN_FIELDS = [
  'deptId', 'summary', 'remark', 'creatorName', 'createTime', 'printCount', 'totalAdjustAmount',
]
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'stock-cost-adjust-form'
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
        defaultAdjustType: parsed.defaults.defaultAdjustType ?? 1,
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
      defaultAdjustType: formData.defaultAdjustType,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

// ── 部门下拉选项 ──
const departmentOptions = ref<any[]>([])

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
}

// 实际渲染的基本信息字段：过滤隐藏项 + 注入动态选项/加载态
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'warehouseId'
        ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
        : f.key === 'handlerId'
          ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
          : f.key === 'deptId'
            ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
            : f.key === 'adjustType'
              ? ADJUST_TYPE_OPTIONS
              : f.key === 'reasonType'
                ? REASON_TYPE_OPTIONS
                : (f as any).options,
      loading: (f.key === 'warehouseId' || f.key === 'handlerId' || f.key === 'deptId')
        ? loadingOptions.value
        : (f as any).loading,
      searchBtn: (f.key === 'warehouseId' || f.key === 'handlerId' || f.key === 'deptId')
        ? '+Q'
        : (f as any).searchBtn,
    }))
)

// 页面配置弹窗字段（含显隐状态）
const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    displayName: f.label,
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

// ════════════════════════════════════════════
// 摘要面板（合计金额）
// ════════════════════════════════════════════

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '调价项数', value: formData.products.length, statusLabel: statusText.value },
  { label: '合计金额', value: `¥${totalAdjustAmount.value.toFixed(2)}`, divider: true },
])

// ════════════════════════════════════════════
// 页脚
// ════════════════════════════════════════════

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '合计金额',
  amountValue: `¥${totalAdjustAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: currentStatus.value === 2 ? '记账执行' : (currentStatus.value === 0 ? '提交审批' : '提交'),
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ════════════════════════════════════════════
// 明细表格列（默认17列 · 全部26列）
// ════════════════════════════════════════════

const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productId', title: '商品名称', type: 'select', width: 220, showScanToggle: true, options: optionRefs.products.map((p: any) => ({ value: p.id, label: p.name })) },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'region', title: '区域', type: 'input', width: 100, defaultHidden: true },
  { key: 'location', title: '货位', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'conversionResult', title: '换算结果', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 100, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 100, precision: 2 },
  { key: 'currentQuantity', title: '库存数量', type: 'number', width: 100, precision: 0, readonly: true },
  { key: 'oldCost', title: '调前成本价', type: 'number', width: 100, precision: 2 },
  { key: 'oldAmount', title: '调前成本金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'newCost', title: '调后成本价', type: 'number', width: 100, precision: 2 },
  { key: 'newAmount', title: '调后成本金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'diffAmount', title: '调整金额(+/-)', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'itemExtNum1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'itemExtNum2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'itemExtNum3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'itemExtText1', title: '单据自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'itemExtText2', title: '单据自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
])

const tableSummaryColumns = computed(() => [
  { key: 'diffAmount', value: Number(totalAdjustAmount.value.toFixed(2)), highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ════════════════════════════════════════════
// 事件处理
// ════════════════════════════════════════════

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId') {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productName = p.name || ''
      record.productCode = p.code || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.specification = p.specification || ''
      record.model = p.model || ''
      record.origin = p.origin || ''
      record.brand = p.brand || ''
      record.taste = p.taste || ''
      record.location = p.location || ''
      record.unit = p.unit || ''
      record.wholesalePrice = p.wholesalePrice || p.wholesale_price || 0
      record.retailPrice = p.retailPrice || p.retail_price || 0
      record.currentQuantity = p.stock ?? 0
      record.oldCost = p.purchasePrice || 0
      record.oldAmount = (record.currentQuantity || 0) * (record.oldCost || 0)
      record.newAmount = (record.currentQuantity || 0) * (record.newCost || 0)
      record.diffAmount = (((record.newCost || 0) - (record.oldCost || 0)) * (record.currentQuantity || 0))
    }
  }
  if (['currentQuantity', 'oldCost', 'newCost'].includes(fieldKey)) {
    const qty = record.currentQuantity || 0
    const oldCost = record.oldCost || 0
    const newCost = record.newCost || 0
    record.oldAmount = qty * oldCost
    record.newAmount = qty * newCost
    record.diffAmount = (newCost - oldCost) * qty
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

function handlePrimarySubmit() {
  if (isLocked.value) return
  if (currentStatus.value === 2) {
    handleComplete()
  } else {
    handleSubmit()
  }
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/erp/stock-cost-adjust')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    case 'import':
      handleImport()
      break
    case 'export':
      handleExport()
      break
    case 'submit':
      handleSubmit()
      break
    case 'approve':
      handleApprove()
      break
    case 'reject':
      handleReject()
      break
    case 'complete':
      handleComplete()
      break
  }
}

// ── 打印(F8) ──
// ═══ 打印（按单据打印） ═══
// 成本调整单：后端已为 pageCode='stock-cost-adjust' 登记装配器并有已发布模板，
// 页面只给单据主键 —— 取数 / 挑模板 / 渲染都在服务端。
// 原先是 window.print() —— 打出来是整个后台界面（菜单、工具栏、翻页都跟着上纸）。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})

function handlePrint() {
  if (!formData.id) {
    message.warning('请先保存单据后再打印')
    return
  }
  printData.value = { id: formData.id }
  printDialogRef.value?.open?.()
}

// ── 导入：读本地文本/CSV 解析商品明细 ──
function handleImport() {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.accept = '.csv,.txt,.xlsx'
  fileInput.onchange = () => {
    const file = fileInput.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (e: any) => {
      const text = String(e.target?.result || '')
      parseImportText(text)
    }
    reader.readAsText(file)
  }
  fileInput.click()
}

function parseImportText(text: string) {
  const lines = text.split(/\r?\n/).map((l: string) => l.trim()).filter(Boolean)
  if (lines.length <= 1) {
    message.warning('未解析到有效明细数据')
    return
  }
  const header = lines[0].split(/[,\t]/).map((h: string) => h.trim())
  const idx = (names: string[]) => names.map((n) => header.findIndex((h) => h.includes(n)))
  const [nameIdx, oldCostIdx, newCostIdx] = idx(['商品', '调前成本', '调后成本'])
  if (nameIdx < 0) {
    message.warning('导入文件需包含「商品」列')
    return
  }
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(/[,\t]/).map((c: string) => c.trim())
    const oldCost = oldCostIdx >= 0 ? Number(cols[oldCostIdx] || 0) : 0
    const newCost = newCostIdx >= 0 ? Number(cols[newCostIdx] || 0) : 0
    const row: any = {
      id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
      productId: undefined,
      productName: cols[nameIdx] || '',
      currentQuantity: 0,
      oldCost,
      newCost,
      diffAmount: 0,
      oldAmount: 0,
      newAmount: 0,
    }
    formData.products.push({ ...row, ...productDefaultsForImport() })
  }
  message.success(`已导入 ${lines.length - 1} 行`)
  if (formData.products.length === 0) message.warning('导入数据为空')
}

function productDefaultsForImport() {
  return {
    itemCode: '', productCode: '', barcode: '', specification: '', model: '', origin: '', brand: '',
    taste: '', region: '', location: '', unit: '', conversionRelation: '', conversionResult: '',
    wholesalePrice: 0, retailPrice: 0, itemExtNum1: 0, itemExtNum2: 0, itemExtNum3: 0,
    itemExtText1: '', itemExtText2: '', image: '', remark: '',
  }
}

// ── 导出：当前单据明细导出 CSV ──
function handleExport() {
  if (formData.products.length === 0) {
    message.warning('没有可导出的明细')
    return
  }
  const headers = ['商品名称', '货号', '条码', '规格', '库存数量', '调前成本价', '调前成本金额', '调后成本价', '调后成本金额', '调整金额', '备注']
  const rows = formData.products
    .filter((p: any) => p.productName)
    .map((p: any) => [
      p.productName, p.itemCode || '', p.barcode || '', p.specification || '',
      p.currentQuantity, p.oldCost, p.oldAmount, p.newCost, p.newAmount, p.diffAmount, p.remark || '',
    ])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `成本调价单明细_${formData.orderNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ── 审批/记账 ──
function handleApprove() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '确认审核',
    content: `确认审核通过调价单 ${formData.orderNo} 吗？`,
    okText: '审核通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.approve(id)
        message.success('审核成功')
        await reloadDetail(id)
      } catch (err: any) {
        message.error(err?.message || '审核失败')
      }
    },
  })
}

function handleReject() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '驳回调价单',
    content: `确认驳回调价单 ${formData.orderNo} 吗？`,
    okText: '确认驳回',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.reject(id, '驳回')
        message.success('已驳回')
        await reloadDetail(id)
      } catch (err: any) {
        message.error(err?.message || '驳回失败')
      }
    },
  })
}

function handleComplete() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '记账执行确认',
    content: `确认完成调价单 ${formData.orderNo} 的记账操作吗？记账后库存成本单价将调整为调后成本价。`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.execute(id)
        message.success('记账完成，库存成本已调整')
        await reloadDetail(id)
      } catch (err: any) {
        message.error(err?.message || '记账失败')
      }
    },
  })
}

async function reloadDetail(id: number) {
  try {
    const res: any = await stockCostAdjustApi.getById(id)
    const data = res?.data || res || {}
    if (data.orderNo === undefined || data.orderNo === '') data.orderNo = data.adjustNo || ''
    Object.assign(formData, { ...data, status: data.status, orderNo: data.orderNo || formData.orderNo })
  } catch {
    // 静默
  }
}

// ════════════════════════════════════════════
// 辅助函数
// ════════════════════════════════════════════

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[成本调价] ErrorBoundary:', err)
}

// ════════════════════════════════════════════
// 生命周期
// ════════════════════════════════════════════

onMounted(async () => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
    // 经手人默认当前登录人
    if (!formData.handlerId && currentUserId.value) {
      formData.handlerId = currentUserId.value
      const u = optionRefs.users.find((x: any) => x.id === currentUserId.value)
      formData.handlerName = u?.name || currentUserName.value || ''
    }
    // 应用录单默认值
    if (formData.defaultWarehouseId) {
      formData.warehouseId = formData.defaultWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
      if (w) formData.warehouseName = w.warehouseName || w.name || ''
    }
    if (formData.defaultAdjustType !== undefined) formData.adjustType = formData.defaultAdjustType
  }
  loadFormConfig()
  loadExtraOptions()
})
</script>

<style scoped>
.action-add-btn {
  color: #1890ff;
  padding: 0;
  font-size: 14px;
}

.action-del-btn {
  color: #ff4d4f;
  padding: 0;
  font-size: 14px;
}

.remark-section {
  padding: 4px 0;
}

.remark-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.remark-label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
  min-width: 60px;
}

.remark-input {
  flex: 1;
}

.doc-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 0;
  font-size: 12px;
  color: #8c8c8c;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.config-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}
</style>
