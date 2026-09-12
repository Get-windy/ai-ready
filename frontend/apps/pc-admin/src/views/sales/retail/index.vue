<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab栏 + 工具栏 ═══ -->
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
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

        <!-- ═══ 工具栏右侧：操作按钮（受页面配置「功能按钮」开关控制） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="isButtonEnabled('config')"
              title="页面配置"
              placement="bottom"
            >
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrintF8()"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（字段由「页面配置 → 查询条件」驱动，勾选即生效） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container" :data-expanded="showMoreCond || null">
              <div class="search-grid" :ref="activeTab === 'doc' ? 'docGridRef' : 'detailGridRef'">
                <template
                  v-for="f in activeQueryFields"
                  :key="f.key"
                >
                  <div v-if="f.key === 'date'" class="search-field-item">
                    <a-range-picker
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <div v-else-if="f.key === 'status'" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option :value="undefined">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">已完成</a-select-option>
                        <a-select-option :value="2">挂单</a-select-option>
                        <a-select-option :value="3">已作废</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-else-if="f.key === 'saleType'" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">销售类型</span>
                      <a-select v-model:value="searchParams.saleType" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="NORMAL">正常销售</a-select-option>
                        <a-select-option value="RETURN">退货</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-else-if="f.key === 'showRedFlush'" class="search-field-item">
                    <a-checkbox v-model:checked="searchParams.showRedFlush">显示红冲</a-checkbox>
                  </div>
                  <div v-else class="search-field-item">
                    <a-input
                      v-model:value="(searchParams as any)[f.key]"
                      :placeholder="f.label"
                      allow-clear
                      size="small"
                    />
                  </div>
                </template>
                <div class="search-action-group" :ref="activeTab === 'doc' ? 'docActionRef' : 'detailActionRef'" :style="{ gridColumn: 'span ' + activeActionSpan }">
                  <div class="search-field-item search-action-item">
                    <a-space :size="8">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                      <a-button size="small" @click="handleReset">重置</a-button>
                    </a-space>
                  </div>
                </div>
              </div>
              <div class="search-more-toggle">
                <a-button type="link" size="small" @click="showMoreCond = !showMoreCond">
                  {{ showMoreCond ? '收起条件' : '更多条件' }}
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <div class="table-wrap">
            <BillTableList
              :columns="currentColumns"
              :storage-key="activeTab === 'doc' ? 'retail-table-columns-doc' : 'retail-table-columns-detail'"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <!-- 单据编号 -->
              <template #retailNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  {{ record.retailNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <!-- 金额 -->
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.amount) }}</span>
              </template>
              <!-- 应付金额 -->
              <template #payableAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payableAmount) }}</span>
              </template>
              <!-- 操作列（__ghost 为表格补齐的空行，不渲染操作） -->
              <template #actionCell="{ record }">
                <a-space v-if="record && !record.__ghost" :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                  <a-button
                    v-if="record.status === 2"
                    type="link"
                    size="small"
                    @click="handleUnhold(record)"
                  >
                    取单
                  </a-button>
                  <a-dropdown v-if="record.status > 0">
                    <a-button type="link" size="small">
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click="handlePrintF8(record)">
                          打印
                        </a-menu-item>
                        <a-menu-item @click="handleCopy(record)">
                          复制
                        </a-menu-item>
                        <a-menu-item v-if="record.status !== 3" @click="handleVoid(record)">
                          作废
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

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeTab === 'detail' ? detailQueryConfig : docQueryConfig"
      :storage-key="'retail-page-config'"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { retailOrderApi, userPageConfigApi } from '@/api/erp'
import { useRouter } from 'vue-router'

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

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  retailNo: '',
  customerName: '',
  handlerName: '',
  departmentName: '',
  warehouseName: '',
  productName: '',
  productAttribute: '',
  itemRemark: '',
  status: undefined as number | undefined,
  saleType: '',
  remark: '',
  creatorName: '',
  memberCardNo: '',
  extNum1: '',
  extNum2: '',
  extText1: '',
  extText2: '',
  extText3: '',
  printCount: '',
  showRedFlush: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const showMoreCond = ref(false)

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)
const activeActionSpan = computed(() => activeTab.value === 'doc' ? docActionSpan.value : detailActionSpan.value)

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选（BillTableList 通过 type=checkbox 列 + selection-change 事件回传）═══
const selectedRowKeys = ref<any[]>([])
function handleSelectionChange(_rows: any[], ids: any[]) {
  selectedRowKeys.value = ids || []
}

// ═══ 配置弹窗 ═══
const showPageConfig = ref(false)

// ═══ 页面配置 ═══
const PAGE_CONFIG_MODULE = 'retail'
const PAGE_CONFIG_PAGE = 'order'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'retailNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'saleType', label: '销售类型', visible: true },
  { key: 'productAttribute', label: '商品行属性', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '表头自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '表头自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '表头自定义字段5(文本)', visible: false },
  { key: 'printCount', label: '打印次数', visible: false },
  { key: 'showRedFlush', label: '显示红冲', visible: true },
]

const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'retailNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'showRedFlush', label: '显示红冲', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

/** 当前 Tab 实际渲染的查询条件（勾选即生效，含排序） */
const activeQueryFields = computed<QueryFieldSetting[]>(() => {
  const list = activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value
  return list.filter(f => f.visible)
})

/** 功能按钮开关是否启用（未配置时默认启用） */
function isButtonEnabled(key: string): boolean {
  return functionButtonConfig.value.find(b => b.key === key)?.enabled !== false
}

async function loadPageConfig() {
  // 先尝试从 API 加载
  try {
    const raw = await userPageConfigApi.get(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE)
    if (raw) {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.detailQueryFields) {
        detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => {
          const saved = parsed.detailQueryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
      return
    }
  } catch { /* API 不可用时切换到 localStorage */ }

  // 切换：从 localStorage 加载
  try {
    const raw = localStorage.getItem('retail-order-page-config')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch { /* ignore */ }
}

async function handlePageConfigChange(config: any) {
  // 面板每次只回传「当前 Tab」的查询条件列表，需按当前 Tab 归类回写，
  // 否则按明细的勾选会覆盖按单据的配置
  const isDetail = activeTab.value === 'detail'
  const edited = config.queryFields
  const payload = {
    queryFields: (isDetail ? docQueryConfig.value : (edited || docQueryConfig.value)),
    detailQueryFields: (isDetail ? (edited || detailQueryConfig.value) : detailQueryConfig.value),
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }
  // 保存到后端 API
  try {
    await userPageConfigApi.save(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
  // 同时写入 localStorage 作为切换
  try {
    localStorage.setItem('retail-order-page-config', JSON.stringify(payload))
  } catch { /* ignore */ }
  await loadPageConfig()
}

// ═══ 列定义 ═══

/** 按单据 Tab 列 (42 数据列；rowCheck 承载行选择，rowNo 承载表头列配置齿轮) */
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheck', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 160, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'retailNo', key: 'retailNo', width: 170, type: 'slot', slotName: 'retailNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 200, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '销售数量', field: 'totalQuantity', key: 'totalQuantity', width: 80, align: 'right', sortable: true },
  { title: '金额', field: 'amount', key: 'amount', width: 120, align: 'right', type: 'slot', slotName: 'amountCell', sortable: true },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 120, align: 'right' },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 120, align: 'right' },
  { title: '本单金额', field: 'payableAmount', key: 'payableAmount', width: 120, align: 'right', type: 'slot', slotName: 'payableAmountCell', sortable: true },
  { title: '促销优惠', field: 'promoDiscount', key: 'promoDiscount', width: 100, align: 'right' },
  { title: '优惠券', field: 'couponDiscount', key: 'couponDiscount', width: 100, align: 'right' },
  { title: '直接优惠', field: 'directDiscount', key: 'directDiscount', width: 100, align: 'right' },
  { title: '优惠金额', field: 'totalDiscount', key: 'totalDiscount', width: 100, align: 'right' },
  { title: '收款现金', field: 'cashAmount', key: 'cashAmount', width: 100, align: 'right' },
  { title: '收款银行卡', field: 'cardAmount', key: 'cardAmount', width: 100, align: 'right' },
  { title: '收款支付宝', field: 'alipayAmount', key: 'alipayAmount', width: 100, align: 'right' },
  { title: '收款微信', field: 'wechatAmount', key: 'wechatAmount', width: 100, align: 'right' },
  { title: '收款聚合支付', field: 'aggregateAmount', key: 'aggregateAmount', width: 110, align: 'right' },
  { title: '收款预收款', field: 'prepaidAmount', key: 'prepaidAmount', width: 100, align: 'right' },
  { title: '收款共享平台支付账户', field: 'paymentAccount1', key: 'paymentAccount1', width: 150 },
  { title: '收款中国农业银行', field: 'abcAmount', key: 'abcAmount', width: 120, align: 'right' },
  { title: '收款中国建设银行', field: 'ccbAmount', key: 'ccbAmount', width: 120, align: 'right' },
  { title: '收款京东支付', field: 'jdAmount', key: 'jdAmount', width: 100, align: 'right' },
  { title: '收款合计', field: 'totalReceived', key: 'totalReceived', width: 100, align: 'right' },
  { title: '销售类型', field: 'saleType', key: 'saleType', width: 80 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 130 },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 130 },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1Doc', width: 130 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2Doc', width: 130 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3Doc', width: 130 },
  { title: '摘要', field: 'summary', key: 'summary', width: 150 },
  { title: '附件', field: 'attachment', key: 'attachmentDoc', width: 60 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

/** 按明细 Tab 列 (52 数据列；rowCheck 承载行选择，rowNo 承载表头列配置齿轮) */
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheck', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'retailNo', key: 'retailNo', width: 170, type: 'slot', slotName: 'retailNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80 },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'itemCode', key: 'itemCode', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'specification', key: 'specification', width: 100 },
  { title: '型号', field: 'model', key: 'model', width: 80 },
  { title: '产地', field: 'origin', key: 'origin', width: 80 },
  { title: '品牌', field: 'brand', key: 'brand', width: 80 },
  { title: '表体自定义1', field: 'extNum1', key: 'extNum1', width: 120 },
  { title: '表体自定义2', field: 'extNum2', key: 'extNum2', width: 120 },
  { title: '表体自定义3', field: 'extNum3', key: 'extNum3', width: 120 },
  { title: '表体自定义4', field: 'extText1', key: 'extText1', width: 120 },
  { title: '表体自定义5', field: 'extText2', key: 'extText2', width: 120 },
  { title: '表体自定义6', field: 'extNum4', key: 'extNum4', width: 120 },
  { title: '表体自定义7', field: 'extNum5', key: 'extNum5', width: 120 },
  { title: '表体自定义8', field: 'extPartner', key: 'extPartner', width: 120 },
  { title: '表体自定义9', field: 'extStaff', key: 'extStaff', width: 100 },
  { title: '表体自定义10', field: 'extDept', key: 'extDept', width: 100 },
  { title: '单位', field: 'unit', key: 'unit', width: 80 },
  { title: '销售数量', field: 'quantity', key: 'quantity', width: 80, align: 'right', sortable: true },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100 },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 100 },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 70, align: 'right' },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 70, align: 'right' },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 70, align: 'right' },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right' },
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 80, align: 'right' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 90, align: 'right' },
  { title: '金额', field: 'amount', key: 'amount', width: 100, align: 'right', sortable: true },
  { title: '折扣(%)', field: 'discountRate', key: 'discountRate', width: 70, align: 'right' },
  { title: '折后单价', field: 'discountedPrice', key: 'discountedPrice', width: 80, align: 'right' },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 100, align: 'right' },
  { title: '优惠折扣', field: 'favorableDiscountRate', key: 'favorableDiscountRate', width: 80, align: 'right' },
  { title: '优惠后单价', field: 'favorableUnitPrice', key: 'favorableUnitPrice', width: 90, align: 'right' },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 100, align: 'right' },
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 120 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

const currentColumns = computed(() => activeTab.value === 'doc' ? docColumns : detailColumns)

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已完成', color: 'green' },
  2: { text: '挂单', color: 'orange' },
  3: { text: '已作废', color: 'red' },
}

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}
function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.amount || 0), 0)
  const payableAmount = tableData.value.reduce((s: number, r: any) => s + (r.payableAmount || 0), 0)
  const totalReceived = tableData.value.reduce((s: number, r: any) => s + (r.totalReceived || 0), 0)
  return [
    { key: 'amount', value: totalAmount, highlight: true },
    { key: 'payableAmount', value: payableAmount, highlight: true },
    { key: 'totalReceived', value: totalReceived, highlight: true },
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
    if (searchParams.retailNo) params.retailNo = searchParams.retailNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.departmentName) params.departmentName = searchParams.departmentName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.saleType) params.saleType = searchParams.saleType
    if (searchParams.productAttribute) params.productAttribute = searchParams.productAttribute
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.memberCardNo) params.memberCardNo = searchParams.memberCardNo
    if (searchParams.printCount !== '' && searchParams.printCount != null) params.printCount = Number(searchParams.printCount)
    if (searchParams.extNum1 !== '' && searchParams.extNum1 != null) params.extNum1 = searchParams.extNum1
    if (searchParams.extNum2 !== '' && searchParams.extNum2 != null) params.extNum2 = searchParams.extNum2
    if (searchParams.extText1) params.extText1 = searchParams.extText1
    if (searchParams.extText2) params.extText2 = searchParams.extText2
    if (searchParams.extText3) params.extText3 = searchParams.extText3
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate
    if (searchParams.showRedFlush) params.showRedFlush = true

    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.itemRemark) params.remark = searchParams.itemRemark
      const res = await retailOrderApi.pageByDetail(params)
      if (res) {
        tableData.value = res.records || []
        pagination.total = Number(res.total) || 0
      }
    } else {
      const res = await retailOrderApi.pageByDoc(params)
      if (res) {
        tableData.value = res.records || []
        pagination.total = Number(res.total) || 0
      }
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[零售单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
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
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() {
  Object.assign(searchParams, {
    retailNo: '', customerName: '', handlerName: '', departmentName: '', warehouseName: '',
    productName: '', productAttribute: '', itemRemark: '', status: undefined, saleType: '', remark: '',
    creatorName: '', memberCardNo: '', printCount: '',
    extNum1: '', extNum2: '', extText1: '', extText2: '', extText3: '',
    showRedFlush: false,
  })
  dateRange.value = [dayjs().subtract(7, 'day'), dayjs()]
  searchParams.startDate = dayjs().subtract(7, 'day').format('YYYY-MM-DD')
  searchParams.endDate = dayjs().format('YYYY-MM-DD')
  pagination.current = 1
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

// ═══ 操作 ═══
function handleAdd() { router.push('/sales/retail/create') }
function handleView(record: any) { router.push(`/sales/retail/form/${record.id}`) }
function handleEdit(record: any) { router.push(`/sales/retail/form/${record.id}`) }
function handleCopy(record: any) {
  Modal.confirm({
    title: '确认复制', content: `确定要复制零售单 ${record.retailNo} 吗？`,
    onOk: async () => {
      try {
        await retailOrderApi.copy(record.id)
        message.success('复制成功')
        fetchData()
      } catch (e: any) { message.error(e?.response?.data?.message || '复制失败') }
    },
  })
}
function handleVoid(record: any) {
  Modal.confirm({
    title: '确认作废', content: `确定要作废零售单 ${record.retailNo} 吗？`,
    okType: 'danger', okText: '确认作废',
    onOk: async () => {
      try {
        await retailOrderApi.voidOrder(record.id)
        message.success('作废成功')
        fetchData()
      } catch (e: any) { message.error(e?.response?.data?.message || '作废失败') }
    },
  })
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定要删除零售单 ${record.retailNo} 吗？`,
    okType: 'danger', okText: '确认删除',
    onOk: async () => {
      try {
        await retailOrderApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (e: any) { message.error(e?.response?.data?.message || '删除失败') }
    },
  })
}

// ═══ 取单（挂单 → 草稿）═══
function handleUnhold(record: any) {
  Modal.confirm({
    title: '确认取单', content: `确定要取回挂单 ${record.retailNo} 吗？`,
    onOk: async () => {
      try {
        await retailOrderApi.unhold(record.id)
        message.success('取单成功，正在打开单据')
        router.push(`/sales/retail/form/${record.id}`)
      } catch (e: any) { message.error(e?.response?.data?.message || '取单失败') }
    },
  })
}

// ═══ 打印(F8)：取选中单据的真实打印数据，渲染小票后调用打印并累加打印次数 ═══
async function handlePrintF8(record?: any) {
  const id = record?.id ?? selectedRowKeys.value[0]
  if (!id) {
    message.warning('请先勾选要打印的零售单')
    return
  }
  try {
    const detail: any = await retailOrderApi.getPrintData(id)
    const order = detail?.order || {}
    const items: any[] = detail?.items || []
    const payments: any[] = detail?.payments || []
    const rows = items.map((it: any, i: number) => `
      <tr>
        <td>${i + 1}</td>
        <td>${escapeHtml(it.productName || '')}</td>
        <td>${escapeHtml(it.barcode || '')}</td>
        <td class="num">${fmtNum(it.quantity)}</td>
        <td class="num">${fmtNum(it.unitPrice)}</td>
        <td class="num">${fmtNum(it.amount)}</td>
      </tr>`).join('')
    const payRows = payments.map((p: any) => `
      <tr><td>${escapeHtml(p.paymentMethodLabel || paymentMethodLabel(p.paymentMethod))}</td><td class="num">${fmtNum(p.paymentAmount)}</td></tr>`).join('')
    const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
      <title>零售单 ${escapeHtml(order.retailNo || '')}</title>
      <style>
        body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
        h2{text-align:center;margin:0 0 12px;font-size:18px}
        .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
        table{width:100%;border-collapse:collapse;font-size:12px}
        th,td{border:1px solid #999;padding:4px 6px;text-align:left}
        th{background:#f2f2f2}
        .num{text-align:right}
        .totals{margin-top:8px;font-size:13px;text-align:right}
        .totals span{margin-left:16px}
      </style></head><body>
      <h2>零售单</h2>
      <div class="meta">
        <span>单据编号：${escapeHtml(order.retailNo || '')}</span>
        <span>单据日期：${escapeHtml(order.orderDate || '')}</span>
        <span>客户：${escapeHtml(order.customerName || '散客')}</span>
        <span>仓库：${escapeHtml(order.warehouseName || '')}</span>
        <span>经手人：${escapeHtml(order.handlerName || '')}</span>
        <span>制单人：${escapeHtml(order.creatorName || '')}</span>
      </div>
      <table>
        <thead><tr><th>#</th><th>商品名称</th><th>条码</th><th>数量</th><th>单价</th><th>金额</th></tr></thead>
        <tbody>${rows || '<tr><td colspan="6">无明细</td></tr>'}</tbody>
      </table>
      <div class="totals">
        <span>商品金额：${fmtNum(order.amount)}</span>
        <span>优惠金额：${fmtNum(order.totalDiscount)}</span>
        <span>本单应收：${fmtNum(order.payableAmount)}</span>
        <span>收款合计：${fmtNum(order.totalReceived)}</span>
        <span>找零：${fmtNum(order.changeAmount)}</span>
      </div>
      ${payRows ? `<table style="margin-top:8px"><thead><tr><th>收款方式</th><th>金额</th></tr></thead><tbody>${payRows}</tbody></table>` : ''}
      </body></html>`
    const win = window.open('', '_blank', 'width=900,height=700')
    if (!win) {
      message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
      return
    }
    win.document.write(html)
    win.document.close()
    win.focus()
    win.print()
    // 打印计数走真实接口
    await retailOrderApi.afterPrint(id)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '打印失败')
  }
}

// ═══ 导出：按当前 Tab 口径导出列表数据（真实查询结果，非桩）═══
async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate
    if (searchParams.retailNo) params.retailNo = searchParams.retailNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.departmentName) params.departmentName = searchParams.departmentName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.saleType) params.saleType = searchParams.saleType
    if (searchParams.showRedFlush) params.showRedFlush = true

    let headers: string[] = []
    let rows: any[][] = []
    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.itemRemark) params.remark = searchParams.itemRemark
      const res: any = await retailOrderApi.pageByDetail(params)
      const data = res?.records || []
      if (!data.length) { message.warning('没有可导出的数据'); return }
      headers = ['单据日期', '单据编号', '单据状态', '仓库', '客户编号', '客户', '经手人', '部门',
        '商品名称', '货号', '条码', '规格', '型号', '产地', '品牌', '单位', '销售数量',
        '换算关系', '换算结果', '单价', '小单位单价', '金额', '折扣(%)', '折后单价', '折后金额',
        '优惠折扣', '优惠后单价', '优惠后金额', '明细备注', '单据备注', '记账人', '制单人', '制单时间', '打印次数']
      rows = data.map((r: any) => [r.orderDate, r.retailNo, getStatusText(r.status), r.warehouseName, r.customerCode, r.customerName,
        r.handlerName, r.departmentName, r.productName, r.itemCode, r.barcode, r.specification, r.model, r.origin, r.brand,
        r.unit, r.quantity, r.conversionRelation, r.conversionResult, r.unitPrice, r.smallUnitPrice, r.amount,
        r.discountRate, r.discountedPrice, r.discountedAmount, r.favorableDiscountRate, r.favorableUnitPrice, r.favorableAmount,
        r.itemRemark, r.remark, r.bookkeeperName, r.creatorName, r.createTime, r.printCount])
    } else {
      const res: any = await retailOrderApi.pageByDoc(params)
      const data = res?.records || []
      if (!data.length) { message.warning('没有可导出的数据'); return }
      headers = ['单据日期', '单据编号', '单据状态', '仓库', '客户编号', '客户', '经手人', '部门',
        '销售数量', '金额', '折后金额', '优惠后金额', '本单金额', '促销优惠', '优惠券', '直接优惠', '优惠金额',
        '收款现金', '收款银行卡', '收款支付宝', '收款微信', '收款聚合支付', '收款预收款',
        '收款中国农业银行', '收款中国建设银行', '收款京东支付', '收款合计', '销售类型', '单据备注',
        '摘要', '记账人', '制单人', '记账时间', '制单时间', '打印次数']
      rows = data.map((r: any) => [r.orderDate, r.retailNo, getStatusText(r.status), r.warehouseName, r.customerCode, r.customerName,
        r.handlerName, r.departmentName, r.totalQuantity, r.amount, r.discountedAmount, r.favorableAmount, r.payableAmount,
        r.promoDiscount, r.couponDiscount, r.directDiscount, r.totalDiscount,
        r.cashAmount, r.cardAmount, r.alipayAmount, r.wechatAmount, r.aggregateAmount, r.prepaidAmount,
        r.abcAmount, r.ccbAmount, r.jdAmount, r.totalReceived, r.saleType === 'RETURN' ? '退货' : '正常销售', r.remark,
        r.summary, r.bookkeeperName, r.creatorName, r.bookkeepingTime, r.createTime, r.printCount])
    }

    const escapeCsv = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
    const csv = [headers.join(','), ...rows.map(r => r.map(escapeCsv).join(','))].join('\r\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `零售单${activeTab.value === 'detail' ? '明细' : ''}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success(`导出成功（${rows.length} 行）`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '导出失败')
  }
}

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string))
}
function fmtNum(v: any): string {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function paymentMethodLabel(method: string): string {
  const map: Record<string, string> = {
    CASH: '现金', CARD: '银行卡', ALIPAY: '支付宝', WECHAT: '微信', AGGREGATE: '聚合支付',
    ABC: '中国农业银行', CCB: '中国建设银行', JD: '京东支付', PREPAID: '预收款', TRANSFER: '转账',
    MIXED: '组合收款',
  }
  return map[method] || method || ''
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[零售单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 初始化 ═══
onMounted(async () => {
  await loadPageConfig()
  setQuickDate('lastWeek')
  fetchData()
})
</script>
<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { font-weight: 600; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container:not(:has([data-expanded])) > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
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
.search-action-item { flex-shrink: 0; }
.search-action-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.search-action-group .search-field-item {
  flex: 0 0 auto;
  width: auto;
  margin-right: 4px;
}
.search-action-group .search-field-item:last-child {
  margin-right: 0;
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
.table-wrap { flex: 1; overflow: hidden; }
.currency-value { font-family: 'Menlo', 'Monaco', 'Consolas', monospace; font-size: 13px; }
</style>
