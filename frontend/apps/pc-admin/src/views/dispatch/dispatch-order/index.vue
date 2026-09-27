<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab 栏 + 工具栏 ═══ -->
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 9 段快捷时间 ═══ -->
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
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 新增 / 刷新 / 打印(F8) / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置" placement="bottom">
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('add')"
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
              v-if="btnEnabled('batchPrint')"
              size="small"
              @click="handleBatchPrint"
            >
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-button
              v-if="btnEnabled('print')"
              size="small"
              @click="handlePrintF8"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
            <a-button
              v-if="btnEnabled('delete')"
              size="small"
              danger
              :disabled="selectedRowKeys.length === 0"
              @click="handleBatchDelete"
            >
              <DeleteOutlined /> 删除
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（页面配置可显隐） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div
                  v-if="fieldVisible('deliveryDate')"
                  class="search-field-item"
                >
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleDateChange"
                  />
                </div>
                <div
                  v-if="fieldVisible('taskNo')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.taskNo"
                    placeholder="任务编号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  v-if="fieldVisible('sourceBillNo')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.sourceBillNo"
                    placeholder="来源单据编号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  v-if="fieldVisible('orderNo')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.orderNo"
                    placeholder="订单号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  v-if="fieldVisible('customerName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.customerName"
                    placeholder="客户"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  v-if="fieldVisible('receiverKeyword')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.receiverKeyword"
                    placeholder="收货人/联系电话"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  v-if="fieldVisible('status')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="statusFilter"
                    mode="multiple"
                    size="small"
                    placeholder="配送状态"
                    allow-clear
                    :max-tag-count="1"
                    style="width: 100%"
                  >
                    <a-select-option
                      v-for="s in STATUS_FILTER_OPTIONS"
                      :key="s.value"
                      :value="s.value"
                    >
                      {{ s.label }}
                    </a-select-option>
                  </a-select>
                </div>
                <div
                  v-if="fieldVisible('riderId')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.riderId"
                    size="small"
                    placeholder="配送司机"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="riderOptions"
                    :loading="filterOptionsLoading"
                    style="width: 100%"
                  />
                </div>
                <div
                  v-if="fieldVisible('vehicleId')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.vehicleId"
                    size="small"
                    placeholder="配送车辆"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="vehicleOptions"
                    :loading="filterOptionsLoading"
                    style="width: 100%"
                  />
                </div>
                <div
                  v-if="fieldVisible('deliverymanId')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.deliverymanId"
                    size="small"
                    placeholder="送货员"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="deliverymanOptions"
                    :loading="filterOptionsLoading"
                    style="width: 100%"
                  />
                </div>
                <div
                  v-if="fieldVisible('routeId')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.routeId"
                    size="small"
                    placeholder="配送线路"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="routeOptions"
                    :loading="routeLoading"
                    style="width: 100%"
                  />
                </div>
                <div
                  v-if="fieldVisible('routeArea')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.routeArea"
                    placeholder="配送区域"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  v-if="fieldVisible('creatorName')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.creatorName"
                    size="small"
                    placeholder="制单人"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="creatorOptions"
                    :loading="filterOptionsLoading"
                    style="width: 100%"
                  />
                </div>
                <div
                  v-if="fieldVisible('createTime')"
                  class="search-field-item"
                >
                  <a-range-picker
                    v-model:value="createTimeRange"
                    size="small"
                    style="width: 100%"
                    @change="handleCreateTimeChange"
                  />
                </div>
                <div
                  v-if="fieldVisible('remark')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.remark"
                    placeholder="备注"
                    allow-clear
                    size="small"
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
                  <div
                    v-if="fieldVisible('showRed')"
                    class="search-field-item"
                  >
                    <a-checkbox v-model:checked="searchParams.showRed">
                      显示红冲
                    </a-checkbox>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（序号列表头齿轮 = 列配置唯一入口） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              ref="tableRef"
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'dispatch-order-columns'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :summary-data="tableFooterColumns"
              :min-empty-rows="0"
              row-key="id"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <template #taskNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  {{ record.taskNo }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <template #deliveryDateCell="{ record }">
                <span>{{ record.deliveryDate || '-' }}</span>
              </template>
              <template #moneyCell="{ record, column }">
                <span class="currency-value">{{ formatMoney(record[columnKey(column)]) }}</span>
              </template>
              <template #qtyCell="{ record, column }">
                <span class="currency-value">{{ formatQty(record[columnKey(column)]) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="[0, 1].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleAudit(record)"
                  >
                    审核
                  </a-button>
                  <a-button
                    v-if="record.status === 1 && !record.riderId"
                    type="link"
                    size="small"
                    @click="handleUnaudit(record)"
                  >
                    反审核
                  </a-button>
                  <a-button
                    v-if="[0, 1].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleCancel(record)"
                  >
                    取消
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  ExportOutlined, DeleteOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { taskApi } from '@/api/dms/task'
import { mdRouteApi } from '@/api/md'
import { printDocuments } from '@/utils/printDocuments'

defineOptions({ name: 'DispatchOrderList' })

const router = useRouter()

// ═══ Tab（单视图：配送单-历史） ═══
const tabs = [{ key: 'doc', label: '配送单-历史' }]
const activeTab = ref('doc')

// ═══ 快捷时间（9 段，对标《配送单开发文档》§2.1） ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'last2', label: '近两日' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'lastMonth', label: '近一月' },
  { key: 'week', label: '本周' },
  { key: 'prevWeek', label: '上周' },
  { key: 'month', label: '本月' },
  { key: 'prevMonth', label: '上月' },
]
const quickDate = ref('lastWeek')
const queryScheme = ref('')

// ═══ 状态映射（执行态 9 → 对外三值口径，对标《配送查询》§4.2） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待配送', color: 'default' },
  1: { text: '待配送', color: 'default' },
  2: { text: '待配送', color: 'blue' },
  3: { text: '配送中', color: 'processing' },
  4: { text: '配送中', color: 'processing' },
  5: { text: '已配送', color: 'success' },
  6: { text: '已配送', color: 'success' },
  7: { text: '已取消', color: 'default' },
  8: { text: '异常', color: 'error' },
}
const STATUS_FILTER_OPTIONS = [
  { label: '待配送', value: 'pending', statuses: [0, 1, 2] },
  { label: '配送中', value: 'delivering', statuses: [3, 4] },
  { label: '已配送', value: 'delivered', statuses: [5, 6] },
]
const statusFilter = ref<string[]>([])

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text ?? '-'
}
function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color ?? 'default'
}

// ═══ 数据与分页 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 搜索网格自适应 ═══
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])
const createTimeRange = ref<[Dayjs, Dayjs] | null>(null)

// ═══ 查询参数（对齐后端 DmsTaskQuery） ═══
const searchParams = reactive({
  taskNo: '',
  sourceBillNo: '',
  orderNo: '',
  customerName: '',
  receiverKeyword: '',
  riderId: undefined as number | undefined,
  vehicleId: undefined as number | undefined,
  deliverymanId: undefined as number | undefined,
  routeId: undefined as number | undefined,
  routeArea: '',
  creatorName: undefined as string | undefined,
  remark: '',
  showRed: false,
  deliveryDateStart: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  deliveryDateEnd: dayjs().format('YYYY-MM-DD'),
  createTimeStart: '',
  createTimeEnd: '',
})

// ═══ 选择器候选数据（复用 DMS 配送查询下拉接口 + 线路主数据） ═══
const filterOptionsLoading = ref(false)
const routeLoading = ref(false)
const riderOptions = ref<{ label: string; value: number }[]>([])
const vehicleOptions = ref<{ label: string; value: number }[]>([])
const deliverymanOptions = ref<{ label: string; value: number }[]>([])
const creatorOptions = ref<{ label: string; value: string }[]>([])
const routeOptions = ref<{ label: string; value: number }[]>([])

async function loadFilterOptions() {
  filterOptionsLoading.value = true
  try {
    const res: any = await taskApi.filterOptions()
    const data = res?.data ?? res ?? {}
    riderOptions.value = (data.drivers || []).map((r: any) => ({
      label: r.extra ? `${r.name}(${r.extra})` : r.name,
      value: r.id,
    }))
    deliverymanOptions.value = (data.deliverymen || []).map((r: any) => ({
      label: r.extra ? `${r.name}(${r.extra})` : r.name,
      value: r.id,
    }))
    vehicleOptions.value = (data.vehicles || []).map((v: any) => ({
      label: v.extra ? `${v.name}(${v.extra})` : v.name,
      value: v.id,
    }))
    creatorOptions.value = (data.creators || []).map((name: string) => ({ label: name, value: name }))
  } catch (e) {
    console.warn('[配送单] 查询条件下拉加载失败', e)
  } finally {
    filterOptionsLoading.value = false
  }
}

async function loadRouteOptions() {
  routeLoading.value = true
  try {
    const res: any = await mdRouteApi.options()
    const list = res?.data ?? res ?? []
    routeOptions.value = (list || []).map((r: any) => ({
      label: r.routeName || r.name,
      value: r.id,
    }))
  } catch (e) {
    console.warn('[配送单] 线路下拉加载失败', e)
  } finally {
    routeLoading.value = false
  }
}

// ═══ 列定义（24 列，默认隐藏 4 列） ═══
const allColumns = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'deliveryDate', title: '指定配送日期', width: 110, type: 'slot', slotName: 'deliveryDateCell', sortable: true },
  { key: 'taskNo', title: '任务编号', width: 170, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { key: 'status', title: '配送状态', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { key: 'pickupTime', title: '配送开始时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'deliveryTime', title: '配送结束时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'riderId', title: '司机编号', width: 100, defaultHidden: true },
  { key: 'riderName', title: '司机名称', width: 110, sortable: true },
  { key: 'vehicleName', title: '配送车辆', width: 120 },
  { key: 'deliverymanName', title: '送货员', width: 110 },
  { key: 'orderCount', title: '配送单量', width: 90, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'depositAmount', title: '订金金额', width: 110, align: 'right', type: 'slot', slotName: 'moneyCell' },
  { key: 'returnOrderCount', title: '退货单量', width: 90, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'totalQuantity', title: '发货数量', width: 100, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'goodsAmount', title: '发货金额', width: 110, align: 'right', type: 'slot', slotName: 'moneyCell' },
  { key: 'returnQuantity', title: '退货数量', width: 100, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'returnAmount', title: '退货金额', width: 110, align: 'right', type: 'slot', slotName: 'moneyCell' },
  { key: 'boxQuantity', title: '装箱数量', width: 100, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'estimatedDistance', title: '配送里程(km)', width: 110, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'totalVolume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true, type: 'slot', slotName: 'qtyCell' },
  { key: 'totalWeight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true, type: 'slot', slotName: 'qtyCell' },
  { key: 'remark', title: '备注', width: 160 },
  { key: 'printCount', title: '打印次数', width: 90, align: 'right', defaultHidden: true },
  { key: 'creatorName', title: '制单人', width: 100, sortable: true },
  { key: 'createTime', title: '制单时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'action', title: '操作', width: 230, fixed: 'right', type: 'action', slotName: 'actionCell' },
]

// ═══ 表尾合计 ═══
const tableFooterColumns = computed(() => {
  const sum = (key: string) => tableData.value.reduce((s: number, r: any) => s + (Number(r[key]) || 0), 0)
  return [
    { key: 'totalQuantity', value: round4(sum('totalQuantity')), highlight: true },
    { key: 'goodsAmount', value: round4(sum('goodsAmount')), highlight: true },
    { key: 'returnQuantity', value: round4(sum('returnQuantity')), highlight: true },
    { key: 'returnAmount', value: round4(sum('returnAmount')), highlight: true },
  ]
})

// ═══ 页面配置（查询条件 + 功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'dispatch-order-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'deliveryDate', label: '配送日期', visible: true },
  { key: 'taskNo', label: '任务编号', visible: true },
  { key: 'sourceBillNo', label: '来源单据编号', visible: true },
  { key: 'orderNo', label: '订单号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'receiverKeyword', label: '收货人/联系电话', visible: true },
  { key: 'status', label: '配送状态', visible: true },
  { key: 'riderId', label: '配送司机', visible: true },
  { key: 'vehicleId', label: '配送车辆', visible: true },
  { key: 'deliverymanId', label: '送货员', visible: true },
  { key: 'routeId', label: '配送线路', visible: true },
  { key: 'routeArea', label: '配送区域', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'createTime', label: '制单时间', visible: true },
  { key: 'remark', label: '备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'delete', label: '删除', enabled: true },
]

const showPageConfig = ref(false)
const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw) as PageConfigData
    if (parsed.queryFields) {
      queryConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
        const saved = parsed.queryFields!.find(f => f.key === df.key)
        return saved ? { ...df, visible: saved.visible !== false } : { ...df }
      })
    }
    if (parsed.functionButtons) {
      functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons!.find(f => f.key === bf.key)
        return saved ? { ...bf, enabled: saved.enabled !== false } : { ...bf }
      })
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

function fieldVisible(key: string): boolean {
  return queryConfig.value.find(f => f.key === key)?.visible !== false
}
function btnEnabled(key: string): boolean {
  return functionButtonConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 选择 ═══
const tableRef = ref<any>(null)
const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<any[]>([])

function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRows.value = rows
  selectedRowKeys.value = ids
}

// ═══ 数据加载 ═══
function buildParams(): any {
  const params: any = {
    current: pagination.current,
    size: pagination.pageSize,
  }
  const mappings: [string, any][] = [
    ['taskNo', searchParams.taskNo],
    ['sourceBillNo', searchParams.sourceBillNo],
    ['orderNo', searchParams.orderNo],
    ['customerName', searchParams.customerName],
    ['receiverKeyword', searchParams.receiverKeyword],
    ['routeArea', searchParams.routeArea],
    ['remark', searchParams.remark],
  ]
  for (const [k, v] of mappings) {
    if (typeof v === 'string' && v.trim()) params[k] = v.trim()
  }
  if (searchParams.riderId) params.riderId = searchParams.riderId
  if (searchParams.vehicleId) params.vehicleId = searchParams.vehicleId
  if (searchParams.deliverymanId) params.deliverymanId = searchParams.deliverymanId
  if (searchParams.routeId) params.routeId = searchParams.routeId
  if (searchParams.creatorName) params.creatorName = searchParams.creatorName
  if (searchParams.showRed) params.showRed = true
  if (searchParams.deliveryDateStart) params.deliveryDateStart = searchParams.deliveryDateStart
  if (searchParams.deliveryDateEnd) params.deliveryDateEnd = searchParams.deliveryDateEnd
  if (searchParams.createTimeStart) params.createTimeStart = searchParams.createTimeStart
  if (searchParams.createTimeEnd) params.createTimeEnd = searchParams.createTimeEnd
  const statusList = STATUS_FILTER_OPTIONS
    .filter(o => statusFilter.value.includes(o.value))
    .flatMap(o => o.statuses)
  if (statusList.length) params.statusList = statusList.join(',')
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await taskApi.page(buildParams())
    const body = res?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
    tableRef.value?.clearSelection?.()
    selectedRowKeys.value = []
    selectedRows.value = []
  } catch (error: any) {
    console.warn('[配送单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs
  let end: Dayjs = now
  switch (key) {
    case 'yesterday':
      start = now.subtract(1, 'day'); end = start; break
    case 'today':
      start = now; break
    case 'last2':
      start = now.subtract(1, 'day'); break
    case 'lastWeek':
      start = now.subtract(7, 'day'); break
    case 'lastMonth':
      start = now.subtract(1, 'month'); break
    case 'week':
      start = now.startOf('week'); break
    case 'prevWeek':
      start = now.startOf('week').subtract(7, 'day'); end = now.startOf('week').subtract(1, 'day'); break
    case 'month':
      start = now.startOf('month'); break
    case 'prevMonth':
      start = now.startOf('month').subtract(1, 'month'); end = now.startOf('month').subtract(1, 'day'); break
    default:
      start = now.subtract(7, 'day')
  }
  dateRange.value = [start, end]
  searchParams.deliveryDateStart = start.format('YYYY-MM-DD')
  searchParams.deliveryDateEnd = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: any) {
  if (dates && dates.length === 2) {
    searchParams.deliveryDateStart = dates[0].format('YYYY-MM-DD')
    searchParams.deliveryDateEnd = dates[1].format('YYYY-MM-DD')
  } else {
    searchParams.deliveryDateStart = ''
    searchParams.deliveryDateEnd = ''
  }
}

function handleCreateTimeChange(dates: any) {
  if (dates && dates.length === 2) {
    searchParams.createTimeStart = dates[0].format('YYYY-MM-DD')
    searchParams.createTimeEnd = dates[1].format('YYYY-MM-DD')
  } else {
    searchParams.createTimeStart = ''
    searchParams.createTimeEnd = ''
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

// ═══ 行操作 ═══
function handleAdd() {
  router.push('/dispatch/dispatch-order/form')
}
function handleView(record: any) {
  router.push(`/dispatch/dispatch-order/form?id=${record.id}&mode=view`)
}
function handleEdit(record: any) {
  router.push(`/dispatch/dispatch-order/form?id=${record.id}`)
}

function handleAudit(record: any) {
  Modal.confirm({
    title: '审核配送单',
    content: `确定审核配送单 ${record.taskNo}？审核后进入「待配送」。`,
    okText: '确认审核',
    cancelText: '取消',
    onOk: async () => {
      try {
        await taskApi.audit(record.id)
        message.success('审核完成')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '审核失败')
      }
    },
  })
}

function handleUnaudit(record: any) {
  Modal.confirm({
    title: '反审核配送单',
    content: `确定反审核配送单 ${record.taskNo}？`,
    okText: '确认反审核',
    cancelText: '取消',
    onOk: async () => {
      try {
        await taskApi.unaudit(record.id)
        message.success('反审核完成')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '反审核失败')
      }
    },
  })
}

function handleCancel(record: any) {
  Modal.confirm({
    title: '取消配送单',
    content: `确定取消配送单 ${record.taskNo}？取消后需勾选「显示红冲」才可见。`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await taskApi.cancel(record.id)
        message.success('已取消')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '取消失败')
      }
    },
  })
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '删除配送单',
    content: `确定删除配送单 ${record.taskNo}？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await taskApi.remove(record.id)
        message.success('删除成功')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    },
  })
}

function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的配送单')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 张配送单？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const ids = [...selectedRowKeys.value]
      let ok = 0
      let fail = 0
      for (const id of ids) {
        try {
          await taskApi.remove(id)
          ok++
        } catch {
          fail++
        }
      }
      if (fail === 0) {
        message.success(`已删除 ${ok} 张配送单`)
      } else {
        message.warning(`删除成功 ${ok} 张，失败 ${fail} 张（已进入配送执行的不可删除）`)
      }
      fetchData()
    },
  })
}

// ═══ 打印 ═══
function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的配送单')
    return
  }
  router.push(`/dispatch/dispatch-order/form?id=${selectedRowKeys.value[0]}&print=1`)
}

function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的配送单')
    return
  }
  Modal.confirm({
    title: '批量打印',
    content: `将打印选中的 ${selectedRowKeys.value.length} 张配送单（后一张在前一张之后分页输出）。`,
    okText: '开始打印',
    cancelText: '取消',
    onOk: async () => {
      // 逐张走后端渲染（挑模板 / 取数 / 渲染都在服务端），再合并成一次打印。
      // 语义保持原样：只打勾选行，不勾选时不打。
      const ids = [...selectedRowKeys.value]
      const done = await printDocuments('dispatch-task', ids, '配送任务单')
      if (!done) return
      // 次数回写放在真的出纸之后 —— 原来先 +1 再打印，渲染失败也会把次数记上
      try {
        await Promise.all(ids.map(id => taskApi.print(id).catch(() => null)))
      } catch { /* 打印次数失败不阻断打印 */ }
      fetchData()
    },
  })
}

// ═══ 导出（真实 xlsx） ═══
async function handleExport() {
  try {
    const params = { ...buildParams(), current: 1, size: 10000 }
    const blob: any = await taskApi.exportExcel(params)
    if (!blob || blob.size === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `配送单_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '导出失败')
  }
}

// ═══ 工具 ═══
function columnKey(column: any): string {
  return column?.key ?? column?.dataIndex ?? ''
}
function formatMoney(val: any): string {
  if (val === undefined || val === null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatQty(val: any): string {
  if (val === undefined || val === null || val === '') return '0'
  return String(Number(val))
}
function round4(val: number): number {
  return Math.round(val * 10000) / 10000
}
function formatDateTime(val: any): string {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

function handleError(error: Error) {
  console.error('[配送单] 页面错误', error)
}

onMounted(() => {
  loadPageConfig()
  loadFilterOptions()
  loadRouteOptions()
  fetchData()
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 120px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 8px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
