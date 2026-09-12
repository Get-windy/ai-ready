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
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <span class="list-title">凭证管理</span>
          </div>
        </template>

        <!-- ═══ 工具栏右侧（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button v-if="showConfigBtn('config')" size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="showConfigBtn('add')" type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增会计凭证
            </a-button>
            <a-button v-if="showConfigBtn('refresh')" size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="showConfigBtn('batchPrint')" size="small" @click="handleBatchPrint">
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-button v-if="showConfigBtn('printF8')" size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="showConfigBtn('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid">
                <template v-for="qf in visibleQueryFields" :key="qf.key">
                  <div class="search-field-item">
                    <a-range-picker
                      v-if="qf.type === 'daterange' && qf.key === 'date'"
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                    <a-input
                      v-else-if="qf.type === 'input'"
                      v-model:value="search[key]"
                      :placeholder="qf.placeholder || qf.label"
                      allow-clear
                      size="small"
                    />
                    <a-select
                      v-else-if="qf.type === 'select'"
                      v-model:value="search[key]"
                      size="small"
                      allow-clear
                      placeholder="全部"
                    >
                      <a-select-option v-for="opt in qf.options" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </a-select-option>
                    </a-select>
                    <a-tree-select
                      v-else-if="qf.type === 'subject'"
                      v-model:value="search[key]"
                      :tree-data="subjectTree"
                      :field-names="{ label: 'title', value: 'value', children: 'children' }"
                      show-search
                      tree-default-expand-all
                      :filter-option="filterOption"
                      size="small"
                      style="width: 100%"
                      allow-clear
                      placeholder="科目"
                    />
                  </div>
                </template>
                <div class="search-field-item search-action-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="search.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="draft">草稿</a-select-option>
                      <a-select-option value="audited">已审核</a-select-option>
                      <a-select-option value="posted">已记账</a-select-option>
                      <a-select-option value="reversed">已冲销</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item search-action-item">
                  <a-checkbox v-model:checked="search.showRed">显示红冲</a-checkbox>
                </div>
                <div class="search-field-item search-action-item">
                  <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'voucher-table-columns'"
              :loading="loading"
              :pagination="pagination"
              :tree-config="treeConfig"
              row-key="id"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              @page-change="handlePageChange"
            >
              <template #voucherNoCell="{ record }">
                <a-button v-if="record._isHeader" type="link" size="small" @click="handleView(record)">
                  {{ record.voucherNo || '-' }}
                </a-button>
                <span v-else>-</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <template #voucherTypeCell="{ record }">
                {{ getVoucherTypeText(record.voucherType) }}
              </template>
              <template #actionCell="{ record }">
                <a-space v-if="record._isHeader" :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 'draft'" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button v-if="record.status === 'draft' || record.status === 'audited'" type="link" size="small" @click="handleConfirm(record)">记账</a-button>
                  <a-button type="link" size="small" @click="handleCopy(record)">复制</a-button>
                  <a-button v-if="record.status === 'draft'" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
                </a-space>
                <span v-else>-</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
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
import { voucherApi, accountSubjectApi } from '@/api/finance'
import { useRouter } from 'vue-router'

defineOptions({ name: 'FinanceVoucherList' })

const router = useRouter()

// ═══ Tab 配置（单视图） ═══
const tabs = []
const activeTab = ref('list')

// ═══ 状态字典 ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  audited: { text: '已审核', color: 'processing' },
  posted: { text: '已记账', color: 'success' },
  reversed: { text: '已冲销', color: 'error' },
}
function getStatusText(status: string): string {
  return STATUS_MAP[status || '']?.text || '未知'
}
function getStatusColor(status: string): string {
  return STATUS_MAP[status || '']?.color || 'default'
}
function getVoucherTypeText(t: string): string {
  if (t === 'system') return '系统凭证'
  if (t === 'manual') return '手工凭证'
  return t || '手工凭证'
}

const loading = ref(false)
const tableData = ref<any[]>([])

const dateRange = ref<[any, any] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const search = reactive<any>({
  date: undefined,
  keyword: '',
  sourceNo: '',
  voucherType: undefined,
  summary: '',
  status: undefined,
  subjectId: undefined,
  handlerName: '',
  deptName: '',
  prepBy: '',
  verifyBy: '',
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 会计期间无需响应 ═══
const treeConfig = { childrenField: 'children' }

// 列配置走数据表表头齿轮（storage-key=voucher-table-columns）
const showPageConfig = ref(false)

// ═══ 页面配置：查询条件（21） + 功能按钮（6） ═══
const PAGE_CONFIG_STORAGE_KEY = 'voucher-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean; type: string; mapped?: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true, type: 'daterange', mapped: true },
  { key: 'keyword', label: '单据编号', visible: true, type: 'input', mapped: true },
  { key: 'sourceNo', label: '来源单据编号', visible: true, type: 'input', mapped: true },
  { key: 'voucherType', label: '单据类型', visible: true, type: 'select', mapped: true, options: [{ label: '手工凭证', value: 'manual' }, { label: '系统凭证', value: 'system' }] },
  { key: 'summary', label: '摘要', visible: true, type: 'input', mapped: true },
  { key: 'status', label: '单据状态', visible: true, type: 'select', mapped: true },
  { key: 'subjectId', label: '科目', visible: true, type: 'subject', mapped: true },
  { key: 'handlerName', label: '经手人', visible: true, type: 'input', mapped: true },
  { key: 'deptName', label: '部门', visible: false, type: 'input', mapped: true },
  { key: 'prepBy', label: '制单人', visible: false, type: 'input', mapped: true },
  { key: 'verifyBy', label: '记账人', visible: false, type: 'input', mapped: true },
  { key: 'remark', label: '单据备注', visible: false, type: 'input', mapped: false },
  { key: 'settleUnit', label: '核算单位', visible: true, type: 'input', mapped: false },
  { key: 'settleDept', label: '核算部门', visible: true, type: 'input', mapped: false },
  { key: 'settleStaff', label: '核算职员', visible: true, type: 'input', mapped: false },
  { key: 'custom1', label: '表头自定义1(数字)', visible: false, type: 'input', mapped: false },
  { key: 'custom2', label: '表头自定义2(数字)', visible: false, type: 'input', mapped: false },
  { key: 'custom3', label: '表头自定义3(文本)', visible: false, type: 'input', mapped: false },
  { key: 'custom4', label: '表头自定义4(文本)', visible: false, type: 'input', mapped: false },
  { key: 'custom5', label: '表头自定义5(文本)', visible: false, type: 'input', mapped: false },
  { key: 'showRed', label: '显示红冲', visible: true, type: 'checkbox', mapped: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增会计凭证', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        const merged = DEFAULT_QUERY_FIELDS.map((df) => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
        queryConfig.value.splice(0, queryConfig.value.length, ...merged)
      }
    }
    const btnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons')
    if (btnRaw) {
      const parsed = JSON.parse(btnRaw)
      if (parsed.functionButtons) {
        const merged = DEFAULT_FUNCTION_BUTTONS.map((bf) => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
        functionButtonConfig.value.splice(0, functionButtonConfig.value.length, ...merged)
      }
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || queryConfig.value,
  }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

function showConfigBtn(key: string): boolean {
  const btn = functionButtonConfig.value.find((b: FunctionButtonSetting) => b.key === key)
  return btn ? btn.enabled : true
}

// 可见查询字段：过滤「显示红冲」(单独渲染) 与已隐藏字段
const visibleQueryFields = computed(() =>
  queryConfig.value.filter(f => f.visible && f.key !== 'showRed' && f.key !== 'status')
)

// ═══ 科目树（供查询「科目」条件） ═══
const subjectTree = ref<any[]>([])
async function loadSubjects() {
  try {
    const res: any = await accountSubjectApi.getTree()
    const tree: any[] = Array.isArray(res) ? res : res?.data || []
    const build = (nodes: any[]): any[] => {
      return (nodes || []).map((n: any) => ({
        value: n.id,
        title: `${n.subjectCode} ${n.subjectName}`,
        children: build(n.children || []),
      }))
    }
    subjectTree.value = build(tree)
  } catch {
    subjectTree.value = []
  }
}

const filterOption = (input: string, option: any) => {
  const text = option?.title || option?.label || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

// ═══ 列定义（28 列 + 行号 + 操作） ═══
const allColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'voucherDate', key: 'voucherDate', width: 110, sortable: true },
  { title: '单据编号', field: 'voucherNo', key: 'voucherNo', width: 170, type: 'slot', slotName: 'voucherNoCell', sortable: true, treeNode: true },
  { title: '科目名称', field: 'subjectName', key: 'subjectName', width: 180 },
  { title: '科目编号', field: 'subjectCode', key: 'subjectCode', width: 110 },
  { title: '明细摘要', field: 'summary', key: 'summary', width: 180 },
  { title: '来源单据编号', field: 'sourceNo', key: 'sourceNo', width: 150, defaultHidden: true },
  { title: '单据类型', field: 'voucherType', key: 'voucherType', width: 100, type: 'slot', slotName: 'voucherTypeCell' },
  { title: '核算单位', field: 'settleUnit', key: 'settleUnit', width: 120 },
  { title: '核算部门', field: 'settleDept', key: 'settleDept', width: 120 },
  { title: '核算职员', field: 'settleStaff', key: 'settleStaff', width: 110 },
  { title: '借方金额', field: 'debitAmount', key: 'debitAmount', width: 130, align: 'right', sortable: true },
  { title: '贷方金额', field: 'creditAmount', key: 'creditAmount', width: 130, align: 'right', sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 100, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '表头自定义1(数字)', field: 'custom1', key: 'custom1', width: 120, defaultHidden: true },
  { title: '表头自定义2(数字)', field: 'custom2', key: 'custom2', width: 120, defaultHidden: true },
  { title: '表头自定义3(文本)', field: 'custom3', key: 'custom3', width: 120, defaultHidden: true },
  { title: '表头自定义4(文本)', field: 'custom4', key: 'custom4', width: 120, defaultHidden: true },
  { title: '表头自定义5(文本)', field: 'custom5', key: 'custom5', width: 120, defaultHidden: true },
  { title: '制单人', field: 'prepBy', key: 'prepBy', width: 100 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 100, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 100, defaultHidden: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '单据备注', field: 'remark', key: 'remark', width: 150, defaultHidden: true },
  { title: '附件', field: 'attachments', key: 'attachments', width: 80 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', defaultHidden: true },
  { title: '制单时间', field: 'prepAt', key: 'prepAt', width: 140, defaultHidden: true },
  { title: '记账时间', field: 'postAt', key: 'postAt', width: 140, defaultHidden: true },
]

// ═══ 数据加载 ═══
function buildTree(records: any[]): any[] {
  return (records || []).map((v: any) => {
    const header = {
      id: `v_${v.id}`,
      _isHeader: true,
      voucherId: v.id,
      voucherNo: v.voucherNo,
      voucherDate: v.voucherDate,
      voucherType: v.voucherType,
      summary: v.summary,
      handlerName: v.handlerName,
      deptName: v.deptName,
      sourceNo: v.sourceNo,
      prepBy: v.prepBy,
      auditBy: v.auditBy,
      postBy: v.postBy,
      status: v.status,
      totalDebit: v.totalDebit,
      totalCredit: v.totalCredit,
      remark: v.remark,
      attachments: v.attachments,
      printCount: v.printCount,
      prepAt: v.prepAt,
      postAt: v.postAt,
      debitAmount: v.totalDebit,
      creditAmount: v.totalCredit,
      bookkeeperName: v.postBy || v.auditBy || '',
      auditorName: v.auditBy || '',
      children: (v.items || []).map((it: any) => ({
        id: `i_${v.id}_${it.id}`,
        _isHeader: false,
        summary: it.summary,
        subjectId: it.subjectId,
        subjectCode: it.subjectCode,
        subjectName: it.subjectName,
        subjectFullName: it.subjectFullName || it.subjectName,
        detailSubject: it.detailSubject,
        debitAmount: it.debitAmount,
        creditAmount: it.creditAmount,
        sourceNo: it.sourceNo || v.sourceNo,
      })),
    }
    return header
  })
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (search.startDate) params.startDate = search.startDate
    if (search.endDate) params.endDate = search.endDate
    if (search.keyword) params.keyword = search.keyword
    if (search.sourceNo) params.sourceNo = search.sourceNo
    if (search.voucherType) params.voucherType = search.voucherType
    if (search.summary) params.summary = search.summary
    if (search.status) params.status = search.status
    if (search.subjectId) params.subjectId = search.subjectId
    if (search.handlerName) params.handlerName = search.handlerName
    if (search.deptName) params.deptName = search.deptName
    if (search.prepBy) params.prepBy = search.prepBy
    if (search.verifyBy) params.verifyBy = search.verifyBy
    if (!search.showRed) params.showRed = false
    const res: any = await voucherApi.getPage(params)
    const body = (res as any)?.data ?? res
    const records = body?.records || []
    tableData.value = buildTree(records)
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[凭证] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleDateChange(dates: any) {
  if (dates && dates.length === 2) {
    search.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    search.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    search.startDate = ''
    search.endDate = ''
  }
}

function handleTabChange(key: string) { activeTab.value = key }
function handleSearch() { pagination.current = 1; fetchData() }
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 操作 ═══
function handleAdd() {
  router.push('/finance/voucher/form')
}
function handleView(record: any) {
  showDetail(record.voucherId || record.id)
}
function handleEdit(record: any) {
  router.push(`/finance/voucher/form?id=${record.voucherId || record.id}`)
}
function handleCopy(record: any) {
  router.push(`/finance/voucher/form?copyOf=${record.voucherId || record.id}`)
}

function showDetail(id: number) {
  router.push(`/finance/voucher/form?id=${id}`)
}

function handleConfirm(record: any) {
  Modal.confirm({
    title: '记账确认',
    content: `确认将凭证 ${record.voucherNo} 记账吗？记账后不可修改分录。`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        const id = record.voucherId || record.id
        if (record.status === 'draft') await voucherApi.audit(id)
        await voucherApi.post(id)
        message.success('记账成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '记账失败')
      }
    },
  })
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '删除凭证',
    content: `确认删除凭证 ${record.voucherNo} 吗？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await voucherApi.batchDelete([record.voucherId || record.id])
        message.success('已删除')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

function handlePrintF8() {
  message.info('打印(F8)待对接打印模板')
}
function handleBatchPrint() {
  message.info('批量打印待对接')
}

async function handleExport() {
  try {
    const res: any = await voucherApi.getPage({ pageNum: 1, pageSize: 9999 })
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '单据类型', '经手人', '部门', '制单人', '借方合计', '贷方合计', '状态']
    const rows = data.map((r: any) => [
      r.voucherNo, r.voucherDate, getVoucherTypeText(r.voucherType), r.handlerName || '',
      r.deptName || '', r.prepBy || '', formatAmount(r.totalDebit), formatAmount(r.totalCredit), getStatusText(r.status),
    ])
    const csv = [headers.join(','), ...rows.map((x: any[]) => x.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `会计凭证_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[凭证] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadPageConfig()
  loadSubjects()
  fetchData()
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 8px; }
.list-title { font-size: 15px; font-weight: 600; color: #303133; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 90px; overflow: hidden; }
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
.table-area { flex: 1; min-height: 0; overflow: hidden; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
