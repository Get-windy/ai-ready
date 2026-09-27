<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        待审批单据（分析 → 综合单据 → 待审批单据，菜单 80401）
        对标 ql361「分析 → 综合单据 → 待审批单据」：单视图台账（13 列 / 默认 12），
        3 行查询区，工具栏 刷新｜审核通过｜驳回（无打印配置），行级「审核」，底部合计行。
        取数：/docquery/pending-docs/page（13 类单据 UNION，pendingSummary 带逐类计数）。
        审批动作分发到各业务域既有 approve/reject 端点（见 shared/docTypes.ts 能力矩阵）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-pending-approval-query-scheme"
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

        <!-- ═══ 工具栏右侧：刷新｜审核通过｜驳回（对标无打印(F8)） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" :disabled="!selectedRows.length" @click="openBatchAudit('approve')">
              <CheckOutlined /> 审核通过
            </a-button>
            <a-button size="small" :disabled="!selectedRows.length" @click="openBatchAudit('reject')">
              <CloseOutlined /> 驳回
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 12 项，横向网格排布） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="query.docNo"
                  size="small"
                  placeholder="单据编号"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">单据类型</span>
                <a-select
                  v-model:value="query.docType"
                  size="small"
                  placeholder="全部单据"
                  allow-clear
                  style="width: 150px"
                  :options="DOC_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">往来单位</span>
                <a-input
                  v-model:value="query.partnerName"
                  size="small"
                  placeholder="客户/供应商"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.handlerName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="query.departmentName"
                  size="small"
                  placeholder="部门"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">单据备注</span>
                <a-input
                  v-model:value="query.remark"
                  size="small"
                  placeholder="单据备注"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">账期</span>
                <a-select
                  v-model:value="query.accountPeriodOp"
                  size="small"
                  style="width: 70px"
                  :options="ACCOUNT_PERIOD_OPS"
                  @change="handleSearch"
                />
                <a-input-number
                  v-model:value="query.accountPeriodDays"
                  size="small"
                  :min="0"
                  :precision="0"
                  placeholder="天数"
                  style="width: 90px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">仓库</span>
                <a-input
                  v-model:value="query.warehouseName"
                  size="small"
                  placeholder="仓库"
                  allow-clear
                  style="width: 140px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">所属区域</span>
                <a-input
                  v-model:value="query.region"
                  size="small"
                  placeholder="所属区域"
                  allow-clear
                  style="width: 140px"
                  @press-enter="handleSearch"
                />
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
              storage-key="analytics-pending-approval-columns"
              global-config-key="analytics-pending-approval-columns"
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
              <template #bizDateCell="{ record }">
                {{ record.bizDate }}
              </template>
              <template #actionCell="{ record }">
                <a-button type="link" size="small" @click="openAudit(record)">审核</a-button>
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

      <!-- ═══ 审核弹窗（行级「审核」/ 批量「审核通过｜驳回」共用） ═══ -->
      <a-modal
        v-model:open="auditVisible"
        :title="auditForm.action === 'approve' ? '审核通过' : '驳回'"
        :width="480"
        :confirm-loading="auditSubmitting"
        @ok="submitAudit"
      >
        <p class="audit-target">
          共 <b>{{ auditTargets.length }}</b> 张单据{{ auditTargets.length === 1 ? `（${auditTargets[0].docType} ${auditTargets[0].docNo}）` : '' }}
        </p>
        <a-textarea
          v-model:value="auditForm.remark"
          :rows="3"
          :placeholder="auditForm.action === 'approve' ? '审批意见（可不填）' : '驳回原因'"
        />
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CheckOutlined, CloseOutlined, PaperClipOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { DocHistoryItem } from '@/api/analytics'
import { ACCOUNT_PERIOD_OPS, DOC_TYPE_OPTIONS, QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { canDo, formatMoney, openDocForm, runBatchDocAction } from '../shared/docActions'
import { useDocQueryTable } from '../shared/useDocQueryTable'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsPendingApproval' })

const router = useRouter()

const quickDate = ref('month')

/** 查询态（逐项对应对标 12 项查询条件） */
const query = reactive({
  docNo: '',
  docType: undefined as string | undefined,
  partnerName: '',
  handlerName: '',
  departmentName: '',
  remark: '',
  accountPeriodOp: 'ge',
  accountPeriodDays: undefined as number | undefined,
  warehouseName: '',
  region: '',
  startDate: quickDateRange('month')[0],
  endDate: quickDateRange('month')[1]
})

/** 金额排序态（对标「金额」列可排序） */
const sortState = reactive({ field: 'bizDate', order: 'desc' })

const {
  loading,
  dataSource,
  pagination,
  summaryAmount,
  fetchData,
  handleSearch,
  handlePageChange,
  handleSort
} = useDocQueryTable('PENDING', () => ({
  docNo: query.docNo,
  docType: query.docType,
  partnerName: query.partnerName,
  handlerName: query.handlerName,
  departmentName: query.departmentName,
  remark: query.remark,
  accountPeriodOp: query.accountPeriodDays != null ? query.accountPeriodOp : undefined,
  accountPeriodDays: query.accountPeriodDays,
  warehouseName: query.warehouseName,
  region: query.region,
  startDate: query.startDate,
  endDate: query.endDate,
  sortField: sortState.field,
  sortOrder: sortState.order
}))

// ═══ 列定义（对标 13 列 / 默认 12：第 10 列「制单人」默认不勾选） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'bizDate', title: '单据日期', type: 'slot', slotName: 'bizDateCell', width: 110 },
  { key: 'docNo', title: '单据编号', type: 'slot', slotName: 'docNoCell', width: 170 },
  { key: 'docType', title: '单据类型', width: 110 },
  { key: 'partnerName', title: '往来单位', width: 180 },
  { key: 'region', title: '所属区域', width: 120 },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'amount', title: '金额', type: 'slot', slotName: 'amountCell', width: 120, align: 'right', sortable: true },
  { key: 'handlerName', title: '经手人', width: 100 },
  { key: 'departmentName', title: '部门', width: 110 },
  { key: 'creatorName', title: '制单人', width: 100, defaultHidden: true },
  { key: 'summary', title: '摘要', width: 180 },
  { key: 'remark', title: '单据备注', width: 180 },
  { key: 'hasAttachment', title: '附件', type: 'slot', slotName: 'hasAttachmentCell', width: 70, align: 'center' }
]

/** 底部合计行（对标：金额列参与合计，取值来自后端按当前过滤范围 SUM） */
const summaryColumns = computed(() => [
  { key: 'amount', value: summaryAmount.value, highlight: false }
])

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

// ═══ 审核 / 驳回 ═══
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const auditTargets = ref<DocHistoryItem[]>([])
const auditForm = reactive({ action: 'approve' as 'approve' | 'reject', remark: '' })

function openAudit(record: DocHistoryItem) {
  auditTargets.value = [record]
  auditForm.action = 'approve'
  auditForm.remark = ''
  auditVisible.value = true
}

function openBatchAudit(action: 'approve' | 'reject') {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要处理的单据')
    return
  }
  // 类型能力校验：按**能力矩阵**（docTypes.ts）筛掉该类型不支持该动作的单据并如实告知，避免整批静默失败
  // ⚠️ 2026-09-23：原为硬编码 `r.docTypeCode !== 'STOCK_DAMAGE'`，只挡了报损单（登记→执行直通流程）。
  //    而 SALE_PRE_ORDER 的能力是 submit/approve/remove（**无 reject**）⇒ 批量驳回时不会被挡，
  //    会被 runBatchDocAction 计入「N 张失败」，与事实（本就不该提交）不符。
  //    改用 canDo(code, action) 后，过滤名单永远与能力矩阵一致，不再随矩阵增删而漂移。
  const capable = selectedRows.value.filter(r => canDo(r.docTypeCode, action))
  const skipped = selectedRows.value.length - capable.length
  if (skipped > 0) message.warning(`已跳过 ${skipped} 张不支持该操作的单据`)
  if (!capable.length) return
  auditTargets.value = capable
  auditForm.action = action
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (auditForm.action === 'reject' && !auditForm.remark.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  auditSubmitting.value = true
  try {
    const payload = auditForm.action === 'approve'
      ? { note: auditForm.remark.trim() || undefined }
      : { reason: auditForm.remark.trim() }
    const { ok, fail } = await runBatchDocAction(auditTargets.value, auditForm.action, payload)
    if (ok) message.success(`已处理 ${ok} 张单据${fail ? `，${fail} 张失败` : ''}`)
    else message.error('处理失败')
    auditVisible.value = false
    clearSelection()
    await fetchData()
  } finally {
    auditSubmitting.value = false
  }
}

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
/** 取当前查询条件快照（不含快捷段高亮态 —— 那是展示态不是查询条件） */
function querySnapshot(): Record<string, any> {
  return {
    docNo: query.docNo, docType: query.docType, partnerName: query.partnerName,
    handlerName: query.handlerName, departmentName: query.departmentName, remark: query.remark,
    accountPeriodOp: query.accountPeriodOp, accountPeriodDays: query.accountPeriodDays,
    warehouseName: query.warehouseName, region: query.region,
    startDate: query.startDate, endDate: query.endDate
  }
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
    docNo: '',
    docType: undefined,
    partnerName: '',
    handlerName: '',
    departmentName: '',
    remark: '',
    accountPeriodOp: 'ge',
    accountPeriodDays: undefined,
    warehouseName: '',
    region: ''
  })
  handleSearch()
}

/**
 * 表头排序（BillDetailTable 只切换图标并 emit，实际排序由后端完成）：
 * 仅「金额」列可排序；点掉排序箭头时回到默认的「单据日期倒序」。
 */
function onSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  if (key === 'amount' && order) {
    handleSort('amount', order, sortState)
  } else {
    handleSort('bizDate', 'desc', sortState)
  }
}

function handleError(err: any) {
  console.error('[待审批单据] 页面异常', err)
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
.audit-target { margin-bottom: 8px; }
</style>
