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
        :function-buttons-config="functionButtonConfig"
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
                <a-button type="link" size="small" @click="goQuality(record)">生成质检单</a-button>
              </a-space>
            </template>
            <template #inboundNoCell="{ record }">
              <a-button type="link" size="small" @click="goDetail(record)">{{ record.inboundNo }}</a-button>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗 -->
      <PageConfigPanel
        :key="'purchase-inbound-page-config'"
        :open="showPageConfig"
        :storage-key="'purchase-inbound-page-config'"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- 打印（模板渲染）：无已发布模板时组件会明确提示，不再假装成功 -->
      <PrintDialog
        ref="printDialogRef"
        page-code="purchase-inbound"
        :document-id="printData.id"
        :print-data="printData"
        @print-success="handlePrintSuccess"
      />

      <!-- 批量导入隐藏文件输入 -->
      <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImportFileChange" />
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
import PrintDialog from '@/components/PrintDialog/index.vue'
import { userPageConfigApi } from '@/api/erp'
import request from '@/utils/request'
import { showImportResult } from '@/utils/importResult'
import { exportCsvFromColumns } from '@/utils/exportCsv'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  SearchConfigMap, SearchCheckboxConfigMap,
  StatCardConfigMap, ToolbarConfigMap,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'PurchaseInboundList' })
const router = useRouter()

function goQuality(record: any) {
  router.push({
    path: '/quality/inspection/form',
    query: {
      bizType: 'PURCHASE_ORDER',
      bizId: record.orderId || record.id,
      bizNo: record.orderNo || record.inboundNo,
    },
  })
}

// ═══ Tab 状态（单一页面） ═══
const mainTab = ref('all')
const subTab = ref('list')
const dateShortcut = ref('month')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])

const mainTabs = [{ key: 'all', label: '全部' }]
const subTabs = [{ key: 'list', label: '入库单' }]

// ═══ 搜索 ═══
const searchForm = reactive<Record<string, any>>({})

// 搜索字段（文档21个，日期由 DocCenterLayout 固定显示在第一格）
const listSearchFields: SearchConfigMap = {
  'all.list': [
    { key: 'inboundNo', label: '单据编号', type: 'input' },
    { key: 'supplierName', label: '供应商', type: 'input' },
    { key: 'purchaserName', label: '经手人', type: 'input' },
    { key: 'departmentName', label: '部门', type: 'input' },
    { key: 'createByName', label: '制单人', type: 'input' },
    { key: 'posterName', label: '记账人', type: 'input' },
    { key: 'approvedByName', label: '审核人', type: 'input' },
    { key: 'warehouseName', label: '仓库', type: 'input' },
    { key: 'orderNo', label: '来源订单编号', type: 'input' },
    { key: 'status', label: '单据状态', type: 'select', options: [
      { label: '草稿', value: 0 }, { label: '待审批', value: 1 },
      { label: '已审批', value: 2 }, { label: '待收货', value: 3 },
      { label: '已收货', value: 4 }, { label: '待质检', value: 5 },
      { label: '已质检', value: 6 }, { label: '待入库', value: 7 },
      { label: '已入库', value: 8 }, { label: '已完成', value: 9 },
      { label: '已取消', value: 10 },
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
    { key: 'printCount', label: '打印次数', type: 'input' },
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
    { label: '入库金额', valueKey: 'totalAmount', color: '#722ed1' },
  ],
}

// ═══ 工具栏（7个，默认全选） ═══
const toolbarConfig: ToolbarConfigMap = {
  'all.list': [
    { key: 'add', label: '新增', type: 'primary' },
    { key: 'refresh', label: '刷新' },
    { key: 'batchPrint', label: '批量打印' },
    { key: 'printF8', label: '打印(F8)' },
    { key: 'batchImport', label: '批量导入' },
    { key: 'export', label: '导出' },
    { key: 'pageConfig', label: '配置', icon: 'SettingsOutlined', visibleFor: () => true },
  ],
}

// ═══ 表格列（文档31列 + 序号/选择框/操作） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { key: 'inboundDate', title: '单据日期', width: 110 },
  { key: 'inboundNo', title: '单据编号', width: 150, slotName: 'inboundNoCell' },
  { key: 'orderNo', title: '来源订单', width: 150 },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'settleStatus', title: '结算状态', width: 90 },
  { key: 'settledAmount', title: '已结金额', width: 110, align: 'right' },
  { key: 'warehouseName', title: '入库仓库', width: 120 },
  { key: 'supplierName', title: '供应商', width: 180 },
  { key: 'totalQuantity', title: '总商品数量', width: 100, align: 'right' },
  { key: 'totalAmount', title: '总商品金额', width: 110, align: 'right' },
  { key: 'discountAmount', title: '优惠金额', width: 100, align: 'right' },
  { key: 'fee', title: '费用', width: 100, align: 'right' },
  { key: 'totalAmountWithTax', title: '本单金额', width: 110, align: 'right' },
  { key: 'weight', title: '重量(kg)', width: 90, align: 'right' },
  { key: 'volume', title: '体积(m³)', width: 90, align: 'right' },
  { key: 'purchaserName', title: '经手人', width: 90 },
  { key: 'departmentName', title: '部门', width: 100 },
  { key: 'summary', title: '摘要', width: 130 },
  { key: 'extNum1', title: '自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '自定义字段5(文本)', width: 120 },
  { key: 'createByName', title: '制单人', width: 90 },
  { key: 'posterName', title: '记账人', width: 90 },
  { key: 'approvedByName', title: '审核人', width: 90 },
  { key: 'remark', title: '单据备注', width: 150 },
  { key: 'attachment', title: '附件', width: 70 },
  { key: 'printCount', title: '打印次数', width: 90 },
  { key: 'posterTime', title: '记账时间', width: 130 },
  { key: 'createTime', title: '制单时间', width: 130 },
  { key: 'action', title: '操作', width: 130, fixed: 'right', slotName: 'actionCell' },
]

const storageKey = 'purchase-inbound-list-columns'

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
    // 把搜索表单各字段直传给后端分页接口（对齐文档21个查询条件）
    const textFields: Record<string, string> = {
      inboundNo: 'inboundNo', supplierName: 'supplierName', orderNo: 'orderNo',
      purchaserName: 'purchaserName', departmentName: 'departmentName',
      createByName: 'createByName', posterName: 'posterName', approvedByName: 'approvedByName',
      warehouseName: 'warehouseName', remark: 'remark',
      extText1: 'extText1', extText2: 'extText2', extText3: 'extText3',
    }
    Object.entries(textFields).forEach(([key, param]) => {
      if (searchForm[key] !== undefined && searchForm[key] !== null && searchForm[key] !== '') params[param] = searchForm[key]
    })
    if (searchForm.status !== undefined && searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.settleStatus !== undefined && searchForm.settleStatus !== null && searchForm.settleStatus !== '') params.settleStatus = searchForm.settleStatus
    if (searchForm.extNum1 !== undefined && searchForm.extNum1 !== null && searchForm.extNum1 !== '') params.extNum1 = searchForm.extNum1
    if (searchForm.extNum2 !== undefined && searchForm.extNum2 !== null && searchForm.extNum2 !== '') params.extNum2 = searchForm.extNum2
    if (searchForm.printCount !== undefined && searchForm.printCount !== null && searchForm.printCount !== '') params.printCount = searchForm.printCount
    if (dateRange.value) {
      const [from, to] = dateRange.value
      if (from) params.startDate = from.format('YYYY-MM-DD')
      if (to) params.endDate = to.format('YYYY-MM-DD')
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })

    const res: any = await request.get('/erp/purchase/inbound/page', { params })
    const data = res?.data || res
    tableData.value = data?.records || []
    pagination.total = Number(data?.total) || 0

    // 更新统计（基于当前页近似）
    stats.value.draft = tableData.value.filter((r: any) => r.status === 0).length
    stats.value.pending = tableData.value.filter((r: any) => r.status === 1).length
    stats.value.approved = tableData.value.filter((r: any) => r.status === 2).length
    stats.value.completed = tableData.value.filter((r: any) => r.status === 9).length
    stats.value.totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.totalAmountWithTax || 0), 0)
  } catch (error) {
    console.warn('[采购入库单] 获取数据失败', error)
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
const PAGE_CONFIG_MODULE = 'purchase-inbound'
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
      localStorage.setItem('purchase-inbound-page-config', JSON.stringify({
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
const fileInputRef = ref<HTMLInputElement | null>(null)
const importLoading = ref(false)

const handleToolbarAction = (action: string) => {
  switch (action) {
    case 'add': router.push('/purchase/inbound/form'); break
    case 'refresh': fetchData(); break
    case 'batchImport': fileInputRef.value?.click(); break
    case 'batchPrint': handleBatchPrint(); break
    case 'printF8': handleBatchPrint(); break
    case 'export': handleExport(); break
    case 'pageConfig': showPageConfig.value = true; break
  }
}

// ═══ 打印（模板渲染）═══
// 收敛到共享 PrintDialog：模板/渲染由 erp-printing 负责，本页只给单据主键。
// pageCode='purchase-inbound' 目前后端**尚未**注册 PrintDataProvider、也没有已发布模板，
// 此时组件会明确提示「还没有已发布的打印模板」并禁用打印 —— 不再像原先那样
// 自写一个假弹窗、点了只把打印次数 +1（2026-09-22 审计 P1）。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
/** 本次打印涉及的单据 ID，打印成功后回写打印次数 */
const printingIds = ref<string[]>([])

function handleBatchPrint() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要打印的入库单')
    return
  }
  printData.value = { id: selectedRowKeys.value[0] }
  printingIds.value = selectedRowKeys.value.map((k: any) => String(k))
  printDialogRef.value?.open()
}

/** 打印成功 → 累加「打印次数」后刷新列表 */
async function handlePrintSuccess() {
  const ids = printingIds.value
  printingIds.value = []
  if (ids.length) {
    try {
      await request.post('/erp/purchase/inbound/batch-print', { ids })
    } catch (e) {
      console.warn('[采购入库单] 打印次数回写失败', e)
    }
  }
  fetchData()
}

async function handleImportFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  importLoading.value = true
  try {
    const res: any = await request.post('/erp/purchase/inbound/import', fd)
    showImportResult(res?.data)
    fetchData()
  } catch { message.error('导入失败，请检查文件格式') }
  finally { importLoading.value = false; input.value = '' }
}

async function handleExport() {
  try {
    const res: any = await request.get('/erp/purchase/inbound/export', { params: {} })
    const list: any[] = res?.data || res || []
    if (!list.length) { message.warning('没有可导出的数据'); return }
    // 原实现只弹提示、不产出文件（点了没有任何下载）；现在按当前列配置真实导出 CSV
    const count = exportCsvFromColumns(columns as any, list, '采购入库单', {
      status: (r: any) => r.statusDesc || getStatusText(r.status),
      settleStatus: (r: any) => r.settleStatusDesc || r.settleStatus,
    })
    message.success(`已导出 ${count} 条`)
  } catch { message.error('导出失败') }
}

// ═══ 行操作 ═══
const goDetail = (record: any) => { router.push(`/purchase/inbound/form?id=${record.id}`) }
const goEdit = (record: any) => { router.push(`/purchase/inbound/form?id=${record.id}`) }

const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '提交审批', content: `确认提交入库单 ${record.inboundNo} 进行审批吗？`,
    okText: '确认提交', cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/inbound/${record.id}/submit`)
        message.success('已提交审批'); fetchData()
      } catch { message.error('提交失败') }
    },
  })
}

// ═══ 状态辅助 ═══
const getStatusColor = (status: number) => {
  const map: Record<number, string> = {
    0: 'default', 1: 'orange', 2: 'blue', 3: 'cyan', 4: 'geekblue',
    5: 'purple', 6: 'magenta', 7: 'gold', 8: 'green', 9: 'green', 10: 'red',
  }
  return map[status] || 'default'
}
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '草稿', 1: '待审批', 2: '已审批', 3: '待收货', 4: '已收货',
    5: '待质检', 6: '已质检', 7: '待入库', 8: '已入库', 9: '已完成', 10: '已取消',
  }
  return map[status] || '未知'
}

const handleError = (err: any) => { console.warn('[采购入库单] ErrorBoundary:', err) }

onMounted(async () => {
  await loadPageConfig()
  applyHiddenSearchFields()
  fetchData()
})
</script>
