<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ 工具栏 + 搜索区域 ═══ -->
      <div class="page-header">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
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
          </div>
          <div class="toolbar-right">
            <a-space :size="8">
              <a-tooltip title="新增">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleAdd"
                >
                  <PlusOutlined /> 新增
                </a-button>
              </a-tooltip>
              <a-tooltip title="刷新">
                <a-button
                  size="small"
                  @click="fetchData"
                >
                  <ReloadOutlined /> 刷新
                </a-button>
              </a-tooltip>
              <a-tooltip title="批量审核">
                <a-button size="small" @click="handleBatchApprove">
                  <AuditOutlined /> 批量审核
                </a-button>
              </a-tooltip>
              <a-tooltip title="批量打印">
                <a-button size="small" @click="handleBatchPrint">
                  <PrinterOutlined /> 批量打印
                </a-button>
              </a-tooltip>
              <a-tooltip title="打印(F8)">
                <a-button size="small" @click="handlePrintSelected">
                  <PrinterOutlined /> 打印(F8)
                </a-button>
              </a-tooltip>
              <a-tooltip title="导出">
                <a-button size="small" @click="handleExport">
                  <ExportOutlined /> 导出
                </a-button>
              </a-tooltip>
              <a-tooltip title="配置">
                <a-button
                  size="small"
                  @click="showPageConfig = true"
                >
                  <SettingOutlined /> 配置
                </a-button>
              </a-tooltip>
              <a-tooltip title="列配置">
                <a-button
                  size="small"
                  @click="showColumnConfig = true"
                >
                  <SettingOutlined />
                </a-button>
              </a-tooltip>
            </a-space>
          </div>
        </div>

        <!-- 搜索区域 -->
        <div class="search-area">
          <div class="search-container" :data-expanded="showMore || null">
          <div class="search-grid" ref="gridRef">
            <!-- Base fields (always visible) -->
            <div class="search-field-item">
              <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.exchangeNo" placeholder="单据编号" allow-clear size="small" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
            </div>
            <div class="search-field-item">
              <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
            </div>
            <div class="search-field-item">
              <div class="search-select-wrap">
                <span class="search-select-label">单据状态</span>
                <a-select v-model:value="searchParams.status" size="small" allow-clear>
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option :value="0">草稿</a-select-option>
                  <a-select-option :value="1">待审核</a-select-option>
                  <a-select-option :value="2">已审核</a-select-option>
                  <a-select-option :value="3">已完成</a-select-option>
                  <a-select-option :value="4">已取消</a-select-option>
                </a-select>
              </div>
            </div>
            <!-- Expanded fields (v-if="showMore") -->
            <template v-if="showMore">
              <div class="search-field-item">
                <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
              </div>
              <div class="search-field-item">
                <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
              </div>
              <div class="search-field-item">
                <a-input v-model:value="searchParams.inWarehouseName" placeholder="入库仓库" allow-clear size="small" />
              </div>
              <div class="search-field-item">
                <a-input v-model:value="searchParams.outWarehouseName" placeholder="出库仓库" allow-clear size="small" />
              </div>
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">结算状态</span>
                  <a-select v-model:value="searchParams.settleStatus" size="small" allow-clear>
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="unsettled">未结算</a-select-option>
                    <a-select-option value="partial">部分结算</a-select-option>
                    <a-select-option value="settled">已结算</a-select-option>
                  </a-select>
                </div>
              </div>
              <div class="search-field-item">
                <a-input v-model:value="searchParams.salesType" placeholder="销售类型" allow-clear size="small" />
              </div>
            </template>
            <!-- Action buttons -->
            <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
            <div class="search-field-item search-action-item">
              <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
            </div>
            </div>
          </div>
            <div class="search-more-toggle">
              <a-button type="link" size="small" @click="showMore = !showMore">
                {{ showMore ? '收起' : '更多条件' }}
                <UpOutlined v-if="showMore" />
                <DownOutlined v-else />
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ 表格区域 ═══ -->
      <div class="table-area">
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
          :selectable="true"
          :row-selection="rowSelection"
          row-key="id"
          @page-change="handlePageChange"
        >
          <!-- 单据编号 -->
          <template #exchangeNoCell="{ record }">
            <a-button
              type="link"
              size="small"
              @click="handleView(record)"
            >
              {{ record.exchangeNo }}
            </a-button>
          </template>
          <!-- 单据状态 -->
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <!-- 结算状态 -->
          <template #settleStatusCell="{ record }">
            <a-tag :color="getSettleStatusColor(record.settleStatus)">
              {{ getSettleStatusText(record.settleStatus) }}
            </a-tag>
          </template>
        </BillTableList>
      </div>

      <!-- ═══ 配置面板 ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
      <ColumnConfigPanel
        :open="showColumnConfig"
        :settings-columns="settingsColumns"
        :is-locked-column="isLockedColumn"
        @update:open="showColumnConfig = $event"
        @change="onSettingChange"
        @reset="resetSettings"
        @drag-end="onSettingChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  ExportOutlined,
  SettingOutlined,
  SearchOutlined,
  DownOutlined,
  UpOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { saleExchangeApi } from '@/api/erp'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { useRouter } from 'vue-router'

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

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const showMore = ref(false)

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs(), dayjs()])

// ══ 搜索参数 ═══
const searchParams = reactive({
  exchangeNo: '',
  customerName: '',
  handlerName: '',
  deptName: '',
  inWarehouseName: '',
  outWarehouseName: '',
  creatorName: '',
  bookkeeperName: '',
  settleStatus: undefined as string | undefined,
  salesType: '',
  status: undefined as number | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => {
    selectedRowKeys.value = keys
  }
}))

// ═══ 配置面板 ═══
const showPageConfig = ref(false)
const showColumnConfig = ref(false)

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'processing' },
  2: { text: '已审核', color: 'blue' },
  3: { text: '已完成', color: 'success' },
  4: { text: '已取消', color: 'default' },
}

const SETTLE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  unsettled: { text: '未结算', color: 'default' },
  partial: { text: '部分结算', color: 'processing' },
  settled: { text: '已结算', color: 'success' },
}

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}

function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

function getSettleStatusText(settleStatus: string): string {
  return SETTLE_STATUS_MAP[settleStatus]?.text || '未知'
}

function getSettleStatusColor(settleStatus: string): string {
  return SETTLE_STATUS_MAP[settleStatus]?.color || 'default'
}

// ═══ 表格列配置（32列） ═══
const exchangeColumns = [
  { title: '单据日期', field: 'exchangeDate', key: 'exchangeDate', width: 110, sortable: true },
  { title: '单据编号', field: 'exchangeNo', key: 'exchangeNo', width: 170, type: 'slot', slotName: 'exchangeNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '入库仓库', field: 'inWarehouseName', key: 'inWarehouseName', width: 120, sortable: true },
  { title: '出库仓库', field: 'outWarehouseName', key: 'outWarehouseName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 120, sortable: true },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 120, sortable: true },
  { title: '入库数量', field: 'inQuantityTotal', key: 'inQuantityTotal', width: 100, align: 'right', sortable: true },
  { title: '出库数量', field: 'outQuantityTotal', key: 'outQuantityTotal', width: 100, align: 'right', sortable: true },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 100, align: 'right', sortable: true },
  { title: '金额', field: 'productAmount', key: 'productAmount', width: 100, align: 'right', sortable: true },
  { title: '折后金额', field: 'discountAmount', key: 'discountAmount', width: 100, align: 'right', sortable: true },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 100, align: 'right', sortable: true },
  { title: '结算状态', field: 'settleStatus', key: 'settleStatus', width: 100, align: 'center', type: 'slot', slotName: 'settleStatusCell' },
  { title: '重量(kg)', field: 'totalWeight', key: 'totalWeight', width: 100, align: 'right', sortable: true },
  { title: '体积(m³)', field: 'totalVolume', key: 'totalVolume', width: 100, align: 'right', sortable: true },
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 100, sortable: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 150 },
  { title: '自定义字段1', field: 'extNum1', key: 'extNum1', width: 100, align: 'right' },
  { title: '自定义字段2', field: 'extNum2', key: 'extNum2', width: 100, align: 'right' },
  { title: '自定义字段3', field: 'extText1', key: 'extText1', width: 120 },
  { title: '自定义字段4', field: 'extText2', key: 'extText2', width: 120 },
  { title: '自定义字段5', field: 'extText3', key: 'extText3', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 150 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 150, sortable: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150, sortable: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', sortable: true },
]

// ═══ 列配置（使用 useColumnConfig composable） ═══
const {
  visibleColumns,
  onSettingChange,
  resetSettings,
  settingsColumns,
} = useColumnConfig(exchangeColumns, 'sale-exchange-list-columns')

// ═══ 数据加载 ══

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.exchangeNo) params.keyword = searchParams.exchangeNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.inWarehouseName) params.inWarehouseName = searchParams.inWarehouseName
    if (searchParams.outWarehouseName) params.outWarehouseName = searchParams.outWarehouseName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
    if (searchParams.settleStatus) params.settleStatus = searchParams.settleStatus
    if (searchParams.salesType) params.salesType = searchParams.salesType
    if (searchParams.status !== undefined && searchParams.status !== null) params.status = searchParams.status
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate

    const res = await saleExchangeApi.page(params)
    if (res) {
      tableData.value = res.records || res.data?.records || []
      pagination.total = res.total || res.data?.total || 0
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售换货单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday':
      start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today':
      start = now; end = now; break
    case 'week':
      start = now.startOf('week'); end = now; break
    case 'lastWeek':
      start = now.subtract(7, 'day'); end = now; break
    case 'month':
      start = now.startOf('month'); end = now; break
    case 'lastMonth':
      start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month':
      start = now.subtract(3, 'month'); end = now; break
    case 'year':
      start = now.startOf('year'); end = now; break
    default:
      start = now.subtract(7, 'day'); end = now
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

function handleAdd() {
  router.push('/sales/exchange/form')
}

function handleView(record: any) {
  router.push(`/sales/exchange/form?id=${record.id}`)
}

function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要审核的换货单')
    return
  }
  Modal.confirm({
    title: '批量审核',
    content: `确定审核选中的 ${selectedRowKeys.value.length} 张换货单？`,
    async onOk() {
      try {
        await saleExchangeApi.batchApprove(selectedRowKeys.value)
        message.success('批量审核成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (e: any) {
        message.error(e.message || '批量审核失败')
      }
    },
  })
}

function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的换货单')
    return
  }
  saleExchangeApi.batchPrint(selectedRowKeys.value)
    .then(() => {
      message.success('批量打印成功')
      fetchData()
    })
    .catch((e: any) => message.error(e.message || '批量打印失败'))
}

function handlePrintSelected() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的换货单')
    return
  }
  // 打印第一张选中单据
  saleExchangeApi.print(selectedRowKeys.value[0])
    .then(() => message.success('打印成功'))
    .catch((e: any) => message.error(e.message || '打印失败'))
}

function handleExport() {
  const params: any = {}
  if (searchParams.exchangeNo) params.keyword = searchParams.exchangeNo
  if (searchParams.customerName) params.customerId = searchParams.customerName
  if (searchParams.status != null) params.status = searchParams.status
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0].format('YYYY-MM-DD')
    params.endDate = dateRange.value[1].format('YYYY-MM-DD')
  }
  saleExchangeApi.export(params)
    .then(() => message.success('导出成功'))
    .catch((e: any) => message.error(e.message || '导出失败'))
}

function handlePageConfigChange(_config: any) {
  // PageConfigPanel 自动持久化到 localStorage，此处无需额外处理
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[销售换货单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  setQuickDate('week')
  fetchData()
})
</script>

<style scoped>
.page-header {
  background: #fff;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn) {
  font-size: 13px;
  padding: 2px 8px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #ff7a45;
  border-color: #ff7a45;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; margin-top: 0; }
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

/* ═══ 表格区域 ═══ */
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  padding: 12px 16px;
}

/* ═══ 紧凑尺寸 ═══ */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
