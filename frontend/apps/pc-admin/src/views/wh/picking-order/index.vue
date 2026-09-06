<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：快捷日期 ═══ -->
        <template #toolbar-left>
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

        <!-- ═══ 工具栏右侧：列配置/页面配置/新增/刷新 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置">
              <a-button size="small" @click="showColumnConfig = true">
                <TableOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增拣货单
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（按 Tab 两套） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <template v-if="activeTab === 'doc'">
              <div class="search-container">
                <div class="search-grid" ref="docGridRef">
                  <div class="search-field-item">
                    <a-range-picker
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.taskNo" placeholder="单号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.keyword" placeholder="单号/来源单号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-select v-model:value="searchParams.sourceType" size="small" allow-clear placeholder="来源类型">
                      <a-select-option v-for="o in PICK_SOURCE_TYPE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
                    </a-select>
                  </div>
                  <div class="search-field-item">
                    <a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="状态">
                      <a-select-option v-for="(m, k) in PICK_STATUS_MAP" :key="k" :value="Number(k)">{{ m.text }}</a-select-option>
                    </a-select>
                  </div>
                  <div class="search-field-item">
                    <a-select v-model:value="searchParams.priority" size="small" allow-clear placeholder="优先级">
                      <a-select-option :value="0">普通</a-select-option>
                      <a-select-option :value="1">紧急</a-select-option>
                      <a-select-option :value="2">加急</a-select-option>
                    </a-select>
                  </div>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="search-container">
                <div class="search-grid" ref="detailGridRef">
                  <div class="search-field-item">
                    <a-range-picker
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.taskNo" placeholder="单号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.locationCode" placeholder="货位" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="状态">
                      <a-select-option v-for="(m, k) in PICK_STATUS_MAP" :key="k" :value="Number(k)">{{ m.text }}</a-select-option>
                    </a-select>
                  </div>
                  <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + detailActionSpan }">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #taskNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.taskNo }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template #sourceTypeCell="{ record }">
                {{ getSourceTypeText(record.sourceType) }}
              </template>
              <template #totalQtyCell="{ record }">
                {{ formatQty(record.totalQuantity) }}
              </template>
              <template #pickedQtyCell="{ record }">
                {{ formatQty(record.pickedQuantity) }}
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleStart(record)">开始拣货</a-button>
                  <a-button v-if="record.status === 1" type="link" size="small" @click="handleComplete(record)">完成拣货</a-button>
                  <a-popconfirm v-if="record.status === 0 || record.status === 1" title="确认取消该拣货单？" @confirm="handleCancel(record)">
                    <a-button type="link" size="small" danger>取消</a-button>
                  </a-popconfirm>
                  <a-popconfirm v-if="record.status === 0" title="确认删除该单据？" @confirm="handleDelete(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
              <template #detailStatusCell="{ record }">
                <a-tag :color="getDetailStatusColor(record.status)">{{ getDetailStatusText(record.status) }}</a-tag>
              </template>
              <template #createTimeCell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

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

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="activePageConfigStorageKey"
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
  PlusOutlined, ReloadOutlined, SettingOutlined, TableOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { pickApi } from '@/api/wms/pick'
import { useUserStore } from '@/stores/user'
import { PICK_STATUS_MAP, PICK_SOURCE_TYPE_OPTIONS, pickSourceTypeText, formatQty, formatTime } from '../whTask'

defineOptions({ name: 'WhPickingOrderList' })

const router = useRouter()
const userStore = useUserStore()

// ═══ Tab ═══
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

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  taskNo: '',
  keyword: '',
  productName: '',
  locationCode: '',
  warehouseName: '',
  customerName: '',
  sourceType: undefined as number | undefined,
  status: undefined as number | undefined,
  priority: undefined as number | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 配置弹窗 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'picking-order-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'taskNo', label: '单号', visible: true },
  { key: 'keyword', label: '单号/来源单号', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'sourceType', label: '来源类型', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'priority', label: '优先级', visible: true },
]

const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'taskNo', label: '单号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'locationCode', label: '货位', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增拣货单', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() =>
  activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value
)
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)

function loadPageConfig() {
  try {
    const docRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-doc')
    if (docRaw) {
      const parsed = JSON.parse(docRaw) as PageConfigData
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
    }
    const detailRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-detail')
    if (detailRaw) {
      const parsed = JSON.parse(detailRaw) as PageConfigData
      if (parsed.queryFields) {
        detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
    }
    const btnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons')
    if (btnRaw) {
      const parsed = JSON.parse(btnRaw) as PageConfigData
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'doc' ? '-doc' : '-detail'
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({ queryFields: config.queryFields || [] }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({ functionButtons: config.functionButtons || functionButtonConfig.value }))
  loadPageConfig()
}

// ═══ 列定义（按单据） ═══
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 240, fixed: 'right', slotName: 'actionCell' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 180, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 110, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '来源类型', field: 'sourceType', key: 'sourceType', width: 100, type: 'slot', slotName: 'sourceTypeCell' },
  { title: '来源单号', field: 'sourceOrderNo', key: 'sourceOrderNo', width: 160, sortable: true },
  { title: '总数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right', type: 'slot', slotName: 'totalQtyCell', sortable: true },
  { title: '已拣货', field: 'pickedQuantity', key: 'pickedQuantity', width: 100, align: 'right', type: 'slot', slotName: 'pickedQtyCell', sortable: true },
  { title: '状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell', sortable: true },
  { title: '优先级', field: 'priority', key: 'priority', width: 80, align: 'center' },
  { title: '备注', field: 'remark', key: 'remark', width: 130 },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 150, type: 'slot', slotName: 'createTimeCell', sortable: true },
]

// ═══ 列定义（按明细） ═══
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 200, fixed: 'right', slotName: 'actionCell' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 180, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 110, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 150, sortable: true },
  { title: '商品编码', field: 'productCode', key: 'productCode', width: 110, sortable: true },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 70 },
  { title: '货位', field: 'locationCode', key: 'locationCode', width: 110, sortable: true },
  { title: '应拣数量', field: 'expectedQuantity', key: 'expectedQuantity', width: 100, align: 'right' },
  { title: '已拣数量', field: 'pickedQuantity', key: 'pickedQuantity', width: 100, align: 'right' },
  { title: '缺货数量', field: 'shortageQuantity', key: 'shortageQuantity', width: 100, align: 'right' },
  { title: '批次号', field: 'batchNo', key: 'batchNo', width: 120 },
  { title: '状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'detailStatusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 150, type: 'slot', slotName: 'createTimeCell', sortable: true },
]

// ═══ 列配置 ═══
const docColumnDefs = computed(() => docColumns.map(col => ({ ...col })))
const detailColumnDefs = computed(() => detailColumns.map(col => ({ ...col })))
const {
  visibleColumns: docVisibleColumns,
  onSettingChange: onDocSettingChange,
  resetSettings: resetDocSettings,
  settingsColumns: docSettingsColumns,
} = useColumnConfig(docColumnDefs.value, 'picking-order-list-columns-doc')
const {
  visibleColumns: detailVisibleColumns,
  onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings,
  settingsColumns: detailSettingsColumns,
} = useColumnConfig(detailColumnDefs.value, 'picking-order-list-columns-detail')

const currentColumns = computed(() => (activeTab.value === 'doc' ? docVisibleColumns.value : detailVisibleColumns.value))
const panelColumns = computed(() => (activeTab.value === 'doc' ? docSettingsColumns.value : detailSettingsColumns.value))

function handleColumnConfigChange() {
  if (activeTab.value === 'doc') onDocSettingChange()
  else onDetailSettingChange()
}
function handleColumnConfigReset() {
  if (activeTab.value === 'doc') resetDocSettings()
  else resetDetailSettings()
}

// ═══ 状态/来源映射 ═══
function getStatusText(status: number): string { return PICK_STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number): string { return PICK_STATUS_MAP[status]?.color || 'default' }
function getSourceTypeText(t: number): string { return pickSourceTypeText(t) }

function getDetailStatusText(status: number): string {
  const map: Record<number, { text: string; color: string }> = {
    0: { text: '待拣货', color: 'orange' },
    1: { text: '已拣货', color: 'green' },
    2: { text: '缺货', color: 'red' },
  }
  return map[status]?.text || '未知'
}
function getDetailStatusColor(status: number): string {
  const map: Record<number, { text: string; color: string }> = {
    0: { text: '待拣货', color: 'orange' },
    1: { text: '已拣货', color: 'green' },
    2: { text: '缺货', color: 'red' },
  }
  return map[status]?.color || 'default'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalQty = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalQuantity) || 0), 0)
  const pickedQty = tableData.value.reduce((s: number, r: any) => s + (Number(r.pickedQuantity) || 0), 0)
  return [
    { key: 'totalQuantity', value: totalQty, highlight: true },
    { key: 'pickedQuantity', value: pickedQty },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.taskNo) params.taskNo = searchParams.taskNo
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.productName) params.productName = searchParams.productName
    if (searchParams.locationCode) params.locationCode = searchParams.locationCode
    if (searchParams.sourceType !== undefined) params.sourceType = searchParams.sourceType
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.priority !== undefined) params.priority = searchParams.priority
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate

    let res: any
    if (activeTab.value === 'detail') {
      res = await pickApi.pageDetail(params)
    } else {
      res = await pickApi.docQuery(params)
    }
    if (res) {
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    }
  } catch (error: any) {
    console.warn('[拣货单] 获取列表失败', error)
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

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 操作 ═══
function currentOperator() {
  return { id: userStore?.userId || 0, name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统' }
}

function handleAdd() {
  router.push('/wh/picking-order/form')
}
function handleView(record: any) {
  router.push(`/wh/picking-order/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/wh/picking-order/form?id=${record.id}`)
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除拣货单 ${record.taskNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await pickApi.taskRemove(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}
async function handleStart(record: any) {
  const u = currentOperator()
  try {
    await pickApi.taskStart(record.id, u.id, u.name)
    message.success('已开始拣货')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}
async function handleComplete(record: any) {
  try {
    await pickApi.taskComplete(record.id)
    message.success('拣货已完成')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}
async function handleCancel(record: any) {
  try {
    await pickApi.taskCancel(record.id, 'PC端取消拣货')
    message.success('拣货单已取消')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}

function handleError(err: any) { console.warn('[拣货单] ErrorBoundary:', err) }

onMounted(() => {
  loadPageConfig()
  fetchData()
})
</script>

<style scoped>
.quick-dates { flex-wrap: wrap; }
.search-area { width: 100%; }
.search-container { width: 100%; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px; align-items: center; }
.search-field-item { min-width: 0; }
.search-action-group { display: flex; align-items: center; gap: 8px; }
.table-area { padding: 0; }
</style>
