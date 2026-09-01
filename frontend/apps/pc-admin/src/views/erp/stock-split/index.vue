<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="true"
        @search="handleSearch"
      >
        <!-- ═══ 工具栏左侧：查询方案 ═══ -->
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
              <a-select-option value="draft">草稿</a-select-option>
              <a-select-option value="pending">待审批</a-select-option>
              <a-select-option value="approved">已审核</a-select-option>
              <a-select-option value="completed">已完成</a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
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
        </template>

        <!-- ═══ 工具栏右侧：列配置/页面配置/新增/刷新/打印/导出 ═══ -->
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
              <PlusOutlined /> 新增
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid" ref="searchGridRef">
                <div class="search-field-item">
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleDateChange"
                  />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.splitNo" placeholder="单据编号" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-select v-model:value="searchParams.outWarehouseId" placeholder="出库仓库" allow-clear size="small" show-search :filter-option="filterOption">
                    <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.warehouseName || w.name }}</a-select-option>
                  </a-select>
                </div>
                <div class="search-field-item">
                  <a-select v-model:value="searchParams.inWarehouseId" placeholder="入库仓库" allow-clear size="small" show-search :filter-option="filterOption">
                    <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.warehouseName || w.name }}</a-select-option>
                  </a-select>
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-select v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" show-search :filter-option="filterOption">
                    <a-select-option v-for="d in departmentOptions" :key="d.id" :value="d.name">{{ d.name }}</a-select-option>
                  </a-select>
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="searchParams.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">待审批</a-select-option>
                      <a-select-option :value="2">已审核</a-select-option>
                      <a-select-option :value="3">已完成</a-select-option>
                      <a-select-option :value="4">已拒绝</a-select-option>
                      <a-select-option :value="5">已取消</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                </div>
                <div class="search-action-group" ref="searchActionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                  <div class="search-field-item search-action-item">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  </div>
                  <div class="search-field-item">
                    <a-checkbox v-model:checked="searchParams.showRed">显示红冲</a-checkbox>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="visibleColumns"
              :data-source="tableData"
              :loading="loading"
              :pagination="pagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :row-selection="rowSelection"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #splitNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.splitNo }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template #totalCostCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalCost) }}</span>
              </template>
              <template #printCountCell="{ record }">
                <span class="currency-value">{{ record.printCount ?? 0 }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button
                    v-if="record.status === 1"
                    type="link"
                    size="small"
                    @click="handleApprove(record)"
                  >审批</a-button>
                  <a-button
                    v-if="record.status === 2"
                    type="link"
                    size="small"
                    @click="handleExecute(record)"
                  >执行</a-button>
                  <a-button
                    v-if="record.status === 0 || record.status === 2"
                    type="link"
                    size="small"
                    danger
                    @click="handleCancel(record)"
                  >取消</a-button>
                </a-space>
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
      :query-fields-config="queryFieldsConfig"
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
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  TableOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { stockSplitApi } from '@/api/erp'
import optionsApi from '@/api/options'

defineOptions({ name: 'StockSplitList' })

const router = useRouter()

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
const warehouseOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])

const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

const dateRange = ref<[any, any] | null>([dayjs().subtract(7, 'day'), dayjs()])

const searchParams = reactive<any>({
  splitNo: '',
  outWarehouseId: undefined,
  inWarehouseId: undefined,
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  status: undefined,
  remark: '',
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

// ═══ 列配置/页面配置弹窗 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'stock-split-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'splitNo', label: '单据编号', visible: true },
  { key: 'outWarehouseId', label: '出库仓库', visible: true },
  { key: 'inWarehouseId', label: '入库仓库', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        queryFieldsConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
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
  } catch {
    // ignore
  }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

// ═══ 列定义（对标文档18列） ═══
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 160, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'splitDate', key: 'splitDate', width: 110, sortable: true },
  { title: '单据编号', field: 'splitNo', key: 'splitNo', width: 170, type: 'slot', slotName: 'splitNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '入库仓库', field: 'inWarehouseName', key: 'inWarehouseName', width: 120, sortable: true },
  { title: '出库仓库', field: 'outWarehouseName', key: 'outWarehouseName', width: 120, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '本单金额', field: 'totalCost', key: 'totalCost', width: 120, align: 'right', type: 'slot', slotName: 'totalCostCell', sortable: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 140 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 90, align: 'right' },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 90, align: 'right' },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', type: 'slot', slotName: 'printCountCell' },
]

const columnDefs = computed(() => docColumns.map(col => ({ ...col })))
const {
  visibleColumns,
  onSettingChange,
  resetSettings,
  settingsColumns,
} = useColumnConfig(columnDefs.value, 'stock-split-list-columns')

const panelColumns = computed(() => settingsColumns.value)

function handleColumnConfigChange() {
  onSettingChange()
}
function handleColumnConfigReset() {
  resetSettings()
}

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'blue' },
  3: { text: '已完成', color: 'green' },
  4: { text: '已拒绝', color: 'red' },
  5: { text: '已取消', color: 'default' },
}
function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}
function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.splitNo) params.splitNo = searchParams.splitNo
    if (searchParams.outWarehouseId) params.outWarehouseId = searchParams.outWarehouseId
    if (searchParams.inWarehouseId) params.inWarehouseId = searchParams.inWarehouseId
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.dateStart) params.dateStart = searchParams.dateStart
    if (searchParams.dateEnd) params.dateEnd = searchParams.dateEnd

    const res: any = await stockSplitApi.page(params)
    const body = (res as any)?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[拆分单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: any, end: any
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

function handleDateChange(dates: any) {
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

function filterOption(input: string, option: any) {
  return (option?.label?.toString() || option?.children?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 操作 ═══
function handleAdd() {
  router.push('/erp/stock-split/form')
}
function handleView(record: any) {
  router.push(`/erp/stock-split/form/${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/erp/stock-split/form/${record.id}`)
}

function handleCancel(record: any) {
  const reason = '取消拆分单'
  Modal.confirm({
    title: '取消确认',
    content: `确认取消拆分单 ${record.splitNo} 吗？`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockSplitApi.cancel(record.id, reason || '取消拆分单')
        message.success('取消成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '取消失败')
      }
    },
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批确认',
    content: `确认审批通过拆分单 ${record.splitNo} 吗？`,
    okText: '审批通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockSplitApi.approve(record.id)
        message.success('审批通过')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '审批失败')
      }
    },
  })
}

function handleExecute(record: any) {
  Modal.confirm({
    title: '执行确认',
    content: `确认执行拆分单 ${record.splitNo} 吗？执行后将按成品扣减库存、按原料增加库存。`,
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockSplitApi.execute(record.id)
        message.success('拆分执行成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '执行失败')
      }
    },
  })
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的拆分单')
    return
  }
  router.push(`/erp/stock-split/form/${selectedRowKeys.value[0]}`)
}

async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.splitNo) params.splitNo = searchParams.splitNo
    const res: any = await stockSplitApi.page(params)
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '入库仓库', '出库仓库', '经手人', '本单金额', '状态']
    const rows = data.map((r: any) => [
      r.splitNo, r.splitDate, r.inWarehouseName, r.outWarehouseName, r.handlerName,
      r.totalCost, getStatusText(r.status),
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `拆分单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

async function loadOptions() {
  try {
    const wRes: any = await optionsApi.getWarehouses()
    warehouseOptions.value = Array.isArray(wRes) ? wRes : []
  } catch { warehouseOptions.value = [] }
  try {
    const dRes: any = await optionsApi.getDepartments()
    departmentOptions.value = Array.isArray(dRes) ? dRes : []
  } catch { departmentOptions.value = [] }
}

const handleError = (error: Error) => {
  console.error('[拆分单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  loadPageConfig()
  setQuickDate('lastWeek')
  loadOptions()
  fetchData()
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 200px; overflow: hidden; }
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
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
