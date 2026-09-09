<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="false"
        :tabs="tabs"
        :active-tab="activeTab"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px">
              <PlusOutlined />
            </a-button>
          </div>
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in quickDates"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
          <!-- 发票查询：销售/采购子Tab -->
          <a-radio-group
            v-if="activeTab === 'invoice'"
            v-model:value="invoiceDirection"
            size="small"
            class="invoice-direction"
            @change="handleInvoiceDirectionChange"
          >
            <a-radio-button value="sales">销售发票查询</a-radio-button>
            <a-radio-button value="purchase">采购发票查询</a-radio-button>
          </a-radio-group>
        </template>

        <!-- ═══ 工具栏右侧：功能按钮（随Tab变化） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <template v-if="activeTab === 'pending'">
              <a-button
                v-if="fnEnabled('noSettleWriteOff')"
                size="small"
                @click="handleNoSettleWriteOff"
              >
                <ToolOutlined /> 无结算付款单核销
              </a-button>
              <a-button
                v-if="fnEnabled('mergePay')"
                size="small"
                @click="handleMergePay"
              >
                <MergeCellsOutlined /> 合并付款
              </a-button>
              <a-button
                v-if="fnEnabled('forceSettle')"
                size="small"
                @click="handleForceSettle"
              >
                <ThunderboltOutlined /> 强制结算
              </a-button>
            </template>
            <template v-if="activeTab === 'all'">
              <a-button v-if="fnEnabled('getInvoice')" size="small" @click="handleGetInvoice">
                <FileDoneOutlined /> 取得发票
              </a-button>
            </template>
            <a-button v-if="fnEnabled('refresh')" size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="fnEnabled('printF8')" size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="fnEnabled('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="列配置">
              <a-button size="small" @click="showColumnConfig = true">
                <TableOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template v-for="field in visibleSearchFields" :key="field.key">
                <!-- 日期范围 -->
                <div v-if="field.type === 'dateRange'" class="search-field-item">
                  <a-range-picker
                    v-model:value="documentDateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleDocumentDateChange"
                  />
                </div>
                <!-- 下拉选择 -->
                <div v-else-if="field.type === 'select'" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">{{ field.label }}</span>
                    <a-select
                      v-model:value="searchParams[field.key]"
                      placeholder="全部"
                      allow-clear
                      size="small"
                    >
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <!-- 勾选 -->
                <div v-else-if="field.type === 'checkbox'" class="search-field-item">
                  <a-checkbox v-model:checked="searchParams[field.key]">
                    {{ field.label }}
                  </a-checkbox>
                </div>
                <!-- 文本输入 -->
                <div v-else class="search-field-item">
                  <a-input
                    v-model:value="searchParams[field.key]"
                    :placeholder="field.placeholder || field.label"
                    allow-clear
                    size="small"
                  />
                </div>
              </template>
              <div class="search-field-item">
                <a-button type="primary" size="small" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="visibleColumns"
            :data-source="tableData"
            :loading="loading"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="activeTab === 'pending'"
            row-key="rowKey"
            @page-change="handlePageChange"
            @selection-change="handleSelectionChange"
          >
            <!-- 操作列 -->
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button
                  v-if="activeTab === 'pending' || activeTab === 'all'"
                  type="link"
                  size="small"
                  @click="handleReconcile(record)"
                >对账</a-button>
                <a-button
                  v-if="activeTab === 'pending' || activeTab === 'all'"
                  type="link"
                  size="small"
                  @click="handleRowPay(record)"
                >付款</a-button>
                <a-button type="link" size="small" @click="handleRowView(record)">查看</a-button>
              </a-space>
            </template>
            <!-- 单据类型文本 -->
            <template #documentTypeCell="{ record }">
              {{ DOC_TYPE_LABELS[record.documentType] || record.documentType || '-' }}
            </template>
            <!-- 结算状态 -->
            <template #settlementStatusCell="{ record }">
              <a-tag
                :color="record.settlementStatus === '已结算' ? 'green' : 'orange'"
                size="small"
              >{{ record.settlementStatus || '未结算' }}</a-tag>
            </template>
            <!-- 对账 -->
            <template #reconcileCell="{ record }">
              <a-tag
                :color="record.reconcile ? 'green' : 'default'"
                size="small"
                style="cursor: pointer"
                @click="handleReconcile(record)"
              >{{ record.reconcile ? '√' : '否' }}</a-tag>
            </template>
            <!-- 发票类型 -->
            <template #invoiceTypeCell="{ record }">
              {{ record.invoiceTypeLabel || record.invoiceType || '-' }}
            </template>
          </BillTableList>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="panelColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SearchOutlined, SettingOutlined, TableOutlined,
  PrinterOutlined, ExportOutlined, ToolOutlined, MergeCellsOutlined, ThunderboltOutlined,
  FileDoneOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { paymentByDocApi } from '@/api/finance'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'FinancePaymentByDoc' })
const router = useRouter()
const userStore = useUserStore()

// ═══ 阶段 Tab ═══
const tabs = [
  { key: 'pending', label: '待付款' },
  { key: 'all', label: '全部单据' },
  { key: 'invoice', label: '发票查询' },
]
const activeTab = ref<'pending' | 'all' | 'invoice'>('pending')

// 发票查询子方向
const invoiceDirection = ref<'sales' | 'purchase'>('sales')

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' },
]
const quickDate = ref('week')
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const showPageConfig = ref(false)
const showColumnConfig = ref(false)
const selectedRows = ref<any[]>([])

// ═══ 日期范围 ═══
const documentDateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive<any>({
  documentNo: '',
  documentType: '',
  supplierName: '',
  settlementUnit: '',
  handlerName: '',
  sourceOrder: '',
  reconcile: '',
  reconcileMarkBy: '',
  invoiceNumber: '',
  invoiceCode: '',
  invoiceType: '',
  partnerName: '',
  issuedByName: '',
  invoiceObtained: '',
  showNoSettle: false,
  dateStart: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  dateEnd: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 单据类型中文 ═══
const DOC_TYPE_LABELS: Record<string, string> = {
  INBOUND: '采购入库单',
  RETURN: '采购退货单',
  EXCHANGE: '采购换货单',
}

// ═══ 快捷日期处理 ═══
function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.subtract(7, 'day'); end = now
  }
  documentDateRange.value = [start, end]
  searchParams.dateStart = start.format('YYYY-MM-DD')
  searchParams.dateEnd = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDocumentDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.dateStart = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.dateEnd = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.dateStart = ''
    searchParams.dateEnd = ''
  }
}

// ═══ 搜索字段定义（每个Tab独立的查询条件） ═══
interface SearchFieldDef {
  key: string
  label: string
  type: 'input' | 'select' | 'dateRange' | 'checkbox'
  placeholder?: string
  options?: Array<{ label: string; value: string }>
}

const PENDING_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '日期', type: 'dateRange' },
  { key: 'documentNo', label: '单据编号', type: 'input', placeholder: '单据编号' },
  { key: 'sourceOrder', label: '来源订单', type: 'input', placeholder: '来源订单' },
  { key: 'supplierName', label: '往来单位', type: 'input', placeholder: '往来单位' },
  { key: 'settlementUnit', label: '结算单位', type: 'input', placeholder: '结算单位' },
  {
    key: 'documentType', label: '单据类型', type: 'select',
    options: [
      { label: '采购入库单', value: 'INBOUND' },
      { label: '采购退货单', value: 'RETURN' },
      { label: '采购换货单', value: 'EXCHANGE' },
    ],
  },
  { key: 'handlerName', label: '经手人', type: 'input', placeholder: '经手人' },
  {
    key: 'reconcile', label: '对账', type: 'select',
    options: [
      { label: '√', value: 'Y' },
      { label: '否', value: 'N' },
    ],
  },
  { key: 'reconcileMarkBy', label: '对账标记人', type: 'input', placeholder: '对账标记人' },
  { key: 'showNoSettle', label: '显示无结算付款单的业务单据', type: 'checkbox' },
]

const ALL_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '日期', type: 'dateRange' },
  { key: 'documentNo', label: '单据编号', type: 'input', placeholder: '单据编号' },
  { key: 'supplierName', label: '往来单位', type: 'input', placeholder: '往来单位' },
  { key: 'settlementUnit', label: '结算单位', type: 'input', placeholder: '结算单位' },
  {
    key: 'reconcile', label: '对账', type: 'select',
    options: [
      { label: '√', value: 'Y' },
      { label: '否', value: 'N' },
    ],
  },
  { key: 'reconcileMarkBy', label: '对账标记人', type: 'input', placeholder: '对账标记人' },
  {
    key: 'documentType', label: '单据类型', type: 'select',
    options: [
      { label: '采购入库单', value: 'INBOUND' },
      { label: '采购退货单', value: 'RETURN' },
      { label: '采购换货单', value: 'EXCHANGE' },
    ],
  },
  {
    key: 'invoiceObtained', label: '是否取得', type: 'select',
    options: [
      { label: '是', value: 'Y' },
      { label: '否', value: 'N' },
    ],
  },
  { key: 'invoiceNumber', label: '发票号码', type: 'input', placeholder: '发票号码' },
  { key: 'invoiceCode', label: '发票代码', type: 'input', placeholder: '发票代码' },
  { key: 'showNoSettle', label: '显示无结算付款单的业务单据', type: 'checkbox' },
]

const INVOICE_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '日期', type: 'dateRange' },
  { key: 'invoiceCode', label: '发票代码', type: 'input', placeholder: '发票代码' },
  { key: 'invoiceNumber', label: '发票号码', type: 'input', placeholder: '发票号码' },
  {
    key: 'invoiceType', label: '发票类型', type: 'select',
    options: [
      { label: '销售发票', value: 'SALES_INVOICE' },
      { label: '采购发票', value: 'PURCHASE_INVOICE' },
      { label: '税务发票', value: 'TAX_INVOICE' },
      { label: '电子发票', value: 'ELECTRONIC_INVOICE' },
      { label: '普通发票', value: 'REGULAR_INVOICE' },
      { label: '专用发票', value: 'SPECIAL_INVOICE' },
    ],
  },
  { key: 'partnerName', label: '往来单位', type: 'input', placeholder: '往来单位' },
  { key: 'issuedByName', label: '开票人', type: 'input', placeholder: '开票人' },
]

// 当前Tab的可见搜索字段（联动页面配置显隐）
const currentSearchFields = computed(() => {
  if (activeTab.value === 'pending') return PENDING_SEARCH_FIELDS
  if (activeTab.value === 'all') return ALL_SEARCH_FIELDS
  return INVOICE_SEARCH_FIELDS
})

const visibleSearchFields = computed(() => {
  const config = queryFieldsConfig.value
  return currentSearchFields.value.filter(f => {
    const c = config.find(cf => cf.key === f.key)
    return c ? c.visible : true
  })
})

// ═══ 列定义（按文档规格） ═══
interface ColumnDef {
  title: string
  key: string
  field?: string
  type?: string
  slotName?: string
  width?: number
  fixed?: string
  align?: string
  defaultHidden?: boolean
  sortable?: boolean
}

// 待付款 (29列，默认显示14)
const PENDING_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 130, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'documentDate', key: 'documentDate', width: 100, sortable: true },
  { title: '单据类型', field: 'documentType', key: 'documentType', width: 100, slotName: 'documentTypeCell' },
  { title: '单据编号', field: 'documentNo', key: 'documentNo', width: 150, sortable: true },
  { title: '往来单位编号', field: 'supplierCode', key: 'supplierCode', width: 120, defaultHidden: true },
  { title: '往来单位', field: 'supplierName', key: 'supplierName', width: 160, defaultHidden: true, sortable: true },
  { title: '结算单位', field: 'supplierName', key: 'settlementUnit', width: 160 },
  { title: '运单号', field: 'waybillNo', key: 'waybillNo', width: 110, defaultHidden: true },
  { title: '付款日期', field: 'paymentDate', key: 'paymentDate', width: 100 },
  { title: '动态付款期限', field: 'dynamicPayTerm', key: 'dynamicPayTerm', width: 100, defaultHidden: true },
  { title: '固定账期', field: 'fixedTerms', key: 'fixedTerms', width: 90, defaultHidden: true },
  { title: '结算期', field: 'settlePeriod', key: 'settlePeriod', width: 90, defaultHidden: true },
  { title: '仓库', field: 'inboundWarehouse', key: 'warehouse', width: 100, defaultHidden: true },
  { title: '来源订单', field: 'sourceOrder', key: 'sourceOrder', width: 140, defaultHidden: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, defaultHidden: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 90, defaultHidden: true },
  { title: '商品金额', field: 'amount', key: 'productAmount', width: 100, defaultHidden: true, align: 'right' },
  { title: '优惠金额', field: 'discountAmount', key: 'discountAmount', width: 90, defaultHidden: true, align: 'right' },
  { title: '费用', field: 'fee', key: 'fee', width: 90, defaultHidden: true, align: 'right' },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 110, align: 'right' },
  { title: '待审金额', field: 'pendingApproveAmount', key: 'pendingApproveAmount', width: 110, align: 'right' },
  { title: '未结金额', field: 'unsettledAmount', key: 'unsettledAmount', width: 110, align: 'right' },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 70, defaultHidden: true, align: 'right' },
  { title: '对账', field: 'reconcile', key: 'reconcile', width: 80, slotName: 'reconcileCell' },
  { title: '最后对账标记时间', field: 'lastReconcileTime', key: 'lastReconcileTime', width: 130 },
  { title: '最后对账标记人', field: 'lastReconcileBy', key: 'lastReconcileBy', width: 110 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 70 },
]

// 全部单据 (33列，默认显示17)
const ALL_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '对账', field: 'reconcile', key: 'reconcile', width: 80, slotName: 'reconcileCell' },
  { title: '单据日期', field: 'documentDate', key: 'documentDate', width: 100, sortable: true },
  { title: '单据类型', field: 'documentType', key: 'documentType', width: 100, slotName: 'documentTypeCell' },
  { title: '单据编号', field: 'documentNo', key: 'documentNo', width: 150, sortable: true },
  { title: '发票代码', field: 'invoiceCode', key: 'invoiceCode', width: 110, defaultHidden: true },
  { title: '发票号码', field: 'invoiceNumber', key: 'invoiceNumber', width: 130 },
  { title: '开票金额', field: 'amount', key: 'invoiceAmount', width: 100, align: 'right' },
  { title: '税额', field: 'taxAmount', key: 'taxAmount', width: 90, align: 'right' },
  { title: '往来单位编号', field: 'supplierCode', key: 'supplierCode', width: 120, defaultHidden: true },
  { title: '往来单位', field: 'supplierName', key: 'supplierName', width: 160, defaultHidden: true, sortable: true },
  { title: '结算单位', field: 'supplierName', key: 'settlementUnit', width: 160 },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 90, slotName: 'settlementStatusCell' },
  { title: '运单号', field: 'waybillNo', key: 'waybillNo', width: 110, defaultHidden: true },
  { title: '付款日期', field: 'paymentDate', key: 'paymentDate', width: 100 },
  { title: '动态付款期限', field: 'dynamicPayTerm', key: 'dynamicPayTerm', width: 100, defaultHidden: true },
  { title: '固定账期', field: 'fixedTerms', key: 'fixedTerms', width: 90, defaultHidden: true },
  { title: '结算期', field: 'settlePeriod', key: 'settlePeriod', width: 90, defaultHidden: true },
  { title: '仓库', field: 'inboundWarehouse', key: 'warehouse', width: 100, defaultHidden: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, defaultHidden: true },
  { title: '来源订单', field: 'sourceOrder', key: 'sourceOrder', width: 140 },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 90, defaultHidden: true },
  { title: '商品金额', field: 'amount', key: 'productAmount', width: 100, defaultHidden: true, align: 'right' },
  { title: '优惠金额', field: 'discountAmount', key: 'discountAmount', width: 90, defaultHidden: true, align: 'right' },
  { title: '费用', field: 'fee', key: 'fee', width: 90, defaultHidden: true, align: 'right' },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 110, defaultHidden: true, align: 'right' },
  { title: '强制结算金额', field: 'forceSettleAmount', key: 'forceSettleAmount', width: 110, defaultHidden: true, align: 'right' },
  { title: '待审金额', field: 'pendingApproveAmount', key: 'pendingApproveAmount', width: 110, align: 'right' },
  { title: '未结金额', field: 'unsettledAmount', key: 'unsettledAmount', width: 110, align: 'right' },
  { title: '最后对账标记时间', field: 'lastReconcileTime', key: 'lastReconcileTime', width: 130 },
  { title: '最后对账标记人', field: 'lastReconcileBy', key: 'lastReconcileBy', width: 110 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 70 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120, defaultHidden: true },
]

// 发票查询：销售/采购共用 (19列，默认显示11)
const INVOICE_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '发票日期', field: 'invoiceDate', key: 'invoiceDate', width: 100, defaultHidden: true },
  { title: '发票号码', field: 'invoiceNumber', key: 'invoiceNumber', width: 140, sortable: true },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 170 },
  { title: '发票代码', field: 'invoiceCode', key: 'invoiceCode', width: 120 },
  { title: '发票类型', field: 'invoiceTypeLabel', key: 'invoiceTypeLabel', width: 100, slotName: 'invoiceTypeCell' },
  { title: '开具单据笔数', field: 'docCount', key: 'docCount', width: 100, align: 'right' },
  { title: '开票金额', field: 'subtotalAmount', key: 'subtotalAmount', width: 110, align: 'right' },
  { title: '税率(%)', field: 'taxRate', key: 'taxRate', width: 90, align: 'right' },
  { title: '价税合计', field: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '附件', field: 'attachmentPaths', key: 'attachmentPaths', width: 70 },
  { title: '税额', field: 'taxAmount', key: 'taxAmount', width: 100, defaultHidden: true, align: 'right' },
  { title: '开票人', field: 'issuedByName', key: 'issuedByName', width: 100, defaultHidden: true },
  { title: '复核人', field: 'reviewedByName', key: 'reviewedByName', width: 100, defaultHidden: true },
  { title: '收款人', field: 'receiver', key: 'receiver', width: 100, defaultHidden: true },
  { title: '单据编号', field: 'orderNumber', key: 'orderNumber', width: 140, defaultHidden: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 100, defaultHidden: true },
  { title: '制单时间', field: 'issuedAt', key: 'issuedAt', width: 140, defaultHidden: true },
  { title: '备注', field: 'notes', key: 'notes', width: 140 },
]

// ═══ 列配置：每个Tab独立实例（对齐 borrow-query 模式） ═══
const pendingColCfg = useColumnConfig(PENDING_COLUMNS, 'payment-by-doc-columns-pending')
const allColCfg = useColumnConfig(ALL_COLUMNS, 'payment-by-doc-columns-all')
const invoiceColCfg = useColumnConfig(INVOICE_COLUMNS, 'payment-by-doc-columns-invoice')

const visibleColumns = computed(() => {
  if (activeTab.value === 'pending') return pendingColCfg.visibleColumns.value
  if (activeTab.value === 'all') return allColCfg.visibleColumns.value
  return invoiceColCfg.visibleColumns.value
})

const panelColumns = computed(() => {
  if (activeTab.value === 'pending') return pendingColCfg.settingsColumns.value
  if (activeTab.value === 'all') return allColCfg.settingsColumns.value
  return invoiceColCfg.settingsColumns.value
})

const activeColCfg = computed(() => {
  if (activeTab.value === 'pending') return pendingColCfg
  if (activeTab.value === 'all') return allColCfg
  return invoiceColCfg
})

function handleColumnConfigChange() {
  activeColCfg.value.onSettingChange()
}
function handleColumnConfigReset() {
  activeColCfg.value.resetSettings()
}

// ═══ 页面配置（查询条件显隐 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const PENDING_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'noSettleWriteOff', label: '无结算付款单核销', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'mergePay', label: '合并付款', enabled: true },
  { key: 'forceSettle', label: '强制结算', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const ALL_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'getInvoice', label: '取得发票', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const INVOICE_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

function makeQueryFields(fields: SearchFieldDef[]): QueryFieldSetting[] {
  return fields.map(f => ({ key: f.key, label: f.label, visible: true }))
}

const defaultQueryFields = computed(() => makeQueryFields(currentSearchFields.value))
const defaultFunctionButtons = computed<FunctionButtonSetting[]>(() => {
  if (activeTab.value === 'pending') return PENDING_FUNCTION_BUTTONS
  if (activeTab.value === 'all') return ALL_FUNCTION_BUTTONS
  return INVOICE_FUNCTION_BUTTONS
})

const queryFieldsConfig = ref<QueryFieldSetting[]>(defaultQueryFields.value)
const functionButtonConfig = ref<FunctionButtonSetting[]>(defaultFunctionButtons.value)

const pageConfigStorageKey = computed(() => `payment-by-doc-page-config-${activeTab.value}`)

function handlePageConfigChange(config: any) {
  queryFieldsConfig.value = config.queryFields || defaultQueryFields.value
  functionButtonConfig.value = config.functionButtons || defaultFunctionButtons.value
}

function fnEnabled(key: string): boolean {
  const c = functionButtonConfig.value.find(f => f.key === key)
  return c ? c.enabled : true
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'invoice') {
      await fetchInvoiceData()
    } else {
      await fetchDocData()
    }
  } catch (e: any) {
    console.warn('[按单付款] 获取数据失败', e)
    message.error(e?.response?.data?.message || '查询失败，请检查网络后重试')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function fetchDocData() {
  const target = activeTab.value as 'pending' | 'all'
  const apiParams: any = {
    current: pagination.current,
    size: pagination.pageSize,
    dateStart: searchParams.dateStart || undefined,
    dateEnd: searchParams.dateEnd || undefined,
    documentNo: searchParams.documentNo || undefined,
    documentType: searchParams.documentType || undefined,
    supplierName: searchParams.supplierName || undefined,
    handlerName: searchParams.handlerName || undefined,
  }
  if (target === 'pending' && !apiParams.documentType) {
    // 待付款：默认看所有应付来源单据中未结算的
  }
  return paymentByDocApi.page(target, apiParams).then((res: any) => {
    const data = res?.data || res
    const records: any[] = data?.records || data?.content || data?.list || []
    tableData.value = decorateDocRows(records)
    pagination.total = data?.total || 0
  })
}

function decorateDocRows(records: any[]): any[] {
  return records.map((r: any, i: number) => ({
    ...r,
    rowKey: `${activeTab.value}-${r.id}-${i}`,
    // 金额口径与对账标记由后端聚合返回（本单金额 = 已结 + 待审 + 未结）
    settledAmount: r.settledAmount ?? 0,
    pendingApproveAmount: r.pendingApproveAmount ?? 0,
    unsettledAmount: r.unsettledAmount ?? (r.totalAmount ?? 0),
    reconcile: r.reconcile === true,
    lastReconcileBy: r.lastReconcileBy ?? '',
    lastReconcileTime: r.lastReconcileTime ?? '',
  }))
}

function fetchInvoiceData() {
  // 后端 InvoiceController /query 使用 Spring Data Pageable（page 0 基 + size），非 MyBatis-Plus current
  const apiParams: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
    direction: invoiceDirection.value,
    invoiceNumber: searchParams.invoiceNumber || undefined,
    invoiceCode: searchParams.invoiceCode || undefined,
    invoiceType: searchParams.invoiceType || undefined,
    partnerName: searchParams.partnerName || undefined,
    issuedByName: searchParams.issuedByName || undefined,
  }
  if (searchParams.dateStart) apiParams.startDate = searchParams.dateStart
  if (searchParams.dateEnd) apiParams.endDate = searchParams.dateEnd
  return paymentByDocApi.invoicePage(apiParams).then((res: any) => {
    const data = res?.data || res
    const records: any[] = data?.content || data?.records || data?.data?.records || data?.list || []
    pagination.total = data?.totalElements ?? data?.total ?? data?.data?.total ?? 0
    tableData.value = records.map((r: any, i: number) => {
      const subtotal = Number(r.subtotalAmount || 0)
      const tax = Number(r.taxAmount || 0)
      return {
        ...r,
        rowKey: `${invoiceDirection.value}-${r.id}-${i}`,
        partnerName: r.customerName || r.supplierName || r.partnerName || '',
        invoiceTypeLabel: invoiceTypeLabel(r.invoiceType),
        taxRate: subtotal > 0 ? (tax / subtotal * 100) : 0,
      }
    })
  })
}

function invoiceTypeLabel(type?: string): string {
  const map: Record<string, string> = {
    SALES_INVOICE: '销售发票',
    PURCHASE_INVOICE: '采购发票',
    TAX_INVOICE: '税务发票',
    ELECTRONIC_INVOICE: '电子发票',
    REGULAR_INVOICE: '普通发票',
    SPECIAL_INVOICE: '专用发票',
    CREDIT_NOTE: '红字发票',
    IMPORT_INVOICE: '进口发票',
    EXPORT_INVOICE: '出口发票',
  }
  return (type && map[type]) || type || '-'
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key as 'pending' | 'all' | 'invoice'
  // 切换Tab时刷新列配置 + 页面配置 + 查询字段
  syncTabConfig()
  pagination.current = 1
  fetchData()
}

function handleInvoiceDirectionChange() {
  pagination.current = 1
  fetchData()
}

function syncTabConfig() {
  // 重置列配置（useColumnConfig 已按列定义重建）
  objectAssignReactive(queryFieldsConfig.value, defaultQueryFields.value)
  objectAssignReactive(functionButtonConfig.value, defaultFunctionButtons.value)
}

function objectAssignReactive(target: any[], source: any[]) {
  target.splice(0, target.length, ...source.map(s => ({ ...s })))
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

// ═══ 工具栏操作 ═══
function handleRefresh() { fetchData() }

function handlePrintF8() {
  if (tableData.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}

function handleExport() {
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const cols = visibleColumns.value.filter((c: any) => c.key !== 'rowNo' && c.key !== 'action')
  const headers = cols.map((c: any) => c.title)
  const lines = tableData.value.map((r: any) => cols.map((c: any) => {
    const v = r[c.field ?? c.key]
    if (v === null || v === undefined) return ''
    if (c.key === 'documentType') return DOC_TYPE_LABELS[v] || v
    return String(v).replace(/,/g, '')
  }).join(','))
  const csv = '\uFEFF' + [headers.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `按单付款-${tabLabel(activeTab.value)}-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function tabLabel(k: string): string {
  return { pending: '待付款', all: '全部单据', invoice: '发票查询' }[k] || k
}

// ═══ 业务操作 ═══
function currentOperator(): string {
  return userStore.nickname || userStore.username || '系统'
}

async function handleNoSettleWriteOff() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要核销的付款单')
    return
  }
  const ids = selectedRows.value.map((r: any) => r.id).filter(Boolean)
  try {
    const res: any = await paymentByDocApi.noSettleWriteOff(ids)
    const data = res?.data ?? res
    message.success(data?.message || `已补核销 ${data?.processed ?? 0} 条付款明细`)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '无结算付款单核销失败')
  }
}

function handleMergePay() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要合并付款的单据')
    return
  }
  const ids = selectedRows.value.map((r: any) => r.id).filter(Boolean)
  const sources = ids.map((id: number) => {
    const row = tableData.value.find((r: any) => r.id === id)
    return row ? { id: row.id, documentNo: row.documentNo } : null
  }).filter(Boolean)
  // 合并付款 → 联动《付款单》表单（携带多笔来源单据）
  router.push({
    path: '/finance/payment-doc/form',
    query: { merge: 1, sources: JSON.stringify(sources) },
  })
}

async function handleForceSettle() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要强制结算的单据')
    return
  }
  const ids = selectedRows.value.map((r: any) => r.id).filter(Boolean)
  try {
    const res: any = await paymentByDocApi.noSettleWriteOff(ids)
    const data = res?.data ?? res
    message.success(data?.message || `强制结算处理 ${data?.updatedDocs ?? 0} 张单据`)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '强制结算失败')
  }
}

function handleGetInvoice() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要取得发票的单据')
    return
  }
  // 取得发票 → 切换至发票查询 Tab，查看所选单据的开票/取得状态
  activeTab.value = 'invoice'
  syncTabConfig()
  pagination.current = 1
  message.success('已切换到发票查询，查看所选单据的开票/取得状态')
  fetchData()
}

async function handleReconcile(record: any) {
  if (record.documentType !== 'INBOUND') {
    message.warning('仅采购入库单支持对账标记（应付已记账）')
    return
  }
  const nextFlag = record.reconcile ? 0 : 1
  try {
    const res: any = await paymentByDocApi.reconcile(record.documentType, record.id, nextFlag, currentOperator())
    const data = res?.data ?? res
    if (data?.success === false) {
      message.warning(data?.message || '对账标记失败')
      return
    }
    message.success(data?.message || (nextFlag === 1 ? '已标记对账' : '已取消对账'))
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '对账标记失败')
  }
}

function handleRowPay(record: any) {
  // 行级付款 → 联动《付款单》
  router.push({ path: '/finance/payment-doc/form', query: { sourceId: record.id, sourceNo: record.documentNo } })
}

function handleRowView(record: any) {
  router.push({ path: '/finance/payment-doc/form', query: { id: record.id, sourceNo: record.documentNo } })
}

// ═══ 工具 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[按单付款] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  syncTabConfig()
  setQuickDate('week')
})
</script>

<style scoped>
.query-scheme-wrap { display: inline-flex; align-items: center; margin-right: 8px; }
.quick-dates { flex-wrap: wrap; }

.invoice-direction {
  margin-right: 8px;
  :deep(.ant-radio-button-wrapper) { font-size: 13px; }
}

.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.search-field-item { display: flex; align-items: center; min-width: 0; }
.search-field-item :deep(.ant-input),
.search-field-item :deep(.ant-select),
.search-field-item :deep(.ant-picker) { font-size: 13px; }
.search-field-item :deep(.ant-input) { width: 160px; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 120px; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }

:deep(.ant-radio-button-wrapper) { padding: 0 12px; }
:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
