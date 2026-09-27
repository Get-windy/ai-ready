<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        业绩提成中心（分析 → 提成分析 → 业绩提成中心，菜单 80443）
        对标 ql361「业绩提成中心」：6 个视图 Tab（配送员 / 每月提成 / 提成构成 / 方案汇总提成 / 业绩概览 / 业绩明细），
        逐 Tab 独立列配置，列数 2/2、2/2、7/7、5/5、21/15、21/10。
        -「配送员 / 每月提成」为人员 × 12 月矩阵（矩阵列 1月~12月 + 合计 由运行期生成，**不进列配置弹窗**）；
        - 结算顺序硬约束（对标提示条逐字）：「若存在某月份提成未结算，此后月份提成将不显示」；
        - 工具栏：刷新 | 提成方案 | 批量结算（仅前两个 Tab）| 打印(F8) | 导出 | 页面配置。
        取数：/erp/marketing/commission/analytics/*（后端 CommissionAnalyticsController）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="onTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 年度/时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            v-if="hasSchemeBar"
            storage-key="analytics-commission-center-query-scheme"
            :snapshot="schemeSnapshot"
            @apply="applyScheme"
          />
          <!-- 矩阵类 Tab：年度口径（去年 / 本年 + 年份下拉） -->
          <template v-if="isYearTab">
            <a-space :size="4" class="quick-dates">
              <a-button size="small" :type="yearQuick === 'last' ? 'primary' : 'link'" @click="setYearQuick('last')">去年</a-button>
              <a-button size="small" :type="yearQuick === 'current' ? 'primary' : 'link'" @click="setYearQuick('current')">本年</a-button>
            </a-space>
            <a-select v-model:value="query.year" size="small" style="width: 110px" :options="yearOptions" @change="handleSearch" />
          </template>
          <!-- 业绩明细：日期区间口径 -->
          <template v-if="activeTab === 'detail'">
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
        </template>

        <!-- ═══ 工具栏右侧：刷新｜提成方案｜批量结算｜打印(F8)｜导出｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('plan') && isYearTab" size="small" @click="openPlanModal">
              <ProfileOutlined /> 提成方案
            </a-button>
            <a-button v-if="isButtonEnabled('settle') && isYearTab" size="small" type="primary" @click="openSettleModal">
              <CheckCircleOutlined /> 批量结算
            </a-button>
            <a-button v-if="isButtonEnabled('print')" size="small" @click="handlePrint">
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

        <!-- ═══ 查询区（逐 Tab 不同，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <!-- 公共：配送员 / 角色（矩阵、提成构成；方案汇总提成无此两项） -->
              <div v-if="queryVisible('rider')" class="search-item">
                <span class="search-label">配送员</span>
                <a-input
                  v-model:value="query.riderName"
                  size="small"
                  placeholder="配送员"
                  allow-clear
                  style="width: 140px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="queryVisible('role')" class="search-item">
                <span class="search-label">配送员角色</span>
                <a-select
                  v-model:value="query.roleKey"
                  size="small"
                  style="width: 150px"
                  :options="ROLE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <!-- 提成构成 / 方案汇总提成：提成方案 + 提成类型 -->
              <div v-if="queryVisible('plan')" class="search-item">
                <span class="search-label">提成方案</span>
                <a-input
                  v-model:value="query.planName"
                  size="small"
                  placeholder="方案名称"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="queryVisible('type')" class="search-item">
                <span class="search-label">提成类型</span>
                <a-select
                  v-model:value="query.typeKey"
                  size="small"
                  style="width: 140px"
                  :options="TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <!-- 业绩明细：日期范围 / 日期类型 / 司机 / 部门 / 仓库 / 商品 -->
              <template v-if="activeTab === 'detail'">
                <div class="search-item">
                  <span class="search-label">日期</span>
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    value-format="YYYY-MM-DD"
                    :allow-clear="false"
                    style="width: 240px"
                    @change="handleSearch"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">日期类型</span>
                  <a-select
                    v-model:value="query.dateType"
                    size="small"
                    style="width: 150px"
                    :options="[{ label: '指定配送日期', value: 'deliveryDate' }]"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">部门</span>
                  <a-input v-model:value="query.deptName" size="small" placeholder="部门" allow-clear style="width: 120px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">仓库</span>
                  <a-input v-model:value="query.warehouseName" size="small" placeholder="仓库" allow-clear style="width: 120px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">商品</span>
                  <a-input v-model:value="query.productName" size="small" placeholder="商品/货号" allow-clear style="width: 150px" @press-enter="handleSearch" />
                </div>
              </template>
              <div class="search-item search-actions">
                <a-checkbox
                  v-if="queryVisible('hideZero')"
                  v-model:checked="query.hideZero"
                  @change="handleSearch"
                >
                  不显示为零数据项
                </a-checkbox>
                <span v-if="activeTab === 'overview'" class="search-label">（业绩概览查询条件对标未抓取，本系统按配送员/角色口径提供）</span>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
            <div v-if="activeTab === 'riders' || activeTab === 'monthly'" class="search-tip">
              若存在某月份提成未结算，此后月份提成将不显示
            </div>
            <div v-if="activeTab === 'composition' || activeTab === 'planSummary'" class="hint-text">
              列表仅显示已产生业绩数据的方案提成数据
            </div>
            <div v-if="activeTab === 'monthly'" class="hint-text">
              本系统「配送员」与「每月提成」两 Tab 的列配置与对标逐字相同（2/2），
              对标侧两者的业务差异未取到实据，本系统按同一矩阵口径输出（已登记）。
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置，逐 Tab 一套） ═══ -->
        <template #table>
          <div class="table-area" :data-cols="colSummary">
            <BillDetailTable
              :data-source="rows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              :storage-key="`analytics-commission-center-columns-${activeTab}`"
              :global-config-key="`analytics-commission-center-columns-${activeTab}`"
            >
              <!-- 年度矩阵列（1月~12月 + 合计）：运行期生成，不进列配置弹窗 -->
              <template #monthCell="{ record, column }">
                {{ monthCellText(record, column) }}
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.page"
            :page-size="pagination.size"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 工具栏「提成方案」（只读入口：真实方案主数据） ═══ -->
      <a-modal v-model:open="planVisible" title="提成方案" :width="900" :footer="null">
        <a-table
          :data-source="planRows"
          :columns="PLAN_COLUMNS"
          :loading="planLoading"
          size="small"
          row-key="ruleCode"
          :pagination="{ pageSize: 10, size: 'small' }"
        />
      </a-modal>

      <!-- ═══ 工具栏「批量结算」 ═══ -->
      <a-modal
        v-model:open="settleVisible"
        title="批量结算"
        :width="460"
        :confirm-loading="settleLoading"
        ok-text="确认结算"
        @ok="submitSettle"
      >
        <a-form layout="vertical">
          <a-form-item label="结算年度">
            <a-select v-model:value="settleYear" style="width: 100%" :options="yearOptions" />
          </a-form-item>
          <a-form-item label="结算月份">
            <a-select v-model:value="settleMonth" style="width: 100%" :options="monthOptions" />
          </a-form-item>
          <div class="hint-text">结算后该月提成置为「已结算」并锁定；存在未结算的更早月份时将被拒绝。</div>
        </a-form>
      </a-modal>

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

    <!-- 打印：结果集打印（列与行由页面给，模板负责版式） -->
    <PrintDialog
      ref="printDialogRef"
      page-code="analytics-commission-center"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined, DownloadOutlined, PrinterOutlined, ProfileOutlined,
  ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { commissionAnalyticsApi } from '@/api/analytics-finance'
import { useExport } from '@/composables/useExport'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsCommissionCenter' })

// ═══ 视图 Tab（顺序逐字取自对标实测：配送员 / 每月提成 / 提成构成 / 方案汇总提成 / 业绩概览 / 业绩明细） ═══
const TABS = [
  { key: 'riders', label: '配送员' },
  { key: 'monthly', label: '每月提成' },
  { key: 'composition', label: '提成构成' },
  { key: 'planSummary', label: '方案汇总提成' },
  { key: 'overview', label: '业绩概览' },
  { key: 'detail', label: '业绩明细' }
]
const activeTab = ref('riders')
const isYearTab = computed(() => ['riders', 'monthly', 'composition', 'planSummary'].includes(activeTab.value))
/** 对标：业绩明细无「查询方案」下拉（文档 §2 Tab6 查询项未含），其余 Tab 均有 */
const hasSchemeBar = computed(() => activeTab.value !== 'detail' && activeTab.value !== 'overview')

const thisYear = new Date().getFullYear()
const yearOptions = [thisYear, thisYear - 1, thisYear - 2].map(y => ({ label: `${y}年`, value: y }))
const monthOptions = Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))

const ROLE_OPTIONS = [
  { label: '全部', value: '全部' },
  { label: '企业员工', value: '企业员工' },
  { label: '众包兼职', value: '众包兼职' },
  { label: '外部平台配送员', value: '外部平台配送员' },
  { label: '社会车辆司机', value: '社会车辆司机' }
]
const TYPE_OPTIONS = [
  { label: '全部', value: '全部' }
]

const yearQuick = ref('current')
const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  year: thisYear,
  riderName: '',
  roleKey: '全部',
  planName: '',
  typeKey: '全部',
  hideZero: false,
  dateType: 'deliveryDate',
  deptName: '',
  warehouseName: '',
  productName: ''
})

const loading = ref(false)
const rows = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const summary = ref<Record<string, any>>({})

// ═══ 数值格式化 ═══
function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}
function fmtMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtText(v: any): string {
  return v === null || v === undefined || v === '' ? '-' : String(v)
}

type Kind = 'money' | 'num' | 'text'
const FORMATTER: Record<Kind, (v: any) => string> = {
  money: fmtMoney,
  num: fmtNum,
  text: fmtText
}

function col(key: string, title: string, kind: Kind, width = 120, hidden = false): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: kind === 'text' ? 'left' : 'right',
    formatter: FORMATTER[kind],
    ...(hidden ? { defaultHidden: true } : {})
  }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

/**
 * 运行期矩阵列（1月~12月 + 合计）：对标不进列配置弹窗。
 * 借 key='checkbox' 命中组件锁定列判定（渲染由 type:'slot' 决定）；取值在插槽按列名换算。
 */
function monthCol(title: string): DetailColumnConfig {
  return { key: 'checkbox', title, type: 'slot', slotName: 'monthCell', width: 100, align: 'right' }
}
const MONTH_MATRIX: DetailColumnConfig[] = [
  ...Array.from({ length: 12 }, (_, i) => monthCol(`${i + 1}月`)),
  monthCol('合计')
]

function monthCellText(record: any, column: any): string {
  const title = String(column?.title || '')
  if (title === '合计') return fmtMoney(record?.total)
  const n = Number(title.replace('月', ''))
  if (!n) return '-'
  return fmtMoney(record?.[`m${String(n).padStart(2, '0')}`])
}

// ── Tab1「配送员」/ Tab2「每月提成」：全部 2 列 / 默认 2（另加 12 个月份列 + 合计，不进列配置） ──
const matrixColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('riderName', '配送员', 'text', 150),
  col('duty', '职务', 'text', 130),
  ...MONTH_MATRIX
]

// ── Tab3「提成构成」：全部 7 列 / 默认 7（全可见） ──
const compositionColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('riderName', '配送员', 'text', 140),
  col('duty', '职务', 'text', 120),
  col('planName', '提成方案名称', 'text', 180),
  col('commissionType', '提成类型', 'text', 120),
  col('commissionRule', '提成规则', 'text', 200),
  col('commissionAmount', '提成金额', 'money', 130),
  col('planDesc', '方案描述', 'text', 200)
]

// ── Tab4「方案汇总提成」：全部 5 列 / 默认 5（全可见） ──
const planSummaryColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('planName', '提成方案名称', 'text', 180),
  col('commissionType', '提成类型', 'text', 120),
  col('commissionRule', '提成规则', 'text', 220),
  col('commissionAmount', '提成金额', 'money', 130),
  col('planDesc', '方案描述', 'text', 220)
]

// ── Tab5「业绩概览」：全部 21 列 / 默认 15（默认隐藏 6） ──
const overviewColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('riderCode', '配送员编号', 'text', 130),
  col('riderName', '配送员名称', 'text', 140),
  col('deptName', '所属部门', 'text', 120),
  col('deliverDocCount', '配送单量', 'num', 110),
  col('returnDocCount', '退货单量', 'num', 110),
  col('bestRouteKm', '最优路线（Km）', 'num', 130),
  col('deliverMileage', '配送里程(km)', 'num', 130),
  col('boxingQty', '装箱数量', 'num', 110),
  col('deliverTimes', '配送次数', 'num', 110),
  col('shipQty', '发货数量', 'num', 110, true),
  col('shipAmount', '发货商品金额(元)', 'money', 150),
  col('returnQty', '退货数量', 'num', 110),
  col('returnAmount', '退货商品金额', 'money', 130),
  col('weight', '重量（kg）', 'num', 110, true),
  col('volume', '体积（m³）', 'num', 110, true),
  col('smallPack', '小包装', 'num', 100, true),
  col('midPack', '中包装', 'num', 100, true),
  col('bigPack', '大包装', 'num', 100, true),
  col('grossProfit', '配送毛利总额', 'money', 130),
  col('packageQty', '配送包件数', 'num', 120),
  col('pointCount', '配送点位', 'num', 110)
]

// ── Tab6「业绩明细」：全部 21 列 / 默认 10（默认隐藏 11） ──
const detailColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('riderCode', '司机编号', 'text', 130, true),
  col('riderName', '司机名称', 'text', 140),
  col('deptName', '部门', 'text', 120),
  col('productName', '商品名称', 'text', 200),
  col('productCode', '货号', 'text', 120, true),
  col('unit', '单位', 'text', 80),
  col('shipQty', '发货数量', 'num', 110),
  col('shipAmount', '发货金额(元)', 'money', 130),
  col('returnQty', '退货数量', 'num', 110, true),
  col('returnAmount', '退货金额', 'money', 120),
  col('giftQty', '赠品数量', 'num', 110),
  col('conversionRelation', '换算关系', 'text', 130, true),
  col('conversionResult', '换算结果', 'text', 120),
  col('smallPack', '小包装', 'num', 100, true),
  col('midPack', '中包装', 'num', 100, true),
  col('bigPack', '大包装', 'num', 100, true),
  col('weight', '重量（kg）', 'num', 110, true),
  col('volume', '体积（m³）', 'num', 110, true),
  col('smallUnit', '小单位', 'text', 100, true),
  col('smallUnitQty', '小单位数量', 'num', 120, true),
  col('grossProfit', '商品毛利', 'money', 120)
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  switch (activeTab.value) {
    case 'monthly': return matrixColumns
    case 'composition': return compositionColumns
    case 'planSummary': return planSummaryColumns
    case 'overview': return overviewColumns
    case 'detail': return detailColumns
    default: return matrixColumns
  }
})

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 2/2、2/2、7/7、5/5、21/15、21/10） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo' && c.key !== 'checkbox')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

const SUMMARY_KEYS = ['commissionAmount', 'deliverDocCount', 'returnDocCount', 'deliverMileage', 'boxingQty',
  'deliverTimes', 'shipQty', 'shipAmount', 'returnQty', 'returnAmount', 'weight', 'volume',
  'packageQty', 'pointCount', 'grossProfit', 'total']
const summaryColumns = computed(() =>
  SUMMARY_KEYS.filter(k => leafColumns.value.some(c => c.key === k))
    .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'rider', label: '配送员', visible: true },
  { key: 'role', label: '配送员角色', visible: true },
  { key: 'plan', label: '提成方案', visible: true },
  { key: 'type', label: '提成类型', visible: true },
  { key: 'hideZero', label: '不显示为零数据项', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'plan', label: '提成方案', enabled: true },
  { key: 'settle', label: '批量结算', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const {
  showPageConfig, queryFields, functionButtons, isQueryVisible,
  isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-commission-center-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

/**
 * 查询项可见性 = **页面配置勾选** ∧ **当前 Tab 是否有该查询项**
 *
 * ⚠️ 2026-09-23 修复：原实现只按 `activeTab` 判断、完全没读页面配置
 *    （`isQueryVisible` 未解构），导致「页面配置」弹窗里取消勾选任一查询项后查询区毫无变化
 *    —— 21 个接了 PageConfigPanel 的页面里唯独本页如此（其余页模板都用 `v-if="isQueryVisible(...)"`）。
 *    对标口径：本页逐 Tab 查询区不同（如「配送员」只在配送员/每月提成/提成构成 3 个 Tab 出现）。
 */
function queryVisible(key: string): boolean {
  if (!isQueryVisible(key)) return false
  if (activeTab.value === 'detail') return false
  if (key === 'rider' || key === 'role') return activeTab.value === 'riders' || activeTab.value === 'monthly' || activeTab.value === 'composition'
  if (key === 'plan' || key === 'type' || key === 'hideZero') {
    return activeTab.value === 'composition' || activeTab.value === 'planSummary'
  }
  return false
}

// ═══ 查询方案 ═══
function schemeSnapshot(): Record<string, any> {
  return { dateRange: [...(dateRange.value || [])], quickDate: quickDate.value, yearQuick: yearQuick.value, ...query }
}
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  if (typeof v.yearQuick === 'string') yearQuick.value = v.yearQuick
  Object.keys(query).forEach(k => { if (k in v) (query as any)[k] = v[k] })
  handleSearch()
}

// ═══ 取数 ═══
function buildParams(extra: Record<string, any> = {}) {
  return {
    tab: activeTab.value,
    year: isYearTab.value ? query.year : undefined,
    startDate: activeTab.value === 'detail' ? dateRange.value?.[0] : undefined,
    endDate: activeTab.value === 'detail' ? dateRange.value?.[1] : undefined,
    riderName: query.riderName || undefined,
    roleKey: query.roleKey && query.roleKey !== '全部' ? query.roleKey : undefined,
    planName: query.planName || undefined,
    typeKey: query.typeKey && query.typeKey !== '全部' ? query.typeKey : undefined,
    hideZero: query.hideZero || undefined,
    deptName: query.deptName || undefined,
    warehouseName: query.warehouseName || undefined,
    productName: query.productName || undefined,
    page: pagination.page,
    size: pagination.size,
    ...extra
  }
}

async function callApi(extra: Record<string, any> = {}) {
  const params = buildParams(extra)
  switch (activeTab.value) {
    case 'monthly': return commissionAnalyticsApi.riderMatrix(params)
    case 'composition': return commissionAnalyticsApi.composition(params)
    case 'planSummary': return commissionAnalyticsApi.planSummary(params)
    case 'overview': return commissionAnalyticsApi.performanceOverview(params)
    case 'detail': return commissionAnalyticsApi.performanceDetail(params)
    default: return commissionAnalyticsApi.riderMatrix(params)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await callApi()
    rows.value = (res?.records || []).map((r: any, i: number) => ({ ...r, rowKey: `${activeTab.value}-${r.riderName || i}-${i}` }))
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[业绩提成中心] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.page = 1; fetchData() }
function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}
function handleReset() {
  query.riderName = ''
  query.roleKey = '全部'
  query.planName = ''
  query.typeKey = '全部'
  query.hideZero = false
  query.deptName = ''
  query.warehouseName = ''
  query.productName = ''
  yearQuick.value = 'current'
  query.year = thisYear
  quickDate.value = 'month'
  dateRange.value = quickDateRange('month') as [string, string]
  handleSearch()
}
function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}
function setYearQuick(key: 'last' | 'current') {
  yearQuick.value = key
  query.year = key === 'last' ? thisYear - 1 : thisYear
  handleSearch()
}
function onTabChange(key: string) {
  activeTab.value = key
  pagination.page = 1
  fetchData()
}

// ═══ 工具栏「提成方案」 ═══
const planVisible = ref(false)
const planLoading = ref(false)
const planRows = ref<any[]>([])
const PLAN_COLUMNS = [
  { title: '规则编码', dataIndex: 'ruleCode', width: 130 },
  { title: '方案名称', dataIndex: 'ruleName', width: 160 },
  { title: '提成类型', dataIndex: 'commissionType', width: 110 },
  { title: '计算基础', dataIndex: 'calcBasis', width: 110 },
  { title: '计算方式', dataIndex: 'calcMethod', width: 110 },
  { title: '佣金值', dataIndex: 'commissionValue', width: 100, align: 'right' as const },
  { title: '最低订单金额', dataIndex: 'minOrderAmount', width: 130, align: 'right' as const },
  { title: '单笔佣金上限', dataIndex: 'maxCommission', width: 130, align: 'right' as const },
  { title: '状态', dataIndex: 'statusText', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 160 }
]

async function openPlanModal() {
  planVisible.value = true
  planLoading.value = true
  planRows.value = []
  try {
    planRows.value = (await commissionAnalyticsApi.plans()) || []
    if (!planRows.value.length) message.info('暂无提成方案数据')
  } catch (e) {
    console.warn('[业绩提成中心] 提成方案取数失败', e)
    message.error('获取提成方案失败')
  } finally {
    planLoading.value = false
  }
}

// ═══ 工具栏「批量结算」 ═══
const settleVisible = ref(false)
const settleLoading = ref(false)
const settleYear = ref(thisYear)
const settleMonth = ref(new Date().getMonth() + 1)

function openSettleModal() {
  settleYear.value = query.year
  settleVisible.value = true
}

async function submitSettle() {
  settleLoading.value = true
  try {
    const res = await commissionAnalyticsApi.settle({ year: settleYear.value, month: settleMonth.value })
    message.success(`结算完成，共结算 ${res?.settledCount ?? 0} 条提成记录`)
    settleVisible.value = false
    fetchData()
  } catch (e: any) {
    console.warn('[业绩提成中心] 批量结算失败', e)
    message.error(e?.response?.data?.message || e?.message || '结算失败')
  } finally {
    settleLoading.value = false
  }
}

// ═══ 打印(F8) / 导出 ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && !c.defaultHidden))

function cellText(c: DetailColumnConfig, r: any): string {
  if (c.type === 'slot' && c.slotName === 'monthCell') return monthCellText(r, c)
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : fmtText(raw)
}

const currentTabLabel = computed(() => TABS.find(t => t.key === activeTab.value)?.label || '业绩提成中心')

/** 打印标题里的区间：矩阵类 Tab 是年度口径，业绩明细是日期区间（对标两者查询口径不同） */
const printPeriod = computed(() => {
  if (activeTab.value === 'detail') return `${dateRange.value?.[0] || ''} ~ ${dateRange.value?.[1] || ''}`
  if (isYearTab.value) return `${query.year}年`
  return ''
})

/**
 * 打印用的**拍平**模型 —— 透视上纸的老办法：列 = [分组字段, ...该透视当前的指标列]，行 = 拍平后的叶子行。
 *
 * 默认 Tab「配送员 / 每月提成」是人员 × 12 月矩阵：`1月~12月 + 合计` 是运行期列
 * （13 列共用 `key='checkbox'`、取值在 `monthCell` 插槽里按列名换算），
 * 直接交给统一列模型会**整列变空**（引擎按 key 取值，矩阵列没有对应的数据字段，key 也不唯一）。
 * 所以这里把每个矩阵列拍成一个唯一 key 的普通列（`m01~m12` / `total` —— 正好就是行数据里的字段名），
 * 取值沿用改造前的 `monthCellText` / `cellText`（钱两位小数 + 千分位、空值 '-'）：
 * 模板按数据列画表头、数据列不含 agg 合计（合计走 pageFooter 的 totalText），
 * 先格式化成显示文本才能与改造前自建 HTML 的单元格逐字一致。
 */
const printModel = computed(() => {
  const columns: Array<{ key: string; title: string; align?: string }> = []
  const cells: Array<(r: any) => string> = []
  for (const c of printableColumns.value) {
    if (c.type === 'slot' && c.slotName === 'monthCell') {
      const title = String(c.title || '')
      const key = title === '合计' ? 'total' : `m${String(title.replace('月', '')).padStart(2, '0')}`
      columns.push({ key, title, align: c.align })
      cells.push((r: any) => monthCellText(r, c))
    } else {
      columns.push({ key: String(c.key), title: String(c.title), align: c.align })
      cells.push((r: any) => cellText(c, r))
    }
  }
  const flatRows = rows.value.map(r => {
    const flat: Record<string, any> = {}
    columns.forEach((col, i) => { flat[col.key] = cells[i](r) })
    return flat
  })
  return { columns, rows: flatRows }
})

/** 页脚合计：与屏幕表尾同口径（逐列取该列自己的 formatter），前面缀条数 */
function printTotalText(): string {
  const colOf = new Map(leafColumns.value.map(c => [c.key, c]))
  const parts = summaryColumns.value.map(s => {
    const c = colOf.get(s.key)
    const text = c?.formatter ? c.formatter(s.value, summary.value) : fmtNum(s.value)
    return `${c?.title || s.key} ${text}`
  })
  const count = `共 ${pagination.total || rows.value.length} 条`
  return parts.length ? `${count}；合计 ${parts.join('，')}` : count
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open 打印窗口（h3 + 一张表：当前可见列 × 当前页行），
// 现在交给 PrintDialog：列与行由页面给（矩阵 Tab 已按上面的拍平模型展开），模板负责版式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-commission-center',
  title: () => `业绩提成中心 · ${currentTabLabel.value}${printPeriod.value ? `（${printPeriod.value}）` : ''}`,
  useDataColumns: true,
  columns: () => printModel.value.columns,
  rows: () => printModel.value.rows,
  totalText: printTotalText,
  emptyTip: '没有可打印的数据'
})

function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 200
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await callApi({ page: p, size })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `业绩提成中心-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[业绩提成中心] 页面异常', err)
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
/* 插槽内容不受宿主 scoped 样式影响，查询区样式随页面自带 */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }
.search-tip {
  margin-top: 6px; padding: 2px 8px; display: inline-block;
  background: #fffbe6; border: 1px solid #ffe58f; border-radius: 2px;
  color: #ad6800; font-size: 12px;
}
.hint-text { margin-top: 6px; color: #999; font-size: 12px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
