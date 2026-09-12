<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入审批/执行流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        :model-value="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :tabs="tabsConfig"
        :summary="summaryConfig"
        :footer="footerConfig"
        @update:model-value="handleModelUpdate"
        @field-change="handleFieldChange"
        @action="handleHeaderAction"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 待付款单据核销明细 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.items"
            :summary-columns="detailSummaryColumns"
            :storage-key="'payment-doc-form-columns'"
            :view-mode="isLocked"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <a-space :size="2">
                <a-button type="link" size="small" class="action-btn" :disabled="isLocked" @click="handleAddRow()">
                  <PlusCircleOutlined />
                </a-button>
                <a-button v-if="!empty" type="link" size="small" class="action-btn danger" :disabled="isLocked" @click="handleRemoveRow(index)">
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
            <template #settleCell="{ record }">
              <a-checkbox
                v-model:checked="record._settle"
                :disabled="isLocked"
              />
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
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span v-if="formData.auditorName" class="doc-info-item">审核人 {{ formData.auditorName }}</span>
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
              <a-form-item label="默认结算单位">
                <a-select v-model:value="formData.defaultSupplierId" show-search size="small" style="width:100%"
                  :filter-option="filterOption" :options="supplierSelectOptions" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerName" show-search size="small" style="width:100%"
                  :filter-option="filterOption" :options="userSelectOptions" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认付款类型">
                <a-select v-model:value="formData.defaultPaymentType" size="small" style="width:100%" :options="paymentTypeOptions" @change="saveFormConfig" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PrinterOutlined, ExportOutlined, SettingOutlined,
  PlusCircleOutlined, MinusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { BasicInfoField, BillHeaderConfig, BillTabConfig, TabField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { paymentApi, payableApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { paymentMethodApi } from '@/api/payment/md'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

defineOptions({ name: 'FinancePaymentDocForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const idParam = computed(() => route.query.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new' || idParam.value === '0')
const currentUserName = computed(() => userStore.nickname || userStore.username || '')

// ═══ 状态字典 ═══
const PAYMENT_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'red' },
  4: { text: '待核销', color: 'purple' },
  5: { text: '核销中', color: 'cyan' },
  6: { text: '已核销', color: 'geekblue' },
  7: { text: '已完成', color: 'green' },
  8: { text: '已取消', color: 'default' },
}
const statusText = computed(() => PAYMENT_STATUS[formData.status ?? 0]?.text || '未知')
const statusColor = computed(() => PAYMENT_STATUS[formData.status ?? 0]?.color || 'default')
const isLocked = computed(() => !isNew.value && formData.status !== 0)

const paymentTypeOptions = [
  { label: '采购付款', value: 1 },
  { label: '预付', value: 2 },
  { label: '费用付款', value: 3 },
  { label: '其他', value: 4 },
]
const yesNoOptions = [
  { label: '是', value: 1 },
  { label: '否', value: 0 },
]

// ═══ 下拉选项 ═══
const supplierOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])
const methodOptions = ref<any[]>([])
const loadingOptions = ref(false)

const supplierSelectOptions = computed(() =>
  supplierOptions.value.map((s: any) => ({ label: `${s.name || ''}${s.code ? '(' + s.code + ')' : ''}`, value: s.id }))
)
const userSelectOptions = computed(() =>
  userOptions.value.map((u: any) => ({ label: u.name, value: u.id }))
)
const departmentSelectOptions = computed(() =>
  departmentOptions.value.map((d: any) => ({ label: d.name, value: d.id }))
)
const methodSelectOptions = computed(() =>
  methodOptions.value.map((m: any) => ({ label: `[${m.methodCode}] ${m.methodName}`, value: m.methodCode }))
)

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.name || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [ss, us, ds, ms] = await Promise.all([
      optionsApi.getSuppliers().catch(() => []),
      optionsApi.getUsers().catch(() => []),
      optionsApi.getDepartments().catch(() => []),
      paymentMethodApi.list().catch(() => []),
    ])
    supplierOptions.value = ss || []
    userOptions.value = us || []
    departmentOptions.value = ds || []
    methodOptions.value = Array.isArray(ms) ? ms : ms?.data || []
  } finally {
    loadingOptions.value = false
  }
}

// ═══ 表单数据 ═══
const formData = reactive<any>({
  id: undefined,
  paymentNo: '',
  paymentType: 1,
  supplierId: undefined,
  supplierName: '',
  supplierCode: '',
  paymentDate: dayjs().format('YYYY-MM-DD'),
  sourceNo: '',
  sourceType: '',
  handlerName: undefined,
  handlerId: undefined,
  departmentName: undefined,
  departmentId: undefined,
  prevPayable: 0,
  payableBalance: 0,
  prevPrepaid: 0,
  availablePrepaid: 0,
  prepaidBalance: 0,
  paymentAmount: 0,
  paymentAccount: undefined,
  moreAccount: '',
  usePrepaidAmount: 0,
  discountAmount: 0,
  overpayAmount: 0,
  summary: '',
  auditor: undefined,
  auditorName: '',
  showAllUnsettled: 0,
  remark: '',
  paymentMethod: undefined,
  bankName: '',
  bankAccount: '',
  checkNo: '',
  transactionNo: '',
  creatorName: '',
  createTime: '',
  printCount: 0,
  items: [] as any[],
  // 录单默认值
  defaultSupplierId: undefined,
  defaultHandlerName: undefined,
  defaultPaymentType: 1,
  printTemplate: 'standard',
  printCopies: 0,
  printPaperSize: 'A4',
})

// ═══ 顶部「表头」基础字段（页面配置可显隐） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'paymentNo', label: '编号', type: 'input', disabled: true },
  { key: 'supplierId', label: '结算单位', type: 'select', required: true },
  { key: 'supplierCode', label: '单位编号', type: 'input', disabled: true, width: 'narrow' },
  { key: 'handlerName', label: '经手人', type: 'select' },
  { key: 'departmentName', label: '部门', type: 'select' },
  { key: 'paymentDate', label: '单据日期', type: 'date', required: true },
  { key: 'paymentType', label: '单据类型', type: 'select', options: paymentTypeOptions },
  { key: 'sourceNo', label: '来源订单', type: 'input', width: 'narrow' },
  { key: 'showAllUnsettled', label: '显示所有未结算单据', type: 'select', options: yesNoOptions, width: 'narrow' },
  { key: 'summary', label: '摘要', type: 'input', width: 'narrow' },
  { key: 'auditor', label: '审核人', type: 'input', width: 'narrow', disabled: true },
  { key: 'remark', label: '单据备注', type: 'input', width: 'wide' },
]

// ═══ 底部「付款」Tab 金额字段（对标销售订单：金额/收付款字段放底部标签页） ═══
const ALL_PAYMENT_FIELDS: TabField[] = [
  { key: 'paymentAccount', label: '付款账户', type: 'select' },
  { key: 'paymentAmount', label: '付款金额', type: 'number', precision: 2, required: true, suffixBtn: '全/清' },
  { key: 'moreAccount', label: '更多账户', type: 'input', suffixBtn: '···' },
  { key: 'prevPayable', label: '此前应付', type: 'number', disabled: true, precision: 2 },
  { key: 'payableBalance', label: '应付余额', type: 'number', disabled: true, precision: 2 },
  { key: 'prevPrepaid', label: '此前预付', type: 'number', disabled: true, precision: 2 },
  { key: 'availablePrepaid', label: '可用预付', type: 'number', disabled: true, precision: 2 },
  { key: 'usePrepaidAmount', label: '使用预付款', type: 'number', precision: 2 },
  { key: 'prepaidBalance', label: '预付款余额', type: 'number', disabled: true, precision: 2 },
  { key: 'discountAmount', label: '优惠金额', type: 'number', precision: 2 },
  { key: 'overpayAmount', label: '多付金额', type: 'number', disabled: true, precision: 2 },
  { key: 'paymentMethod', label: '支付方式', type: 'select' },
  { key: 'bankName', label: '开户银行', type: 'input' },
  { key: 'bankAccount', label: '银行账号', type: 'input' },
  { key: 'checkNo', label: '支票号', type: 'input' },
  { key: 'transactionNo', label: '交易号', type: 'input' },
]

// ═══ 页面配置（字段显隐 · 含顶部 + 底部付款Tab 全量） ═══
const PAGE_FIELD_DEFAULTS: Record<string, { visible: boolean; enterJump: boolean }> = {
  paymentNo: { visible: true, enterJump: false },
  supplierId: { visible: true, enterJump: true },
  supplierCode: { visible: false, enterJump: false },
  handlerName: { visible: true, enterJump: true },
  departmentName: { visible: false, enterJump: false },
  paymentDate: { visible: true, enterJump: true },
  paymentType: { visible: true, enterJump: true },
  sourceNo: { visible: true, enterJump: true },
  showAllUnsettled: { visible: true, enterJump: false },
  summary: { visible: false, enterJump: false },
  auditor: { visible: false, enterJump: false },
  remark: { visible: true, enterJump: false },
  paymentAccount: { visible: true, enterJump: true },
  paymentAmount: { visible: true, enterJump: false },
  moreAccount: { visible: true, enterJump: false },
  prevPayable: { visible: true, enterJump: false },
  payableBalance: { visible: true, enterJump: false },
  prevPrepaid: { visible: true, enterJump: false },
  availablePrepaid: { visible: true, enterJump: false },
  usePrepaidAmount: { visible: true, enterJump: false },
  prepaidBalance: { visible: true, enterJump: false },
  discountAmount: { visible: true, enterJump: false },
  overpayAmount: { visible: true, enterJump: false },
  paymentMethod: { visible: true, enterJump: false },
  bankName: { visible: true, enterJump: false },
  bankAccount: { visible: true, enterJump: false },
  checkNo: { visible: true, enterJump: false },
  transactionNo: { visible: true, enterJump: false },
}

const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>(
  Object.fromEntries(Object.entries(PAGE_FIELD_DEFAULTS).map(([k, v]) => [k, { ...v }]))
)

// ═══ 顶部「表头」字段渲染：仅基础字段 ═══
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    // 单据备注在底部 bottom-extra 单据信息区，顶部不重复渲染
    .filter(f => pageConfig[f.key]?.visible !== false && f.key !== 'remark')
    .map(f => ({
      ...f,
      options: f.key === 'supplierId'
        ? supplierSelectOptions.value
        : f.key === 'handlerName'
          ? userSelectOptions.value
          : f.key === 'departmentName'
            ? departmentSelectOptions.value
            : (f as any).options,
      loading: ['supplierId', 'handlerName', 'departmentName'].includes(f.key)
        ? loadingOptions.value
        : (f as any).loading,
      searchBtn: ['supplierId', 'handlerName', 'departmentName'].includes(f.key)
        ? '+Q'
        : (f as any).searchBtn,
    }))
)

// ═══ 底部「付款」Tab ═══
const tabsConfig = computed<BillTabConfig[]>(() => [
  {
    key: 'payment',
    tab: '付款',
    fields: ALL_PAYMENT_FIELDS
      .filter(f => pageConfig[f.key]?.visible !== false)
      .map(f => ({
        ...f,
        options: f.key === 'paymentAccount' || f.key === 'paymentMethod'
          ? methodSelectOptions.value
          : (f as any).options,
      })) as TabField[],
  },
])

// ═══ 页面配置弹窗字段（顶部 + 底部付款Tab 全量） ═══
const pageConfigFields = computed(() =>
  [...ALL_BASIC_INFO_FIELDS, ...ALL_PAYMENT_FIELDS].map((f, i) => ({
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

const FORM_CONFIG_MODULE = 'payment-doc-form'
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
        defaultSupplierId: parsed.defaults.defaultSupplierId,
        defaultHandlerName: parsed.defaults.defaultHandlerName,
        defaultPaymentType: parsed.defaults.defaultPaymentType ?? 1,
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
      defaultSupplierId: formData.defaultSupplierId,
      defaultHandlerName: formData.defaultHandlerName,
      defaultPaymentType: formData.defaultPaymentType,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 配置弹窗 ═══
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

// ═══ Header 配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '付款单',
  orderNo: formData.paymentNo || '待生成',
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

function handleHeaderAction(key: string) {
  if (key === 'print') handlePrint()
  else if (key === 'export') handleExport()
  else if (key === 'config') showFormConfig.value = true
}

// ═══ BillFormPage 协同 ═══
function handleModelUpdate(newVal: Record<string, any>) {
  Object.assign(formData, newVal)
  onFieldValueChange()
}
function handleSearchBtn(_fieldKey: string, _btn: string) {
  // 预留：搜索按钮弹选择器
}
function handleFieldChange(fieldKey: string, val: any) {
  if (fieldKey === 'supplierId') {
    const opt = supplierOptions.value.find((s: any) => s.id === val)
    formData.supplierName = opt?.name || ''
    formData.supplierCode = opt?.code || ''
    loadPayables(val)
  }
  onFieldValueChange()
}

function onFieldValueChange() {
  // 付款金额守恒：本单金额 = 付款金额 + 使用预付款
  const settleTotal = formData.items
    .filter((r: any) => r._settle)
    .reduce((s: number, r: any) => s + Number(r.currentSettle || 0), 0)
  const settleDiscount = formData.items
    .filter((r: any) => r._settle)
    .reduce((s: number, r: any) => s + Number(r.currentDiscount || 0), 0)
  const payableBalance = Math.max(0, Number(formData.prevPayable || 0) - settleTotal - settleDiscount)
  formData.payableBalance = payableBalance
  formData.prepaidBalance = Math.max(0, Number(formData.prevPrepaid || 0) - Number(formData.usePrepaidAmount || 0))
  formData.overpayAmount = Math.max(0, Number(formData.paymentAmount || 0) + Number(formData.usePrepaidAmount || 0) - settleTotal)
}

// ═══ 核销明细（待付款单据 = 应付来源） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 56, fixed: 'left' },
  { key: 'settle', title: '结算', type: 'slot', slotName: 'settleCell', width: 60, fixed: 'left' },
  { key: 'billDate', title: '单据日期', type: 'input', width: 110 },
  { key: 'billType', title: '单据类型', type: 'input', width: 90 },
  { key: 'billNo', title: '单据编号', type: 'input', width: 170 },
  { key: 'tradeUnit', title: '往来单位', type: 'input', width: 150 },
  { key: 'settleUnitCode', title: '结算单位编号', type: 'input', width: 100, defaultHidden: true },
  { key: 'settleUnit', title: '结算单位', type: 'input', width: 150 },
  { key: 'paymentTerm', title: '付款期限', type: 'input', width: 110, defaultHidden: true },
  { key: 'totalProductAmount', title: '总商品金额', type: 'number', width: 110, precision: 2 },
  { key: 'purchaseAmount', title: '采购金额', type: 'number', width: 110, precision: 2 },
  { key: 'discountAmount', title: '优惠金额', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'otherFee', title: '费用', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'billAmount', title: '本单金额', type: 'number', width: 120, precision: 2 },
  { key: 'freight', title: '运费金额', type: 'number', width: 90, precision: 2 },
  { key: 'settledAmount', title: '已结算', type: 'number', width: 100, precision: 2 },
  { key: 'pendingAmount', title: '待审金额', type: 'number', width: 100, precision: 2 },
  { key: 'unsettledAmount', title: '未结算', type: 'number', width: 100, precision: 2 },
  { key: 'currentDiscount', title: '本次优惠', type: 'number', width: 100, precision: 2 },
  { key: 'currentSettle', title: '本次结算', type: 'number', width: 110, precision: 2 },
  { key: 'sourceOrder', title: '来源订单', type: 'input', width: 140, defaultHidden: true },
  { key: 'handlerName', title: '经手人', type: 'input', width: 100 },
  { key: 'attachment', title: '附件', type: 'input', width: 80, defaultHidden: true },
  { key: 'remark', title: '单据备注', type: 'input', width: 150 },
]

const detailSummaryColumns = computed(() => {
  const totalBill = formData.items.reduce((s: number, r: any) => s + Number(r.billAmount || 0), 0)
  const totalSettle = formData.items.reduce((s: number, r: any) => s + Number(r.currentSettle || 0), 0)
  return [
    { key: 'billAmount', value: totalBill.toFixed(2), highlight: true },
    { key: 'currentSettle', value: totalSettle.toFixed(2), highlight: true },
  ]
})

async function loadPayables(supplierId: number) {
  try {
    const res: any = await payableApi.getPage({ supplierId: String(supplierId), page: 1, size: 500 })
    const list: any[] = res?.records || res?.list || []
    formData.items = list
      .filter((r: any) => r.status !== 'written_off' && Number(r.remainingAmount) > 0)
      .map((r: any) => ({
        id: r.id,
        billDate: r.dueDate || r.paymentDate || '',
        billType: r.sourceType === 'purchase_inbound' ? '采购入库单'
          : r.sourceType === 'purchase_order' ? '采购订单'
          : r.sourceType || '',
        billNo: r.sourceNo || r.orderNo || '',
        tradeUnit: r.supplierName || '',
        settleUnit: r.supplierName || '',
        paymentTerm: r.dueDate || '',
        totalProductAmount: r.totalAmount || 0,
        purchaseAmount: r.totalAmount || 0,
        billAmount: r.totalAmount || 0,
        settledAmount: r.paidAmount || 0,
        unsettledAmount: r.remainingAmount || 0,
        pendingAmount: r.pendingAmount || 0,
        handlerName: r.handlerName || r.salesPersonName || '',
        remark: r.remark || '',
        _settle: false,
        currentSettle: 0,
        currentDiscount: 0,
        _source: r,
      }))
    onFieldValueChange()
  } catch {
    formData.items = []
  }
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  record[fieldKey] = value
  if (fieldKey === 'currentSettle' || fieldKey === 'currentDiscount' || fieldKey === 'settle') {
    onFieldValueChange()
  }
}

// ═══ 核销明细：手动加/删行 ═══
function handleAddRow() {
  formData.items.push({
    _key: `manual_${Date.now()}`,
    _settle: false,
    billDate: dayjs().format('YYYY-MM-DD'),
    billType: '手工核销',
    billNo: '',
    tradeUnit: '',
    settleUnit: '',
    billAmount: 0,
    settledAmount: 0,
    unsettledAmount: 0,
    currentSettle: 0,
    currentDiscount: 0,
    _source: null,
  })
  onFieldValueChange()
}

function handleRemoveRow(index: number) {
  if (index >= 0 && index < formData.items.length) {
    formData.items.splice(index, 1)
    onFieldValueChange()
  }
}

// ═══ 摘要面板 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '付款金额', value: Number(formData.paymentAmount || 0).toFixed(2), statusLabel: statusText.value },
  { label: '使用预付款', value: Number(formData.usePrepaidAmount || 0).toFixed(2) },
  { label: '优惠金额', value: Number(formData.discountAmount || 0).toFixed(2) },
  { label: '多付金额', value: Number(formData.overpayAmount || 0).toFixed(2), divider: true },
  { label: '应付余额', value: Number(formData.payableBalance || 0).toFixed(2) },
  { label: '预付款余额', value: Number(formData.prepaidBalance || 0).toFixed(2) },
  { label: '本单金额', value: `¥${(Number(formData.paymentAmount || 0) + Number(formData.usePrepaidAmount || 0)).toFixed(2)}` },
])

// ═══ 页脚 ═══
const saving = ref(false)
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${(Number(formData.paymentAmount || 0) + Number(formData.usePrepaidAmount || 0)).toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  primaryBtnText: isLocked.value ? undefined : '记账',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryShortcut: isLocked.value ? undefined : 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 提交流程 ═══
function buildPayload(): any {
  const items = formData.items
    .filter((r: any) => r._settle)
    .map((r: any) => ({
      orderId: r._source?.sourceId || undefined,
      orderNo: r.billNo || undefined,
      invoiceId: r._source?.sourceType === 'invoice' ? r._source?.sourceId : undefined,
      invoiceNo: r._source?.invoiceNo || undefined,
      orderAmount: Number(r.billAmount || 0),
      invoiceAmount: r._source?.sourceType === 'invoice' ? Number(r.billAmount || 0) : undefined,
      pendingAmount: Number(r.unsettledAmount || 0),
      verifyAmount: Number(r.currentSettle || 0),
      payableId: r._source?.payableId || r._source?.id || undefined,
      sourceType: r._source?.sourceType || undefined,
      settleUnit: r.settleUnit || undefined,
      tradeUnit: r.tradeUnit || undefined,
      settlementNo: r.billNo || undefined,
      currentSettle: Number(r.currentSettle || 0),
      currentDiscount: Number(r.currentDiscount || 0),
      billAmount: Number(r.billAmount || 0),
    }))
  return {
    paymentType: formData.paymentType,
    supplierId: formData.supplierId,
    supplierName: formData.supplierName,
    paymentDate: formData.paymentDate,
    paymentAmount: formData.paymentAmount,
    usePrepaidAmount: formData.usePrepaidAmount,
    discountAmount: formData.discountAmount,
    overpayAmount: formData.overpayAmount,
    prevPayable: formData.prevPayable,
    payableBalance: formData.payableBalance,
    prepaidBalance: formData.prepaidBalance,
    paymentAccount1: formData.paymentAccount,
    paymentAmount1: formData.paymentAmount,
    paymentMethod: formData.paymentMethod || formData.paymentAccount,
    bankName: formData.bankName || undefined,
    bankAccount: formData.bankAccount || undefined,
    checkNo: formData.checkNo || undefined,
    transactionNo: formData.transactionNo || undefined,
    purchaserName: formData.handlerName,
    departmentName: formData.departmentName,
    sourceNo: formData.sourceNo || undefined,
    remark: formData.remark || undefined,
    items: items.length > 0 ? items : undefined,
  }
}

async function handleSaveDraft() {
  if (!formData.supplierId) { message.warning('请选择结算单位'); return }
  saving.value = true
  try {
    const payload = buildPayload()
    if (formData.id) {
      await paymentApi.update(formData.id, payload)
    } else {
      const res: any = await paymentApi.create(payload)
      if (res?.id) formData.id = res.id
    }
    message.success('保存草稿成功')
    afterSave()
  } catch (e: any) {
    message.error(e?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePrimarySubmit() {
  if (!formData.supplierId) { message.warning('请选择结算单位'); return }
  if (!formData.paymentAmount || formData.paymentAmount <= 0) { message.warning('请输入付款金额'); return }
  saving.value = true
  try {
    let id = formData.id
    if (!id) {
      const payload = buildPayload()
      const res: any = await paymentApi.create(payload)
      id = res?.id
      if (id) {
        formData.id = id
        await paymentApi.submit(id)
        await paymentApi.approve(id)
      }
    } else {
      await paymentApi.submit(id)
      await paymentApi.approve(id)
    }
    message.success('记账成功')
    afterSave()
  } catch (e: any) {
    message.error(e?.data?.message || '记账失败')
  } finally {
    saving.value = false
  }
}

function afterSave() {
  router.push('/finance/payment-doc')
}

// ═══ 表头操作 ═══
function handlePrint() {
  message.info('打印(F8)待对接打印模板')
}
async function handleExport() {
  try {
    const res = await request.get('/erp/payment/export', {
      params: { paymentNo: formData.paymentNo, supplierId: formData.supplierId },
      responseType: 'blob',
    })
    const url = window.URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = `付款单_${formData.paymentDate || ''}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ═══ 详情加载 ═══
async function loadDetail(id: number) {
  try {
    const res: any = await paymentApi.getById(id)
    if (!res) return
    Object.assign(formData, {
      id: res.id,
      paymentNo: res.paymentNo || '',
      paymentType: res.paymentType ?? 1,
      supplierId: res.supplierId,
      supplierName: res.supplierName || '',
      supplierCode: res.supplierCode || '',
      paymentDate: res.paymentDate || dayjs().format('YYYY-MM-DD'),
      paymentAmount: res.paymentAmount || 0,
      usePrepaidAmount: res.usePrepaidAmount || 0,
      discountAmount: res.discountAmount || 0,
      overpayAmount: res.overpayAmount || 0,
      prevPayable: res.prevPayable || 0,
      payableBalance: res.payableBalance || 0,
      prepaidBalance: res.prepaidBalance || 0,
      paymentMethod: res.paymentMethod,
      paymentAccount: res.paymentAccount1,
      bankName: res.bankName || '',
      bankAccount: res.bankAccount || '',
      checkNo: res.checkNo || '',
      transactionNo: res.transactionNo || '',
      purchaserName: res.purchaserName || '',
      departmentName: res.departmentName || '',
      sourceNo: res.sourceNo || '',
      summary: res.summary || '',
      remark: res.remark || '',
      creatorName: res.creatorName || '',
      createTime: res.createTime || '',
      printCount: res.printCount || 0,
      status: res.status ?? 0,
    })
    if (res.items?.length) {
      formData.items = res.items.map((d: any) => ({
        ...d,
        billDate: d.paymentDate || '',
        billNo: d.orderNo || d.settlementNo || d.invoiceNo || '',
        billAmount: Number(d.billAmount || d.orderAmount || 0),
        unsettledAmount: Number(d.unsettledAmount || d.pendingAmount || 0),
        currentSettle: Number(d.currentSettle || d.verifyAmount || 0),
        currentDiscount: Number(d.currentDiscount || 0),
        _settle: Number(d.currentSettle || d.verifyAmount || 0) > 0,
        _source: d,
      }))
      onFieldValueChange()
    }
  } catch (e) {
    console.warn('[付款单] 加载失败', e)
  }
}

function generateBillNo() {
  // 文档：单号前缀 FKD-，前端生成，后端 save 原样落库。
  // 后端 /next-no 返回裸 String（非 wrapper），会触发 request 拦截器误判为业务错误而弹"请求失败"，
  // 故参照收款单(doc)前端本地生成，不依赖该接口。
  formData.paymentNo = `FKD-${dayjs().format('YYYYMMDD')}-001`
}

function formatNow(): string {
  return dayjs().format('YYYY-MM-DD HH:mm:ss')
}

const handleError = (error: Error) => {
  console.error('[付款单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(async () => {
  await loadOptions()
  loadFormConfig()
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId && editId > 0) {
    await loadDetail(editId)
  } else {
    await generateBillNo()
    if (formData.defaultSupplierId) formData.supplierId = formData.defaultSupplierId
    if (formData.defaultHandlerName) formData.handlerName = formData.defaultHandlerName
    if (formData.defaultPaymentType) formData.paymentType = formData.defaultPaymentType
  }
})
</script>

<style scoped>
.config-hint { color: #999; font-size: 13px; margin-bottom: 8px; }
.remark-section { display: flex; flex-direction: column; gap: 6px; padding: 6px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; }
.remark-label { font-size: 13px; color: #595959; white-space: nowrap; flex-shrink: 0; }
.remark-input { flex: 1; max-width: 420px; }
.doc-info-row { display: flex; flex-wrap: wrap; gap: 12px; padding: 8px 0; }
.doc-info-item { font-size: 13px; color: #595959; display: inline-flex; align-items: center; gap: 4px; }
</style>
