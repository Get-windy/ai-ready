<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        业务草稿（分析 → 综合单据 → 业务草稿，菜单 80402）
        对标 ql361「分析 → 综合单据 → 业务草稿」：单视图台账（13 列 / 默认 12），
        3 行查询区，工具栏 刷新｜提交记账｜删除（无打印），行级「删除 / 复制」，底部合计行。
        取数：/docquery/draft-docs/page（13 类单据草稿态 UNION）。
        提交记账 / 删除分发到各业务域的 submit / DELETE 端点（见 shared/docTypes.ts）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-draft-query-scheme"
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

        <!-- ═══ 工具栏右侧：刷新｜提交记账｜删除｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('submit')"
              size="small"
              :disabled="!selectedRows.length"
              @click="handleBatchSubmit"
            >
              <CheckOutlined /> 提交记账
            </a-button>
            <a-button
              v-if="isButtonEnabled('remove')"
              size="small"
              danger
              :disabled="!selectedRows.length"
              @click="handleBatchRemove"
            >
              <DeleteOutlined /> 删除
            </a-button>
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 11 项，横向网格排布） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('draft.docNo')" class="search-item">
                <span class="search-label">单据编号</span>
                <a-input v-model:value="query.docNo" size="small" placeholder="单据编号" allow-clear style="width: 160px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.docType')" class="search-item">
                <span class="search-label">单据类型</span>
                <a-select v-model:value="query.docType" size="small" placeholder="全部单据" allow-clear style="width: 150px" :options="DOC_TYPE_OPTIONS" @change="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.partnerName')" class="search-item">
                <span class="search-label">往来单位</span>
                <a-input v-model:value="query.partnerName" size="small" placeholder="客户/供应商" allow-clear style="width: 170px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.warehouseName')" class="search-item">
                <span class="search-label">仓库</span>
                <a-input v-model:value="query.warehouseName" size="small" placeholder="仓库" allow-clear style="width: 140px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.handlerName')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input v-model:value="query.handlerName" size="small" placeholder="经手人" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.departmentName')" class="search-item">
                <span class="search-label">部门</span>
                <a-input v-model:value="query.departmentName" size="small" placeholder="部门" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.creatorName')" class="search-item">
                <span class="search-label">制单人</span>
                <a-input v-model:value="query.creatorName" size="small" placeholder="制单人" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.remark')" class="search-item">
                <span class="search-label">单据备注</span>
                <a-input v-model:value="query.remark" size="small" placeholder="单据备注" allow-clear style="width: 160px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible('draft.sourceOrderNo')" class="search-item">
                <span class="search-label">来源订单</span>
                <a-input v-model:value="query.sourceOrderNo" size="small" placeholder="来源订单号" allow-clear style="width: 160px" @press-enter="handleSearch" />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置 + 行勾选 + 底部合计行） ═══ -->
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
              storage-key="analytics-draft-columns"
              global-config-key="analytics-draft-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
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
                  <!-- 2026-09-23：按能力矩阵渲染 —— SALE_OUTBOUND/PURCHASE_INBOUND/PURCHASE_RETURN/RECEIPT/PAYMENT
                       在 docTypes.ts 里都没有 remove，此前无条件渲染 ⇒ 点下去只会弹「不支持该操作」 -->
                  <a-button v-if="canDo(record.docTypeCode, 'remove')" type="link" size="small" danger @click="handleRemove(record)">删除</a-button>
                  <a-button v-if="canDo(record.docTypeCode, 'copy')" type="link" size="small" @click="handleCopy(record)">复制</a-button>
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
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  CheckOutlined, DeleteOutlined, PaperClipOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { DocHistoryItem } from '@/api/analytics'
import { DOC_TYPE_OPTIONS, QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { canDo, formatMoney, openDocForm, runBatchDocAction, runDocAction } from '../shared/docActions'
import { useDocQueryTable } from '../shared/useDocQueryTable'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsDraft' })

const router = useRouter()

const quickDate = ref('month')

/** 查询态（逐项对应对标 11 项查询条件） */
const query = reactive({
  docNo: '',
  docType: undefined as string | undefined,
  partnerName: '',
  warehouseName: '',
  handlerName: '',
  departmentName: '',
  creatorName: '',
  remark: '',
  sourceOrderNo: '',
  startDate: quickDateRange('month')[0],
  endDate: quickDateRange('month')[1]
})

const sortState = reactive({ field: 'bizDate', order: 'desc' })

const {
  loading, dataSource, pagination, summaryAmount,
  fetchData, handleSearch, handlePageChange, handleSort
} = useDocQueryTable('DRAFT', () => ({
  docNo: query.docNo,
  docType: query.docType,
  partnerName: query.partnerName,
  warehouseName: query.warehouseName,
  handlerName: query.handlerName,
  departmentName: query.departmentName,
  creatorName: query.creatorName,
  remark: query.remark,
  sourceOrderNo: query.sourceOrderNo,
  startDate: query.startDate,
  endDate: query.endDate,
  sortField: sortState.field,
  sortOrder: sortState.order
}))

// ═══ 列定义（对标 13 列 / 默认 12：第 10 列「制单人」默认不勾选） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 120, fixed: 'left' },
  { key: 'bizDate', title: '单据日期', width: 110 },
  { key: 'docNo', title: '单据编号', type: 'slot', slotName: 'docNoCell', width: 170 },
  { key: 'docType', title: '单据类型', width: 110 },
  { key: 'partnerName', title: '往来单位', width: 180 },
  { key: 'sourceOrderNo', title: '来源订单', width: 160 },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'amount', title: '金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right', sortable: true },
  { key: 'handlerName', title: '经手人', width: 100 },
  { key: 'departmentName', title: '部门', width: 110 },
  { key: 'creatorName', title: '制单人', width: 100, defaultHidden: true },
  { key: 'summary', title: '摘要', width: 180 },
  { key: 'remark', title: '单据备注', width: 180 },
  { key: 'hasAttachment', title: '附件', type: 'slot', slotName: 'hasAttachmentCell', width: 70, align: 'center' }
]

/** 底部合计行（金额，取值来自后端按当前过滤范围 SUM） */
const summaryColumns = computed(() => [{ key: 'amount', value: summaryAmount.value }])

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'draft.docNo', label: '单据编号', visible: true },
  { key: 'draft.docType', label: '单据类型', visible: true },
  { key: 'draft.partnerName', label: '往来单位', visible: true },
  { key: 'draft.warehouseName', label: '仓库', visible: true },
  { key: 'draft.handlerName', label: '经手人', visible: true },
  { key: 'draft.departmentName', label: '部门', visible: true },
  { key: 'draft.creatorName', label: '制单人', visible: true },
  { key: 'draft.remark', label: '单据备注', visible: true },
  { key: 'draft.sourceOrderNo', label: '来源订单', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'submit', label: '提交记账', enabled: true },
  { key: 'remove', label: '删除', enabled: true }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-draft-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 行勾选 ═══
const selectedRows = ref<DocHistoryItem[]>([])
const selectedKeys = ref<string[]>([])

function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  if (checked) {
    if (!selectedKeys.value.includes(record.rowKey)) {
      selectedKeys.value.push(record.rowKey)
      selectedRows.value.push(record)
    }
  } else {
    selectedKeys.value = selectedKeys.value.filter(k => k !== record.rowKey)
    selectedRows.value = selectedRows.value.filter(r => r.rowKey !== record.rowKey)
  }
}

function handleCheckboxAll(checked: boolean, records: any[]) {
  const pageKeys = new Set(dataSource.value.map(r => (r as any).rowKey))
  if (checked) {
    const added = records.filter(r => !selectedKeys.value.includes(r.rowKey))
    selectedKeys.value.push(...added.map(r => r.rowKey))
    selectedRows.value = [...selectedRows.value.filter(r => !pageKeys.has((r as any).rowKey)), ...records]
  } else {
    selectedKeys.value = selectedKeys.value.filter(k => !pageKeys.has(k))
    selectedRows.value = selectedRows.value.filter(r => !pageKeys.has((r as any).rowKey))
  }
}

function clearSelection() {
  selectedRows.value = []
  selectedKeys.value = []
}

/** 该类型不支持的动作先剔除并如实告知，避免整批静默失败 */
function pickCapable(kind: 'submit' | 'remove'): DocHistoryItem[] {
  const capable = selectedRows.value.filter(r => canDo(r.docTypeCode, kind))
  const skipped = selectedRows.value.length - capable.length
  if (skipped > 0) message.warning(`已跳过 ${skipped} 张不支持该操作的单据`)
  return capable
}

// ═══ 批量提交记账（过账） ═══
function handleBatchSubmit() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要提交记账的草稿')
    return
  }
  const capable = pickCapable('submit')
  if (!capable.length) return
  Modal.confirm({
    title: '提交记账',
    content: `确认将勾选的 ${capable.length} 张草稿提交记账？提交后单据进入正式流转，并从本页移除。`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      const { ok, fail } = await runBatchDocAction(capable, 'submit')
      if (ok) message.success(`已提交 ${ok} 张${fail ? `，${fail} 张失败` : ''}`)
      else message.error('提交记账失败')
      clearSelection()
      await fetchData()
    }
  })
}

// ═══ 批量 / 行级删除 ═══
function doRemove(rows: DocHistoryItem[]) {
  Modal.confirm({
    title: '删除草稿',
    content: `确认删除选中的 ${rows.length} 张草稿单据？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const { ok, fail } = await runBatchDocAction(rows, 'remove')
      if (ok) message.success(`已删除 ${ok} 张${fail ? `，${fail} 张失败` : ''}`)
      else message.error('删除失败')
      clearSelection()
      await fetchData()
    }
  })
}

function handleBatchRemove() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要删除的草稿')
    return
  }
  const capable = pickCapable('remove')
  if (!capable.length) return
  doRemove(capable)
}

function handleRemove(record: DocHistoryItem) {
  doRemove([record])
}

async function handleCopy(record: DocHistoryItem) {
  if (await runDocAction(record, 'copy')) {
    message.success('已复制为新草稿')
    await fetchData()
  }
}

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { docNo: query.docNo, docType: query.docType, partnerName: query.partnerName, warehouseName: query.warehouseName, handlerName: query.handlerName, departmentName: query.departmentName, creatorName: query.creatorName, remark: query.remark, sourceOrderNo: query.sourceOrderNo, startDate: query.startDate, endDate: query.endDate }
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
    docNo: '', docType: undefined, partnerName: '', warehouseName: '', handlerName: '',
    departmentName: '', creatorName: '', remark: '', sourceOrderNo: ''
  })
  handleSearch()
}

/** 表头排序：仅「金额」可排；点掉箭头回到默认「单据日期倒序」 */
function onSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  if (key === 'amount' && order) handleSort('amount', order, sortState)
  else handleSort('bizDate', 'desc', sortState)
}

function handleError(err: any) {
  console.error('[业务草稿] 页面异常', err)
}

onMounted(() => {
  fetchData()
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
</style>
