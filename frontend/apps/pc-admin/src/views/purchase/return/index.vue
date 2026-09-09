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
            v-model:data-source="tableData"
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
                <a-button v-if="record.status === 0" type="link" size="small" @click="goEdit(record)">编辑</a-button>
                <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmit(record)">提交</a-button>
              </a-space>
            </template>
            <template #returnNoCell="{ record }">
              <a-button type="link" size="small" @click="goDetail(record)">{{ record.returnNo }}</a-button>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ record.statusDesc || getStatusText(record.status) }}</a-tag>
            </template>
            <template #settleStatusCell="{ record }">
              <a-tag :color="record.settleStatus === 1 ? 'green' : 'default'">{{ record.settleStatus === 1 ? '已结算' : '未结算' }}</a-tag>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗 -->
      <PageConfigPanel
        :key="'purchase-return-page-config'"
        :open="showPageConfig"
        :storage-key="'purchase-return-page-config'"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- 批量打印弹窗 -->
      <a-modal
        v-model:open="showPrintDialog"
        title="批量打印"
        :width="400"
        @ok="confirmPrint"
      >
        <div style="padding: 16px 0;">
          <div style="margin-bottom: 8px;">打印模板：</div>
          <a-select v-model:value="printTemplate" style="width: 100%;">
            <a-select-option value="default">标准模板</a-select-option>
            <a-select-option value="simple">简化模板</a-select-option>
            <a-select-option value="detailed">详细模板</a-select-option>
          </a-select>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import DocCenterLayout from '@/components/DocCenterLayout/DocCenterLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { userPageConfigApi } from '@/api/erp'
import request from '@/utils/request'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  SearchConfigMap, SearchCheckboxConfigMap,
  StatCardConfigMap, ToolbarConfigMap,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'PurchaseReturnList' })
const router = useRouter()

// ═══ Tab 状态（单一页面） ═══
const mainTab = ref('all')
const subTab = ref('list')
const dateShortcut = ref('month')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])

const mainTabs = [{ key: 'all', label: '全部' }]
const subTabs = [{ key: 'list', label: '采购退货单' }]

// ═══ 搜索 ═══
const searchForm = reactive<Record<string, any>>({})

// 搜索字段（文档17个，日期由 DocCenterLayout 固定显示在第一格）
const listSearchFields: SearchConfigMap = {
  'all.list': [
    { key: 'returnNo', label: '单据编号', type: 'input' },
    { key: 'supplierName', label: '供应商', type: 'input' },
    { key: 'purchaserName', label: '经手人', type: 'input' },
    { key: 'departmentName', label: '部门', type: 'input' },
    { key: 'createByName', label: '制单人', type: 'input' },
    { key: 'posterName', label: '记账人', type: 'input' },
    { key: 'warehouseName', label: '仓库', type: 'input' },
    { key: 'status', label: '单据状态', type: 'select', options: [
      { label: '草稿', value: 0 }, { label: '待审批', value: 1 },
      { label: '已审批', value: 2 }, { label: '已驳回', value: 3 },
      { label: '已完成', value: 4 }, { label: '已取消', value: 5 },
    ]},
    { key: 'settleStatus', label: '结算状态', type: 'select', options: [
      { label: '未结算', value: 0 }, { label: '已结算', value: 1 },
    ]},
    { key: 'remark', label: '单据备注', type: 'input' },
    { key: 'extNum1', label: '自定义字段1(数字)', type: 'input' },
    { key: 'extNum2', label: '自定义字段2(数字)', type: 'input' },
    { key: 'extText1', label: '自定义字段3(文本)', type: 'input' },
    { key: 'extText2', label: '自定义字段4(文本)', type: 'input' },
    { key: 'extText3', label: '自定义字段5(文本)', type: 'input' },
  ],
}

const searchConfig: SearchConfigMap = listSearchFields

// 搜索复选框配置
const searchCheckboxConfig: SearchCheckboxConfigMap = {
  'all.list': [
    { key: 'showRed', label: '显示红冲' },
    { key: 'showSelected', label: '仅显示已选中' },
  ],
}

// 搜索字段隐藏配置
const hiddenSearchFieldKeys = ref<string[]>([])

// ═══ 统计卡片 ═══
const stats = ref<Record<string, number>>({ draft: 0, pending: 0, approved: 0, completed: 0, totalAmount: 0 })

const statCardConfig: StatCardConfigMap = {
  'all.list': [
    { label: '草稿', valueKey: 'draft', color: '#d9d9d9' },
    { label: '待审批', valueKey: 'pending', color: '#faad14' },
    { label: '已审批', valueKey: 'approved', color: '#1890ff' },
    { label: '已完成', valueKey: 'completed', color: '#52c41a' },
    { label: '退货金额', valueKey: 'totalAmount', color: '#722ed1' },
  ],
}

// ═══ 工具栏（6个，默认全选） ═══
const toolbarConfig: ToolbarConfigMap = {
  'all.list': [
    { key: 'add', label: '新增', type: 'primary' },
    { key: 'refresh', label: '刷新' },
    { key: 'batchPrint', label: '批量打印' },
    { key: 'printF8', label: '打印(F8)' },
    { key: 'export', label: '导出' },
    { key: 'pageConfig', label: '配置', icon: 'SettingsOutlined', visibleFor: () => true },
  ],
}

// ═══ 表格列（文档35列 + 序号/选择框/操作） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { key: 'returnDate', title: '单据日期', width: 110 },
  { key: 'returnNo', title: '单据编号', width: 150, slotName: 'returnNoCell' },
  { key: 'purchaseOrderNo', title: '源单', width: 150 },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'supplierNo', title: '供应商编号', width: 110 },
  { key: 'supplierName', title: '供应商', width: 180 },
  { key: 'contactName', title: '联系人', width: 90 },
  { key: 'contactPhone', title: '联系电话', width: 120 },
  { key: 'contactAddress', title: '联系地址', width: 150 },
  { key: 'supplierRemark', title: '供应商备注', width: 150 },
  { key: 'purchaserName', title: '经手人', width: 90 },
  { key: 'departmentName', title: '部门', width: 100 },
  { key: 'totalQuantity', title: '退货数量', width: 100, align: 'right' },
  { key: 'totalAmount', title: '金额', width: 110, align: 'right' },
  { key: 'discountAmount', title: '折后金额', width: 100, align: 'right' },
  { key: 'taxAmount', title: '税额', width: 90, align: 'right' },
  { key: 'totalAmountWithTax', title: '本单金额', width: 110, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 110, align: 'right' },
  { key: 'settleStatus', title: '结算状态', width: 90, slotName: 'settleStatusCell' },
  { key: 'weight', title: '重量(kg)', width: 90, align: 'right' },
  { key: 'volume', title: '体积(m³)', width: 90, align: 'right' },
  { key: 'remark', title: '单据备注', width: 150 },
  { key: 'extNum1', title: '自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '自定义字段5(文本)', width: 120 },
  { key: 'summary', title: '摘要', width: 130 },
  { key: 'attachment', title: '附件', width: 70 },
  { key: 'posterName', title: '记账人', width: 90 },
  { key: 'createByName', title: '制单人', width: 90 },
  { key: 'postTime', title: '记账时间', width: 130 },
  { key: 'createTime', title: '制单时间', width: 130 },
  { key: 'printCount', title: '打印次数', width: 90 },
  { key: 'action', title: '操作', width: 130, fixed: 'right', slotName: 'actionCell' },
]

const storageKey = 'purchase-return-list-columns'

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
    // 文本类查询条件合并为 keyword，核心字段精确映射
    const keywordParts: string[] = []
    const pushKeyword = (v: any) => { if (v !== undefined && v !== null && v !== '') keywordParts.push(String(v)) }
    ;['returnNo', 'supplierName', 'supplierNo', 'purchaserName', 'departmentName', 'createByName',
      'posterName', 'warehouseName', 'remark', 'extText1', 'extText2', 'extText3'].forEach(k => pushKeyword(searchForm[k]))
    if (keywordParts.length) params.keyword = keywordParts.join(' ')
    if (searchForm.status !== undefined && searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.settleStatus !== undefined && searchForm.settleStatus !== null && searchForm.settleStatus !== '') params.settleStatus = searchForm.settleStatus
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })

    const res: any = await request.get('/erp/purchase/return/page', { params })
    const data = res?.data || res
    tableData.value = data?.records || []
    pagination.total = Number(data?.total) || 0

    // 更新统计（基于当前页近似）
    stats.value.draft = tableData.value.filter((r: any) => r.status === 0).length
    stats.value.pending = tableData.value.filter((r: any) => r.status === 1).length
    stats.value.approved = tableData.value.filter((r: any) => r.status === 2).length
    stats.value.completed = tableData.value.filter((r: any) => r.status === 4).length
    stats.value.totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.totalAmountWithTax || 0), 0)
  } catch (error) {
    console.warn('[采购退货] 获取数据失败', error)
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
const PAGE_CONFIG_MODULE = 'purchase-return'
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
      localStorage.setItem('purchase-return-page-config', JSON.stringify({
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
const showPrintDialog = ref(false)
const printTemplate = ref('default')

const handleToolbarAction = (action: string) => {
  switch (action) {
    case 'add': router.push('/purchase/return/form'); break
    case 'refresh': fetchData(); break
    case 'batchPrint': handleBatchPrint(); break
    case 'printF8': handleBatchPrint(); break
    case 'export': handleExport(); break
    case 'pageConfig': showPageConfig.value = true; break
  }
}

function handleBatchPrint() {
  printTemplate.value = localStorage.getItem('purchase-return-last-print-template') || 'default'
  showPrintDialog.value = true
}

async function confirmPrint() {
  try {
    await request.post('/erp/purchase/return/batch-print', { template: printTemplate.value, ids: selectedRowKeys.value })
    localStorage.setItem('purchase-return-last-print-template', printTemplate.value)
    message.success(`已发送打印（模板: ${printTemplate.value}）`)
    showPrintDialog.value = false
    fetchData()
  } catch { message.error('打印失败') }
}

async function handleExport() {
  try {
    const res: any = await request.get('/erp/purchase/return/export', { params: {} })
    message.success(`已导出 ${res?.data?.length || 0} 条`)
  } catch { message.error('导出失败') }
}

// ═══ 行操作 ═══
const goDetail = (record: any) => { router.push(`/purchase/return/form?id=${record.id}`) }
const goEdit = (record: any) => { router.push(`/purchase/return/form?id=${record.id}`) }

const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '提交审批', content: `确认提交退货单 ${record.returnNo} 进行审批吗？`,
    okText: '确认提交', cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/return/${record.id}/submit`)
        message.success('已提交审批'); fetchData()
      } catch { message.error('提交失败') }
    },
  })
}

// ═══ 状态辅助 ═══
const getStatusColor = (status: number) => {
  const map: Record<number, string> = {
    0: 'default', 1: 'orange', 2: 'blue', 3: 'red', 4: 'green', 5: 'red',
  }
  return map[status] || 'default'
}
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '草稿', 1: '待审批', 2: '已审批', 3: '已驳回', 4: '已完成', 5: '已取消',
  }
  return map[status] || '未知'
}

const handleError = (err: any) => { console.warn('[采购退货] ErrorBoundary:', err) }

onMounted(async () => {
  await loadPageConfig()
  applyHiddenSearchFields()
  fetchData()
})
</script>
