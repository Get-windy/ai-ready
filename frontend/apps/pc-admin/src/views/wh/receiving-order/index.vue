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
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select v-model:value="queryScheme" style="width: 140px" size="small" placeholder="--查询方案--">
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px"><PlusOutlined /></a-button>
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

        <!-- ═══ 工具栏右侧：操作按钮 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置"><a-button size="small" @click="showColumnConfig = true"><TableOutlined /></a-button></a-tooltip>
            <a-tooltip title="页面配置"><a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button></a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增收货单</a-button>
            <a-button size="small" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
            <a-button size="small" @click="handlePrintF8"><PrinterOutlined /> 打印(F8)</a-button>
            <a-button size="small" @click="handleExport"><ExportOutlined /> 导出</a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（双 Tab 各自搜索行） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <template v-if="activeTab === 'doc'">
              <div class="search-container">
                <div class="search-grid" ref="docGridRef">
                  <div class="search-field-item"><a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.keyword" placeholder="单号/来源单号" allow-clear size="small" @press-enter="handleSearch" /></div>
                  <div class="search-field-item"><a-select v-model:value="searchParams.sourceType" size="small" allow-clear placeholder="来源类型"><a-select-option value="">全部</a-select-option><a-select-option v-for="o in sourceTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option></a-select></div>
                  <div class="search-field-item"><a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="状态"><a-select-option value="">全部</a-select-option><a-select-option v-for="(m, k) in STATUS_MAP" :key="k" :value="Number(k)">{{ m.text }}</a-select-option></a-select></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" /></div>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="search-container">
                <div class="search-grid" ref="detailGridRef">
                  <div class="search-field-item"><a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.keyword" placeholder="单号/来源单号" allow-clear size="small" @press-enter="handleSearch" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.batchNo" placeholder="批次号" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-select v-model:value="searchParams.sourceType" size="small" allow-clear placeholder="来源类型"><a-select-option value="">全部</a-select-option><a-select-option v-for="o in sourceTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option></a-select></div>
                  <div class="search-field-item"><a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="状态"><a-select-option value="">全部</a-select-option><a-select-option v-for="(m, k) in STATUS_MAP" :key="k" :value="Number(k)">{{ m.text }}</a-select-option></a-select></div>
                  <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + actionSpan }">
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
              :selectable="true"
              :row-selection="rowSelection"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #taskNoCell="{ record }"><a-button type="link" size="small" @click="handleView(record)">{{ record.taskNo }}</a-button></template>
              <template #sourceTypeCell="{ record }">{{ sourceTypeText(record.sourceType) }}</template>
              <template #totalQuantityCell="{ record }"><span class="currency-value">{{ formatQty(record.totalQuantity) }}</span></template>
              <template #receivedQuantityCell="{ record }"><span class="currency-value">{{ formatQty(record.receivedQuantity) }}</span></template>
              <template #progressCell="{ record }"><span class="currency-value">{{ formatQty(record.receivedQuantity) }} / {{ formatQty(record.totalQuantity) }}</span></template>
              <template #statusCell="{ record }"><a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag></template>
              <template #docStatusCell="{ record }"><a-tag :color="statusColor(record.docStatus)">{{ statusText(record.docStatus) }}</a-tag></template>
              <template #detailStatusCell="{ record }"><a-tag :color="detailStatusColor(record.status)">{{ detailStatusText(record.status) }}</a-tag></template>
              <template #expectedQuantityCell="{ record }"><span class="currency-value">{{ formatQty(record.expectedQuantity) }}</span></template>
              <template #putawayQuantityCell="{ record }"><span class="currency-value">{{ formatQty(record.putawayQuantity) }}</span></template>
              <template #createTimeCell="{ record }">{{ formatTime(record.createTime) }}</template>
              <template #taskCreateTimeCell="{ record }">{{ formatTime(record.taskCreateTime) }}</template>
              <template #productionDateCell="{ record }">{{ formatTime(record.productionDate) }}</template>
              <template #validityDateCell="{ record }">{{ formatTime(record.validityDate) }}</template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button type="link" size="small" @click="goQuality(record)">生成质检单</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleStart(record)">开始收货</a-button>
                  <a-button v-if="record.status === 1" type="link" size="small" @click="handleConfirm(record)">确认收货</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="panelColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />

    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="activePageStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ExportOutlined, PrinterOutlined, TableOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { receiptApi } from '@/api/wms/receipt'
import { WMS_STATUS_MAP, DETAIL_STATUS_MAP, formatQty, formatTime } from '../whTask'

defineOptions({ name: 'WhReceivingOrderList' })

const router = useRouter()

function goQuality(record: any) {
  router.push({
    path: '/quality/inspection/form',
    query: {
      bizType: 'PURCHASE_ORDER',
      bizId: record.sourceOrderId || record.sourceId || record.id,
      bizNo: record.sourceOrderNo || record.sourceNo || record.orderNo,
      warehouseId: record.warehouseId || undefined,
    },
  })
}
const STATUS_MAP = WMS_STATUS_MAP
const sourceTypeOptions = [
  { label: '采购入库', value: 0 }, { label: '生产入库', value: 1 }, { label: '退货入库', value: 2 },
  { label: '调拨入库', value: 3 }, { label: '其他', value: 4 },
]
function sourceTypeText(t: number) { return sourceTypeOptions.find(o => o.value === t)?.label || '其他' }
function statusText(s: number) { return STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return STATUS_MAP[s]?.color || 'default' }
function detailStatusText(s: number) { return DETAIL_STATUS_MAP[s]?.text || '未知' }
function detailStatusColor(s: number) { return DETAIL_STATUS_MAP[s]?.color || 'default' }

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('doc')

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' }, { key: 'today', label: '今日' }, { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' }, { key: 'month', label: '本月' }, { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' }, { key: 'year', label: '本年' },
]
const quickDate = ref('week')
const queryScheme = ref('')

// ═══ 弹窗开关 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 搜索参数 ═══
const searchParams = reactive<any>({
  keyword: '', productName: '', batchNo: '', sourceType: undefined, status: undefined, warehouseName: '',
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'), endDate: dayjs().format('YYYY-MM-DD'),
})
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

const docGridRef = ref<any>(null)
const docActionRef = ref<any>(null)
const detailGridRef = ref<any>(null)
const detailActionRef = ref<any>(null)
const { span: actionSpan } = useAutoGridSpan(activeTab.value === 'doc' ? docActionRef : detailActionRef,
  activeTab.value === 'doc' ? docGridRef : detailGridRef)

const tableData = ref<any[]>([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const params: any = { current: pagination.current, size: pagination.pageSize }
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (searchParams.sourceType !== undefined && searchParams.sourceType !== '') params.sourceType = searchParams.sourceType
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName

    let res: any
    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.batchNo) params.batchNo = searchParams.batchNo
      res = await receiptApi.pageDetail(params)
    } else {
      res = await receiptApi.page(params)
    }
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e: any) { message.error(e?.message || '查询失败') }
  finally { loading.value = false }
}
function handleSearch() { pagination.current = 1; fetchData() }
function handlePageChange(page: number, pageSize: number) { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
function handleTabChange(key: string) { activeTab.value = key; pagination.current = 1; fetchData() }
function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) { searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''; searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || '' }
  else { searchParams.startDate = ''; searchParams.endDate = '' }
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

// ═══ 页面配置（按 Tab 分 key） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const PAGE_CONFIG_STORAGE_KEY = 'receiving-order-page-config'
const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'keyword', label: '单号/来源单号', visible: true },
  { key: 'sourceType', label: '来源类型', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
]
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'keyword', label: '单号/来源单号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'batchNo', label: '批次号', visible: true },
  { key: 'sourceType', label: '来源类型', visible: true },
  { key: 'status', label: '状态', visible: true },
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
const activeQueryFields = computed(() => activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value)
const activePageStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)
function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'doc' ? '-doc' : '-detail'
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({ queryFields: config.queryFields || [] }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({ functionButtons: config.functionButtons || functionButtonConfig.value }))
  loadPageConfig()
}
function loadPageConfig() {
  try {
    const docRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-doc')
    if (docRaw) {
      const parsed = JSON.parse(docRaw)
      if (parsed.queryFields) docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => { const saved = parsed.queryFields.find((f: any) => f.key === df.key); return saved ? { ...df, ...saved } : { ...df } })
    }
    const detailRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-detail')
    if (detailRaw) {
      const parsed = JSON.parse(detailRaw)
      if (parsed.queryFields) detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => { const saved = parsed.queryFields.find((f: any) => f.key === df.key); return saved ? { ...df, ...saved } : { ...df } })
    }
    const btnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons')
    if (btnRaw) {
      const parsed = JSON.parse(btnRaw)
      if (parsed.functionButtons) functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => { const saved = parsed.functionButtons.find((f: any) => f.key === bf.key); return saved ? { ...bf, ...saved } : { ...bf } })
    }
  } catch (e) { /* 忽略 */ }
}

// ═══ 列定义（必须在 useColumnConfig 之前声明） ═══
/** 按单据 Tab 列 */
const docColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 180, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '来源类型', field: 'sourceType', key: 'sourceType', width: 100, type: 'slot', slotName: 'sourceTypeCell' },
  { title: '来源单号', field: 'sourceOrderNo', key: 'sourceOrderNo', width: 150, ellipsis: true },
  { title: '供应商', field: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '总数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right', type: 'slot', slotName: 'totalQuantityCell', sortable: true },
  { title: '已收货', field: 'receivedQuantity', key: 'receivedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'receivedQuantityCell' },
  { title: '进度', field: 'progress', key: 'progress', width: 130, type: 'slot', slotName: 'progressCell' },
  { title: '状态', field: 'status', key: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '优先级', field: 'priority', key: 'priority', width: 80 },
  { title: '预计时间', field: 'expectedTime', key: 'expectedTime', width: 140 },
  { title: '经手人', field: 'assigneeName', key: 'assigneeName', width: 90 },
  { title: '备注', field: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 130, type: 'slot', slotName: 'createTimeCell' },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' },
]

/** 按明细 Tab 列 */
const detailColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 170, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '来源类型', field: 'sourceType', key: 'sourceType', width: 96, type: 'slot', slotName: 'sourceTypeCell' },
  { title: '来源单号', field: 'sourceOrderNo', key: 'sourceOrderNo', width: 140, ellipsis: true },
  { title: '供应商', field: 'supplierName', key: 'supplierName', width: 130, ellipsis: true },
  { title: '商品名称', field: 'productName', key: 'productName', width: 190, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 70 },
  { title: '应收数量', field: 'expectedQuantity', key: 'expectedQuantity', width: 100, align: 'right', type: 'slot', slotName: 'expectedQuantityCell' },
  { title: '实收数量', field: 'receivedQuantity', key: 'receivedQuantity', width: 100, align: 'right', type: 'slot', slotName: 'receivedQuantityCell' },
  { title: '已上架', field: 'putawayQuantity', key: 'putawayQuantity', width: 90, align: 'right', type: 'slot', slotName: 'putawayQuantityCell' },
  { title: '货位', field: 'locationCode', key: 'locationCode', width: 110 },
  { title: '批次号', field: 'batchNo', key: 'batchNo', width: 120 },
  { title: '生产日期', field: 'productionDate', key: 'productionDate', width: 110, type: 'slot', slotName: 'productionDateCell' },
  { title: '有效期', field: 'validityDate', key: 'validityDate', width: 110, type: 'slot', slotName: 'validityDateCell' },
  { title: '明细状态', field: 'detailStatus', key: 'detailStatus', width: 90, type: 'slot', slotName: 'detailStatusCell' },
  { title: '单据状态', field: 'docStatus', key: 'docStatus', width: 90, type: 'slot', slotName: 'docStatusCell' },
  { title: '单据创建时间', field: 'taskCreateTime', key: 'taskCreateTime', width: 130, type: 'slot', slotName: 'taskCreateTimeCell' },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' },
]

// ═══ 列配置（按 Tab 分 key） ═══
const docColumnDefs = computed(() => docColumns.map(c => ({ ...c })))
const detailColumnDefs = computed(() => detailColumns.map(c => ({ ...c })))
const {
  visibleColumns: docVisibleColumns, onSettingChange: onDocSettingChange,
  resetSettings: resetDocSettings, settingsColumns: docSettingsColumns,
} = useColumnConfig(docColumnDefs.value, 'receiving-order-list-columns-doc')
const {
  visibleColumns: detailVisibleColumns, onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings, settingsColumns: detailSettingsColumns,
} = useColumnConfig(detailColumnDefs.value, 'receiving-order-list-columns-detail')
const currentColumns = computed(() => activeTab.value === 'doc' ? docVisibleColumns.value : detailVisibleColumns.value)
const panelColumns = computed(() => activeTab.value === 'doc' ? docSettingsColumns.value : detailSettingsColumns.value)
function handleColumnConfigChange() { if (activeTab.value === 'doc') onDocSettingChange(); else onDetailSettingChange() }
function handleColumnConfigReset() { if (activeTab.value === 'doc') resetDocSettings(); else resetDetailSettings() }

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalQty = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalQuantity) || 0), 0)
  const recvQty = tableData.value.reduce((s: number, r: any) => s + (Number(r.receivedQuantity) || 0), 0)
  return [
    { key: 'totalQuantity', value: totalQty, highlight: true },
    { key: 'receivedQuantity', value: recvQty, highlight: true },
  ]
})

// ═══ 操作 ═══
function handleAdd() { router.push('/wh/receiving-order/form') }
function handleView(record: any) { router.push(`/wh/receiving-order/form?id=${record.id}`) }
async function handleStart(record: any) {
  try { await receiptApi.startReceipt(record.id, 0, ''); message.success('已开始收货'); fetchData() } catch (e: any) { message.error(e?.message || '操作失败') }
}
async function handleConfirm(record: any) {
  try { await receiptApi.confirmReceipt(record.id, 0, ''); message.success('收货已确认'); fetchData() } catch (e: any) { message.error(e?.message || '操作失败') }
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定要删除收货单 ${record.taskNo} 吗？此操作不可恢复。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      try { await receiptApi.remove(record.id); message.success('删除成功'); fetchData() } catch (e: any) { message.error(e?.message || '删除失败') }
    },
  })
}
function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) { message.warning('请先选择要打印的收货单'); return }
  router.push(`/wh/receiving-order/form?id=${selectedRowKeys.value[0]}`)
}
async function handleExport() {
  try {
    const params: any = { current: 1, size: 9999 }
    if (activeTab.value === 'detail') {
      if (searchParams.keyword) params.keyword = searchParams.keyword
      const res: any = await receiptApi.pageDetail(params)
      const rows = res?.records || []
      const headers = ['单号', '仓库', '商品名称', '货号', '规格', '单位', '应收数量', '实收数量', '已上架', '货位', '批次号', '状态']
      const lines = rows.map((r: any) => [r.taskNo, r.warehouseName, r.productName, r.productCode, r.productSpec, r.productUnit, r.expectedQuantity, r.receivedQuantity, r.putawayQuantity, r.locationCode, r.batchNo, statusText(r.status)].join(','))
      const csv = '\ufeff' + [headers.join(','), ...lines].join('\n')
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
      const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '收货明细.csv'; a.click()
      return
    }
    if (searchParams.keyword) params.keyword = searchParams.keyword
    const res: any = await receiptApi.page(params)
    const rows = res?.records || []
    const headers = docColumns.filter(c => c.key && c.key !== 'rowNo' && c.key !== 'action').map(c => c.title)
    const lines = rows.map((r: any) => docColumns.filter(c => c.key && c.key !== 'rowNo' && c.key !== 'action').map(c => r[c.field ?? c.key] ?? '').join(','))
    const csv = '\ufeff' + [headers.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '收货单列表.csv'; a.click()
  } catch (e: any) { message.error(e?.message || '导出失败') }
}
function handleError(e: any) { console.error(e) }

onMounted(() => { loadPageConfig(); fetchData() })
</script>

<style scoped>
.search-area { display: flex; flex-direction: column; }
.search-container { width: 100%; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 140px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; min-width: 180px; }
.query-scheme-wrap { display: flex; align-items: center; }
.table-area { width: 100%; }
.currency-value { font-variant-numeric: tabular-nums; }
</style>
