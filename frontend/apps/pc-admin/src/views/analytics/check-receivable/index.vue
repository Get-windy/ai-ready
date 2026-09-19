<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        查应收（分析 → 财务分析 → 查应收，菜单 80457）
        对标 ql361：**3 个视图 Tab**（查应收 / 职员应收 / 部门应收），逐 Tab 独立列定义 + 独立查询项 +
        独立列配置 storage-key；行级「收款（仅查应收 Tab）/ 对账」；底部合计行。
        列名与顺序逐字取自《查应收开发文档》§3：
          · 查应收   28 列 / 默认 9（defaultHidden 19）
          · 职员应收 16 列 / 默认 8（defaultHidden 8）
          · 部门应收 16 列 / 默认 8（defaultHidden 8）

        取数（真实接口，无硬编码数据）：
          · 主表 auxiliaryBalanceApi.getPage → /erp/finance/auxiliary/balance/page
            subjectCode=1122（应收账款），auxType 逐 Tab 取 CUSTOMER / EMPLOYEE / DEPT，
            按结算单位/职员/部门给出四段余额（期初借贷 / 本期借贷 / 期末借贷）。
          · 辅表（仅查应收 Tab）financeAnalyticsApi.partnerBalancePage → /erp/finance/partner-balance/page
            按结算单位名补齐「预收余额」与对方ID，用于「期末余额 = 应收余额 − 预收余额」与对账联查。
        后端缺口（见开发文档 §5，已在汇报中列出）：无「结款方式 / 优惠 / 预订货款滚动 / 信用额度 / 可用额度 /
        动态收款期限 / 固定账期 / 结算期 / 联系人 / 联系电话 / 客户备注 / 客户一票通 / 已开票未收款 / 收款合计」
        等列的数据源，这些列以空白呈现（不做假数据填充）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-check-receivable\index.vue-query-scheme"
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

        <!-- ═══ 查询区（逐 Tab 独立查询项，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible(`${activeTab}.dateRange`)" class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="query.dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :allow-clear="false"
                  style="width: 230px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.dateType`)" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select
                  v-model:value="query.dateType"
                  size="small"
                  style="width: 120px"
                  disabled
                  title="后端辅助核算余额端点无日期类型条件，待补"
                  :options="DATE_TYPE_OPTIONS"
                />
              </div>

              <!-- 查应收 Tab：结算单位 / 结款方式 / 部门 / 经手人 -->
              <template v-if="activeTab === 'receivable'">
                <div v-if="isQueryVisible('receivable.partnerName')" class="search-item">
                  <span class="search-label">结算单位</span>
                  <a-input
                    v-model:value="query.partnerName"
                    size="small"
                    placeholder="结算单位"
                    allow-clear
                    style="width: 170px"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="isQueryVisible('receivable.settleMethod')" class="search-item">
                  <span class="search-label">结款方式</span>
                  <a-select
                    v-model:value="query.settleMethod"
                    size="small"
                    allow-clear
                    placeholder="全部"
                    style="width: 120px"
                    disabled
                    title="结款方式列与筛选均无后端数据源（应收台账未落该字段），待补"
                    :options="SETTLE_METHOD_OPTIONS"
                  />
                </div>
                <div v-if="isQueryVisible('receivable.departmentName')" class="search-item">
                  <span class="search-label">部门</span>
                  <a-input
                    v-model:value="query.departmentName"
                    size="small"
                    placeholder="部门"
                    allow-clear
                    disabled
                    style="width: 130px"
                    title="后端辅助核算余额端点无部门条件（结算单位维度），待补"
                  />
                </div>
                <div v-if="isQueryVisible('receivable.handlerName')" class="search-item">
                  <span class="search-label">经手人</span>
                  <a-input
                    v-model:value="query.handlerName"
                    size="small"
                    placeholder="经手人"
                    allow-clear
                    disabled
                    style="width: 130px"
                    title="后端辅助核算余额端点无经手人条件，待补"
                  />
                </div>
              </template>

              <!-- 职员应收 Tab：职员 -->
              <div v-if="activeTab === 'staff' && isQueryVisible('staff.staffName')" class="search-item">
                <span class="search-label">职员</span>
                <a-input
                  v-model:value="query.staffName"
                  size="small"
                  placeholder="职员名称"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>

              <!-- 部门应收 Tab：部门 -->
              <div v-if="activeTab === 'dept' && isQueryVisible('dept.departmentName')" class="search-item">
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="query.departmentName"
                  size="small"
                  placeholder="部门"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>

              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>

            <!-- 勾选项：逐 Tab 文案不同（对标实测） -->
            <div class="search-grid search-grid-sub">
              <a-checkbox v-model:checked="query.showZeroPeriod" @change="handleSearch">
                显示有发生期末为0的{{ DIMENSION_LABEL }}
              </a-checkbox>
              <a-checkbox v-model:checked="query.showAllPartners" @change="handleSearch">
                显示全部{{ DIMENSION_LABEL }}
              </a-checkbox>
              <a-checkbox
                v-model:checked="query.includeUnposted"
                disabled
                title="后端辅助核算余额端点无此口径开关，待补"
              >
                包含已审核未记账收款单/预收款单
              </a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（逐 Tab 独立列配置 storage-key + 合计行） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              :storage-key="`analytics-check-receivable-columns-${activeTab}`"
              :global-config-key="`analytics-check-receivable-columns-${activeTab}`"
            >
              <template #codeCell="{ record }">
                <span>{{ record.code || '-' }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="2">
                  <a-button v-if="activeTab === 'receivable'" type="link" size="small" @click="handleReceipt(record)">收款</a-button>
                  <a-button type="link" size="small" @click="openReconcile(record)">对账</a-button>
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

      <!-- ═══ 页面配置（对标有逐 Tab「页面配置弹窗」实测截图 → 逐 Tab 独立 storage-key） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="activeCfg.queryFields.value"
        :function-buttons-config="activeCfg.functionButtons.value"
        :default-query-fields-config="activeQueryDefaults"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="activeCfg.pageConfigStorageKey"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="activeCfg.handlePageConfigChange"
      />

      <!-- ═══ 对账（四段余额明细 + 查应收 Tab 联查该单位应收单据明细，均为真实接口数据） ═══ -->
      <a-modal
        v-model:open="reconcileVisible"
        :title="`对账 · ${reconcileRow?.name || ''}`"
        :width="820"
        :footer="null"
      >
        <a-descriptions
          bordered
          size="small"
          :column="3"
          class="reconcile-desc"
        >
          <a-descriptions-item label="期初应收">{{ formatMoney(reconcileRow?.openingReceivable) }}</a-descriptions-item>
          <a-descriptions-item label="本期应收款">{{ formatMoney(reconcileRow?.currentReceivable) }}</a-descriptions-item>
          <a-descriptions-item :label="activeTab === 'receivable' ? '本期已结' : '本期收款'">{{ formatMoney(reconcileRow?.currentSettled) }}</a-descriptions-item>
          <a-descriptions-item label="本年借方">{{ formatMoney(reconcileRow?.yearDebit) }}</a-descriptions-item>
          <a-descriptions-item label="本年贷方">{{ formatMoney(reconcileRow?.yearCredit) }}</a-descriptions-item>
          <a-descriptions-item label="应收余额">{{ formatMoney(reconcileRow?.receivableBalance) }}</a-descriptions-item>
          <a-descriptions-item label="预收余额">{{ formatMoney(reconcileRow?.preReceiptBalance) }}</a-descriptions-item>
          <a-descriptions-item :label="activeTab === 'receivable' ? '期末余额' : '期末发生余额'">{{ formatMoney(reconcileRow?.endBalance) }}</a-descriptions-item>
          <a-descriptions-item label="科目">{{ reconcileRow?.subjectCode }} {{ reconcileRow?.subjectName }}</a-descriptions-item>
        </a-descriptions>

        <div class="reconcile-detail">
          <div class="reconcile-detail-title">
            应收明细
            <span v-if="activeTab !== 'receivable'" class="reconcile-hint">（对标的「对账」按{{ DIMENSION_LABEL }}维度联查；凭证级明细端点待补）</span>
            <span v-else-if="!reconcileRow?.partnerId" class="reconcile-hint">（未匹配到结算单位主数据，暂无法联查应收单据明细）</span>
          </div>
          <a-table
            v-if="activeTab === 'receivable' && reconcileRow?.partnerId"
            :columns="DETAIL_COLUMNS"
            :data-source="reconcileDetails"
            :loading="reconcileLoading"
            :pagination="false"
            row-key="id"
            size="small"
            :locale="{ emptyText: '暂无应收单据明细' }"
            :scroll="{ y: 240 }"
          >
            <template #bodyCell="{ column, text }">
              <template v-if="['totalAmount', 'paidAmount', 'remainingAmount'].includes(column.dataIndex as string)">
                {{ formatMoney(text) }}
              </template>
            </template>
          </a-table>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined, PrinterOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { financeAnalyticsApi, receivableApi } from '@/api/analytics'
import { auxiliaryBalanceApi } from '@/api/finance'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { formatMoney } from '../shared/docActions'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsCheckReceivable' })

const router = useRouter()

/** 视图 Tab（顺序逐字取自对标实测：查应收 / 职员应收 / 部门应收） */
const TABS = [
  { key: 'receivable', label: '查应收' },
  { key: 'staff', label: '职员应收' },
  { key: 'dept', label: '部门应收' }
]
const activeTab = ref('receivable')

/** 三个 Tab 的核算维度（用于辅助核算余额端点 auxType 与文案） */
const AUX_TYPE: Record<string, string> = { receivable: 'CUSTOMER', staff: 'EMPLOYEE', dept: 'DEPT' }
const DIMENSION_LABEL = computed(() => (activeTab.value === 'receivable' ? '结算单位' : activeTab.value === 'staff' ? '职员' : '部门'))
/** 应收账款科目（辅助核算余额按此科目取数） */
const AR_SUBJECT_CODE = '1122'

const quickDate = ref('month')

const DATE_TYPE_OPTIONS = [
  { label: '单据日期', value: 'bizDate' },
  { label: '记账日期', value: 'bookkeepingDate' }
]
const SETTLE_METHOD_OPTIONS = [
  { label: '挂账', value: 'credit' },
  { label: '现结', value: 'cash' }
]

/** 查询态（逐 Tab 共用日期部分，维度条件各自独立） */
const query = reactive({
  dateRange: quickDateRange('month') as [string, string],
  dateType: 'bizDate',
  partnerName: '',
  settleMethod: undefined as string | undefined,
  departmentName: '',
  handlerName: '',
  staffName: '',
  showZeroPeriod: true,
  showAllPartners: true,
  includeUnposted: false
})

const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const summaryRaw = ref<any>({})
/** 结算单位名 → { partnerId, preReceipt }（往来余额表，用于预收余额与对账联查） */
const partnerMap = ref(new Map<string, { partnerId: string; preReceipt: number }>())

function num(v: any): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

// ═══ Tab1「查应收」列定义：28 列 / 默认 9（defaultHidden 19），逐字取自开发文档 §3 ═══
const receivableColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'code', title: '结算单位编号', type: 'slot', slotName: 'codeCell', width: 120, defaultHidden: true },
  { key: 'name', title: '结算单位', width: 200 },
  { key: 'settleMethod', title: '结款方式', width: 100 },
  { key: 'region', title: '所属区域', width: 120, defaultHidden: true },
  { key: 'defaultHandler', title: '默认经手人', width: 110, defaultHidden: true },
  { key: 'openingReceivable', title: '期初应收', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'currentReceivable', title: '本期应收款', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'currentSettled', title: '本期已结', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'discount', title: '优惠', width: 100, align: 'right', formatter: v => formatMoney(v) },
  { key: 'receivableBalance', title: '应收余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  { key: 'openingPreReceipt', title: '期初预收', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'currentPreReceipt', title: '本期预收', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'preReceiptBalance', title: '预收余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  { key: 'openingPreOrder', title: '期初预订货款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'currentPreOrder', title: '本期预订货款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'preOrderBalance', title: '预订货款余额', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'endBalance', title: '期末余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  { key: 'receiptTotal', title: '收款合计', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'creditLimit', title: '信用额度', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'availableLimit', title: '可用额度', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'dynamicReceiptTerm', title: '动态收款期限', width: 130, defaultHidden: true },
  { key: 'fixedAccountPeriod', title: '固定账期', width: 110, defaultHidden: true },
  { key: 'settlementPeriod', title: '结算期', width: 110, defaultHidden: true },
  { key: 'contactName', title: '联系人', width: 110, defaultHidden: true },
  { key: 'contactPhone', title: '联系电话', width: 130, defaultHidden: true },
  { key: 'customerRemark', title: '客户备注', width: 160, defaultHidden: true },
  { key: 'customerYiPiaoTong', title: '客户一票通', width: 120, defaultHidden: true },
  { key: 'invoicedUnreceived', title: '已开票未收款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true }
]

/** Tab2「职员应收」/ Tab3「部门应收」列定义：16 列 / 默认 8（defaultHidden 8），仅维度列名不同 */
function dimensionColumns(dim: '职员' | '部门'): DetailColumnConfig[] {
  return [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 90, fixed: 'left' },
    { key: 'code', title: `${dim}编号`, type: 'slot', slotName: 'codeCell', width: 120, defaultHidden: true },
    { key: 'name', title: `${dim}名称`, width: 180 },
    { key: 'openingReceivable', title: '此前应收余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
    { key: 'currentReceivable', title: '本期应收款', width: 120, align: 'right', formatter: v => formatMoney(v) },
    { key: 'currentSettled', title: '本期收款', width: 120, align: 'right', formatter: v => formatMoney(v) },
    { key: 'discount', title: '结算优惠', width: 110, align: 'right', formatter: v => formatMoney(v) },
    { key: 'receivableBalance', title: '应收余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
    { key: 'openingPreReceipt', title: '此前预收', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
    { key: 'currentPreReceipt', title: '本期预收', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
    { key: 'preReceiptBalance', title: '预收余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
    { key: 'openingPreOrder', title: '此前预订货款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
    { key: 'currentPreOrder', title: '本期预订货款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
    { key: 'preOrderBalance', title: '预订货款余额', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
    { key: 'endBalance', title: '期末发生余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
    { key: 'receiptTotal', title: '收款合计', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
    { key: 'invoicedUnreceived', title: '已开票未收款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true }
  ]
}

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'receivable') return receivableColumns
  return dimensionColumns(activeTab.value === 'staff' ? '职员' : '部门')
})

/** 合计行：四段余额取后端 summary（全量口径），预收/期末按当前页真实行求和 */
const summaryColumns = computed(() => {
  const s = summaryRaw.value || {}
  const preSum = dataSource.value.reduce((acc, r) => acc + (Number(r.preReceiptBalance) || 0), 0)
  const endSum = dataSource.value.reduce((acc, r) => acc + (Number(r.endBalance) || 0), 0)
  return [
    { key: 'openingReceivable', value: num(s.beginDebit) - num(s.beginCredit) },
    { key: 'currentReceivable', value: num(s.periodDebit) },
    { key: 'currentSettled', value: num(s.periodCredit) },
    { key: 'receivableBalance', value: num(s.endDebit) - num(s.endCredit) },
    { key: 'preReceiptBalance', value: preSum },
    { key: 'endBalance', value: endSum }
  ]
})

// ═══ 页面配置（逐 Tab 独立一套：查询项 + storage-key；功能按钮三 Tab 相同） ═══
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const QUERY_FIELDS_BY_TAB: Record<string, QueryFieldSetting[]> = {
  receivable: [
    { key: 'receivable.dateRange', label: '日期', visible: true },
    { key: 'receivable.dateType', label: '日期类型', visible: true },
    { key: 'receivable.partnerName', label: '结算单位', visible: true },
    { key: 'receivable.settleMethod', label: '结款方式', visible: true },
    { key: 'receivable.departmentName', label: '部门', visible: true },
    { key: 'receivable.handlerName', label: '经手人', visible: true }
  ],
  staff: [
    { key: 'staff.dateRange', label: '日期', visible: true },
    { key: 'staff.dateType', label: '日期类型', visible: true },
    { key: 'staff.staffName', label: '职员', visible: true }
  ],
  dept: [
    { key: 'dept.dateRange', label: '日期', visible: true },
    { key: 'dept.dateType', label: '日期类型', visible: true },
    { key: 'dept.departmentName', label: '部门', visible: true }
  ]
}

/** 逐 Tab 实例化（三个 Tab 各自独立的查询项/按钮配置与持久化键） */
const cfgByTab = {
  receivable: useAnalyticsPageConfig({
    storageKey: 'analytics-check-receivable-page-config-receivable',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.receivable,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  staff: useAnalyticsPageConfig({
    storageKey: 'analytics-check-receivable-page-config-staff',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.staff,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  dept: useAnalyticsPageConfig({
    storageKey: 'analytics-check-receivable-page-config-dept',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.dept,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  })
}
type Cfg = typeof cfgByTab.receivable
const activeCfg = computed<Cfg>(() => (cfgByTab as any)[activeTab.value])
const activeQueryDefaults = computed(() => (QUERY_FIELDS_BY_TAB as any)[activeTab.value])
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  return activeCfg.value.isQueryVisible(key)
}
function isButtonEnabled(key: string): boolean {
  return activeCfg.value.isButtonEnabled(key)
}

// ═══ 取数 ═══
/** 结算单位 → 预收余额/对方ID（往来余额表；仅查应收 Tab 需要，失败不影响主表） */
async function loadPartnerMap() {
  try {
    const res: any = await financeAnalyticsApi.partnerBalancePage({ partnerType: 'customer', page: 1, size: 500 })
    const next = new Map<string, { partnerId: string; preReceipt: number }>()
    for (const r of res?.records || []) {
      if (r?.partnerName) next.set(String(r.partnerName), { partnerId: String(r.partnerId), preReceipt: num(r.preReceiptBalance) })
    }
    partnerMap.value = next
  } catch (e) {
    console.warn('[查应收] 往来余额取数失败，预收余额列将为空白', e)
    partnerMap.value = new Map()
  }
}

/** 逐 Tab 组装辅助核算余额查询参数（主表与导出共用，保证同一过滤口径） */
function buildAuxParams(pageNum: number, pageSize: number) {
  const [start, end] = query.dateRange
  const tab = activeTab.value
  const keyword = tab === 'receivable'
    ? (query.partnerName || undefined)
    : tab === 'staff' ? (query.staffName || undefined) : (query.departmentName || undefined)
  return {
    auxType: AUX_TYPE[tab],
    subjectCode: AR_SUBJECT_CODE,
    startMonth: start?.slice(0, 7),
    endMonth: end?.slice(0, 7),
    keyword,
    hideZeroBalance: query.showAllPartners ? undefined : true,
    hideNoPeriodAmount: query.showZeroPeriod ? undefined : true,
    pageNum,
    pageSize
  }
}

/** 辅助核算行 → 表格行（对应对标滚动台账的列口径） */
function normalize(r: any, idx = 0) {
  const openingReceivable = num(r.beginDebit) - num(r.beginCredit)
  const receivableBalance = num(r.endDebit) - num(r.endCredit)
  const mate = partnerMap.value.get(String(r.auxName))
  const preReceiptBalance = mate ? mate.preReceipt : undefined
  return {
    rowKey: `${r.auxCode || r.auxName || 'row'}-${idx}`,
    code: r.auxCode,
    name: r.auxName,
    partnerId: mate?.partnerId,
    subjectCode: r.subjectCode,
    subjectName: r.subjectName,
    openingReceivable,
    currentReceivable: num(r.periodDebit),
    currentSettled: num(r.periodCredit),
    receivableBalance,
    yearDebit: num(r.yearDebit),
    yearCredit: num(r.yearCredit),
    preReceiptBalance,
    endBalance: preReceiptBalance === undefined ? undefined : receivableBalance - preReceiptBalance
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await auxiliaryBalanceApi.getPage(buildAuxParams(pagination.current, pagination.pageSize))
    dataSource.value = (res?.records || []).map((r: any, idx: number) => normalize(r, idx))
    pagination.total = Number(res?.total) || 0
    summaryRaw.value = res?.summary || {}
  } catch (e: any) {
    console.warn('[查应收] 取数失败', e)
    message.error('获取应收台账失败')
    dataSource.value = []
    pagination.total = 0
    summaryRaw.value = {}
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  return fetchData()
}

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { ...query }
}

function applyQuerySnapshot(v: Record<string, any>) {
  Object.assign(query, v)
  if (typeof quickDate !== 'undefined') quickDate.value = ''
  handleSearch()
}

function handleSearch() {
  pagination.current = 1
  return fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  return fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const [start, end] = quickDateRange(key)
  query.dateRange = [start, end]
  handleSearch()
}

function handleRefresh() {
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    dateRange: quickDateRange('month'),
    dateType: 'bizDate',
    partnerName: '',
    settleMethod: undefined,
    departmentName: '',
    handlerName: '',
    staffName: '',
    showZeroPeriod: true,
    showAllPartners: true,
    includeUnposted: false
  })
  handleSearch()
}

// ═══ 行级动作 ═══
/** 收款：跳收款单表单页（财务域既有路由，不新建端点） */
function handleReceipt(record: any) {
  router.push({ path: '/finance/receipt-doc/form', query: { partnerName: record.name } })
}

const reconcileVisible = ref(false)
const reconcileLoading = ref(false)
const reconcileRow = ref<any>(null)
const reconcileDetails = ref<any[]>([])
const DETAIL_COLUMNS = [
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 160 },
  { title: '应收总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '已收金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 110, align: 'right' },
  { title: '未收余额', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 110, align: 'right' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 }
]

async function openReconcile(record: any) {
  reconcileRow.value = record
  reconcileDetails.value = []
  reconcileVisible.value = true
  if (activeTab.value !== 'receivable' || !record.partnerId) return
  reconcileLoading.value = true
  try {
    const res: any = await receivableApi.getPage({ customerId: record.partnerId, page: 1, size: 20 })
    reconcileDetails.value = res?.records || []
  } catch (e) {
    console.warn('[查应收] 应收明细取数失败', e)
    message.error('获取应收明细失败')
  } finally {
    reconcileLoading.value = false
  }
}

// ═══ 打印(F8) ═══
const printColumns = computed(() => activeColumns.value.filter(c => c.key !== 'action' && c.key !== 'rowNo'))

function handlePrint() {
  const header = printColumns.value.map(c => c.title)
  const body = dataSource.value.map(r => printColumns.value.map(c => {
    const v = r[c.key]
    if (v === undefined || v === null) return ''
    return c.align === 'right' ? formatMoney(v) : String(v)
  }))
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const total = printColumns.value.map(c => {
    const found = summaryColumns.value.find(s => s.key === c.key)
    return found ? formatMoney(found.value) : ''
  })
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '查应收'
  const html = `<html><head><meta charset="utf-8"><title>${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}
    tfoot td{font-weight:600}</style></head><body>
    <h3>${tabLabel}（${query.dateRange[0]} ~ ${query.dateRange[1]}）</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    <tfoot><tr>${total.map((v, i) => `<td>${i === 0 ? '合计' : v}</td>`).join('')}</tr></tfoot>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV；逐 Tab 同口径取全量） ═══
const { execute: executeExport, exporting } = useExport()
const EXPORT_HEADERS = computed(() => printColumns.value.map(c => c.title))

async function fetchAllRows(): Promise<any[]> {
  const size = 200
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await auxiliaryBalanceApi.getPage(buildAuxParams(p, size))
    const list = res?.records || []
    all.push(...list.map((r: any, idx: number) => normalize(r, idx)))
    if (list.length < size) break
  }
  return all
}

function toExportRow(r: any): string[] {
  return printColumns.value.map(c => {
    const v = r[c.key]
    if (v === undefined || v === null) return ''
    return c.align === 'right' ? formatMoney(v) : String(v)
  })
}

function handleExport() {
  executeExport({
    fileName: `查应收-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: EXPORT_HEADERS.value,
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(toExportRow),
    fallbackRows: () => dataSource.value.map(toExportRow)
  })
}

function handleError(err: any) {
  console.error('[查应收] 页面异常', err)
}

onMounted(async () => {
  await loadPartnerMap()
  await fetchData()
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
.search-grid-sub { margin-top: 8px; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.reconcile-desc { margin-bottom: 12px; }
.reconcile-detail-title { margin-bottom: 8px; font-weight: 600; }
.reconcile-hint { font-weight: 400; color: #999; font-size: 12px; }
</style>
