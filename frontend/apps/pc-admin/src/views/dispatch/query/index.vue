<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 9 段快捷时间（对标 ql361 配送查询） ═══ -->
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
            <a-tooltip
              title="保存当前查询条件为方案"
              placement="bottom"
            >
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
            :size="2"
            class="quick-dates"
          >
            <a-button
              v-for="d in QUICK_DATES"
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
            <a-button
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="openBatchPrintModal"
            >
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向自适应网格：与系统其它单据页统一，控件内 placeholder 即条件名） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div class="search-field-item">
                  <a-range-picker
                    v-model:value="deliveryDateRange"
                    size="small"
                    style="width: 100%"
                    :placeholder="['配送起', '配送止']"
                    @change="handleDeliveryDateChange"
                  />
                </div>
                <div class="search-field-item">
                  <a-select
                    v-model:value="searchParams.statusList"
                    mode="multiple"
                    size="small"
                    style="width: 100%"
                    placeholder="配送状态"
                    allow-clear
                    :max-tag-count="1"
                    :options="STATUS_OPTIONS"
                  />
                </div>
                <div class="search-field-item">
                  <a-select
                    v-model:value="searchParams.riderId"
                    size="small"
                    style="width: 100%"
                    placeholder="配送司机"
                    show-search
                    allow-clear
                    :filter-option="filterOption"
                    :options="driverOptions"
                  />
                </div>
                <div class="search-field-item">
                  <a-select
                    v-model:value="searchParams.vehicleId"
                    size="small"
                    style="width: 100%"
                    placeholder="配送车辆"
                    show-search
                    allow-clear
                    :filter-option="filterOption"
                    :options="vehicleOptions"
                  />
                </div>
                <div class="search-field-item">
                  <a-select
                    v-model:value="searchParams.deliverymanId"
                    size="small"
                    style="width: 100%"
                    placeholder="送货员"
                    show-search
                    allow-clear
                    :filter-option="filterOption"
                    :options="deliverymanOptions"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.taskNo"
                    size="small"
                    placeholder="任务编号"
                    allow-clear
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.remark"
                    size="small"
                    placeholder="备注"
                    allow-clear
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.sourceBillNo"
                    size="small"
                    placeholder="配送单据编号"
                    allow-clear
                  />
                </div>
                <div class="search-field-item">
                  <a-select
                    v-model:value="searchParams.creatorName"
                    size="small"
                    style="width: 100%"
                    placeholder="制单人"
                    show-search
                    allow-clear
                    :filter-option="filterOption"
                    :options="creatorOptions"
                  />
                </div>
                <div class="search-field-item">
                  <a-range-picker
                    v-model:value="createTimeRange"
                    size="small"
                    style="width: 100%"
                    :placeholder="['制单起', '制单止']"
                    @change="handleCreateTimeChange"
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                  </div>
                  <div class="search-field-item">
                    <a-checkbox v-model:checked="searchParams.showRed">
                      显示红冲
                    </a-checkbox>
                  </div>
                  <!-- 对标：仅当选了「查询方案」才显现（默认隐藏），点击=覆盖当前方案并查询 -->
                  <a-button
                    v-if="queryScheme"
                    type="link"
                    size="small"
                    class="save-and-search"
                    @click="saveAndSearch"
                  >
                    ●保存并查询
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格：24 列（默认 20）+ 勾选列 + 操作列 ═══ -->
        <template #table>
          <BillTableList
            :columns="columns"
            :data-source="tableData"
            storage-key="dispatch-query-columns"
            global-config-key="dispatch-query-columns"
            :loading="loading"
            :pagination="false"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            row-key="id"
            @page-change="handlePageChange"
            @selection-change="handleSelectionChange"
          >
            <!-- 操作列（对标 ql361 PredeliveryTaskList：打印/补打 互斥 + 单据明细 + 取消） -->
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button
                  v-if="!Number(record.printCount)"
                  type="link"
                  size="small"
                  @click="openPrint(record)"
                >
                  打印
                </a-button>
                <a-button
                  v-else
                  type="link"
                  size="small"
                  @click="openPrint(record)"
                >
                  补打
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleBillDetail(record)"
                >
                  单据明细
                </a-button>
                <a-button
                  v-if="canCancel(record)"
                  type="link"
                  size="small"
                  danger
                  @click="handleCancel(record)"
                >
                  取消
                </a-button>
              </a-space>
            </template>
          </BillTableList>
        </template>

        <!-- ═══ 底部：合计行 + 经典分页栏（对标 ql361：首页/上页/第(x/y)页/下页/尾页/跳转/共N条记录/每页显示N行） ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
          <div class="table-footer">
            <div class="footer-label">
              合计（本页 {{ tableData.length }} 个配送任务）
            </div>
            <div class="footer-values">
              <span>配送单量: {{ summary.orderCount }}</span>
              <span>订金金额: {{ summary.depositAmount }}</span>
              <span>发货数量: {{ summary.shipQuantity }}</span>
              <span>发货金额: {{ summary.shipAmount }}</span>
              <span>退货单量: {{ summary.returnOrderCount }}</span>
              <span>退货数量: {{ summary.returnQuantity }}</span>
              <span>退货金额: {{ summary.returnAmount }}</span>
              <span>装箱数量: {{ summary.boxQuantity }}</span>
              <span>配送里程(km): {{ summary.distance }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 打印弹窗（打印模板渲染，批量打印按队列依次打印） ═══ -->
    <PrintDialog
      ref="printDialogRef"
      page-code="dispatch-query"
      :document-id="printData.id"
      :print-data="printData"
      @print-success="handlePrintSuccess"
    />

    <!-- ═══ 批量打印：打印模板选择（对标 ql361 BatchPsSelectPrintTemplate） ═══ -->
    <a-modal
      v-model:open="showBatchPrint"
      title="打印模板选择"
      :width="560"
      ok-text="打印"
      cancel-text="取消"
      @ok="confirmBatchPrint"
    >
      <div class="batch-print-body">
        <div class="bp-row">
          <span class="bp-label">打印内容</span>
          <a-checkbox-group v-model:value="printScope">
            <a-checkbox value="bill">
              配送单
            </a-checkbox>
            <a-checkbox value="detail">
              运单明细
            </a-checkbox>
            <a-checkbox value="summary">
              运单汇总
            </a-checkbox>
            <a-checkbox value="outbound">
              出库单据
            </a-checkbox>
          </a-checkbox-group>
        </div>
        <div class="bp-row">
          <a-checkbox v-model:checked="onlyUnprinted">
            仅打印未打印过的单据
          </a-checkbox>
        </div>
        <div class="bp-hint">
          已勾选 {{ selectedRowKeys.length }} 条配送任务，打印将按队列逐单渲染模板。
        </div>
      </div>
    </a-modal>

    <!-- ═══ 单据明细（任务内商品行） ═══ -->
    <a-drawer
      v-model:open="showBillDetail"
      title="单据明细"
      :width="760"
    >
      <a-spin :spinning="billDetailLoading">
        <div class="bill-detail-head">
          任务编号：{{ billDetail.taskNo || '-' }}
        </div>
        <a-table
          :columns="billDetailColumns"
          :data-source="billDetail.items"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="['quantity', 'unitPrice', 'amount'].includes(column.key as string)">
              {{ formatNumber(record[column.dataIndex as string]) }}
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-drawer>

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
/**
 * 配送查询（配发收 → 配送业务 → 配送查询，菜单 70150）
 *
 * 对标 ql361：全部配送任务台账（单视图、纯查询），24 列（默认 20）、无页面配置弹窗、
 * 13 项查询条件 + 9 段时间快捷段、工具栏 刷新/批量打印/打印(F8)/导出、底部合计行。
 * 配送状态对外统一映射为三值：待配送 / 配送中 / 已配送（执行态 9 态只在执行侧展示）。
 */
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import * as XLSX from 'xlsx'
import {
  ReloadOutlined,
  PlusOutlined,
  PrinterOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { taskApi, type DmsTask, type DmsTaskPageQuery } from '@/api/dms/task'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'

const route = useRoute()

/**
 * 下钻入口：《配送仪表盘》卡片 → ?status=PENDING|DELIVERING|DELIVERED（三值口径），
 * 可叠加 ?createTimeStart=YYYY-MM-DD&createTimeEnd=YYYY-MM-DD 覆盖默认「近一周」。
 */
function applyRouteQuery() {
  const status = route.query.status
  if (status !== undefined && status !== null && status !== '') {
    // 支持逗号分隔多值（如「超时未签收」下钻 PENDING,DELIVERING）
    searchParams.statusList = String(status).split(',').map(s => s.trim()).filter(Boolean)
  }
  const cs = route.query.createTimeStart
  const ce = route.query.createTimeEnd
  if (cs && ce) {
    searchParams.createTimeStart = String(cs)
    searchParams.createTimeEnd = String(ce)
    createTimeRange.value = [dayjs(String(cs)), dayjs(String(ce))]
    quickDate.value = ''
  }
}

// ═══════════════════════════════════════════════
// 状态口径：对外三值（对标），执行态 9 态仅兜底展示
// ═══════════════════════════════════════════════
const DELIVERY_STATUS_MAP: Record<number, { label: string; color: string; code: string }> = {
  0: { label: '待配送', color: 'orange', code: 'PENDING' },
  1: { label: '待配送', color: 'orange', code: 'PENDING' },
  2: { label: '待配送', color: 'cyan', code: 'PENDING' },
  3: { label: '配送中', color: 'processing', code: 'DELIVERING' },
  4: { label: '配送中', color: 'processing', code: 'DELIVERING' },
  5: { label: '已配送', color: 'geekblue', code: 'DELIVERED' },
  6: { label: '已配送', color: 'green', code: 'DELIVERED' },
  7: { label: '已取消', color: 'default', code: 'CANCELLED' },
  8: { label: '异常', color: 'magenta', code: 'EXCEPTION' }
}

/** 三值筛选 → 执行态集合（筛选在后端按 status 过滤） */
const STATUS_FILTER_GROUPS: Record<string, number[]> = {
  PENDING: [0, 1, 2],
  DELIVERING: [3, 4],
  DELIVERED: [5, 6]
}

const STATUS_OPTIONS = [
  { label: '待配送', value: 'PENDING' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '已配送', value: 'DELIVERED' }
]

function statusLabel(status?: number | null): string {
  if (status === null || status === undefined) return '-'
  return DELIVERY_STATUS_MAP[status]?.label || `状态${status}`
}

function statusColor(status?: number | null): string {
  if (status === null || status === undefined) return 'default'
  return DELIVERY_STATUS_MAP[status]?.color || 'default'
}

// ═══════════════════════════════════════════════
// 9 段快捷时间（对标 ql361：比销售域多「近两日 / 近一月 / 上周」）
// ═══════════════════════════════════════════════
const QUICK_DATES = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'last2Days', label: '近两日' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'lastMonth', label: '近一月' },
  { key: 'thisWeek', label: '本周' },
  { key: 'prevWeek', label: '上周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'prevMonth', label: '上月' }
]

const quickDate = ref('lastWeek')
const deliveryDateRange = ref<[Dayjs, Dayjs] | null>(null)
const createTimeRange = ref<[Dayjs, Dayjs] | null>(null)

// 查询区横向网格：查询/显示红冲按内容宽度自动占满本行剩余列
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

function setQuickDate(key: string, search = true) {
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
    case 'last2Days':
      start = today.subtract(1, 'day')
      end = today
      break
    case 'lastWeek':
      start = today.subtract(6, 'day')
      end = today
      break
    case 'lastMonth':
      start = today.subtract(1, 'month').add(1, 'day')
      end = today
      break
    case 'thisWeek':
      start = today.startOf('week')
      end = today
      break
    case 'prevWeek':
      start = today.subtract(1, 'week').startOf('week')
      end = today.subtract(1, 'week').endOf('week')
      break
    case 'thisMonth':
      start = today.startOf('month')
      end = today
      break
    case 'prevMonth':
      start = today.subtract(1, 'month').startOf('month')
      end = today.subtract(1, 'month').endOf('month')
      break
    default:
      return
  }
  deliveryDateRange.value = [start, end]
  searchParams.deliveryDateStart = start.format('YYYY-MM-DD')
  searchParams.deliveryDateEnd = end.format('YYYY-MM-DD')
  if (search) handleSearch()
}

function handleDeliveryDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates?.length === 2) {
    searchParams.deliveryDateStart = dates[0].format('YYYY-MM-DD')
    searchParams.deliveryDateEnd = dates[1].format('YYYY-MM-DD')
    quickDate.value = ''
  } else {
    searchParams.deliveryDateStart = ''
    searchParams.deliveryDateEnd = ''
  }
}

function handleCreateTimeChange(dates: [Dayjs, Dayjs] | null) {
  if (dates?.length === 2) {
    searchParams.createTimeStart = dates[0].format('YYYY-MM-DD')
    searchParams.createTimeEnd = dates[1].format('YYYY-MM-DD')
  } else {
    searchParams.createTimeStart = ''
    searchParams.createTimeEnd = ''
  }
}

// ═══════════════════════════════════════════════
// 查询条件（对标 13 项；无页面配置弹窗 → 固定项）
// ═══════════════════════════════════════════════
const DEFAULT_SEARCH_PARAMS = {
  /** 三值多选（PENDING/DELIVERING/DELIVERED），落库前展开为 statusList */
  statusList: [] as string[],
  riderId: undefined as number | undefined,
  vehicleId: undefined as number | undefined,
  deliverymanId: undefined as number | undefined,
  taskNo: '',
  remark: '',
  sourceBillNo: '',
  creatorName: undefined as string | undefined,
  deliveryDateStart: '',
  deliveryDateEnd: '',
  createTimeStart: '',
  createTimeEnd: '',
  showRed: false
}

const searchParams = reactive({ ...DEFAULT_SEARCH_PARAMS })

/** 三值多选 → 执行态集合 */
function buildStatusList(): number[] {
  const result: number[] = []
  for (const code of searchParams.statusList || []) {
    const group = STATUS_FILTER_GROUPS[code]
    if (group) result.push(...group)
  }
  return result
}

// ═══════════════════════════════════════════════
// 选择器候选（配送司机 / 配送车辆 / 送货员 / 制单人）
// ═══════════════════════════════════════════════
interface SelectOption {
  label: string
  value: any
}

const driverOptions = ref<SelectOption[]>([])
const vehicleOptions = ref<SelectOption[]>([])
const deliverymanOptions = ref<SelectOption[]>([])
const creatorOptions = ref<SelectOption[]>([])

function filterOption(input: string, option: any) {
  const text = String(option?.label ?? '').toLowerCase()
  return text.includes(String(input).toLowerCase())
}

async function loadFilterOptions() {
  try {
    const res: any = await taskApi.filterOptions()
    const data = res?.data || res || {}
    driverOptions.value = (data.drivers || []).map((r: any) => ({
      label: r.extra ? `${r.name}（${r.extra}）` : r.name,
      value: r.id
    }))
    deliverymanOptions.value = (data.deliverymen || []).map((r: any) => ({
      label: r.extra ? `${r.name}（${r.extra}）` : r.name,
      value: r.id
    }))
    vehicleOptions.value = (data.vehicles || []).map((v: any) => ({
      label: v.extra ? `${v.name}（${v.extra}）` : v.name,
      value: v.id
    }))
    creatorOptions.value = (data.creators || []).map((name: string) => ({
      label: name,
      value: name
    }))
  } catch (e) {
    // 下拉候选加载失败不阻塞台账查询
    console.warn('[配送查询] 下拉候选加载失败', e)
  }
}

// ═══════════════════════════════════════════════
// 列定义：24 列（默认 20，隐藏 4）
// ═══════════════════════════════════════════════
const DATE_ONLY_KEYS = new Set(['deliveryDate'])
const DATETIME_KEYS = new Set(['pickupTime', 'deliveryTime', 'createTime'])
const MONEY_KEYS = new Set([
  'depositAmount', 'goodsAmount', 'returnAmount'
])
const NUMBER_KEYS = new Set([
  'orderCount', 'returnOrderCount', 'totalQuantity', 'returnQuantity',
  'boxQuantity', 'estimatedDistance', 'totalVolume', 'totalWeight', 'printCount'
])

function formatMoney(value: any): string {
  if (value === null || value === undefined || value === '') return ''
  const num = Number(value)
  if (isNaN(num)) return ''
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatNumber(value: any): string {
  if (value === null || value === undefined || value === '') return ''
  const num = Number(value)
  if (isNaN(num)) return ''
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function buildFormatter(key: string) {
  return (value: any, record: any) => {
    if (key === 'status') return statusLabel(record?.status)
    if (value === null || value === undefined || value === '') return ''
    if (DATE_ONLY_KEYS.has(key)) return dayjs(value).format('YYYY-MM-DD')
    if (DATETIME_KEYS.has(key)) return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
    if (MONEY_KEYS.has(key)) return formatMoney(value)
    if (NUMBER_KEYS.has(key)) return formatNumber(value)
    return String(value)
  }
}

const columns = [
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' as const },
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', width: 220, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '指定配送日期', field: 'deliveryDate', key: 'deliveryDate', width: 110, sortable: true, formatter: buildFormatter('deliveryDate') },
  { title: '任务编号', field: 'taskNo', key: 'taskNo', width: 170, sortable: true, formatter: buildFormatter('taskNo') },
  { title: '配送状态', field: 'status', key: 'status', width: 90, sortable: true, formatter: buildFormatter('status') },
  { title: '配送开始时间', field: 'pickupTime', key: 'pickupTime', width: 150, formatter: buildFormatter('pickupTime') },
  { title: '配送结束时间', field: 'deliveryTime', key: 'deliveryTime', width: 150, formatter: buildFormatter('deliveryTime') },
  { title: '司机编号', field: 'riderId', key: 'riderId', width: 100, defaultHidden: true, formatter: buildFormatter('riderId') },
  { title: '司机名称', field: 'riderName', key: 'riderName', width: 100, formatter: buildFormatter('riderName') },
  { title: '配送车辆', field: 'vehicleName', key: 'vehicleName', width: 110, formatter: buildFormatter('vehicleName') },
  { title: '送货员', field: 'deliverymanName', key: 'deliverymanName', width: 100, formatter: buildFormatter('deliverymanName') },
  { title: '配送单量', field: 'orderCount', key: 'orderCount', width: 90, align: 'right' as const, formatter: buildFormatter('orderCount') },
  { title: '订金金额', field: 'depositAmount', key: 'depositAmount', width: 110, align: 'right' as const, formatter: buildFormatter('depositAmount') },
  { title: '退货单量', field: 'returnOrderCount', key: 'returnOrderCount', width: 90, align: 'right' as const, formatter: buildFormatter('returnOrderCount') },
  { title: '发货数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' as const, formatter: buildFormatter('totalQuantity') },
  { title: '发货金额', field: 'goodsAmount', key: 'goodsAmount', width: 110, align: 'right' as const, formatter: buildFormatter('goodsAmount') },
  { title: '退货数量', field: 'returnQuantity', key: 'returnQuantity', width: 100, align: 'right' as const, formatter: buildFormatter('returnQuantity') },
  { title: '退货金额', field: 'returnAmount', key: 'returnAmount', width: 110, align: 'right' as const, formatter: buildFormatter('returnAmount') },
  { title: '装箱数量', field: 'boxQuantity', key: 'boxQuantity', width: 100, align: 'right' as const, formatter: buildFormatter('boxQuantity') },
  { title: '配送里程(km)', field: 'estimatedDistance', key: 'estimatedDistance', width: 110, align: 'right' as const, formatter: buildFormatter('estimatedDistance') },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 100, align: 'right' as const, defaultHidden: true, formatter: buildFormatter('totalVolume') },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 100, align: 'right' as const, defaultHidden: true, formatter: buildFormatter('totalWeight') },
  { title: '备注', field: 'remark', key: 'remark', width: 140, formatter: buildFormatter('remark') },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' as const, defaultHidden: true, formatter: buildFormatter('printCount') },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 100, formatter: buildFormatter('creatorName') },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150, sortable: true, formatter: buildFormatter('createTime') }
]

// ═══════════════════════════════════════════════
// 数据
// ═══════════════════════════════════════════════
const loading = ref(false)
const tableData = ref<DmsTask[]>([])
const selectedRows = ref<DmsTask[]>([])
const selectedRowKeys = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

function buildQueryParams(current: number, size: number): DmsTaskPageQuery {
  const params: Record<string, any> = {
    current,
    size,
    taskNo: searchParams.taskNo || undefined,
    sourceBillNo: searchParams.sourceBillNo || undefined,
    remark: searchParams.remark || undefined,
    riderId: searchParams.riderId,
    vehicleId: searchParams.vehicleId,
    deliverymanId: searchParams.deliverymanId,
    creatorName: searchParams.creatorName,
    deliveryDateStart: searchParams.deliveryDateStart || undefined,
    deliveryDateEnd: searchParams.deliveryDateEnd || undefined,
    createTimeStart: searchParams.createTimeStart || undefined,
    createTimeEnd: searchParams.createTimeEnd || undefined,
    showRed: searchParams.showRed || undefined
  }
  const statusList = buildStatusList()
  if (statusList.length) params.statusList = statusList
  return params as DmsTaskPageQuery
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await taskApi.page(buildQueryParams(pagination.current, pagination.pageSize))
    const data = res?.data || res || {}
    tableData.value = data.records || []
    pagination.total = Number(data.total) || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    message.error('查询失败，请检查网络后重试')
    console.warn('[配送查询] 查询失败', e)
  } finally {
    loading.value = false
  }
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

function handleSelectionChange(rows: DmsTask[], ids: any[]) {
  selectedRows.value = rows
  selectedRowKeys.value = ids
}

// ═══════════════════════════════════════════════
// 本页合计（口径：当前页）
// ═══════════════════════════════════════════════
const summary = computed(() => {
  const sum = (key: keyof DmsTask, money = false) => {
    const total = tableData.value.reduce((acc, row) => acc + (Number(row[key]) || 0), 0)
    return money ? formatMoney(total) : formatNumber(total)
  }
  return {
    orderCount: formatNumber(tableData.value.reduce((a, r) => a + (Number(r.orderCount) || 0), 0)),
    depositAmount: sum('depositAmount', true),
    shipQuantity: sum('totalQuantity'),
    shipAmount: sum('goodsAmount', true),
    returnOrderCount: sum('returnOrderCount'),
    returnQuantity: sum('returnQuantity'),
    returnAmount: sum('returnAmount', true),
    boxQuantity: sum('boxQuantity'),
    distance: sum('estimatedDistance')
  }
})

// ═══════════════════════════════════════════════
// 导出（真实 xlsx；按当前查询条件全量拉取）
// ═══════════════════════════════════════════════
const EXPORT_PAGE_SIZE = 5000

async function handleExport() {
  loading.value = true
  try {
    const res: any = await taskApi.page(buildQueryParams(1, EXPORT_PAGE_SIZE))
    const data = res?.data || res || {}
    const list: DmsTask[] = data.records || []
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    if (list.length >= EXPORT_PAGE_SIZE) {
      message.warning(`导出达到上限 ${EXPORT_PAGE_SIZE} 条，请缩小查询范围`)
    }
    const cols = columns.filter(c => c.type !== 'checkbox' && c.type !== 'rowNo' && c.type !== 'action')
    const rows = list.map(row => {
      const item: Record<string, any> = {}
      cols.forEach((col: any) => {
        item[col.title] = col.formatter ? col.formatter(row[col.field as keyof DmsTask], row) : (row[col.field as keyof DmsTask] ?? '')
      })
      return item
    })
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '配送查询')
    XLSX.writeFile(wb, `配送查询_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success(`已导出 ${list.length} 条`)
  } catch (e) {
    message.error('导出失败')
    console.warn('[配送查询] 导出失败', e)
  } finally {
    loading.value = false
  }
}

// ═══════════════════════════════════════════════
// 打印：打印(F8) 单张 / 批量打印 队列
// ═══════════════════════════════════════════════
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
const printQueue = ref<DmsTask[]>([])

/** 批量打印「打印模板选择」 */
const showBatchPrint = ref(false)
const printScope = ref<string[]>(['bill'])
const onlyUnprinted = ref(false)

/** 单据明细抽屉 */
const showBillDetail = ref(false)
const billDetailLoading = ref(false)
const billDetail = reactive<{ taskNo: string; items: any[] }>({ taskNo: '', items: [] })
const billDetailColumns = [
  { title: '行号', dataIndex: 'lineNo', key: 'lineNo', width: 60 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 160 },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 110 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90, align: 'right' as const },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 90, align: 'right' as const },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100, align: 'right' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120 }
]

async function openPrint(record: DmsTask) {
  printData.value = { ...record, deliveryStatusText: statusLabel(record.status) }
  await nextTick()
  printDialogRef.value?.open()
}

/** 打印(F8)：打印勾选的第一条 */
async function handlePrint() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要打印的配送任务')
    return
  }
  printQueue.value = []
  await openPrint(selectedRows.value[0])
}

/** 批量打印：打开「打印模板选择」弹窗（对标 ql361：未勾选提示「请选中至少一条数据！」） */
function openBatchPrintModal() {
  if (!selectedRowKeys.value.length) {
    message.warning('请选中至少一条数据！')
    return
  }
  showBatchPrint.value = true
}

/** 确认批量打印：按「仅打印未打印过的单据」过滤后入队逐单打印 */
async function confirmBatchPrint() {
  showBatchPrint.value = false
  const list = onlyUnprinted.value
    ? selectedRows.value.filter(r => !Number(r.printCount))
    : selectedRows.value
  if (!list.length) {
    message.warning('没有符合打印条件的配送任务（均已打印过）')
    return
  }
  const [first, ...rest] = list
  printQueue.value = rest
  await openPrint(first)
}

/** 单据明细（任务内商品行） */
async function handleBillDetail(record: DmsTask) {
  showBillDetail.value = true
  billDetailLoading.value = true
  billDetail.taskNo = record.taskNo
  billDetail.items = []
  try {
    const res: any = await taskApi.detail(record.id as number)
    const d = res?.data || res || {}
    billDetail.items = d.items || []
  } catch (e) {
    message.error('单据明细加载失败')
    console.warn('[配送查询] 单据明细加载失败', e)
  } finally {
    billDetailLoading.value = false
  }
}

/** 取消按钮显隐：仅「待分配 / 已分配」可取消（对标 columnVisible 条件 + 状态机约束） */
function canCancel(record: DmsTask): boolean {
  return record.status === 0 || record.status === 1
}

function handleCancel(record: DmsTask) {
  Modal.confirm({
    title: '取消配送任务',
    content: `确认取消任务「${record.taskNo}」？取消后需勾选「显示红冲」才能查到。`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await taskApi.cancel(record.id as number)
        message.success('任务已取消')
        fetchData()
      } catch (e) {
        message.error('取消失败')
        console.warn('[配送查询] 取消任务失败', e)
      }
    }
  })
}

/** 打印成功：回写打印次数（真实落库）并驱动队列 */
async function handlePrintSuccess() {
  const id = printData.value?.id
  if (id) {
    try {
      await taskApi.print(id)
    } catch (e) {
      console.warn('[配送查询] 打印次数回写失败', e)
    }
  }
  const next = printQueue.value.shift()
  if (next) {
    await openPrint(next)
  } else {
    fetchData()
  }
}

// ═══════════════════════════════════════════════
// F8 快捷键
// ═══════════════════════════════════════════════
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══════════════════════════════════════════════
// 查询方案（保存并查询）
// ═══════════════════════════════════════════════
const SCHEME_STORAGE_KEY = 'dispatch-query-schemes'

interface QueryScheme {
  name: string
  params: Record<string, any>
  deliveryDate: [string, string] | null
  createTime: [string, string] | null
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

function currentSnapshot(name: string): QueryScheme {
  return {
    name,
    params: JSON.parse(JSON.stringify(searchParams)),
    deliveryDate: deliveryDateRange.value
      ? [deliveryDateRange.value[0].format('YYYY-MM-DD'), deliveryDateRange.value[1].format('YYYY-MM-DD')]
      : null,
    createTime: createTimeRange.value
      ? [createTimeRange.value[0].format('YYYY-MM-DD'), createTimeRange.value[1].format('YYYY-MM-DD')]
      : null
  }
}

function saveScheme() {
  const name = newSchemeName.value.trim()
  if (!name) {
    message.warning('请输入方案名称')
    return
  }
  const snapshot = currentSnapshot(name)
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

/** ●保存并查询：保存当前条件为方案后立即查询 */
function saveAndSearch() {
  if (!queryScheme.value) return
  const snapshot = currentSnapshot(queryScheme.value)
  const idx = querySchemes.value.findIndex(s => s.name === queryScheme.value)
  if (idx >= 0) {
    querySchemes.value[idx] = snapshot
  } else {
    querySchemes.value.push(snapshot)
  }
  persistSchemes()
  message.success(`查询方案「${queryScheme.value}」已更新`)
  handleSearch()
}

function applyScheme(name: string) {
  if (!name) return
  const scheme = querySchemes.value.find(s => s.name === name)
  if (!scheme) return
  Object.assign(searchParams, DEFAULT_SEARCH_PARAMS, scheme.params)
  deliveryDateRange.value = scheme.deliveryDate
    ? [dayjs(scheme.deliveryDate[0]), dayjs(scheme.deliveryDate[1])]
    : null
  createTimeRange.value = scheme.createTime
    ? [dayjs(scheme.createTime[0]), dayjs(scheme.createTime[1])]
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

// ═══════════════════════════════════════════════
// 错误处理 / 生命周期
// ═══════════════════════════════════════════════
function handleError(e: Error) {
  console.error('[配送查询] 页面错误', e)
}

onMounted(() => {
  loadSchemes()
  loadFilterOptions()
  // 默认「近一周」（对标默认快捷段）
  setQuickDate('lastWeek', false)
  // 下钻带入的状态/日期条件覆盖默认范围
  applyRouteQuery()
  fetchData()
  window.addEventListener('keydown', onKeydown)
})

// 同一路由重复下钻（如仪表盘 在途任务 → 已完成）时组件被复用，需响应 query 变化
watch(() => route.query, () => {
  applyRouteQuery()
  handleSearch()
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷时间（9 段） ═══ */
.quick-dates :deep(.ant-btn-link) {
  color: #555;
  padding: 0 6px;
  height: 24px;
  line-height: 24px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #fa8c16;
  border-color: #fa8c16;
}

/* ═══ 查询区：横向自适应网格（与系统其它单据页统一，不再纵向单列） ═══ */
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  align-items: center;
}
.search-field-item {
  display: flex;
  min-width: 0;
}
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
  font-size: 13px;
}
.search-field-item :deep(.ant-select),
.search-field-item :deep(.ant-picker) {
  width: 100%;
  font-size: 13px;
}
.search-field-item :deep(.ant-select .ant-select-selector) {
  font-size: 13px;
}
.search-action-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.search-action-group .search-field-item {
  width: auto;
  flex: 0 0 auto;
  margin-right: 8px;
}
.search-action-group .search-field-item:last-child {
  margin-right: 0;
}
.save-and-search {
  color: #1677ff;
  padding: 0;
}

/* ═══ 合计行 ═══ */
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
  min-width: 190px;
}
.footer-values {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

/* ═══ 批量打印弹窗（打印模板选择） ═══ */
.batch-print-body {
  padding: 4px 0;
}
.bp-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.bp-label {
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
}
.bp-hint {
  font-size: 12px;
  color: #999;
}

/* ═══ 单据明细抽屉 ═══ */
.bill-detail-head {
  margin-bottom: 10px;
  font-size: 13px;
  color: #333;
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
