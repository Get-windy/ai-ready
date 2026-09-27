<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
              @change="handleSchemeChange"
            >
              <a-select-option value="">
                --查询方案--
              </a-select-option>
              <a-select-option
                v-for="s in querySchemes"
                :key="s.name"
                :value="s.name"
              >
                {{ s.name }}
              </a-select-option>
            </a-select>
            <a-tooltip title="保存当前查询条件为方案">
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="openSchemeModal"
              >
                <PlusOutlined />
              </a-button>
            </a-tooltip>
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

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="isButtonEnabled('config')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('print')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-dropdown v-if="isButtonEnabled('export')">
              <a-button size="small">
                <ExportOutlined /> 导出 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleExportMenu">
                  <a-menu-item key="csv">
                    导出CSV
                  </a-menu-item>
                  <a-menu-item key="excel">
                    导出Excel
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（字段显隐 / 顺序由「页面配置 → 查询条件」驱动） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              class="search-container"
              :data-expanded="showMoreConditions || null"
            >
              <div class="search-grid">
                <div
                  v-for="f in gridSearchFields"
                  :key="f.key"
                  class="search-field-item"
                >
                  <!-- 日期区间 -->
                  <template v-if="f.type === 'dateRange'">
                    <a-range-picker
                      v-if="f.key === 'dateRange'"
                      v-model:value="documentDateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDocumentDateChange"
                    />
                    <a-range-picker
                      v-else
                      v-model:value="sourceOrderDateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleSourceOrderDateChange"
                    />
                  </template>

                  <!-- 下拉选择 -->
                  <div
                    v-else-if="f.type === 'select'"
                    class="search-select-wrap"
                  >
                    <span class="search-select-label">{{ f.label }}</span>
                    <a-select
                      v-model:value="searchParams[f.key]"
                      :placeholder="f.placeholder || '全部'"
                      allow-clear
                      size="small"
                    >
                      <a-select-option value="">
                        全部
                      </a-select-option>
                      <a-select-option
                        v-for="o in f.options"
                        :key="String(o.value)"
                        :value="o.value"
                      >
                        {{ o.label }}
                      </a-select-option>
                    </a-select>
                  </div>

                  <!-- 数值区间 -->
                  <template v-else-if="f.type === 'numberRange'">
                    <a-input-number
                      v-model:value="searchParams[f.minKey!]"
                      placeholder="最小值"
                      size="small"
                      style="width: 100%"
                    />
                    <span style="margin: 0 4px; flex-shrink: 0">-</span>
                    <a-input-number
                      v-model:value="searchParams[f.maxKey!]"
                      placeholder="最大值"
                      size="small"
                      style="width: 100%"
                    />
                  </template>

                  <!-- 文本 -->
                  <a-input
                    v-else
                    v-model:value="searchParams[f.key]"
                    :placeholder="f.label"
                    allow-clear
                    size="small"
                    :suffix="f.searchIcon ? h(SearchOutlined, { style: 'color:#bbb' }) : undefined"
                  />
                </div>
              </div>

              <!-- ═══ 查询操作行 ═══
                   固定排在字段折叠区之外：字段再多，「查询」按钮也不会被折叠高度裁掉 -->
              <div class="search-action-row">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >
                  <SearchOutlined /> 查询
                </a-button>
                <a-checkbox
                  v-for="f in actionSearchFields"
                  :key="f.key"
                  v-model:checked="searchParams[f.key]"
                >
                  {{ f.label }}
                </a-checkbox>
              </div>
              <div class="search-more-toggle">
                <a-button
                  size="small"
                  @click="toggleMoreConditions"
                >
                  <DownOutlined v-if="!showMoreConditions" />
                  <UpOutlined v-else />
                  {{ showMoreConditions ? '收起' : '更多条件' }}
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="columns"
            :data-source="tableData"
            :storage-key="'sales-doc-query-table-columns'"
            global-config-key="sales-doc-query-columns"
            :loading="loading"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            row-key="id"
            @page-change="handlePageChange"
            @selection-change="handleSelectionChange"
          >
            <!-- 操作列 -->
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button
                  type="link"
                  size="small"
                  @click="handleCopy(record)"
                >
                  复制
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleNote(record)"
                >
                  整单备注
                </a-button>
              </a-space>
            </template>
          </BillTableList>
        </template>

        <!-- ═══ 表格底部：本页合计（口径为当前页） ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <div class="footer-label">
              本页合计（{{ tableData.length }} 条）
            </div>
            <div class="footer-values">
              <span v-if="summaryData.salesQuantity">销售数量: {{ summaryData.salesQuantity }}</span>
              <span v-if="summaryData.amount">金额: {{ summaryData.amount }}</span>
              <span v-if="summaryData.totalAmount">本单金额: {{ summaryData.totalAmount }}</span>
              <span v-if="summaryData.grossProfit">毛利: {{ summaryData.grossProfit }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 打印弹窗（打印模板渲染 → 本地打印 / 远程打印链） ═══ -->
    <PrintDialog
      ref="printDialogRef"
      :page-code="printPageCode"
      :document-id="printData.id"
      :print-data="printData"
      :always-last-template="printConfig.alwaysLastTemplate"
      @print-success="handlePrintSuccess"
    />

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :print-config-items="printConfigItems"
      storage-key="sales-doc-query-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 整单备注弹窗 ═══ -->
    <a-modal
      v-model:open="showNoteModal"
      title="整单备注"
      :confirm-loading="noteSaving"
      :width="480"
      @ok="saveNote"
      @cancel="showNoteModal = false"
    >
      <div style="margin-bottom: 8px; color: #666; font-size: 12px">
        单据编号: {{ noteRecord.documentNo }}
      </div>
      <a-textarea
        v-model:value="noteContent"
        :rows="4"
        placeholder="请输入整单备注内容"
      />
    </a-modal>

    <!-- ═══ 查询方案管理 ═══ -->
    <a-modal
      v-model:open="showSchemeModal"
      title="查询方案"
      :width="480"
      :footer="null"
    >
      <div class="scheme-save">
        <a-input
          v-model:value="newSchemeName"
          placeholder="方案名称"
          size="small"
          @press-enter="saveScheme"
        />
        <a-button
          type="primary"
          size="small"
          @click="saveScheme"
        >
          保存当前条件
        </a-button>
      </div>
      <div class="scheme-list">
        <div
          v-for="s in querySchemes"
          :key="s.name"
          class="scheme-item"
        >
          <span class="scheme-name">{{ s.name }}</span>
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              @click="applyScheme(s.name); showSchemeModal = false"
            >
              应用
            </a-button>
            <a-button
              type="link"
              size="small"
              danger
              @click="removeScheme(s.name)"
            >
              删除
            </a-button>
          </a-space>
        </div>
        <div
          v-if="!querySchemes.length"
          class="scheme-empty"
        >
          暂无查询方案
        </div>
      </div>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import * as XLSX from 'xlsx'
import {
  ReloadOutlined, SearchOutlined, DownOutlined, UpOutlined,
  SettingOutlined, PlusOutlined, PrinterOutlined, ExportOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import request from '@/utils/request'

const router = useRouter()

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const selectedRowKeys = ref<any[]>([])
const showMoreConditions = ref(false)
const showPageConfig = ref(false)

// 查询字段分两组：字段网格（可折叠）/ 操作行（查询按钮 + 勾选项，固定可见）

// ═══ 查询方案 ═══
const SCHEME_STORAGE_KEY = 'sales-doc-query-schemes'
const PAGE_CONFIG_STORAGE_KEY = 'sales-doc-query-page-config'

interface QueryScheme {
  name: string
  params: Record<string, any>
  documentDate: [string, string] | null
  sourceOrderDate: [string, string] | null
}

const queryScheme = ref('')
const querySchemes = ref<QueryScheme[]>([])
const showSchemeModal = ref(false)
const newSchemeName = ref('')

function loadSchemes() {
  try {
    querySchemes.value = JSON.parse(localStorage.getItem(SCHEME_STORAGE_KEY) || '[]')
  } catch {
    querySchemes.value = []
  }
}

function persistSchemes() {
  localStorage.setItem(SCHEME_STORAGE_KEY, JSON.stringify(querySchemes.value))
}

function openSchemeModal() {
  newSchemeName.value = queryScheme.value || ''
  showSchemeModal.value = true
}

function saveScheme() {
  const name = newSchemeName.value.trim()
  if (!name) {
    message.warning('请输入方案名称')
    return
  }
  const snapshot: QueryScheme = {
    name,
    params: { ...searchParams },
    documentDate: documentDateRange.value
      ? [documentDateRange.value[0].format('YYYY-MM-DD'), documentDateRange.value[1].format('YYYY-MM-DD')]
      : null,
    sourceOrderDate: sourceOrderDateRange.value
      ? [sourceOrderDateRange.value[0].format('YYYY-MM-DD'), sourceOrderDateRange.value[1].format('YYYY-MM-DD')]
      : null
  }
  const idx = querySchemes.value.findIndex(s => s.name === name)
  if (idx >= 0) {
    querySchemes.value[idx] = snapshot
  } else {
    querySchemes.value.push(snapshot)
  }
  persistSchemes()
  queryScheme.value = name
  newSchemeName.value = ''
  showSchemeModal.value = false
  message.success(`查询方案「${name}」已保存`)
}

function applyScheme(name: string) {
  if (!name) return
  const scheme = querySchemes.value.find(s => s.name === name)
  if (!scheme) return
  Object.assign(searchParams, DEFAULT_SEARCH_PARAMS, scheme.params)
  documentDateRange.value = scheme.documentDate
    ? [dayjs(scheme.documentDate[0]), dayjs(scheme.documentDate[1])]
    : null
  sourceOrderDateRange.value = scheme.sourceOrderDate
    ? [dayjs(scheme.sourceOrderDate[0]), dayjs(scheme.sourceOrderDate[1])]
    : null
  quickDate.value = ''
  handleSearch()
}

function handleSchemeChange(value: any) {
  if (value) {
    applyScheme(String(value))
  }
}

function removeScheme(name: string) {
  querySchemes.value = querySchemes.value.filter(s => s.name !== name)
  persistSchemes()
  if (queryScheme.value === name) {
    queryScheme.value = ''
  }
  message.success('已删除查询方案')
}

// ═══ 快捷日期 ═══
const quickDate = ref('thisWeek')
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Months', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]

const documentDateRange = ref<[Dayjs, Dayjs] | null>(null)
const sourceOrderDateRange = ref<[Dayjs, Dayjs] | null>(null)

function setQuickDate(key: string) {
  quickDate.value = key
  const today = dayjs()
  let start: Dayjs
  let end: Dayjs

  switch (key) {
    case 'yesterday':
      start = end = today.subtract(1, 'day')
      break
    case 'today':
      start = end = today
      break
    case 'thisWeek':
      start = today.startOf('week')
      end = today
      break
    case 'lastWeek':
      start = today.subtract(7, 'day')
      end = today
      break
    case 'thisMonth':
      start = today.startOf('month')
      end = today
      break
    case 'lastMonth':
      start = today.subtract(1, 'month').startOf('month')
      end = today.subtract(1, 'month').endOf('month')
      break
    case 'last3Months':
      start = today.subtract(3, 'month')
      end = today
      break
    case 'thisYear':
      start = today.startOf('year')
      end = today
      break
    default:
      return
  }

  documentDateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

// ═══ 搜索参数 ═══
const DEFAULT_SEARCH_PARAMS: Record<string, any> = {
  dateType: 'documentDate',
  documentNo: '',
  documentType: '',
  startDate: '',
  endDate: '',
  customerName: '',
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  handlerName: '',
  departmentName: '',
  creatorName: '',
  bookkeeperName: '',
  warehouseName: '',
  settlementStatus: '',
  sourceOrder: '',
  sourceOrderStartDate: '',
  sourceOrderEndDate: '',
  generationMethod: '',
  salesType: '',
  productAttribute: '',
  remark: '',
  buyerRemark: '',
  extNum1Min: null,
  extNum1Max: null,
  extNum2Min: null,
  extNum2Max: null,
  extText1: '',
  extText2: '',
  extText3: '',
  logisticsCompany: '',
  trackingNumber: '',
  region: '',
  minTotalAmount: null,
  maxTotalAmount: null,
  source: '',
  showRed: false,
  onlyVehicleWarehouse: false
}

const searchParams = reactive<Record<string, any>>({ ...DEFAULT_SEARCH_PARAMS })

// ═══ 查询字段定义（与「页面配置 → 查询条件」的 key 一一对应） ═══
type SearchFieldType = 'text' | 'select' | 'dateRange' | 'numberRange' | 'checkbox'

interface SearchFieldDef {
  key: string
  label: string
  type: SearchFieldType
  placeholder?: string
  searchIcon?: boolean
  options?: Array<{ label: string; value: string }>
  minKey?: string
  maxKey?: string
}

const OPTION_DATE_TYPE = [
  { label: '单据日期', value: 'documentDate' },
  { label: '制单时间', value: 'createTime' },
  { label: '记账时间', value: 'bookkeepingTime' },
  { label: '来源订单日期', value: 'sourceOrderDate' },
]
const OPTION_SETTLEMENT_STATUS = [
  { label: '未结算', value: 'UNPAID' },
  { label: '部分结算', value: 'PARTIAL_PAID' },
  { label: '已结算', value: 'PAID' },
]
const OPTION_DOC_TYPE = [
  { label: '销售订单', value: 'SALE_ORDER' },
  { label: '销售出库单', value: 'OUTBOUND' },
  { label: '销售退货单', value: 'RETURN' },
  { label: '销售换货单', value: 'EXCHANGE' },
]
const OPTION_PRODUCT_ATTRIBUTE = [
  { label: '普通商品', value: 'NORMAL' },
  { label: '赠品', value: 'GIFT' },
  { label: '组合商品', value: 'COMBO' },
]
const OPTION_GENERATION_METHOD = [
  { label: '手工录入', value: 'MANUAL' },
  { label: '系统生成', value: 'SYSTEM' },
  { label: '批量导入', value: 'IMPORT' },
  { label: 'API接口', value: 'API' },
]
const OPTION_SALES_TYPE = [
  { label: '零售', value: 'RETAIL' },
  { label: '批发', value: 'WHOLESALE' },
  { label: '线上', value: 'ONLINE' },
  { label: '线下', value: 'OFFLINE' },
]
const OPTION_SOURCE = [
  { label: '电脑端', value: 'PC' },
  { label: '移动端', value: 'MOBILE' },
  { label: 'API接口', value: 'API' },
  { label: '批量导入', value: 'IMPORT' },
]

const SEARCH_FIELD_DEFS: Record<string, SearchFieldDef> = {
  dateRange: { key: 'dateRange', label: '日期', type: 'dateRange' },
  dateType: { key: 'dateType', label: '日期类型', type: 'select', options: OPTION_DATE_TYPE },
  documentNo: { key: 'documentNo', label: '单据编号', type: 'text' },
  customerName: { key: 'customerName', label: '客户', type: 'text', searchIcon: true },
  receiverName: { key: 'receiverName', label: '收货人', type: 'text' },
  receiverPhone: { key: 'receiverPhone', label: '联系电话', type: 'text' },
  shippingAddress: { key: 'shippingAddress', label: '收货地址', type: 'text' },
  handlerName: { key: 'handlerName', label: '经手人', type: 'text', searchIcon: true },
  departmentName: { key: 'departmentName', label: '部门', type: 'text' },
  creatorName: { key: 'creatorName', label: '制单人', type: 'text' },
  bookkeeperName: { key: 'bookkeeperName', label: '记账人', type: 'text' },
  warehouseName: { key: 'warehouseName', label: '仓库', type: 'text', searchIcon: true },
  settlementStatus: { key: 'settlementStatus', label: '结算状态', type: 'select', options: OPTION_SETTLEMENT_STATUS },
  sourceOrder: { key: 'sourceOrder', label: '来源订单', type: 'text' },
  sourceOrderDate: { key: 'sourceOrderDate', label: '来源订单日期', type: 'dateRange' },
  generationMethod: { key: 'generationMethod', label: '产生方式', type: 'select', options: OPTION_GENERATION_METHOD },
  documentType: { key: 'documentType', label: '单据类型', type: 'select', placeholder: '全部单据', options: OPTION_DOC_TYPE },
  salesType: { key: 'salesType', label: '销售类型', type: 'select', options: OPTION_SALES_TYPE },
  productAttribute: { key: 'productAttribute', label: '商品行属性', type: 'select', options: OPTION_PRODUCT_ATTRIBUTE },
  remark: { key: 'remark', label: '单据备注', type: 'text' },
  buyerRemark: { key: 'buyerRemark', label: '买家备注', type: 'text' },
  extNum1: { key: 'extNum1', label: '表头自定义字段1(数字)', type: 'numberRange', minKey: 'extNum1Min', maxKey: 'extNum1Max' },
  extNum2: { key: 'extNum2', label: '表头自定义字段2(数字)', type: 'numberRange', minKey: 'extNum2Min', maxKey: 'extNum2Max' },
  extText1: { key: 'extText1', label: '表头自定义字段3(文本)', type: 'text' },
  extText2: { key: 'extText2', label: '表头自定义字段4(文本)', type: 'text' },
  extText3: { key: 'extText3', label: '表头自定义字段5(文本)', type: 'text' },
  logisticsCompany: { key: 'logisticsCompany', label: '物流公司', type: 'text' },
  trackingNumber: { key: 'trackingNumber', label: '运单号', type: 'text' },
  region: { key: 'region', label: '区域', type: 'text' },
  totalAmount: { key: 'totalAmount', label: '本单金额', type: 'numberRange', minKey: 'minTotalAmount', maxKey: 'maxTotalAmount' },
  source: { key: 'source', label: '来源', type: 'select', options: OPTION_SOURCE },
  showRed: { key: 'showRed', label: '显示红冲', type: 'checkbox' },
  onlyVehicleWarehouse: { key: 'onlyVehicleWarehouse', label: '仅统计车辆库', type: 'checkbox' },
}

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ═══ 枚举 / 字段展示口径 ═══
const DOC_TYPE_LABELS: Record<string, string> = {
  SALE_ORDER: '销售订单',
  OUTBOUND: '销售出库单',
  RETURN: '销售退货单',
  EXCHANGE: '销售换货单'
}

const SETTLEMENT_STATUS_LABELS: Record<string, string> = {
  UNPAID: '未结算',
  PARTIAL_PAID: '部分结算',
  PAID: '已结算'
}

/** 日期列只显示日期，时间列显示到秒 */
const DATE_ONLY_KEYS = new Set(['documentDate', 'sourceOrderDate'])
const DATETIME_KEYS = new Set(['bookkeepingTime', 'createTime'])

/**
 * 换货单业务不产生的字段（换货只做货物互换，不涉及收款 / 物流 / 优惠）。
 * 这些列对换货单显示「—」表示不适用，与「有字段但无数据」区分。
 * 注：成本金额 / 毛利对换货单同样有真实来源（明细 cost_amount 聚合），故不在此列。
 */
const EXCHANGE_NOT_APPLICABLE = new Set([
  'receiverName', 'receiverPhone', 'shippingAddress',
  'logisticsCompany', 'trackingNumber', 'region',
  'salesRevenue', 'freightPayer', 'freight', 'otherFee', 'roundingAmount',
  'promoDiscount', 'couponAmount', 'pointsDeduction'
])

function buildColumnFormatter(key: string) {
  return (value: any, record: any) => {
    if (record?.documentType === 'EXCHANGE' && EXCHANGE_NOT_APPLICABLE.has(key)) {
      return '—'
    }
    if (value === null || value === undefined || value === '') {
      return ''
    }
    if (key === 'documentType') return DOC_TYPE_LABELS[value] || value
    if (key === 'settlementStatus') return SETTLEMENT_STATUS_LABELS[value] || value
    if (DATE_ONLY_KEYS.has(key)) return dayjs(value).format('YYYY-MM-DD')
    if (DATETIME_KEYS.has(key)) return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
    return String(value)
  }
}

function withColumnFormatters(cols: any[]): any[] {
  return cols.map(col => ({
    ...col,
    formatter: col.formatter || buildColumnFormatter(col.key),
    tooltip: col.tooltip || (EXCHANGE_NOT_APPLICABLE.has(col.key)
      ? '换货单不适用该字段（该类型显示「—」）'
      : undefined)
  }))
}

// ═══ 51个表格列（按文档要求顺序，BillTableList格式） ═══
const columns = withColumnFormatters([
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' as const },
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' as const },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '单据日期', field: 'documentDate', key: 'documentDate', width: 100, sortable: true },
  { title: '单据编号', field: 'documentNo', key: 'documentNo', width: 140, sortable: true },
  { title: '单据类型', field: 'documentType', key: 'documentType', width: 90 },
  { title: '入库仓库', field: 'inboundWarehouse', key: 'inboundWarehouse', width: 100 },
  { title: '出库仓库', field: 'outboundWarehouse', key: 'outboundWarehouse', width: 100, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 110 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 90 },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 80 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 110 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 140 },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 80, align: 'right' as const },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 80, align: 'right' as const },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1', width: 100 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2', width: 100 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3', width: 100 },
  { title: '买家备注', field: 'buyerRemark', key: 'buyerRemark', width: 120 },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  { title: '来源订单', field: 'sourceOrder', key: 'sourceOrder', width: 140 },
  { title: '来源订单日期', field: 'sourceOrderDate', key: 'sourceOrderDate', width: 100 },
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 100 },
  { title: '运单号', field: 'trackingNumber', key: 'trackingNumber', width: 120 },
  { title: '区域', field: 'region', key: 'region', width: 80 },
  { title: '产生方式', field: 'generationMethod', key: 'generationMethod', width: 80 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80 },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 80 },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 80 },
  { title: '销售数量', field: 'salesQuantity', key: 'salesQuantity', width: 80, align: 'right' as const },
  { title: '金额', field: 'amount', key: 'amount', width: 90, align: 'right' as const },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 90, align: 'right' as const },
  { title: '销售收入', field: 'salesRevenue', key: 'salesRevenue', width: 90, align: 'right' as const },
  { title: '运费承担方', field: 'freightPayer', key: 'freightPayer', width: 90 },
  { title: '运费', field: 'freight', key: 'freight', width: 80, align: 'right' as const },
  { title: '其它费用', field: 'otherFee', key: 'otherFee', width: 80, align: 'right' as const },
  { title: '抹零金额', field: 'roundingAmount', key: 'roundingAmount', width: 80, align: 'right' as const },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 90, align: 'right' as const },
  { title: '促销优惠', field: 'promoDiscount', key: 'promoDiscount', width: 80, align: 'right' as const },
  { title: '优惠券优惠', field: 'couponAmount', key: 'couponAmount', width: 90, align: 'right' as const },
  { title: '直接优惠', field: 'directDiscount', key: 'directDiscount', width: 80, align: 'right' as const },
  { title: '积分抵扣', field: 'pointsDeduction', key: 'pointsDeduction', width: 80, align: 'right' as const },
  { title: '成本金额', field: 'costAmount', key: 'costAmount', width: 90, align: 'right' as const },
  { title: '毛利', field: 'grossProfit', key: 'grossProfit', width: 80, align: 'right' as const },
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 80 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 100 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 100 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 60, align: 'right' as const }
])

// ═══ 本页合计 ═══
const summaryData = computed(() => {
  const data = tableData.value
  if (!data.length) return {}
  const sum = (key: string) => {
    const val = data.reduce((acc, r) => acc + (Number(r[key]) || 0), 0)
    return val ? val.toFixed(2) : ''
  }
  return {
    salesQuantity: sum('salesQuantity'),
    amount: sum('amount'),
    totalAmount: sum('totalAmount'),
    grossProfit: sum('grossProfit'),
  }
})

// ═══ 页面配置 ═══
const queryFieldsConfig = ref([
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'dateType', label: '日期类型', visible: true },
  { key: 'documentNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'sourceOrder', label: '来源订单', visible: true },
  { key: 'documentType', label: '单据类型', visible: true },
  { key: 'productAttribute', label: '商品行属性', visible: true },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
  { key: 'shippingAddress', label: '收货地址', visible: false },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'sourceOrderDate', label: '来源订单日期', visible: false },
  { key: 'generationMethod', label: '产生方式', visible: false },
  { key: 'salesType', label: '销售类型', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'buyerRemark', label: '买家备注', visible: false },
  { key: 'extNum1', label: '自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '自定义字段5(文本)', visible: false },
  { key: 'logisticsCompany', label: '物流公司', visible: false },
  { key: 'trackingNumber', label: '运单号', visible: false },
  { key: 'region', label: '区域', visible: false },
  { key: 'totalAmount', label: '本单金额', visible: false },
  { key: 'source', label: '来源', visible: false },
  // 对标截图：第 2 排「查询」按钮旁只跟一个勾选项
  { key: 'showRed', label: '显示红冲', visible: false },
  { key: 'onlyVehicleWarehouse', label: '仅统计车辆库', visible: true }
])

/** 搜索区实际渲染字段：按页面配置的勾选与顺序（配置真实生效） */
const visibleSearchFields = computed(() =>
  queryFieldsConfig.value
    .filter(cfg => cfg.visible)
    .map(cfg => SEARCH_FIELD_DEFS[cfg.key])
    .filter((def): def is SearchFieldDef => !!def)
)

/** 字段网格：参与「更多条件」折叠 */
const gridSearchFields = computed(() => visibleSearchFields.value.filter(f => f.type !== 'checkbox'))
/** 操作行：查询按钮旁的勾选项，始终可见 */
const actionSearchFields = computed(() => visibleSearchFields.value.filter(f => f.type === 'checkbox'))

const functionButtonConfig = ref([
  { key: 'search', label: '查询', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '页面配置', enabled: true },
  { key: 'more', label: '更多条件', enabled: true }
])

/** 功能按钮开关（页面配置勾选真实生效：取消勾选即隐藏对应按钮） */
const enabledButtons = ref<Record<string, boolean>>({})
const isButtonEnabled = (key: string) => enabledButtons.value[key] !== false

const printConfigItems = [
  { key: 'alwaysLastTemplate', label: '始终使用最后一次打印的模板，打印时不再选择' }
]
const printConfig = reactive<Record<string, boolean>>({ alwaysLastTemplate: false })

function syncPageConfig(config: any) {
  if (!config) return
  if (Array.isArray(config.queryFields) && config.queryFields.length) {
    const orderMap = new Map<string, number>()
    config.queryFields.forEach((f: any, i: number) => orderMap.set(f.key, i))
    queryFieldsConfig.value = [...queryFieldsConfig.value]
      .sort((a, b) => (orderMap.get(a.key) ?? 999) - (orderMap.get(b.key) ?? 999))
      .map(f => {
        const saved = config.queryFields.find((c: any) => c.key === f.key)
        return saved ? { ...f, visible: saved.visible } : f
      })
  }
  if (Array.isArray(config.functionButtons)) {
    const map: Record<string, boolean> = {}
    config.functionButtons.forEach((b: any) => { map[b.key] = b.enabled })
    enabledButtons.value = map
  }
  if (config.printConfig) {
    Object.assign(printConfig, config.printConfig)
  }
}

const handlePageConfigChange = (config: any) => {
  syncPageConfig(config)
}

/** 首次进入时同步已保存的页面配置（含打印配置），避免必须打开弹窗才生效 */
function loadSavedPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) syncPageConfig(JSON.parse(raw))
  } catch {
    // 配置损坏时按默认值运行
  }
}

// ═══ 日期处理 ═══
const handleDocumentDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

const handleSourceOrderDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.sourceOrderStartDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.sourceOrderEndDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.sourceOrderStartDate = ''
    searchParams.sourceOrderEndDate = ''
  }
}

const toggleMoreConditions = () => {
  showMoreConditions.value = !showMoreConditions.value
}

// ═══ 数据请求 ═══
/** 查询参数：搜索条件 + 分页；空值不传 */
const buildQueryParams = (current: number, size: number): Record<string, any> => {
  const params: Record<string, any> = { current, size, ...searchParams }
  Object.keys(params).forEach(key => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) {
      delete params[key]
    }
  })
  return params
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await request.get('/sales/doc-query/page', {
      params: buildQueryParams(pagination.current, pagination.pageSize)
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = Number(data.total) || 0
    }
  } catch (e: any) {
    hasError.value = true
    message.error('查询失败，请检查网络后重试')
    console.warn('[销售单据查询] 获取失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleSelectionChange = (_rows: any[], ids: any[]) => {
  selectedRowKeys.value = ids
}

// ═══ 导出（真实 Excel：xlsx 生成 .xlsx；按当前查询条件全量拉取，而非仅当前页） ═══
const EXPORT_PAGE_SIZE = 5000

function buildExportRows(list: any[]) {
  const cols = columns.filter((c: any) => c.key !== 'rowNo' && c.key !== 'action' && c.type !== 'checkbox')
  return list.map(row => {
    const item: Record<string, any> = {}
    cols.forEach((col: any) => {
      item[col.title] = col.formatter
        ? col.formatter(row[col.field], row)
        : (row[col.field] ?? '')
    })
    return item
  })
}

async function handleExportMenu({ key }: { key: string | number }) {
  loading.value = true
  try {
    const res: any = await request.get('/sales/doc-query/page', {
      params: buildQueryParams(1, EXPORT_PAGE_SIZE)
    })
    const data = res?.data || res
    const list = data.records || data.content || data.list || []
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    if (list.length >= EXPORT_PAGE_SIZE) {
      message.warning(`导出达到上限 ${EXPORT_PAGE_SIZE} 条，请缩小查询范围`)
    }

    const rows = buildExportRows(list)
    const fileName = `销售单据查询_${dayjs().format('YYYYMMDD_HHmmss')}`
    const ws = XLSX.utils.json_to_sheet(rows)
    if (key === 'csv') {
      const csv = XLSX.utils.sheet_to_csv(ws)
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${fileName}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } else {
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, '销售单据')
      XLSX.writeFile(wb, `${fileName}.xlsx`)
    }
    message.success(`已导出 ${list.length} 条`)
  } catch (e) {
    message.error('导出失败')
    console.warn('[销售单据查询] 导出失败', e)
  } finally {
    loading.value = false
  }
}

// ═══ 打印（F8）：接入通用打印组件（打印模板渲染 → 本地打印 / 远程打印链） ═══
/**
 * 单据类型 → 打印模板所属页面编码。
 * ⚠️ 必须与各页 `<PrintDialog page-code>` 逐字一致：
 * 原来写的是 `'sale-order'`（销售订单页用的是 `'sale'`）—— 那个编码全站只存在于这一行，
 * 既没有模板也没有装配器，点打印必然报「还没有已发布的打印模板」。
 */
const PRINT_PAGE_CODE: Record<string, string> = {
  SALE_ORDER: 'sale',
  OUTBOUND: 'sale-outbound',
  RETURN: 'sale-return-doc',
  EXCHANGE: 'sale-exchange'
}

const showPrintDialog = ref(false)
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printPageCode = ref('sale')
const printData = ref<Record<string, any>>({})

async function handlePrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先勾选要打印的单据')
    return
  }
  if (selectedRowKeys.value.length > 1) {
    message.info('打印为单张单据模板，已取勾选的第一条')
  }
  const record = tableData.value.find(r => r.id === selectedRowKeys.value[0])
  if (!record) {
    message.warning('未找到选中单据，请刷新后重试')
    return
  }
  printPageCode.value = PRINT_PAGE_CODE[record.documentType] || 'sale'
  printData.value = { ...record }
  showPrintDialog.value = true
  await nextTick()
  printDialogRef.value?.open()
}

const handlePrintSuccess = () => {
  fetchData()
}

// ═══ 复制 ═══
const COPY_ROUTE_MAP: Record<string, string> = {
  SALE_ORDER: '/erp/sale/form',
  OUTBOUND: '/sales/outbound/create',
  RETURN: '/sales/return-doc/create',
  EXCHANGE: '/sales/exchange/form',
}

function handleCopy(record: any) {
  const route = COPY_ROUTE_MAP[record.documentType]
  if (route) {
    router.push({ path: route, query: { copyFrom: record.id } })
  } else {
    message.warning('暂不可用该单据类型的复制')
  }
}

// ═══ 整单备注（落库接口：PUT /sales/doc-query/{docType}/{id}/remark） ═══
const showNoteModal = ref(false)
const noteSaving = ref(false)
const noteRecord = reactive<any>({ documentNo: '', id: null, documentType: '' })
const noteContent = ref('')

function handleNote(record: any) {
  noteRecord.documentNo = record.documentNo
  noteRecord.id = record.id
  noteRecord.documentType = record.documentType
  noteContent.value = record.remark || ''
  showNoteModal.value = true
}

async function saveNote() {
  if (!noteRecord.id || !noteRecord.documentType) {
    message.error('单据信息缺失，请刷新后重试')
    return
  }
  noteSaving.value = true
  try {
    const res: any = await request.put(
      `/sales/doc-query/${noteRecord.documentType}/${noteRecord.id}/remark`,
      { remark: noteContent.value }
    )
    const data = res?.data || res
    if (data && data.success === false) {
      message.error(data.message || '备注保存失败')
      return
    }
    message.success('备注保存成功')
    showNoteModal.value = false
    fetchData()
  } catch (e) {
    message.error('备注保存失败')
    console.warn('[销售单据查询] 备注保存失败', e)
  } finally {
    noteSaving.value = false
  }
}

// ═══ 错误处理 ═══
const handleError = (e: Error) => {
  hasError.value = true
  console.error(e)
}

onMounted(() => {
  loadSchemes()
  loadSavedPageConfig()
  // 初始化默认快捷日期（本周）
  setQuickDate('thisWeek')
})
</script>

<style scoped>
/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn-link) {
  color: #555;
  padding: 0 8px;
  height: 24px;
  line-height: 24px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #fa8c16;
  border-color: #fa8c16;
}
.quick-dates :deep(.ant-btn-primary:hover) {
  background: #fa8c16;
  border-color: #fa8c16;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container:not(:has([data-expanded])) > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; align-items: center; min-width: 0; }
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
/* 查询操作行：位于字段折叠区之外，字段再多也不会被裁掉 */
.search-action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}
.search-more-toggle {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.search-more-toggle::before,
.search-more-toggle::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.search-more-toggle :deep(.ant-btn) {
  font-size: 13px;
}

/* ═══ 表格底部合计 ═══ */
.table-footer {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-top: none;
  font-size: 12px;
  color: #333;
}
.footer-label {
  font-weight: 600;
  min-width: 170px;
}
.footer-values {
  flex: 1;
  display: flex;
  gap: 16px;
}

/* ═══ 查询方案管理 ═══ */
.scheme-save {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.scheme-list {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}
.scheme-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-bottom: 1px solid #f5f5f5;
}
.scheme-item:last-child {
  border-bottom: none;
}
.scheme-name {
  font-size: 13px;
  color: #333;
}
.scheme-empty {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: #999;
}
</style>
