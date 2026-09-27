<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab栏 + 工具栏 ═══ -->
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="activeTab === 'detail'"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-title="'商品分类'"
        :category-editable="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
        @category-select="handleCategorySelect"
        @category-expand="handleCategoryExpand"
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
              <a-select-option value="">
                --查询方案--
              </a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-space
            :size="4"
            class="quick-dates"
          >
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
        </template>

        <!-- ═══ 工具栏右侧：操作按钮（显隐由页面配置「功能按钮」驱动） ═══ -->
        <template #toolbar-right>
          <a-space :size="4">
            <!-- 按钮权限：与后端 @SaCheckPermission("sale:pre-order:*") 同一套码 -->
            <a-button
              v-if="btnEnabled('add')"
              v-permission="'sale:pre-order:create'"
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('batchOrder')"
              size="small"
              :disabled="selectedOrderIds.length === 0"
              @click="handleBatchOrder"
            >
              <ImportOutlined /> 批量订货
            </a-button>
            <a-button
              v-if="btnEnabled('printF8')"
              v-permission="'sale:pre-order:print'"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              v-permission="'sale:pre-order:export'"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
            <a-tooltip
              v-if="btnEnabled('config')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined /> 配置
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（字段显隐/排序由页面配置驱动，按 Tab 两套查询条件） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              class="search-grid"
              :class="{ 'search-grid-collapsed': !searchExpanded }"
            >
              <div
                v-for="f in visibleQueryFields"
                :key="f.key"
                class="search-field-item"
              >
                <!-- 日期区间 -->
                <a-range-picker
                  v-if="f.key === 'dateRange'"
                  v-model:value="dateRange"
                  size="small"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  @change="handleDateChange"
                />
                <!-- 下拉（单据状态支持多选） -->
                <div
                  v-else-if="selectOptionsFor(f.key).length"
                  class="search-select-wrap"
                >
                  <span class="search-select-label">{{ f.label }}</span>
                  <a-select
                    v-model:value="searchParams[f.key]"
                    size="small"
                    allow-clear
                    :mode="f.key === 'status' ? 'multiple' : undefined"
                    :max-tag-count="1"
                    placeholder="全部"
                  >
                    <a-select-option
                      v-for="o in selectOptionsFor(f.key)"
                      :key="o.value"
                      :value="o.value"
                    >
                      {{ o.label }}
                    </a-select-option>
                  </a-select>
                </div>
                <!-- 日期 -->
                <a-date-picker
                  v-else-if="DATE_FIELDS.includes(f.key)"
                  v-model:value="searchParams[f.key]"
                  size="small"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  :placeholder="f.label"
                />
                <!-- 数字 -->
                <a-input-number
                  v-else-if="NUMBER_FIELDS.includes(f.key)"
                  v-model:value="searchParams[f.key]"
                  size="small"
                  style="width: 100%"
                  :placeholder="f.label"
                />
                <!-- 文本 -->
                <a-input
                  v-else
                  v-model:value="searchParams[f.key]"
                  :placeholder="f.label"
                  allow-clear
                  size="small"
                  :suffix="SEARCH_ICON_FIELDS.includes(f.key) ? h(SearchOutlined, { style: 'color:#bbb' }) : undefined"
                />
              </div>
            </div>
            <div class="search-action-bar">
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-checkbox
                v-if="activeTab === 'detail'"
                v-model:checked="searchParams.showGift"
              >
                是否赠品
              </a-checkbox>
              <a-checkbox v-model:checked="searchParams.showCancelled">
                显示已取消
              </a-checkbox>
              <a-checkbox v-model:checked="searchParams.showCompleted">
                显示已完成
              </a-checkbox>
              <a-checkbox v-model:checked="searchParams.showDraft">
                显示草稿
              </a-checkbox>
              <a-button
                v-if="visibleQueryFields.length > DEFAULT_VISIBLE_COUNT"
                type="link"
                size="small"
                @click="searchExpanded = !searchExpanded"
              >
                {{ searchExpanded ? '收起' : '更多条件' }}
                <UpOutlined v-if="searchExpanded" />
                <DownOutlined v-else />
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :storage-key="activeTab === 'doc' ? 'sale-pre-order-table-columns-doc' : 'sale-pre-order-table-columns-detail'"
              :global-config-key="activeTab === 'doc' ? 'sale-pre-order-columns-doc' : 'sale-pre-order-columns-detail'"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :row-selection="rowSelection"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <!-- 单据编号 -->
              <template #orderNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleEdit(record)"
                >
                  {{ record.orderNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(rowStatus(record))">
                  {{ getStatusText(rowStatus(record)) }}
                </a-tag>
              </template>
              <!-- 金额 -->
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <template #discountedAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.discountedAmount) }}</span>
              </template>
              <template #orderAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.orderAmount || record.totalAmount) }}</span>
              </template>
              <!-- 预订金 -->
              <template #receivedDepositCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.receivedDeposit) }}</span>
              </template>
              <template #unreceivedDepositCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.unreceivedDeposit) }}</span>
              </template>
              <template #depositBalanceCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.depositBalance) }}</span>
              </template>
              <!-- 数量 -->
              <template #preOrderQtyCell="{ record }">
                <span class="currency-value">{{ formatQty(activeTab === 'doc' ? record.preOrderQuantity : record.quantity) }}</span>
              </template>
              <template #orderedQtyCell="{ record }">
                <span class="currency-value">{{ formatQty(record.orderedQuantity) }}</span>
              </template>
              <template #unOrderedQtyCell="{ record }">
                <span class="currency-value">{{ formatQty(record.unOrderedQuantity) }}</span>
              </template>
              <template #shippedQtyCell="{ record }">
                <span class="currency-value">{{ formatQty(record.shippedQuantity) }}</span>
              </template>
              <template #unShippedQtyCell="{ record }">
                <span class="currency-value">{{ formatQty(record.unShippedQuantity) }}</span>
              </template>
              <!-- 重量/体积 -->
              <template #weightCell="{ record }">
                <span class="currency-value">{{ formatQty(record.totalWeight || record.weight) }}</span>
              </template>
              <template #volumeCell="{ record }">
                <span class="currency-value">{{ formatQty(record.totalVolume || record.volume) }}</span>
              </template>
              <!-- 明细金额 -->
              <template #unitPriceCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.unitPrice) }}</span>
              </template>
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.amount) }}</span>
              </template>
              <template #discountedPriceCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.discountedPrice) }}</span>
              </template>
              <template #discountedAmountItemCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.discountedAmount) }}</span>
              </template>
              <template #terminateAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.terminateAmount) }}</span>
              </template>
              <!-- 操作列：单据行工作流（提交 → 审核 → 订货） -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-dropdown v-if="rowWorkflowActions(record).length">
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item
                          v-for="act in rowWorkflowActions(record)"
                          :key="act.key"
                          :danger="act.danger"
                          @click="act.run(record)"
                        >
                          {{ act.label }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（查询条件按 Tab 独立 + 功能按钮全页共用） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryConfig"
      :function-buttons-config="activeFunctionButtons"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  SearchOutlined,
  DownOutlined,
  UpOutlined,
  ExportOutlined,
  ImportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { FunctionButtonSetting, QueryFieldSetting } from '@/components/PageConfigPanel/index.vue'
import { preOrderApi } from '@/api/erp'
import { productCategoryApi } from '@/api/erp/product'
import { printDocuments } from '@/utils/printDocuments'
import { useRouter } from 'vue-router'

defineOptions({ name: 'PreOrderList' })

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('doc')

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
const hasError = ref(false)
const tableData = ref<any[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive<Record<string, any>>({
  orderNo: '',
  customerName: '',
  handlerName: '',
  deptName: '',
  warehouseName: '',
  productName: '',
  creatorName: '',
  auditorName: '',
  status: undefined as number[] | undefined,
  settlementStatus: undefined as number | undefined,
  saleType: undefined as number | undefined,
  productAttribute: '',
  remark: '',
  itemRemark: '',
  extNum1: undefined as number | undefined,
  extNum2: undefined as number | undefined,
  extText1: '',
  extText2: '',
  extText3: '',
  showGift: false,
  showCancelled: false,
  showCompleted: false,
  showDraft: false,
  depositDeadlineStart: undefined as string | undefined,
  depositDeadlineEnd: undefined as string | undefined,
  categoryId: undefined as number | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys }
}))

/** 批量操作面向「单据」：按明细 Tab 的选中行换算为所属单据ID（去重） */
const selectedOrderIds = computed(() => {
  if (activeTab.value === 'doc') return selectedRowKeys.value as number[]
  const ids = tableData.value
    .filter(r => selectedRowKeys.value.includes(r.id))
    .map(r => r.orderId)
  return Array.from(new Set(ids)) as number[]
})

const showPageConfig = ref(false)
const searchExpanded = ref(false)

// ═══ 商品分类树 ═══
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const categoryExpandedKeys = ref<(string | number)[]>([])

// ══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '审核中', color: 'processing' },
  2: { text: '待订货', color: 'orange' },
  3: { text: '部分订货', color: 'blue' },
  4: { text: '已订货', color: 'cyan' },
  5: { text: '已完成', color: 'green' },
  '-1': { text: '已取消', color: 'red' },
}

function getStatusText(status: number): string { return STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number): string { return STATUS_MAP[status]?.color || 'default' }

/** 行所属单据ID / 单据状态（按明细 Tab 取单据维度字段） */
function rowOrderId(record: any): number { return activeTab.value === 'doc' ? record.id : record.orderId }
function rowStatus(record: any): number { return activeTab.value === 'doc' ? record.status : record.orderStatus }

// ═══ 页面配置：查询条件（按 Tab 两套，对标：按单据 20 / 按明细 16） ═══
const PAGE_CONFIG_STORAGE_KEY = 'sale-pre-order-page-config'
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)
/** 折叠阈值：默认展示约 2 排（含查询按钮所在操作条），超出部分由「更多条件」展开 */
const DEFAULT_VISIBLE_COUNT = 8

const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '表头自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '表头自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '表头自定义字段5(文本)', visible: false },
  { key: 'depositDeadlineStart', label: '收款期限(起)', visible: false },
  { key: 'depositDeadlineEnd', label: '收款期限(止)', visible: false },
  { key: 'saleType', label: '销售类型', visible: false },
  { key: 'productAttribute', label: '商品行属性', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
]

const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: false },
  { key: 'status', label: '单据状态', visible: false },
  { key: 'settlementStatus', label: '结算状态', visible: false },
  { key: 'saleType', label: '销售类型', visible: false },
  { key: 'productAttribute', label: '商品行属性', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'itemRemark', label: '明细备注', visible: false },
]

// 功能按钮（默认全选，配置面板可关闭 → 工具栏按钮真实显隐）
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchOrder', label: '批量订货', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))

const activeQueryConfig = computed(() =>
  activeTab.value === 'detail' ? detailQueryConfig.value : docQueryConfig.value
)
const activeFunctionButtons = computed(() => functionButtonConfig.value)

/** 当前 Tab 生效的可见查询字段（勾选隐藏 + 拖拽排序均真实生效） */
const visibleQueryFields = computed(() => activeQueryConfig.value.filter(f => f.visible))

function btnEnabled(key: string): boolean {
  return functionButtonConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 查询控件类型映射（通用渲染：区间/下拉/日期/数字/文本） ═══
const SELECT_OPTIONS: Record<string, { label: string; value: any }[]> = {
  status: [
    { label: '草稿', value: 0 },
    { label: '审核中', value: 1 },
    { label: '待订货', value: 2 },
    { label: '部分订货', value: 3 },
    { label: '已订货', value: 4 },
    { label: '已完成', value: 5 },
    { label: '已取消', value: -1 },
  ],
  settlementStatus: [
    { label: '未结算', value: 0 },
    { label: '部分结算', value: 1 },
    { label: '已结算', value: 2 },
  ],
  saleType: [
    { label: '正常销售', value: 0 },
    { label: '样品销售', value: 1 },
    { label: '促销销售', value: 2 },
  ],
}
const DATE_FIELDS = ['depositDeadlineStart', 'depositDeadlineEnd']
const NUMBER_FIELDS = ['extNum1', 'extNum2']
const SEARCH_ICON_FIELDS = ['customerName', 'handlerName', 'deptName', 'warehouseName', 'productName']

function selectOptionsFor(key: string) { return SELECT_OPTIONS[key] || [] }

// ═══ 页面配置持久化（按 Tab 落 localStorage，与 PageConfigPanel 同键同结构） ═══
function mergeQueryFields(defaults: QueryFieldSetting[], saved: any[]): QueryFieldSetting[] {
  const merged = saved
    .map(s => {
      const def = defaults.find(d => d.key === s.key)
      return def ? { ...def, ...s, label: def.label } : null
    })
    .filter((f): f is QueryFieldSetting => !!f)
  defaults.forEach(def => {
    if (!merged.find(m => m.key === def.key)) merged.push({ ...def })
  })
  return merged
}

function persistPageConfig(tab: 'doc' | 'detail') {
  const payload = {
    queryFields: (tab === 'doc' ? docQueryConfig.value : detailQueryConfig.value).map(f => ({ ...f })),
    functionButtons: functionButtonConfig.value.map(b => ({ ...b })),
  }
  localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-${tab}`, JSON.stringify(payload))
  // 功能按钮为全页共用：同步到另一 Tab 的存档，避免切换 Tab 后被旧值回退
  const otherKey = `${PAGE_CONFIG_STORAGE_KEY}-${tab === 'doc' ? 'detail' : 'doc'}`
  try {
    const otherRaw = localStorage.getItem(otherKey)
    const other = otherRaw ? JSON.parse(otherRaw) : {}
    localStorage.setItem(otherKey, JSON.stringify({ ...other, functionButtons: payload.functionButtons }))
  } catch { /* 忽略另一 Tab 存档解析异常 */ }
}

function applyFunctionButtons(saved: FunctionButtonSetting[]) {
  saved.forEach((b: any) => {
    const target = functionButtonConfig.value.find(d => d.key === b.key)
    if (target) target.enabled = b.enabled !== false
  })
}

function loadPageConfigFromStorage() {
  try {
    const docRaw = localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-doc`)
    if (docRaw) {
      const parsed = JSON.parse(docRaw)
      if (Array.isArray(parsed.queryFields)) {
        docQueryConfig.value = mergeQueryFields(DEFAULT_DOC_QUERY_FIELDS, parsed.queryFields)
      }
      if (Array.isArray(parsed.functionButtons)) applyFunctionButtons(parsed.functionButtons)
    }
    const detailRaw = localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-detail`)
    if (detailRaw) {
      const parsed = JSON.parse(detailRaw)
      if (Array.isArray(parsed.queryFields)) {
        detailQueryConfig.value = mergeQueryFields(DEFAULT_DETAIL_QUERY_FIELDS, parsed.queryFields)
      }
      if (Array.isArray(parsed.functionButtons)) applyFunctionButtons(parsed.functionButtons)
    }
  } catch { /* 存档损坏时回退默认配置 */ }
}

/** 页面配置变更：真实驱动查询条件显隐/排序 + 工具栏按钮显隐，并持久化 */
function handlePageConfigChange(config: any) {
  const tab: 'doc' | 'detail' = activeTab.value === 'detail' ? 'detail' : 'doc'
  const targetConfig = tab === 'detail' ? detailQueryConfig : docQueryConfig
  if (Array.isArray(config.queryFields)) {
    const next = config.queryFields
      .map((f: any) => {
        const existing = targetConfig.value.find(d => d.key === f.key)
        return existing ? { ...existing, visible: f.visible !== false } : null
      })
      .filter((f: any) => !!f)
    // 拖拽排序生效：按配置面板顺序重排查询条件
    if (next.length) targetConfig.value.splice(0, targetConfig.value.length, ...next)
  }
  if (Array.isArray(config.functionButtons)) applyFunctionButtons(config.functionButtons)
  persistPageConfig(tab)
}

// ═══ 按单据 Tab 列（47列 + 序号 + 操作） ═══
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'orderNo', key: 'orderNo', width: 170, type: 'slot', slotName: 'orderNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 110, sortable: true },
  { title: '往来单位', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '往来单位编号', field: 'customerCode', key: 'customerCode', width: 120 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 80 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 90 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 150 },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 90, sortable: true },
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 110, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 110, align: 'right', type: 'slot', slotName: 'discountedAmountCell', sortable: true },
  { title: '本单金额', field: 'orderAmount', key: 'orderAmount', width: 110, align: 'right', type: 'slot', slotName: 'orderAmountCell', sortable: true },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 90 },
  { title: '已收预订金', field: 'receivedDeposit', key: 'receivedDeposit', width: 110, align: 'right', type: 'slot', slotName: 'receivedDepositCell', sortable: true },
  { title: '未收预订金', field: 'unreceivedDeposit', key: 'unreceivedDeposit', width: 110, align: 'right', type: 'slot', slotName: 'unreceivedDepositCell', sortable: true },
  { title: '本单预订金余额', field: 'depositBalance', key: 'depositBalance', width: 130, align: 'right', type: 'slot', slotName: 'depositBalanceCell', sortable: true },
  { title: '预订数量', field: 'preOrderQuantity', key: 'preOrderQuantity', width: 90, align: 'right', type: 'slot', slotName: 'preOrderQtyCell', sortable: true },
  { title: '已订数量', field: 'orderedQuantity', key: 'orderedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'orderedQtyCell', sortable: true },
  { title: '未订数量', field: 'unOrderedQuantity', key: 'unOrderedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'unOrderedQtyCell', sortable: true },
  { title: '已发数量', field: 'shippedQuantity', key: 'shippedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'shippedQtyCell', sortable: true },
  { title: '未发数量', field: 'unShippedQuantity', key: 'unShippedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'unShippedQtyCell', sortable: true },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 90, align: 'right', type: 'slot', slotName: 'weightCell' },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 90, align: 'right', type: 'slot', slotName: 'volumeCell' },
  { title: '预订金账户1', field: 'depositAccount1', key: 'depositAccount1', width: 120 },
  { title: '预订金账户2', field: 'depositAccount2', key: 'depositAccount2', width: 120 },
  { title: '预订金账户3', field: 'depositAccount3', key: 'depositAccount3', width: 120 },
  { title: '预订金账户4', field: 'depositAccount4', key: 'depositAccount4', width: 120 },
  { title: '区域', field: 'region', key: 'region', width: 80 },
  { title: '销售类型', field: 'saleType', key: 'saleType', width: 80 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60 },
  { title: '表头自定义1(数字)', field: 'extNum1', key: 'extNum1', width: 130, align: 'right' },
  { title: '表头自定义2(数字)', field: 'extNum2', key: 'extNum2', width: 130, align: 'right' },
  { title: '表头自定义3(文本)', field: 'extText1', key: 'extText1', width: 130 },
  { title: '表头自定义4(文本)', field: 'extText2', key: 'extText2', width: 130 },
  { title: '表头自定义5(文本)', field: 'extText3', key: 'extText3', width: 130 },
  { title: '提交时间', field: 'submitTime', key: 'submitTime', width: 150 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80 },
  { title: '提交人', field: 'submitterName', key: 'submitterName', width: 80 },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 80 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 70, align: 'center' },
]

// ═══ 按明细 Tab 列（63列 + 序号 + 操作，对标：表体自定义1~10） ═══
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 80, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'orderNo', key: 'orderNo', width: 170, type: 'slot', slotName: 'orderNoCell', sortable: true },
  { title: '单据状态', field: 'orderStatus', key: 'orderStatus', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 110, sortable: true },
  { title: '往来单位', field: 'customerName', key: 'customerName', width: 150, sortable: true },
  { title: '往来单位编号', field: 'customerCode', key: 'customerCode', width: 120 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 80 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 90 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 150 },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 90 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 160, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 120 },
  { title: '规格', field: 'specification', key: 'specification', width: 100 },
  { title: '型号', field: 'model', key: 'model', width: 100 },
  { title: '产地', field: 'origin', key: 'origin', width: 80 },
  { title: '品牌', field: 'brand', key: 'brand', width: 80 },
  { title: '表体自定义1(数字)', field: 'extNum1', key: 'extNum1', width: 130, align: 'right' },
  { title: '表体自定义2(数字)', field: 'extNum2', key: 'extNum2', width: 130, align: 'right' },
  { title: '表体自定义3(数字)', field: 'extNum3', key: 'extNum3', width: 130, align: 'right' },
  { title: '表体自定义4(文本)', field: 'extText1', key: 'extText1', width: 130 },
  { title: '表体自定义5(文本)', field: 'extText2', key: 'extText2', width: 130 },
  { title: '表体自定义6(数字)', field: 'extNum4', key: 'extNum4', width: 130, align: 'right' },
  { title: '表体自定义7(数字)', field: 'extNum5', key: 'extNum5', width: 130, align: 'right' },
  { title: '表体自定义8(往来单位)', field: 'extPartner', key: 'extPartner', width: 130 },
  { title: '表体自定义9(职员)', field: 'extStaff', key: 'extStaff', width: 120 },
  { title: '表体自定义10(部门)', field: 'extDept', key: 'extDept', width: 120 },
  { title: '单位', field: 'unit', key: 'unit', width: 70 },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right' },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 90 },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 90, align: 'right' },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 80, align: 'right' },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 80, align: 'right' },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 80, align: 'right' },
  { title: '预订数量', field: 'quantity', key: 'quantity', width: 90, align: 'right', type: 'slot', slotName: 'preOrderQtyCell', sortable: true },
  { title: '已订数量', field: 'orderedQuantity', key: 'orderedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'orderedQtyCell', sortable: true },
  { title: '未订数量', field: 'unOrderedQuantity', key: 'unOrderedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'unOrderedQtyCell', sortable: true },
  { title: '已发数量', field: 'shippedQuantity', key: 'shippedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'shippedQtyCell', sortable: true },
  { title: '未发数量', field: 'unShippedQuantity', key: 'unShippedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'unShippedQtyCell', sortable: true },
  { title: '终止数量', field: 'terminateQuantity', key: 'terminateQuantity', width: 90, align: 'right' },
  { title: '终止金额', field: 'terminateAmount', key: 'terminateAmount', width: 100, align: 'right', type: 'slot', slotName: 'terminateAmountCell' },
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 90, align: 'right', type: 'slot', slotName: 'unitPriceCell' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 100, align: 'right' },
  { title: '金额', field: 'amount', key: 'amount', width: 100, align: 'right', type: 'slot', slotName: 'amountCell' },
  { title: '折扣(%)', field: 'discountRate', key: 'discountRate', width: 80, align: 'right' },
  { title: '折后单价', field: 'discountedPrice', key: 'discountedPrice', width: 90, align: 'right', type: 'slot', slotName: 'discountedPriceCell' },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 100, align: 'right', type: 'slot', slotName: 'discountedAmountItemCell' },
  { title: '重量（kg）', field: 'weight', key: 'weight', width: 90, align: 'right', type: 'slot', slotName: 'weightCell' },
  { title: '体积（m³）', field: 'volume', key: 'volume', width: 90, align: 'right', type: 'slot', slotName: 'volumeCell' },
  { title: '明细备注', field: 'remark', key: 'remark', width: 120 },
  { title: '销售类型', field: 'saleType', key: 'saleType', width: 80 },
  { title: '商品行属性', field: 'productAttribute', key: 'productAttribute', width: 90 },
  { title: '单据备注', field: 'orderRemark', key: 'orderRemark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80 },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 80 },
  { title: '提交时间', field: 'submitTime', key: 'submitTime', width: 150 },
]

const currentColumns = computed(() => activeTab.value === 'doc' ? docColumns : detailColumns)

// ═══ 列配置入口 = 数据表表头齿轮（个人/全局双配置，全局配置落后端） ═══

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.totalAmount || 0), 0)
  const receivedDeposit = tableData.value.reduce((s: number, r: any) => s + (r.receivedDeposit || 0), 0)
  return [
    { key: 'totalAmount', value: totalAmount, highlight: true },
    { key: 'receivedDeposit', value: receivedDeposit, highlight: true },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.auditorName) params.auditorName = searchParams.auditorName
    if (searchParams.settlementStatus !== undefined) params.settlementStatus = searchParams.settlementStatus
    if (searchParams.productAttribute) params.productAttribute = searchParams.productAttribute

    // 状态过滤：复选框控制默认隐藏的特殊状态（草稿0/已取消-1/已完成5）
    const effectiveStatuses = new Set<number>()
    if (searchParams.status?.length) {
      searchParams.status.forEach((s: number) => effectiveStatuses.add(s))
    }
    if (searchParams.showCancelled) effectiveStatuses.add(-1)
    if (searchParams.showCompleted) effectiveStatuses.add(5)
    if (searchParams.showDraft) effectiveStatuses.add(0)
    // 默认显示活跃状态（非草稿、非已取消、非已完成）
    if (effectiveStatuses.size > 0) {
      params.status = Array.from(effectiveStatuses).join(',')
    } else {
      params.status = '1,2,3,4'
    }

    if (activeTab.value === 'detail') {
      if (searchParams.orderNo) params.orderNo = searchParams.orderNo
      if (searchParams.productName) params.keyword = searchParams.productName
      if (searchParams.categoryId) params.categoryId = searchParams.categoryId
      if (searchParams.itemRemark) params.itemRemark = searchParams.itemRemark
      if (searchParams.remark) params.remark = searchParams.remark
      if (searchParams.saleType !== undefined && searchParams.saleType !== null) params.saleType = searchParams.saleType
      if (searchParams.showGift) params.gift = true
      const res = await preOrderApi.pageDetail(params)
      if (res) {
        tableData.value = res.records || []
        pagination.total = res.total || 0
      }
    } else {
      if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
      if (searchParams.saleType !== undefined && searchParams.saleType !== null) params.saleType = searchParams.saleType
      if (searchParams.depositDeadlineStart) params.depositDeadlineStart = searchParams.depositDeadlineStart
      if (searchParams.depositDeadlineEnd) params.depositDeadlineEnd = searchParams.depositDeadlineEnd
      if (searchParams.remark) params.remark = searchParams.remark
      if (searchParams.extNum1 !== undefined && searchParams.extNum1 !== null) params.extNum1 = searchParams.extNum1
      if (searchParams.extNum2 !== undefined && searchParams.extNum2 !== null) params.extNum2 = searchParams.extNum2
      if (searchParams.extText1) params.extText1 = searchParams.extText1
      if (searchParams.extText2) params.extText2 = searchParams.extText2
      if (searchParams.extText3) params.extText3 = searchParams.extText3
      const res = await preOrderApi.page(params)
      if (res) {
        tableData.value = res.records || []
        pagination.total = res.total || 0
      }
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[预订货单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const tree = await productCategoryApi.getTree()
    categoryTreeData.value = tree || []
    if (categoryTreeData.value.length > 0) {
      categoryExpandedKeys.value = [categoryTreeData.value[0].id]
    }
  } catch { categoryTreeData.value = [] }
  finally { categoryLoading.value = false }
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  selectedRowKeys.value = []
  searchExpanded.value = false
  fetchData()
}

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
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else { searchParams.startDate = ''; searchParams.endDate = '' }
}

function handleSearch() { pagination.current = 1; fetchData() }
function handlePageChange(page: number, pageSize: number) { pagination.current = page; pagination.pageSize = pageSize; fetchData() }

function handleCategorySelect(keys: (string | number)[]) {
  if (keys.length > 0 && keys[0] !== '0') {
    selectedCategoryId.value = keys[0]
    searchParams.categoryId = Number(keys[0])
  } else {
    selectedCategoryId.value = '0'
    searchParams.categoryId = undefined
  }
  pagination.current = 1; fetchData()
}

function handleCategoryExpand(keys: (string | number)[]) { categoryExpandedKeys.value = keys }

function handleAdd() { router.push('/sales/pre-order/create') }
function handleEdit(record: any) { router.push(`/sales/pre-order/form/${rowOrderId(record)}`) }

// ═══ 行工作流（提交 → 审核 → 订货） ═══
function rowWorkflowActions(record: any): { key: string; label: string; danger?: boolean; run: (r: any) => void }[] {
  const status = rowStatus(record)
  const actions: { key: string; label: string; danger?: boolean; run: (r: any) => void }[] = []
  if (status === 0) {
    actions.push({ key: 'submit', label: '提交', run: handleSubmitOne })
    actions.push({ key: 'delete', label: '删除', danger: true, run: handleDelete })
  }
  if (status === 1) {
    actions.push({ key: 'approve', label: '审核', run: handleApproveOne })
  }
  if (status === 2 || status === 3) {
    actions.push({ key: 'order', label: '订货', run: handleOrderOne })
  }
  return actions
}

async function handleSubmitOne(record: any) {
  try {
    await preOrderApi.submit(rowOrderId(record))
    message.success('已提交，等待审核')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '提交失败')
  }
}

async function handleApproveOne(record: any) {
  try {
    await preOrderApi.approve(rowOrderId(record))
    message.success('审核通过，可执行订货')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审核失败')
  }
}

function handleOrderOne(record: any) {
  Modal.confirm({
    title: '确认订货',
    content: `确定要将单据 ${record.orderNo} 转为销售订单吗？`,
    okText: '确认', cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await preOrderApi.batchOrder([rowOrderId(record)])
        const data = res as any
        if (data?.errors?.length) message.warning(`订货失败：${data.errors[0]}`)
        else message.success('订货成功，已生成销售订单')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '订货失败')
      }
    },
  })
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除预订货单 ${record.orderNo} 吗？此操作不可恢复。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      try { await preOrderApi.delete(rowOrderId(record)); message.success('删除成功'); fetchData() }
      catch (error: any) { message.error(error?.response?.data?.message || '删除失败') }
    },
  })
}

// ═══ 导出 ═══
function handleExport() {
  const params: any = {}
  if (searchParams.startDate) params.startDate = searchParams.startDate
  if (searchParams.endDate) params.endDate = searchParams.endDate
  if (searchParams.customerName) params.customerName = searchParams.customerName
  if (searchParams.handlerName) params.handlerName = searchParams.handlerName
  if (searchParams.deptName) params.deptName = searchParams.deptName
  if (searchParams.status?.length) params.status = searchParams.status.join(',')
  const url = `/api/erp/sale/pre-order/export?` + new URLSearchParams(params).toString()
  window.open(url, '_blank')
  message.success('导出任务已提交')
}

// ═══ 批量订货 ═══
async function handleBatchOrder() {
  const ids = selectedOrderIds.value
  if (ids.length === 0) {
    message.warning('请先选择要订货的预订货单')
    return
  }
  Modal.confirm({
    title: '确认批量订货',
    content: `确定要对选中的 ${ids.length} 条预订货单执行订货操作吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await preOrderApi.batchOrder(ids as number[])
        const data = res as any
        if (data?.errors?.length) {
          message.warning(`成功 ${data.successCount} 条，失败 ${data.errors.length} 条：${data.errors[0]}`)
        } else {
          message.success(`批量订货成功，共 ${data.successCount} 条`)
        }
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量订货失败')
      }
    },
  })
}

// ═══ 打印（F8）：按模板逐张渲染已勾选单据 ═══
// 原先是 preOrderApi.print(ids[0])（只回写打印次数）+ window.print() —— 打出来是整个后台界面，
// 不是单据。现在走后端「单据打印」接口取 HTML（pageCode=sale-pre-order 已注册装配器与模板）。
async function handlePrint() {
  const ids = selectedOrderIds.value
  if (ids.length === 0) {
    message.warning('请先选择要打印的预订货单')
    return
  }
  const done = await printDocuments('sale-pre-order', ids as number[], '预订货单')
  if (!done) return
  // 次数回写：后端只有单张计数端点，失败不影响已出纸
  await Promise.all(ids.map(id => preOrderApi.print(id as number).catch(() => null)))
  fetchData()
}

const handleError = (error: Error) => {
  hasError.value = true
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(qty: number): string {
  if (!qty) return '0'
  return qty.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfigFromStorage()
  setQuickDate('lastWeek')
  fetchData()
  loadCategoryTree()
})
// F8 打印快捷键
function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'F8') { e.preventDefault(); handlePrint() }
}
onMounted(() => window.addEventListener('keydown', onKeyDown))
onUnmounted(() => window.removeEventListener('keydown', onKeyDown))
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
/* 默认显示 2 排，超出自动折叠；点击「更多条件」展开 */
.search-grid-collapsed { max-height: 68px; overflow: hidden; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-bar { display: flex; align-items: center; gap: 12px; margin-top: 8px; padding-top: 6px; border-top: 1px dashed #f0f0f0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
