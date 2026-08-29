<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <DocCenterLayout
        v-model:active-main-tab="mainTab"
        v-model:active-sub-tab="subTab"
        v-model:date-shortcut="dateShortcut"
        v-model:date-range="dateRange"
        v-model:search-values="searchForm"
        v-model:page-current="pagination.current"
        v-model:page-size="pagination.pageSize"
        :main-tabs="mainTabs"
        :sub-tabs="subTabs"
        :search-config="searchConfig"
        :search-checkbox-config="searchCheckboxConfig"
        :hidden-field-keys="hiddenSearchFieldKeys"
        :stat-card-config="statCardConfig"
        :toolbar-config="toolbarConfig"
        :show-pagination="true"
        :page-total="pagination.total"
        :stats-data="stats"
        @search="handleSearch"
        @refresh="fetchData"
        @toolbar-action="handleToolbarAction"
        @page-change="handlePageChange"
      >
        <template #table>
          <BillDetailTable
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :storage-key="storageKey"
            style="height: 100%"
            @checkbox-change="handleRowCheck"
            @checkbox-all="handleRowCheckAll"
          >
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="goDetail(record)">查看</a-button>
                <a-button v-if="record.status === 0 || record.status === '0'" type="link" size="small" @click="goEdit(record)">编辑</a-button>
              </a-space>
            </template>
            <template #sharingNoCell="{ record }">
              <a-button type="link" size="small" @click="goDetail(record)">{{ record.sharingNo }}</a-button>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗 -->
      <PageConfigPanel
        :key="'purchase-cost-sharing-page-config'"
        :open="showPageConfig"
        :storage-key="'purchase-cost-sharing-page-config'"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import DocCenterLayout from '@/components/DocCenterLayout/DocCenterLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { userPageConfigApi, costSharingApi } from '@/api/erp'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  SearchConfigMap, SearchCheckboxConfigMap,
  StatCardConfigMap, ToolbarConfigMap,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'PurchaseCostSharingList' })
const router = useRouter()

// ═══ Tab 状态（单一页面） ═══
const mainTab = ref('all')
const subTab = ref('list')
const dateShortcut = ref('month')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])

const mainTabs = [{ key: 'all', label: '全部' }]
const subTabs = [{ key: 'list', label: '分摊单' }]

// ═══ 搜索 ═══
const searchForm = reactive<Record<string, any>>({})

// 搜索字段（日期由 DocCenterLayout 固定显示在第一格）
const listSearchFields: SearchConfigMap = {
  'all.list': [
    { key: 'sharingNo', label: '单据编号', type: 'input' },
    { key: 'handlerName', label: '经手人', type: 'input' },
    { key: 'departmentName', label: '部门', type: 'input' },
    { key: 'createByName', label: '制单人', type: 'input' },
    { key: 'bookkeeperName', label: '记账人', type: 'input' },
    { key: 'remark', label: '单据备注', type: 'input' },
  ],
}

const searchConfig: SearchConfigMap = listSearchFields

// 搜索复选框配置
const searchCheckboxConfig: SearchCheckboxConfigMap = {
  'all.list': [
    { key: 'showRed', label: '显示红冲' },
  ],
}

// 搜索字段隐藏配置
const hiddenSearchFieldKeys = ref<string[]>([])

// ═══ 统计卡片 ═══
const stats = ref<Record<string, number>>({ draft: 0, completed: 0, cancelled: 0, totalAmount: 0 })

const statCardConfig: StatCardConfigMap = {
  'all.list': [
    { label: '草稿', valueKey: 'draft', color: '#d9d9d9' },
    { label: '已完成', valueKey: 'completed', color: '#52c41a' },
    { label: '已取消', valueKey: 'cancelled', color: '#ff4d4f' },
    { label: '本单金额', valueKey: 'totalAmount', color: '#722ed1' },
  ],
}

// ═══ 工具栏（新增、刷新、导出、配置） ═══
const toolbarConfig: ToolbarConfigMap = {
  'all.list': [
    { key: 'add', label: '新增', type: 'primary' },
    { key: 'refresh', label: '刷新' },
    { key: 'export', label: '导出' },
    { key: 'pageConfig', label: '配置', icon: 'SettingsOutlined', visibleFor: () => true },
  ],
}

// ═══ 表格列（13列 + 序号/选择框/操作） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { key: 'sharingDate', title: '单据日期', width: 110 },
  { key: 'sharingNo', title: '单据编号', width: 150, slotName: 'sharingNoCell' },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'accountTime', title: '记账时间', width: 130 },
  { key: 'createTime', title: '制单时间', width: 130 },
  { key: 'totalAmount', title: '本单金额', width: 110, align: 'right' },
  { key: 'handlerName', title: '经手人', width: 90 },
  { key: 'departmentName', title: '部门', width: 100 },
  { key: 'createByName', title: '制单人', width: 90 },
  { key: 'bookkeeperName', title: '记账人', width: 90 },
  { key: 'summary', title: '摘要', width: 130 },
  { key: 'remark', title: '单据备注', width: 150 },
  { key: 'attachment', title: '附件', width: 70 },
  { key: 'action', title: '操作', width: 130, fixed: 'right', slotName: 'actionCell' },
]

const storageKey = 'purchase-cost-sharing-list-columns'

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 数据加载 ═══
const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchForm.sharingNo) params.sharingNo = searchForm.sharingNo
    if (searchForm.handlerName) params.handlerName = searchForm.handlerName
    if (searchForm.departmentName) params.departmentName = searchForm.departmentName
    if (searchForm.createByName) params.createByName = searchForm.createByName
    if (searchForm.bookkeeperName) params.bookkeeperName = searchForm.bookkeeperName
    if (searchForm.remark) params.remark = searchForm.remark
    if (dateRange.value?.[0]) params.startDate = dateRange.value[0].format('YYYY-MM-DD')
    if (dateRange.value?.[1]) params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })

    const res: any = await costSharingApi.page(params)
    const data = res?.data || res
    tableData.value = data?.records || []
    pagination.total = Number(data?.total) || 0

    // 统计
    stats.value.draft = tableData.value.filter((r: any) => Number(r.status) === 0).length
    stats.value.completed = tableData.value.filter((r: any) => Number(r.status) === 1).length
    stats.value.cancelled = tableData.value.filter((r: any) => Number(r.status) === 2).length
    stats.value.totalAmount = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalAmount) || 0), 0)
  } catch (error) {
    console.warn('[采购费用分摊] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

// ═══ 行选择 ═══
const selectedRowKeys = ref<any[]>([])
function handleRowCheck(record: any, index: number, checked: boolean) {
  const key = record.id
  if (checked) { if (!selectedRowKeys.value.includes(key)) selectedRowKeys.value.push(key) }
  else { selectedRowKeys.value = selectedRowKeys.value.filter((k: any) => k !== key) }
}
function handleRowCheckAll(checked: boolean, records: any[]) {
  selectedRowKeys.value = checked ? records.map((r: any) => r.id) : []
}

// ═══ 页面配置（查询条件显隐 + 功能按钮） ═══
const PAGE_CONFIG_MODULE = 'purchase-cost-sharing'
const PAGE_CONFIG_PAGE = 'list'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = listSearchFields['all.list'].map(f => ({ key: f.key, label: f.label, visible: true }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = toolbarConfig['all.list'].map(b => ({ key: b.key, label: b.label, enabled: true }))

const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

async function loadPageConfig() {
  try {
    const raw = await userPageConfigApi.get(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE)
    if (raw) {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
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
      localStorage.setItem('purchase-cost-sharing-page-config', JSON.stringify({
        queryFields: queryFieldsConfig.value,
        functionButtons: functionButtonConfig.value,
      }))
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function handlePageConfigChange(config: any) {
  const payload = {
    queryFields: config.queryFields || queryFieldsConfig.value,
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }
  try { await userPageConfigApi.save(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE, JSON.stringify(payload)) } catch { /* 静默失败 */ }
  await loadPageConfig()
  applyHiddenSearchFields()
}

function applyHiddenSearchFields() {
  hiddenSearchFieldKeys.value = queryFieldsConfig.value.filter(f => !f.visible).map(f => f.key)
}

// ═══ 工具栏操作 ═══
const showPageConfig = ref(false)

const handleToolbarAction = (action: string) => {
  switch (action) {
    case 'add': router.push('/purchase/cost-sharing/form'); break
    case 'refresh': fetchData(); break
    case 'export': handleExport(); break
    case 'pageConfig': showPageConfig.value = true; break
  }
}

async function handleExport() {
  try {
    const res: any = await costSharingApi.page({
      pageNum: 1,
      pageSize: pagination.total || 9999,
      ...searchForm,
    })
    const list = res?.data?.records || res?.records || []
    if (!list.length) { message.info('暂无数据可导出'); return }
    downloadCsv(list)
  } catch { message.error('导出失败') }
}

function downloadCsv(list: any[]) {
  const header = ['单据日期', '单据编号', '单据状态', '记账时间', '制单时间', '本单金额', '经手人', '部门', '制单人', '记账人', '摘要', '单据备注', '附件']
  const rows = list.map((r: any) => [
    r.sharingDate, r.sharingNo, getStatusText(r.status), r.accountTime, r.createTime,
    r.totalAmount, r.handlerName, r.departmentName, r.createByName, r.bookkeeperName,
    r.summary, r.remark, r.attachment,
  ])
  const csv = [header, ...rows].map(row => row.map(v => `"${v ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = '采购费用分摊.csv'; a.click()
  URL.revokeObjectURL(url)
}

// ═══ 行操作 ═══
const goDetail = (record: any) => { router.push(`/purchase/cost-sharing/form?id=${record.id}`) }
const goEdit = (record: any) => { router.push(`/purchase/cost-sharing/form?id=${record.id}`) }

// ═══ 状态辅助 ═══
const getStatusColor = (status: any) => {
  const map: Record<number, string> = { 0: 'default', 1: 'green', 2: 'red' }
  return map[Number(status)] || 'default'
}
const getStatusText = (status: any) => {
  const map: Record<number, string> = { 0: '草稿', 1: '已完成', 2: '已取消' }
  return map[Number(status)] || '未知'
}

const handleError = (err: any) => { console.warn('[采购费用分摊] ErrorBoundary:', err) }

onMounted(async () => {
  await loadPageConfig()
  applyHiddenSearchFields()
  fetchData()
})
</script>
