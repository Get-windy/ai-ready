<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        交易分析（分析 → 营销分析 → 交易分析，菜单 80474）
        对标 ql361「分析 → 商城分析 → 交易分析」：**单视图**统计报表（查询方案 + 时间快捷段 + 日期范围 +
        按天/按周/按月 粒度切换 + 表格 + 合计行）。对标本页**无打印(F8)、无导出**，故不写 handlePrint/handleExport。
        列数口径（对标 2026-09-15 全局配置实测）：7 列 / 默认 7（全可见），无 defaultHidden，无固定「操作」列。

        ⚠️ 后端硬缺口（已核实后端源码，已在开发文档「剩余缺口」与验收汇报中登记）：
          对标 7 列中的 `登录客户数 / 下单客户数 / 下单转化率(%)` **无真实数据源** ——
          `MallTradeAnalysisController` + `MallTradeAnalysisMapper` 只聚合 erp_sale_order（order_source IN (2,3)），
          DTO（summary/daily/paymentStatusDistribution）无任何登录相关字段；全库亦**无商城登录日志/访问埋点表**
          （仅 `sys_login_log`，属 ERP 后台账号登录审计，与商城客户无关）。
          故这 3 列**不渲染**（禁返回 0 / 占位假值），最终 4 列：日期 / 下单笔数 / 下单金额 / 客单价。
          其中 `客单价` 取后端 avgOrderAmount = 下单金额 ÷ **下单笔数**，
          对标口径为 ÷ **下单客户数**（客户数取不到，分母口径差异已在文档登记）。
          另：对标「图形」视图切换无图表数据源，本页未接；支付状态分布为本系统附加模块，对标无此块，已移除。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段（对标 8 段） ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-trade-analysis-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
          <a-space
            :size="4"
            class="quick-dates"
          >
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

        <!-- ═══ 工具栏右侧：刷新（对标实测 图形｜刷新，图形无数据源故只接刷新） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（日期范围 + 粒度切换，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">日期范围</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 240px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">统计粒度</span>
                <a-radio-group
                  v-model:value="granularity"
                  size="small"
                  option-type="button"
                  button-style="solid"
                  :options="GRANULARITY_OPTIONS"
                  @change="handleGranularityChange"
                />
              </div>
              <div class="search-item search-actions">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleReset"
                >
                  重置
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（表头序号列齿轮承载列配置）+ 底部合计行（量额列求和，比率列不合计） ═══ -->
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
              storage-key="analytics-trade-analysis-columns"
              global-config-key="analytics-trade-analysis-columns"
            >
              <template #gmvCell="{ record }">
                {{ formatMoney(record.gmv) }}
              </template>
              <template #avgOrderAmountCell="{ record }">
                {{ formatMoney(record.avgOrderAmount) }}
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
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { mallAnalyticsApi } from '@/api/analytics'
import type { MallTradeAnalysis } from '@/api/analytics'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { formatMoney } from '../shared/docActions'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsTradeAnalysis' })

const quickDate = ref('month')
const dateRange = ref<[string, string] | undefined>(quickDateRange('month'))

// ═══ 粒度切换（对标表格上方三段按钮：按天 / 按周 / 按月；后端只出按日明细，周/月为真实明细的区间聚合） ═══
const GRANULARITY_OPTIONS = [
  { label: '按天', value: 'day' },
  { label: '按周', value: 'week' },
  { label: '按月', value: 'month' }
]
const granularity = ref<'day' | 'week' | 'month'>('day')

// ═══ 取数（聚合端点：summary + daily） ═══
const loading = ref(false)
const dailyRaw = ref<MallTradeAnalysis['daily']>([])
const summaryRaw = ref<MallTradeAnalysis['summary'] | null>(null)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

interface TradeRow { day: string; orderCount: number; gmv: number; avgOrderAmount: number }

/** 按粒度聚合（周=周一起算；客单价 = 区间下单金额 ÷ 区间下单笔数） */
function buildRows(daily: MallTradeAnalysis['daily'] = [], gran: 'day' | 'week' | 'month'): TradeRow[] {
  if (gran === 'day') {
    return daily.map(d => ({
      day: String(d.day || ''),
      orderCount: Number(d.orderCount) || 0,
      gmv: Number(d.gmv) || 0,
      avgOrderAmount: Number(d.avgOrderAmount) || 0
    })).sort((a, b) => (a.day < b.day ? 1 : -1))
  }
  const groups = new Map<string, { label: string; orderCount: number; gmv: number }>()
  daily.forEach(d => {
    const day = String(d.day || '')
    if (!day) return
    let label: string
    if (gran === 'month') {
      label = day.slice(0, 7)
    } else {
      // 周一为一周起点（不依赖 dayjs 语言包）
      const dt = dayjs(day)
      const offset = (dt.day() + 6) % 7
      const start = dt.subtract(offset, 'day')
      label = `${start.format('YYYY-MM-DD')} ~ ${start.add(6, 'day').format('YYYY-MM-DD')}`
    }
    const cur = groups.get(label) || { label, orderCount: 0, gmv: 0 }
    cur.orderCount += Number(d.orderCount) || 0
    cur.gmv += Number(d.gmv) || 0
    groups.set(label, cur)
  })
  return [...groups.values()]
    .map(g => ({
      day: g.label,
      orderCount: g.orderCount,
      gmv: g.gmv,
      avgOrderAmount: g.orderCount > 0 ? g.gmv / g.orderCount : 0
    }))
    .sort((a, b) => (a.day < b.day ? 1 : -1))
}

/** 客户端分页（聚合端点一次性返回全部按日明细，无分页参数） */
function applyPage() {
  const rows = buildRows(dailyRaw.value, granularity.value)
  pagination.total = rows.length
  const start = (pagination.current - 1) * pagination.pageSize
  dataSource.value = rows.slice(start, start + pagination.pageSize).map((r, i) => ({
    ...r,
    rowKey: `${granularity.value}:${r.day}:${start + i}`
  }))
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await mallAnalyticsApi.tradeAnalysis({
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    })
    dailyRaw.value = Array.isArray(res?.daily) ? res.daily : []
    summaryRaw.value = res?.summary || null
    applyPage()
  } catch (e) {
    console.warn('[交易分析] 取数失败', e)
    dailyRaw.value = []
    summaryRaw.value = null
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 列定义（对标 7 列 / 默认 7；已按后端硬缺口去掉无数据源的 登录客户数 / 下单客户数 / 下单转化率(%) → 4 列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'day', title: '日期', width: 200 },
  { key: 'orderCount', title: '下单笔数', width: 120, align: 'right' },
  { key: 'gmv', title: '下单金额', type: 'slot', slotName: 'gmvCell', width: 140, align: 'right' },
  { key: 'avgOrderAmount', title: '客单价', type: 'slot', slotName: 'avgOrderAmountCell', width: 130, align: 'right' }
]

/** 合计行：量额列取后端 summary（按当前过滤范围 SUM）；比率列（客单价）不合计（对标实测为空） */
const summaryColumns = computed(() => ([
  { key: 'orderCount', value: Number(summaryRaw.value?.totalOrderCount) || 0 },
  { key: 'gmv', value: Number(summaryRaw.value?.totalGmv) || 0 }
]))

// ═══ 交互 ═══
// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { dateRange: dateRange.value, granularity: granularity.value }
}

function applyQuerySnapshot(v: Record<string, any>) {
  if (v.dateRange) dateRange.value = v.dateRange
  if (v.granularity) granularity.value = v.granularity
  handleSearch()
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  quickDate.value = 'month'
  dateRange.value = quickDateRange('month')
  granularity.value = 'day'
  handleSearch()
}

function handleGranularityChange() {
  pagination.current = 1
  applyPage()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  applyPage()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key)
  handleSearch()
}

function handleError(err: any) {
  console.error('[交易分析] 页面异常', err)
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
</style>
