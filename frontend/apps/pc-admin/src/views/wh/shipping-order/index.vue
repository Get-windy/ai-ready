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
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select v-model:value="queryScheme" style="width:140px" size="small" placeholder="--查询方案--">
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding:0 4px"><PlusOutlined /></a-button>
          </div>
          <a-space :size="4" class="quick-dates">
            <a-button v-for="d in quickDates" :key="d.key" :type="quickDate === d.key ? 'primary' : 'link'" size="small" @click="setQuickDate(d.key)">
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置">
              <a-button size="small" @click="showColumnConfig = true"><TableOutlined /></a-button>
            </a-tooltip>
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button>
            </a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增</a-button>
            <a-button size="small" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
            <a-button size="small" @click="handlePrintF8"><PrinterOutlined /> 打印(F8)</a-button>
            <a-button size="small" @click="handleExport"><ExportOutlined /> 导出</a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <template v-if="activeTab === 'doc'">
              <div class="search-container">
                <div class="search-grid" ref="docGridRef">
                  <div class="search-field-item">
                    <a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" />
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
                    <a-input v-model:value="searchParams.carrierName" placeholder="承运商" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">待复核</a-select-option>
                        <a-select-option :value="1">复核中</a-select-option>
                        <a-select-option :value="2">已发货</a-select-option>
                        <a-select-option :value="3">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.remark" placeholder="备注" allow-clear size="small" />
                  </div>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                    <div class="search-field-item search-action-item"><a-button type="primary" size="small" @click="handleSearch">查询</a-button></div>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="search-container">
                <div class="search-grid" ref="detailGridRef">
                  <div class="search-field-item">
                    <a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.keyword" placeholder="单据编号/来源单号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.productName" placeholder="商品名称" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.productCode" placeholder="商品编码" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">明细状态</span>
                      <a-select v-model:value="searchParams.productStatus" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">待扫描</a-select-option>
                        <a-select-option :value="1">已扫描</a-select-option>
                        <a-select-option :value="2">已确认</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + detailActionSpan }">
                    <div class="search-field-item search-action-item"><a-button type="primary" size="small" @click="handleSearch">查询</a-button></div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>

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
              <template #taskNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">{{ record.taskNo }}</a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template #detailStatusCell="{ record }">
                <a-tag :color="getDetailStatusColor(record.status)">{{ getDetailStatusText(record.status) }}</a-tag>
              </template>
              <template #qtyCell="{ record }">
                <span class="currency-value">{{ formatQty(record.totalQuantity) }}</span>
              </template>
              <template #scannedCell="{ record }">
                <span class="currency-value">{{ formatQty(record.scannedQuantity) }}</span>
              </template>
              <template #expectedCell="{ record }">
                <span class="currency-value">{{ formatQty(record.expectedQuantity) }}</span>
              </template>
              <template #confirmedCell="{ record }">
                <span class="currency-value">{{ formatQty(record.confirmedQuantity) }}</span>
              </template>
              <template #createTimeCell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >删除</a-button>
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
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined, TableOutlined, ExportOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { shipApi } from '@/api/wms/ship'
import { formatQty, formatTime } from '../whTask'
import { useRouter } from 'vue-router'

defineOptions({ name: 'WhShippingOrderList' })
const router = useRouter()

const tabs = [{ key: 'doc', label: '按单据' }, { key: 'detail', label: '按明细' }]
const activeTab = ref('doc')

const quickDates = [
  { key: 'yesterday', label: '昨日' }, { key: 'today', label: '今日' }, { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' }, { key: 'month', label: '本月' }, { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' }, { key: 'year', label: '本年' },
]
const quickDate = ref('week')
const queryScheme = ref('')

const loading = ref(false)
const tableData = ref<any[]>([])
const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])
const searchParams = reactive({
  keyword: '', warehouseName: '', customerName: '', carrierName: '', remark: '',
  productName: '', productCode: '', productStatus: undefined as number | undefined,
  status: undefined as number | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({ selectedRowKeys: selectedRowKeys.value, onChange: (keys: any[]) => { selectedRowKeys.value = keys } }))

const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// 页面配置
const PAGE_CONFIG_STORAGE_KEY = 'shipping-order-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }
const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true }, { key: 'keyword', label: '单号/来源单号', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true }, { key: 'customerName', label: '客户', visible: true },
  { key: 'carrierName', label: '承运商', visible: true }, { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '备注', visible: true },
]
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true }, { key: 'keyword', label: '单据/来源单号', visible: true },
  { key: 'productName', label: '商品名称', visible: true }, { key: 'productCode', label: '商品编码', visible: true },
  { key: 'customerName', label: '客户', visible: true }, { key: 'productStatus', label: '明细状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true }, { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true }, { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const activeQueryFields = computed(() => (activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value))
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)

function loadPageConfig() {
  try {
    const docRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-doc')
    if (docRaw) {
      const parsed = JSON.parse(docRaw) as PageConfigData
      if (parsed.queryFields) docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => { const s = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key); return s ? { ...df, ...s } : { ...df } })
    }
    const detailRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-detail')
    if (detailRaw) {
      const parsed = JSON.parse(detailRaw) as PageConfigData
      if (parsed.queryFields) detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => { const s = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key); return s ? { ...df, ...s } : { ...df } })
    }
    const btnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons')
    if (btnRaw) {
      const parsed = JSON.parse(btnRaw) as PageConfigData
      if (parsed.functionButtons) functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => { const s = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key); return s ? { ...bf, ...s } : { ...bf } })
    }
  } catch { /* ignore */ }
}
function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'doc' ? '-doc' : '-detail'
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({ queryFields: config.queryFields || [] }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({ functionButtons: config.functionButtons || functionButtonConfig.value }))
  loadPageConfig()
}

// 列定义（在 useColumnConfig 前声明）
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 175, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '来源单号', field: 'sourceOrderNo', key: 'sourceOrderNo', width: 150, ellipsis: true },
  { title: '总数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right', type: 'slot', slotName: 'qtyCell', sortable: true },
  { title: '已扫描', field: 'scannedQuantity', key: 'scannedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'scannedCell' },
  { title: '承运商', field: 'carrierName', key: 'carrierName', width: 110 },
  { title: '运单号', field: 'trackingNo', key: 'trackingNo', width: 140 },
  { title: '状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '备注', field: 'remark', key: 'remark', width: 130, ellipsis: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 140, type: 'slot', slotName: 'createTimeCell', sortable: true },
]
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' },
  { title: '单据编号', field: 'taskNo', key: 'taskNo', width: 175, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 70 },
  { title: '应发数量', field: 'expectedQuantity', key: 'expectedQuantity', width: 100, align: 'right', type: 'slot', slotName: 'expectedCell', sortable: true },
  { title: '已扫描', field: 'scannedQuantity', key: 'scannedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'scannedCell' },
  { title: '已确认', field: 'confirmedQuantity', key: 'confirmedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'confirmedCell' },
  { title: '批次号', field: 'batchNo', key: 'batchNo', width: 110 },
  { title: '序列号', field: 'serialNo', key: 'serialNo', width: 110 },
  { title: '货位编码', field: 'locationCode', key: 'locationCode', width: 110 },
  { title: '明细状态', field: 'status', key: 'detailStatus', width: 90, align: 'center', type: 'slot', slotName: 'detailStatusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 140, type: 'slot', slotName: 'createTimeCell', sortable: true },
]

const docColumnDefs = computed(() => docColumns.map(c => ({ ...c })))
const detailColumnDefs = computed(() => detailColumns.map(c => ({ ...c })))
const { visibleColumns: docVisible, onSettingChange: onDocSettingChange, resetSettings: resetDocSettings, settingsColumns: docSettings } = useColumnConfig(docColumnDefs.value, 'shipping-order-list-columns-doc')
const { visibleColumns: detailVisible, onSettingChange: onDetailSettingChange, resetSettings: resetDetailSettings, settingsColumns: detailSettings } = useColumnConfig(detailColumnDefs.value, 'shipping-order-list-columns-detail')
const currentColumns = computed(() => (activeTab.value === 'doc' ? docVisible.value : detailVisible.value))
const panelColumns = computed(() => (activeTab.value === 'doc' ? docSettings.value : detailSettings.value))
function handleColumnConfigChange() { if (activeTab.value === 'doc') onDocSettingChange(); else onDetailSettingChange() }
function handleColumnConfigReset() { if (activeTab.value === 'doc') resetDocSettings(); else resetDetailSettings() }

// 状态映射
const SHIP_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待复核', color: 'orange' }, 1: { text: '复核中', color: 'blue' },
  2: { text: '已发货', color: 'green' }, 3: { text: '已取消', color: 'red' },
}
const DETAIL_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待扫描', color: 'orange' }, 1: { text: '已扫描', color: 'blue' }, 2: { text: '已确认', color: 'green' },
}
function getStatusText(s: number) { return SHIP_STATUS_MAP[s]?.text || '未知' }
function getStatusColor(s: number) { return SHIP_STATUS_MAP[s]?.color || 'default' }
function getDetailStatusText(s: number) { return DETAIL_STATUS_MAP[s]?.text || '未知' }
function getDetailStatusColor(s: number) { return DETAIL_STATUS_MAP[s]?.color || 'default' }

// 表格底部合计（按单据：总数量/已扫描）
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalQty = tableData.value.reduce((s: number, r: any) => s + (r.totalQuantity || 0), 0)
  const scanned = tableData.value.reduce((s: number, r: any) => s + (r.scannedQuantity || 0), 0)
  return [
    { key: 'totalQuantity', value: totalQty, highlight: true },
    { key: 'scannedQuantity', value: scanned, highlight: true },
  ]
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.carrierName) params.carrierName = searchParams.carrierName
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    let res: any
    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.productCode) params.productCode = searchParams.productCode
      if (searchParams.productStatus !== undefined) params.status = searchParams.productStatus
      res = await shipApi.pageDetail(params)
    } else {
      res = await shipApi.queryPage(params)
    }
    if (res) {
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    }
  } catch (error: any) {
    console.warn('[发货单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) { activeTab.value = key; pagination.current = 1; fetchData() }
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

// 操作
function handleAdd() { router.push('/wms/ship/form') }
function handleView(record: any) { router.push({ path: '/wms/ship/form', query: { id: record.id } }) }
function handleEdit(record: any) { router.push({ path: '/wms/ship/form', query: { id: record.id } }) }
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除发货单 ${record.taskNo} 吗？`,
    okText: '确认删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      try { await shipApi.remove(record.id); message.success('删除成功'); fetchData() }
      catch (error: any) { message.error(error?.response?.data?.message || '删除失败') }
    },
  })
}
function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) { message.warning('请先选择要打印的发货单'); return }
  router.push({ path: '/wms/ship/form', query: { id: selectedRowKeys.value[0] } })
}
async function handleExport() {
  try {
    const params: any = {}
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    const res: any = activeTab.value === 'detail'
      ? await shipApi.pageDetail({ ...params, productName: searchParams.productName, productCode: searchParams.productCode, pageNum: 1, pageSize: 9999 })
      : await shipApi.queryPage({ ...params, pageNum: 1, pageSize: 9999 })
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) { message.warning('没有可导出的数据'); return }
    const headers = activeTab.value === 'detail'
      ? ['单据编号', '仓库', '客户', '商品名称', '货号', '规格', '单位', '应发数量', '已扫描', '已确认', '批次号', '序列号']
      : ['单据编号', '仓库', '客户', '来源单号', '总数量', '已扫描', '承运商', '运单号', '状态']
    const rows = data.map((r: any) => activeTab.value === 'detail'
      ? [r.taskNo, r.warehouseName, r.customerName, r.productName, r.productCode, r.productSpec, r.productUnit, r.expectedQuantity, r.scannedQuantity, r.confirmedQuantity, r.batchNo, r.serialNo]
      : [r.taskNo, r.warehouseName, r.customerName, r.sourceOrderNo, r.totalQuantity, r.scannedQuantity, r.carrierName, r.trackingNo, getStatusText(r.status)])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `发货单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => { console.error('[发货单] 页面错误', error); message.error(`页面错误: ${error.message}`) }

onMounted(() => { setQuickDate('week'); fetchData(); loadPageConfig() })
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper), .search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-item { flex-shrink: 0; }
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
