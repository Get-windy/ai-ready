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
            >{{ d.label }}</a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：操作按钮（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button>
            </a-tooltip>
            <a-button type="primary" size="small" v-if="fnEnabled('add')" @click="handleAdd"><PlusOutlined /> 新增</a-button>
            <a-button size="small" v-if="fnEnabled('refresh')" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
            <a-button size="small" v-if="fnEnabled('printF8')" @click="handlePrintF8"><PrinterOutlined /> 打印(F8)</a-button>
            <a-button size="small" v-if="fnEnabled('export')" @click="handleExport"><ExportOutlined /> 导出</a-button>
            <a-button size="small" v-if="fnEnabled('config')" @click="showPageConfig = true"><SettingOutlined /> 配置</a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid" ref="docGridRef">
                <div class="search-field-item"><a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" /></div>
                <div class="search-field-item"><a-input v-model:value="searchParams.adjustNo" placeholder="单据编号" allow-clear size="small" /></div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="searchParams.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">待审批</a-select-option>
                      <a-select-option :value="2">已审核</a-select-option>
                      <a-select-option :value="3">已记账</a-select-option>
                      <a-select-option :value="4">已拒绝</a-select-option>
                      <a-select-option :value="5">已取消</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item"><a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" /></div>
                <div class="search-field-item"><a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" /></div>
                <div class="search-field-item"><a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" /></div>
                <div class="search-field-item"><a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" /></div>
                <div v-if="activeTab === 'detail'" class="search-field-item"><a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" /></div>
                <div class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.showRed">显示红冲</a-checkbox>
                </div>
                <div class="search-action-group" :style="{ gridColumn: 'span ' + docActionSpan }">
                  <div class="search-field-item search-action-item">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
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
              :columns="currentColumns"
              :data-source="tableData"
              :storage-key="activeTab === 'bill' ? 'stock-cost-adjust-table-columns-bill' : 'stock-cost-adjust-table-columns-detail'"
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
              <!-- 单据编号 -->
              <template #adjustNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">{{ record.adjustNo }}</a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <!-- 调整金额（单据级） -->
              <template #totalAdjustAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAdjustAmount) }}</span>
              </template>
              <!-- 调整金额（明细级） -->
              <template #diffAmountCell="{ record }">
                <span :class="record.diffAmount > 0 ? 'positive' : record.diffAmount < 0 ? 'negative' : ''" class="currency-value">
                  {{ formatAmount(record.diffAmount) }}
                </span>
              </template>
              <!-- 调前/调后成本金额 -->
              <template #oldAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.oldAmount) }}</span>
              </template>
              <template #newAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.newAmount) }}</span>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleView(record)">修改</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmit(record)">提交</a-button>
                  <a-button v-if="record.status === 1" type="link" size="small" @click="handleApprove(record)">审核</a-button>
                  <a-button v-if="record.status === 1" type="link" size="small" @click="handleReject(record)">驳回</a-button>
                  <a-button v-if="record.status === 2" type="link" size="small" @click="handleExecute(record)">记账</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
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
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { stockCostAdjustApi } from '@/api/erp'
import { useRouter } from 'vue-router'

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'bill', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('bill')

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

const loading = ref(false)
const tableData = ref<any[]>([])

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)

const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

const searchParams = reactive<any>({
  adjustNo: '', handlerName: '', deptName: '', warehouseName: '', remark: '',
  productName: '',
  status: undefined as string | number | undefined,
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const showPageConfig = ref(false)

// ═══ 页面配置 ═══
const PAGE_CONFIG_STORAGE_KEY = 'stock-cost-adjust-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_BILL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'adjustNo', label: '单据编号', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'adjustNo', label: '单据编号', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
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

const billQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_BILL_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() => activeTab.value === 'bill' ? billQueryConfig.value : detailQueryConfig.value)
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)

function loadPageConfig() {
  try {
    const load = (tab: string, setter: (f: QueryFieldSetting[]) => void, defaultConfig: QueryFieldSetting[]) => {
      const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-' + tab)
      if (!raw) return
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.queryFields) {
        setter(defaultConfig.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        }))
      }
    }
    load('bill', v => { billQueryConfig.value = v }, DEFAULT_BILL_QUERY_FIELDS)
    load('detail', v => { detailQueryConfig.value = v }, DEFAULT_DETAIL_QUERY_FIELDS)
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons')
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.functionButtons) functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'bill' ? '-bill' : '-detail'
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({ queryFields: config.queryFields || [] }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({ functionButtons: functionButtonConfig.value }))
  loadPageConfig()
}

function fnEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(f => f.key === key)
  return found ? found.enabled : true
}

// ═══ 列定义 ═══
const billColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 210, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'adjustDate', key: 'adjustDate', width: 110, sortable: true },
  { title: '单据编号', field: 'adjustNo', key: 'adjustNo', width: 190, type: 'slot', slotName: 'adjustNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 130, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '调整金额', field: 'totalAdjustAmount', key: 'totalAdjustAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAdjustAmountCell', sortable: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 140 },
  { title: '摘要', field: 'summary', key: 'summary', width: 140 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 160 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 160 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'center' },
]

const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 100, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'adjustDate', key: 'adjustDate', width: 110, sortable: true },
  { title: '单据编号', field: 'adjustNo', key: 'adjustNo', width: 190, type: 'slot', slotName: 'adjustNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 130 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 110 },
  { title: '口味', field: 'taste', key: 'taste', width: 90 },
  { title: '型号', field: 'model', key: 'model', width: 90 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 120 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '产地', field: 'origin', key: 'origin', width: 90 },
  { title: '品牌', field: 'brand', key: 'brand', width: 90 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 80 },
  { title: '库存数量', field: 'currentQuantity', key: 'currentQuantity', width: 100, align: 'right', sortable: true },
  { title: '调前成本价', field: 'oldCost', key: 'oldCost', width: 100, align: 'right' },
  { title: '调前成本金额', field: 'oldAmount', key: 'oldAmount', width: 110, align: 'right', type: 'slot', slotName: 'oldAmountCell' },
  { title: '调后成本价', field: 'newCost', key: 'newCost', width: 100, align: 'right' },
  { title: '调后成本金额', field: 'newAmount', key: 'newAmount', width: 110, align: 'right', type: 'slot', slotName: 'newAmountCell' },
  { title: '调整金额', field: 'diffAmount', key: 'diffAmount', width: 110, align: 'right', type: 'slot', slotName: 'diffAmountCell' },
  { title: '明细备注', field: 'remark', key: 'remark', width: 130 },
  { title: '单据备注', field: 'docRemark', key: 'docRemark', width: 130 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 160 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 160 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'center' },
]

const currentColumns = computed(() => activeTab.value === 'bill' ? billColumns : detailColumns)

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'blue' },
  3: { text: '已记账', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' },
}
function getStatusText(status: number): string { return STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number): string { return STATUS_MAP[status]?.color || 'default' }

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value === 'bill') {
    const totalAmt = tableData.value.reduce((s: number, r: any) => s + (r.totalAdjustAmount || 0), 0)
    return [{ key: 'totalAdjustAmount', value: Number(totalAmt.toFixed(2)), highlight: true }]
  }
  const totalAmt = tableData.value.reduce((s: number, r: any) => s + (r.diffAmount || 0), 0)
  return [{ key: 'diffAmount', value: Number(totalAmt.toFixed(2)), highlight: true }]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.adjustNo) params.adjustNo = searchParams.adjustNo
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.showRed) params.showRed = true
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate
    if (activeTab.value === 'bill') {
      const res: any = await stockCostAdjustApi.page(params)
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    } else {
      if (searchParams.productName) params.productName = searchParams.productName
      const res: any = await stockCostAdjustApi.pageDetail(params)
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    }
  } catch (error: any) {
    console.warn('[成本调价] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

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
function handleAdd() {
  router.push('/erp/stock-cost-adjust/form')
}
function handleView(record: any) {
  router.push(`/erp/stock-cost-adjust/form?id=${record.id}`)
}
function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确认提交调价单 ${record.adjustNo} 进行审批吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.submit(record.id)
        message.success('提交成功')
        fetchData()
      } catch (error: any) {
        console.warn('[成本调价] 提交失败', error)
        message.error(error?.response?.data?.message || '提交失败')
      }
    },
  })
}
function handleApprove(record: any) {
  Modal.confirm({
    title: '审核确认',
    content: `确认审核通过调价单 ${record.adjustNo} 吗？`,
    okText: '确认通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.approve(record.id)
        message.success('审核通过')
        fetchData()
      } catch (error: any) {
        console.warn('[成本调价] 审核失败', error)
        message.error(error?.response?.data?.message || '审核失败')
      }
    },
  })
}
function handleReject(record: any) {
  Modal.confirm({
    title: '驳回确认',
    content: `确认驳回调价单 ${record.adjustNo} 吗？`,
    okText: '确认驳回',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.reject(record.id, '驳回')
        message.success('已驳回')
        fetchData()
      } catch (error: any) {
        console.warn('[成本调价] 驳回失败', error)
        message.error(error?.response?.data?.message || '驳回失败')
      }
    },
  })
}
function handleExecute(record: any) {
  Modal.confirm({
    title: '记账确认',
    content: `确认执行调价单 ${record.adjustNo} 的记账操作吗？记账后将调整库存成本单价。`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.execute(record.id)
        message.success('记账成功，库存成本已调整')
        fetchData()
      } catch (error: any) {
        console.warn('[成本调价] 记账失败', error)
        message.error(error?.response?.data?.message || '记账失败')
      }
    },
  })
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除调价单 ${record.adjustNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockCostAdjustApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        console.warn('[成本调价] 删除失败', error)
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

function handlePrintF8() {
  const sel = tableData.value.find((r: any) => r.id)
  router.push(`/erp/stock-cost-adjust/form?id=${sel?.id || ''}`)
}

async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    const res: any = activeTab.value === 'bill'
      ? await stockCostAdjustApi.page(params)
      : await stockCostAdjustApi.pageDetail(params)
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = activeTab.value === 'bill'
      ? ['单据日期', '单据编号', '单据状态', '仓库', '经手人', '部门', '调整金额', '单据备注', '摘要', '记账人', '制单人', '记账时间', '制单时间', '打印次数']
      : ['单据日期', '单据编号', '单据状态', '仓库', '经手人', '部门', '商品名称', '货号', '口味', '型号', '条码', '规格', '产地', '品牌', '单位', '库存数量', '调前成本价', '调前成本金额', '调后成本价', '调后成本金额', '调整金额', '明细备注', '单据备注', '记账人', '制单人', '记账时间', '制单时间', '打印次数']
    const rows = activeTab.value === 'bill'
      ? data.map((r: any) => [r.adjustDate, r.adjustNo, getStatusText(r.status), r.warehouseName, r.handlerName, r.deptName, r.totalAdjustAmount, r.remark, r.summary, r.bookkeeperName, r.creatorName, r.bookkeepingTime, r.createTime, r.printCount])
      : data.map((r: any) => [r.adjustDate, r.adjustNo, getStatusText(r.status), r.warehouseName, r.handlerName, r.deptName, r.productName, r.productCode, r.taste, r.model, r.barcode, r.productSpec, r.origin, r.brand, r.productUnit, r.currentQuantity, r.oldCost, r.oldAmount, r.newCost, r.newAmount, r.diffAmount, r.remark, r.docRemark, r.bookkeeperName, r.creatorName, r.bookkeepingTime, r.createTime, r.printCount])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `成本调价单_${activeTab.value === 'bill' ? '按单据' : '按明细'}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    console.warn('[成本调价] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[成本调价] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('lastWeek')
  fetchData()
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
.positive { color: #3f8600; font-weight: bold; }
.negative { color: #ff4d4f; font-weight: bold; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
