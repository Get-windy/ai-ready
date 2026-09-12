<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ 工具栏 ═══ -->
      <CategoryListLayout
        :tabs="[]"
        :active-tab="''"
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
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px">
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

        <!-- ═══ 工具栏右侧：页面配置/新增/刷新/打印/导出（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
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
              <div class="search-grid" ref="gridRef">
                <div class="search-field-item">
                  <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.docNo" placeholder="单据编号" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.partnerName" placeholder="结算单位" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="searchParams.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">已记账</a-select-option>
                      <a-select-option :value="2">已取消</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">调账类型</span>
                    <a-select v-model:value="searchParams.direction" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option :value="1">应收增加</a-select-option>
                      <a-select-option :value="2">应收减少</a-select-option>
                      <a-select-option :value="3">应付增加</a-select-option>
                      <a-select-option :value="4">应付减少</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                </div>
                <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
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
              :columns="baseColumns"
              :data-source="tableData"
              :storage-key="'ar-ap-adjust-table-columns'"
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
              <template #docNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.docNo }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template #directionCell="{ record }">
                <a-tag :color="getDirectionColor(record.direction)">{{ record.directionName || getDirectionText(record.direction) }}</a-tag>
              </template>
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <template v-if="record.status === 0">
                    <a-button type="link" size="small" @click="handleEdit(record)">修改</a-button>
                    <a-button type="link" size="small" @click="handlePost(record)">记账</a-button>
                    <a-dropdown>
                      <a-button type="link" size="small">更多 <DownOutlined /></a-button>
                      <template #overlay>
                        <a-menu @click="({ key }: any) => handleMoreAction(key, record)">
                          <a-menu-item key="cancel">取消</a-menu-item>
                          <a-menu-item key="delete">删除</a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </template>
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
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="'ar-ap-adjust-list-page-config'"
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
  ExportOutlined, DownOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { arApAdjustApi } from '@/api/finance/ar-ap-adjust'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'FinanceArApAdjustList' })

const router = useRouter()
const userStore = useUserStore()

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
const quickDate = ref('lastWeek')
const queryScheme = ref('')

// ═══ 状态/方向映射（应收应付调整：0草稿 1已记账 2已取消） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已记账', color: 'blue' },
  2: { text: '已取消', color: 'red' },
}
const DIRECTION_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '应收增加', color: 'green' },
  2: { text: '应收减少', color: 'volcano' },
  3: { text: '应付增加', color: 'cyan' },
  4: { text: '应付减少', color: 'purple' },
}
function getStatusText(status: number): string { return STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number): string { return STATUS_MAP[status]?.color || 'default' }
function getDirectionText(direction: number): string { return DIRECTION_MAP[direction]?.text || '未知' }
function getDirectionColor(direction: number): string { return DIRECTION_MAP[direction]?.color || 'default' }

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  docNo: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  remark: '',
  status: undefined as string | number | undefined,
  direction: undefined as string | number | undefined,
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

// ═══ 配置弹窗（列配置走数据表表头齿轮，storage-key=ar-ap-adjust-table-columns） ═══
const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'ar-ap-adjust-list-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// 默认显示：日期/单据编号/结算单位/经手人/单据状态/显示红冲；默认隐藏：部门/制单人/记账人/调账类型/单据备注
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'partnerName', label: '结算单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
  { key: 'deptName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'direction', label: '调账类型', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map((f, i) => ({ ...f, order: i })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.queryFields) {
        queryFieldsConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
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
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

// ═══ 列定义（对标文档：16数据列 + 行勾选框 + 操作列；默认显示8列） ═══
const baseColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'docDate', key: 'docDate', width: 110, sortable: true },
  { title: '单据编号', field: 'docNo', key: 'docNo', width: 175, type: 'slot', slotName: 'docNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '结算单位', field: 'partnerName', key: 'partnerName', width: 200, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '调账类型', field: 'direction', key: 'direction', width: 110, align: 'center', type: 'slot', slotName: 'directionCell' },
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 130, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true, defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 140, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140, defaultHidden: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140, defaultHidden: true },
]

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.docNo) params.docNo = searchParams.docNo
    if (searchParams.partnerName) params.partnerName = searchParams.partnerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.direction !== undefined && searchParams.direction !== '') params.direction = searchParams.direction
    params.showRed = searchParams.showRed === true
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate

    const res: any = await arApAdjustApi.docQuery(params)
    const body = (res as any)?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[应收应付调整] 获取列表失败', error)
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
  return { id: userStore?.userId || userStore?.userInfo?.id || 0, name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统' }
}

function handleAdd() {
  router.push('/finance/ar-ap-adjust/form')
}
function handleView(record: any) {
  router.push(`/finance/ar-ap-adjust/form?id=${record.id}`)
}
function handleMoreAction(key: string, record: any) {
  switch (key) {
    case 'edit': handleEdit(record); break
    case 'post': handlePost(record); break
    case 'cancel': handleCancel(record); break
    case 'delete': handleDelete(record); break
  }
}
function handleEdit(record: any) {
  router.push(`/finance/ar-ap-adjust/form?id=${record.id}`)
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除调整单 ${record.docNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await arApAdjustApi.remove(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}
function handlePost(record: any) {
  Modal.confirm({
    title: '记账确认',
    content: `确定对调整单 ${record.docNo} 执行记账吗？记账后生成会计凭证并调整应收/应付余额。`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        const u = currentOperator()
        await arApAdjustApi.confirm(record.id, u.id, u.name)
        message.success('记账完成，已生成会计凭证')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '记账失败')
      }
    },
  })
}
function handleCancel(record: any) {
  Modal.confirm({
    title: '取消确认',
    content: `确定取消调整单 ${record.docNo} 吗？`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await arApAdjustApi.cancel(record.id)
        message.success('取消成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '取消失败')
      }
    },
  })
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的调整单')
    return
  }
  router.push(`/finance/ar-ap-adjust/form?id=${selectedRowKeys.value[0]}`)
}
async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.docNo) params.docNo = searchParams.docNo
    if (searchParams.partnerName) params.partnerName = searchParams.partnerName
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate
    const res: any = await arApAdjustApi.docQuery(params)
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '单据状态', '结算单位', '经手人', '调账类型', '金额', '打印次数']
    const rows = data.map((r: any) => [
      r.docNo, r.docDate, getStatusText(r.status), r.partnerName, r.handlerName,
      r.directionName || getDirectionText(r.direction), r.totalAmount, r.printCount,
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `应收应付调整_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[应收应付调整] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('lastWeek')
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
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
