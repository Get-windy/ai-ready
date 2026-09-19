<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        经营历程（分析 → 综合单据 → 经营历程，菜单 80403）
        对标 ql361「分析 → 综合单据 → 经营历程」：单视图流水台账（17 列 / 默认 9），
        2 行查询区 + 「显示红冲」勾选，工具栏 刷新｜打印(F8)｜导出（本组唯一有打印的页），
        行级「红冲(作废) / 复制 / 备注」，**无行勾选框**（故无批量动作），底部合计行。
        取数：/docquery/business-history/page（13 类单据全量 UNION，默认排除已取消）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-business-history-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 9 项 + 「显示红冲」勾选，横向网格排布） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('history.docType')" class="search-item">
                <span class="search-label">单据类型</span>
                <a-select v-model:value="query.docType" size="small" placeholder="全部单据" allow-clear style="width: 150px" :options="DOC_TYPE_OPTIONS" @change="handleSearch" />
              </div>
              <div v-if="isQueryVisible('history.docNo')" class="search-item">
                <span class="search-label">单据编号</span>
                <a-input v-model:value="query.docNo" size="small" placeholder="单据编号" allow-clear style="width: 170px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('history.partnerName')" class="search-item">
                <span class="search-label">往来单位</span>
                <a-input v-model:value="query.partnerName" size="small" placeholder="客户/供应商" allow-clear style="width: 180px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('history.warehouseName')" class="search-item">
                <span class="search-label">仓库</span>
                <a-input v-model:value="query.warehouseName" size="small" placeholder="仓库" allow-clear style="width: 140px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('history.handlerName')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input v-model:value="query.handlerName" size="small" placeholder="经手人" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('history.departmentName')" class="search-item">
                <span class="search-label">部门</span>
                <a-input v-model:value="query.departmentName" size="small" placeholder="部门" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.includeReversed" @change="handleSearch">显示红冲</a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（无行勾选框，与同组另两页不同） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              storage-key="analytics-business-history-columns"
              global-config-key="analytics-business-history-columns"
              @sort-change="onSortChange"
            >
              <template #docNoCell="{ record }">
                <a class="doc-link" @click="openDocForm(router, record)">{{ record.docNo }}</a>
              </template>
              <template #amountCell="{ record }">
                <span :class="{ 'amount-negative': Number(record.amount) < 0 }">{{ formatMoney(record.amount) }}</span>
              </template>
              <template #hasAttachmentCell="{ record }">
                <PaperClipOutlined v-if="record.hasAttachment" class="attach-on" />
                <span v-else class="attach-off">-</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="2">
                  <a-button v-if="canDo(record.docTypeCode, 'cancel')" type="link" size="small" danger @click="handleReverse(record)">红冲(作废)</a-button>
                  <a-button v-if="canDo(record.docTypeCode, 'copy')" type="link" size="small" @click="handleCopy(record)">复制</a-button>
                  <a-button type="link" size="small" @click="openRemark(record)">备注</a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（对标有「页面配置」弹窗 → 接 PageConfigPanel） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 备注弹窗（行级「备注」，写回单据自身的单据备注） ═══ -->
      <a-modal
        v-model:open="remarkVisible"
        title="单据备注"
        :width="480"
        :confirm-loading="remarkSaving"
        @ok="submitRemark"
      >
        <p class="remark-target">{{ remarkTarget?.docType }} {{ remarkTarget?.docNo }}</p>
        <a-textarea v-model:value="remarkText" :rows="4" placeholder="请输入单据备注" />
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  DownloadOutlined, PaperClipOutlined, PrinterOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { docQueryApi } from '@/api/analytics'
import type { DocHistoryItem } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import { DOC_TYPE_OPTIONS, QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { canDo, formatMoney, openDocForm, runDocAction, updateDocRemark } from '../shared/docActions'
import { useDocQueryTable } from '../shared/useDocQueryTable'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsBusinessHistory' })

const router = useRouter()

const quickDate = ref('month')

/** 查询态（逐项对应对标 9 项查询条件 + 「显示红冲」勾选） */
const query = reactive({
  docType: undefined as string | undefined,
  docNo: '',
  partnerName: '',
  warehouseName: '',
  handlerName: '',
  departmentName: '',
  includeReversed: false,
  startDate: quickDateRange('month')[0],
  endDate: quickDateRange('month')[1]
})

const sortState = reactive({ field: 'bizDate', order: 'desc' })

function buildParams() {
  return {
    docType: query.docType,
    docNo: query.docNo,
    partnerName: query.partnerName,
    warehouseName: query.warehouseName,
    handlerName: query.handlerName,
    departmentName: query.departmentName,
    includeReversed: query.includeReversed ? true : undefined,
    startDate: query.startDate,
    endDate: query.endDate,
    sortField: sortState.field,
    sortOrder: sortState.order
  }
}

const {
  loading, dataSource, pagination, summaryAmount,
  fetchData, handleSearch, handlePageChange, handleSort
} = useDocQueryTable('ALL', buildParams)

// ═══ 列定义（对标 17 列 / 默认 9；第 10 列起 8 列默认不勾选） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'bizDate', title: '单据日期', width: 110 },
  { key: 'docNo', title: '单据编号', type: 'slot', slotName: 'docNoCell', width: 175 },
  { key: 'docType', title: '单据类型', width: 110 },
  { key: 'sourceOrderNo', title: '来源订单', width: 160 },
  { key: 'partnerName', title: '往来单位', width: 180 },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'amount', title: '金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right', sortable: true },
  { key: 'handlerName', title: '经手人', width: 100 },
  { key: 'departmentName', title: '部门', width: 110 },
  { key: 'creatorName', title: '制单人', width: 100, defaultHidden: true },
  { key: 'bookkeeperName', title: '记账人', width: 100, defaultHidden: true },
  { key: 'summary', title: '摘要', width: 180, defaultHidden: true },
  { key: 'remark', title: '单据备注', width: 180, defaultHidden: true },
  { key: 'hasAttachment', title: '附件', type: 'slot', slotName: 'hasAttachmentCell', width: 70, align: 'center', defaultHidden: true },
  { key: 'createTime', title: '制单时间', width: 160, defaultHidden: true },
  { key: 'bookkeepingTime', title: '记账时间', width: 160, defaultHidden: true },
  { key: 'printCount', title: '打印次数', width: 90, align: 'right', defaultHidden: true }
]

/** 底部合计行（红冲单以负数参与合计） */
const summaryColumns = computed(() => [{ key: 'amount', value: summaryAmount.value }])

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'history.docType', label: '单据类型', visible: true },
  { key: 'history.docNo', label: '单据编号', visible: true },
  { key: 'history.partnerName', label: '往来单位', visible: true },
  { key: 'history.warehouseName', label: '仓库', visible: true },
  { key: 'history.handlerName', label: '经手人', visible: true },
  { key: 'history.departmentName', label: '部门', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
/** 打印配置项（对标本页有打印；打印只对当前操作员生效） */
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-business-history-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 行级动作 ═══
function handleReverse(record: DocHistoryItem) {
  Modal.confirm({
    title: '红冲（作废）',
    content: `确认红冲 ${record.docType}「${record.docNo}」？红冲后该单据作废并影响库存/账务，操作不可撤销。`,
    okText: '确认红冲',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      if (await runDocAction(record, 'cancel')) {
        message.success('已红冲')
        await fetchData()
      }
    }
  })
}

async function handleCopy(record: DocHistoryItem) {
  if (await runDocAction(record, 'copy')) {
    message.success('已复制为新草稿')
    await fetchData()
  }
}

// ═══ 备注 ═══
const remarkVisible = ref(false)
const remarkSaving = ref(false)
const remarkTarget = ref<DocHistoryItem | null>(null)
const remarkText = ref('')

function openRemark(record: DocHistoryItem) {
  remarkTarget.value = record
  remarkText.value = record.remark || ''
  remarkVisible.value = true
}

async function submitRemark() {
  if (!remarkTarget.value) return
  remarkSaving.value = true
  try {
    if (await updateDocRemark(remarkTarget.value, remarkText.value)) {
      message.success('备注已保存')
      remarkVisible.value = false
      await fetchData()
    }
  } finally {
    remarkSaving.value = false
  }
}

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { docType: query.docType, docNo: query.docNo, partnerName: query.partnerName, warehouseName: query.warehouseName, handlerName: query.handlerName, departmentName: query.departmentName, includeReversed: query.includeReversed, startDate: query.startDate, endDate: query.endDate }
}

function applyQuerySnapshot(v: Record<string, any>) {
  Object.assign(query, v)
  quickDate.value = ''
  handleSearch()
}

// ═══ 查询区交互 ═══
function setQuickDate(key: string) {
  quickDate.value = key
  const [start, end] = quickDateRange(key)
  query.startDate = start
  query.endDate = end
  handleSearch()
}

function handleRefresh() {
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    docType: undefined, docNo: '', partnerName: '', warehouseName: '',
    handlerName: '', departmentName: '', includeReversed: false
  })
  handleSearch()
}

function onSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  if (key === 'amount' && order) handleSort('amount', order, sortState)
  else handleSort('bizDate', 'desc', sortState)
}

// ═══ 打印(F8) ═══
/** 打印列（不含隐藏列，按当前列配置取可见列更贴近「所见即所打」） */
function printRows(): { header: string[]; body: string[][] } {
  const visible = columns.filter(c => c.key !== 'action' && c.key !== 'rowNo')
  const header = visible.map(c => c.title)
  const body = dataSource.value.map((r: any) => visible.map(c => {
    if (c.key === 'amount') return formatMoney(r.amount)
    if (c.key === 'hasAttachment') return r.hasAttachment ? '有' : ''
    return r[c.key] ?? ''
  }))
  return { header, body }
}

function handlePrint() {
  const { header, body } = printRows()
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const html = `<html><head><meta charset="utf-8"><title>经营历程</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}
    tfoot td{font-weight:600}</style></head><body>
    <h3>经营历程（${query.startDate} ~ ${query.endDate}）</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    <tfoot><tr>${header.map((h, i) => `<td>${i === header.indexOf('金额') ? '合计 ' + formatMoney(summaryAmount.value) : (i === 0 ? '合计' : '')}</td>`).join('')}</tr></tfoot>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（后端逐页取全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()
const EXPORT_HEADERS = columns
  .filter(c => c.key !== 'action' && c.key !== 'rowNo')
  .map(c => c.title)
const EXPORT_FIELDS = columns
  .filter(c => c.key !== 'action' && c.key !== 'rowNo')
  .map(c => c.key)

async function fetchAllRows(): Promise<any[]> {
  const size = 100
  const all: any[] = []
  const total = pagination.total
  const pages = Math.max(1, Math.ceil(total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await docQueryApi.businessHistoryPage({ ...buildParams(), page: p, size })
    const list = res?.list || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  executeExport({
    fileName: '经营历程',
    headers: EXPORT_HEADERS,
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => EXPORT_FIELDS.map(k => {
      if (k === 'amount') return formatMoney(r.amount)
      if (k === 'hasAttachment') return r.hasAttachment ? '有' : ''
      return r[k] ?? ''
    })),
    fallbackRows: () => dataSource.value.map((r: any) => EXPORT_FIELDS.map(k => {
      if (k === 'amount') return formatMoney(r.amount)
      if (k === 'hasAttachment') return r.hasAttachment ? '有' : ''
      return r[k] ?? ''
    }))
  })
}

function handleError(err: any) {
  console.error('[经营历程] 页面异常', err)
}

onMounted(() => {
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不在 scoped 作用域内，样式随页面自带（见《插槽内容的样式必须自备》） */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.doc-link { color: #1677ff; }
.doc-link:hover { text-decoration: underline; }
.amount-negative { color: #cf1322; }
.attach-on { color: #1677ff; }
.attach-off { color: #bfbfbf; }
.remark-target { margin-bottom: 8px; color: #666; }
</style>
