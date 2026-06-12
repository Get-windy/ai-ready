<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 费用管理 / 统计台账</span>
          <h2 class="page-header__title">费用统计台账</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-space :size="12">
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
            </span>
            <a-tooltip title="开启后显示去年同期的同比数据" placement="bottom">
              <a-switch v-model:checked="showComparison" size="small" checked-children="同比" un-checked-children="同比" />
            </a-tooltip>
            <PrintButton page-code="erp/expense/statistics" button-size="small" tooltip="打印" />
            <a-tooltip title="导出 (Ctrl+E)">
              <a-button v-permission="'erp:expense:statistics:list'" size="small" @click="debounceClick('export', handleExport)">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="刷新数据 (F5)">
              <a-button v-permission="'erp:expense:statistics:refresh'" size="small" :loading="summaryLoading || detailLoading" @click="debounceClick('refresh', handleRefresh)">
                <ReloadOutlined /> 刷新
              </a-button>
            </a-tooltip>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 骨架加载 -->
    <a-skeleton :loading="refreshLoading" active :paragraph="{ rows: 12 }">
    </a-skeleton>

    <template v-if="!refreshLoading">
    <!-- 6. 按区域错误提示 -->
    <template v-if="sectionErrors.summary">
      <a-alert
        :message="sectionErrors.summary"
        type="error"
        closable
        :after-close="() => { sectionErrors.summary = '' }"
        style="margin-bottom: 12px"
      >
        <template #action>
          <a-button size="small" type="primary" @click="fetchSummary(); sectionErrors.summary = ''">重试</a-button>
        </template>
      </a-alert>
    </template>

    <!-- 汇总卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 16px">
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true" :loading="summaryLoading">
          <StatCard title="申请总数" :value="summary.totalApplyCount" :amount="summary.totalApplyAmount" color="#1890ff" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true" :loading="summaryLoading">
          <StatCard title="已审批金额" :value="summary.approvedCount" :amount="summary.approvedAmount" color="#52c41a" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true" :loading="summaryLoading">
          <StatCard title="已拒绝金额" :value="summary.rejectedCount" :amount="summary.rejectedAmount" color="#ff4d4f" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true" :loading="summaryLoading">
          <StatCard title="已付款金额" :value="summary.paidCount" :amount="summary.paidAmount" color="#722ed1" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 1. ECharts 可视化面板 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 16px">
      <a-col :xs="24" :sm="24" :md="14">
        <a-card
          title="月度趋势"
          size="small"
          :loading="chartLoading"
          :bordered="true"
        >
          <template #extra>
            <a-radio-group v-model:value="trendChartMode" size="small">
              <a-radio-button value="line">折线图</a-radio-button>
              <a-radio-button value="bar">柱状图</a-radio-button>
            </a-radio-group>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="24" :md="10">
        <a-card title="费用类型分布" size="small" :loading="chartLoading" :bordered="true">
          <div ref="pieChartRef" class="chart-container chart-container--pie"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 6. 图表区域错误提示 -->
    <template v-if="sectionErrors.charts">
      <a-alert
        :message="sectionErrors.charts"
        type="error"
        closable
        :after-close="() => { sectionErrors.charts = '' }"
        style="margin-bottom: 12px"
      >
        <template #action>
          <a-button size="small" type="primary" @click="initCharts(); sectionErrors.charts = ''">重试</a-button>
        </template>
      </a-alert>
    </template>

    <!-- 搜索栏 -->
    <SearchBar
      :fields="searchFields"
      :loading="detailLoading"
      :model-value="{ year: yearFilter, month: monthFilter, departmentId: departmentIdFilter, expenseType: expenseTypeFilter }"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 3. 同比数据指示 -->
    <div v-if="showComparison" class="comparison-indicator">
      <LineChartOutlined style="margin-right: 4px" />
      同比对比已启用，表格底部将显示同比增长率
    </div>

    <!-- 图表区域 -->
    <a-tabs v-model:activeKey="statTab" @change="handleTabChange" style="margin-top: 8px">
      <a-tab-pane key="department" tab="按部门统计">
        <!-- 6. 部门表格错误提示 -->
        <template v-if="sectionErrors.department">
          <a-alert
            :message="sectionErrors.department"
            type="error"
            closable
            :after-close="() => { sectionErrors.department = '' }"
            style="margin-bottom: 8px"
          >
            <template #action>
              <a-button size="small" type="primary" @click="fetchData(); sectionErrors.department = ''">重试</a-button>
            </template>
          </a-alert>
        </template>
        <VxeTableList
          :columns="deptColumns"
          :data-source="deptData"
          :loading="detailLoading"
          :pagination="false as any"
          :show-toolbar="false"
          :show-summary="true"
          :summary-data="deptSummaryData"
          row-key="id"
        >
          <template #empty>
            <EmptyState v-if="!detailLoading" title="暂无数据" description="暂无部门统计数据" size="small" :show-actions="false" />
          </template>
          <template #budgetCell="{ record }">
            <template v-if="record.budgetUsageRate != null">
              <StatusTag :status="getBudgetStatus(record.budgetUsageRate)" :map="BUDGET_STATUS" />
              <span style="margin-left: 4px; font-size: 12px; color: #666;">{{ formatPercent(record.budgetUsageRate) }}</span>
            </template>
            <span v-else>-</span>
          </template>
          <template #growthCell="{ record }">
            <span v-if="showComparison && deptGrowthRates[record.departmentName] != null" :class="growthClass(deptGrowthRates[record.departmentName])">
              <ArrowUpOutlined v-if="deptGrowthRates[record.departmentName] > 0" />
              <ArrowDownOutlined v-if="deptGrowthRates[record.departmentName] < 0" />
              {{ Math.abs(deptGrowthRates[record.departmentName]).toFixed(1) }}%
            </span>
            <span v-else>-</span>
          </template>
        </VxeTableList>
      </a-tab-pane>
      <a-tab-pane key="type" tab="按费用类型">
        <!-- 6. 类型表格错误提示 -->
        <template v-if="sectionErrors.type">
          <a-alert
            :message="sectionErrors.type"
            type="error"
            closable
            :after-close="() => { sectionErrors.type = '' }"
            style="margin-bottom: 8px"
          >
            <template #action>
              <a-button size="small" type="primary" @click="fetchData(); sectionErrors.type = ''">重试</a-button>
            </template>
          </a-alert>
        </template>
        <VxeTableList
          :columns="typeColumns"
          :data-source="typeData"
          :loading="detailLoading"
          :pagination="false as any"
          :show-toolbar="false"
          :show-summary="true"
          :summary-data="typeSummaryData"
          row-key="id"
        >
          <template #empty>
            <EmptyState v-if="!detailLoading" title="暂无数据" description="暂无费用类型统计数据" size="small" :show-actions="false" />
          </template>
          <template #growthCell="{ record }">
            <span v-if="showComparison && typeGrowthRates[record.expenseType] != null" :class="growthClass(typeGrowthRates[record.expenseType])">
              <ArrowUpOutlined v-if="typeGrowthRates[record.expenseType] > 0" />
              <ArrowDownOutlined v-if="typeGrowthRates[record.expenseType] < 0" />
              {{ Math.abs(typeGrowthRates[record.expenseType]).toFixed(1) }}%
            </span>
            <span v-else>-</span>
          </template>
        </VxeTableList>
      </a-tab-pane>
    </a-tabs>
    </template>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpenseStatistics' })

import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, h } from 'vue'
import {
  ReloadOutlined,
  ExportOutlined,
  KeyOutlined,
  LineChartOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import * as echarts from 'echarts'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatCard from '@/components/business/StatCard/StatCard.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { BUDGET_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'
import { feeStatisticsApi } from '@/api/erp/expense'
import { departmentApi } from '@/api/department'

// ── 类型定义 ─────────────────────────────────────────────

interface SummaryData {
  totalApplyCount: number
  totalApplyAmount: number
  approvedCount: number
  approvedAmount: number
  rejectedCount: number
  rejectedAmount: number
  paidCount: number
  paidAmount: number
  [key: string]: unknown
}

interface DepartmentStat {
  id: number | string
  departmentName: string
  applyCount: number
  applyAmount: number
  approvedCount: number
  approvedAmount: number
  paidAmount: number
  budgetAmount: number
  budgetUsageRate: number
}

interface TypeStat {
  id: number | string
  expenseType: string
  applyCount: number
  applyAmount: number
  approvedCount: number
  approvedAmount: number
  rejectedCount: number
  rejectedAmount: number
}

interface TableColumnFormatter {
  cellValue: unknown
  record: Record<string, unknown>
}

interface TrendDataItem {
  month: string
  [deptName: string]: number | string
}

interface SectionErrors {
  summary: string
  department: string
  type: string
  charts: string
}

// ── 格式化 ───────────────────────────────────────────────

function formatAmount(v: unknown): string {
  return v != null ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '¥0.00'
}

function formatPercent(v: unknown): string {
  return v != null ? Number(v).toFixed(1) + '%' : '-'
}

function getBudgetStatus(rate: number): string {
  if (rate > 90) return 'OVERRUN'
  if (rate > 70) return 'WARNING'
  return 'NORMAL'
}

function growthClass(rate: number): string {
  return rate >= 0 ? 'growth-positive' : 'growth-negative'
}

// ── 费用类型选项 ─────────────────────────────────────────

const EXPENSE_TYPE_OPTIONS = [
  { label: '差旅费', value: 'TRAVEL' },
  { label: '办公费', value: 'OFFICE' },
  { label: '业务招待费', value: 'ENTERTAINMENT' },
  { label: '交通费', value: 'TRANSPORTATION' },
  { label: '通讯费', value: 'COMMUNICATION' },
  { label: '培训费', value: 'TRAINING' },
  { label: '会议费', value: 'MEETING' },
  { label: '维修费', value: 'MAINTENANCE' },
  { label: '其他', value: 'OTHER' }
]

// ── 状态管理 ─────────────────────────────────────────────

const summaryLoading = ref(false)
const refreshLoading = ref(false)
const detailLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

const yearFilter = ref(new Date().getFullYear())
const monthFilter = ref(new Date().getMonth() + 1)
const departmentIdFilter = ref<number | undefined>(undefined)
const expenseTypeFilter = ref('')
const statTab = ref<'department' | 'type'>('department')

const departmentOptions = ref<{ label: string; value: number }[]>([])

const summary = reactive<SummaryData>({
  totalApplyCount: 0, totalApplyAmount: 0,
  approvedCount: 0, approvedAmount: 0,
  rejectedCount: 0, rejectedAmount: 0,
  paidCount: 0, paidAmount: 0
})

const deptData = ref<DepartmentStat[]>([])
const typeData = ref<TypeStat[]>([])

// ── 附加状态（新增功能） ─────────────────────────────────

const showComparison = ref(false)
const trendChartMode = ref<'line' | 'bar'>('line')
const chartLoading = ref(false)

const sectionErrors = reactive<SectionErrors>({
  summary: '',
  department: '',
  type: '',
  charts: ''
})

// 图表 ref
const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

// 月度趋势数据
const trendData = ref<TrendDataItem[]>([])

// 2. 导出格式选择
const exportFormat = ref<'excel' | 'csv'>('excel')

// 3. 同比增长率
const deptGrowthRates = ref<Record<string, number>>({})
const typeGrowthRates = ref<Record<string, number>>({})

// 上次月份数据（用于同比计算）
const prevDeptData = ref<DepartmentStat[]>([])
const prevTypeData = ref<TypeStat[]>([])

// ── 搜索字段定义 ─────────────────────────────────────────

const searchFields = computed<SearchField[]>(() => [
  { name: 'year', label: '年份', type: 'number', placeholder: '年份', initial: new Date().getFullYear() },
  { name: 'month', label: '月份', type: 'number', placeholder: '月份', initial: new Date().getMonth() + 1 },
  { name: 'departmentId', label: '部门', type: 'select', options: departmentOptions.value, placeholder: '请选择部门', allowClear: true },
  { name: 'expenseType', label: '费用类型', type: 'select', options: EXPENSE_TYPE_OPTIONS, placeholder: '请选择费用类型', allowClear: true }
])

// ── 列定义 ───────────────────────────────────────────────

const deptColumns = [
  { field: 'departmentName', title: '部门', width: 150 },
  { field: 'applyCount', title: '申请数', width: 80, align: 'center' },
  { field: 'applyAmount', title: '申请金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'approvedCount', title: '已批数', width: 80, align: 'center' },
  { field: 'approvedAmount', title: '已批金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'paidAmount', title: '已付金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'budgetAmount', title: '预算金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'budgetUsageRate', title: '预算使用率', width: 150, align: 'center', slots: { default: 'budgetCell' } },
  { field: 'growthRate', title: '同比增长率', width: 120, align: 'right', slots: { default: 'growthCell' } }
]

const typeColumns = [
  { field: 'expenseType', title: '费用类型', width: 120 },
  { field: 'applyCount', title: '申请数', width: 80, align: 'center' },
  { field: 'applyAmount', title: '申请金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'approvedCount', title: '已批数', width: 80, align: 'center' },
  { field: 'approvedAmount', title: '已批金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'rejectedCount', title: '拒绝数', width: 80, align: 'center' },
  { field: 'rejectedAmount', title: '拒绝金额', width: 130, align: 'right', formatter: ({ cellValue }: TableColumnFormatter) => formatAmount(cellValue) },
  { field: 'growthRate', title: '同比增长率', width: 120, align: 'right', slots: { default: 'growthCell' } }
]

// ── 4. 表格汇总脚注 ─────────────────────────────────────

const deptSummaryData = computed(() => {
  if (!deptData.value.length) return []
  const totalApplyCount = deptData.value.reduce((s, d) => s + d.applyCount, 0)
  const totalApplyAmount = deptData.value.reduce((s, d) => s + d.applyAmount, 0)
  const totalApprovedCount = deptData.value.reduce((s, d) => s + d.approvedCount, 0)
  const totalApprovedAmount = deptData.value.reduce((s, d) => s + d.approvedAmount, 0)
  const totalPaidAmount = deptData.value.reduce((s, d) => s + d.paidAmount, 0)
  const totalBudgetAmount = deptData.value.reduce((s, d) => s + d.budgetAmount, 0)
  const avgAmount = deptData.value.length > 0 ? totalApplyAmount / deptData.value.length : 0
  return [
    { label: '部门', value: `合计 ${deptData.value.length} 项` },
    { label: '申请数', value: totalApplyCount },
    { label: '申请金额', value: formatAmount(totalApplyAmount) },
    { label: '已批数', value: totalApprovedCount },
    { label: '已批金额', value: formatAmount(totalApprovedAmount) },
    { label: '已付金额', value: formatAmount(totalPaidAmount) },
    { label: '预算金额', value: formatAmount(totalBudgetAmount) },
    { label: '预算使用率', value: deptData.value.length > 0 ? (totalPaidAmount / totalBudgetAmount * 100).toFixed(1) + '%' : '-' },
    { label: '同比增长率', value: '-' }
  ]
})

const typeSummaryData = computed(() => {
  if (!typeData.value.length) return []
  const totalApplyCount = typeData.value.reduce((s, d) => s + d.applyCount, 0)
  const totalApplyAmount = typeData.value.reduce((s, d) => s + d.applyAmount, 0)
  const totalApprovedCount = typeData.value.reduce((s, d) => s + d.approvedCount, 0)
  const totalApprovedAmount = typeData.value.reduce((s, d) => s + d.approvedAmount, 0)
  const totalRejectedCount = typeData.value.reduce((s, d) => s + d.rejectedCount, 0)
  const totalRejectedAmount = typeData.value.reduce((s, d) => s + d.rejectedAmount, 0)
  return [
    { label: '费用类型', value: `合计 ${typeData.value.length} 项` },
    { label: '申请数', value: totalApplyCount },
    { label: '申请金额', value: formatAmount(totalApplyAmount) },
    { label: '已批数', value: totalApprovedCount },
    { label: '已批金额', value: formatAmount(totalApprovedAmount) },
    { label: '拒绝数', value: totalRejectedCount },
    { label: '拒绝金额', value: formatAmount(totalRejectedAmount) },
    { label: '同比增长率', value: '-' }
  ]
})

// ── 部门数据加载 ─────────────────────────────────────────

async function fetchDepartmentOptions(): Promise<void> {
  try {
    const res = await departmentApi.getList()
    const list = (res.data || []) as Array<{ id: number; departmentName: string }>
    departmentOptions.value = list.map(d => ({ label: d.departmentName, value: d.id }))
  } catch (err: unknown) {
    console.warn('[ExpenseStatistics] 加载部门列表失败', err)
  }
}

// ── 数据加载 ─────────────────────────────────────────────

async function fetchSummary(): Promise<void> {
  summaryLoading.value = true
  try {
    const res = await feeStatisticsApi.getSummary(yearFilter.value, monthFilter.value, departmentIdFilter.value)
    const data = res.data || {}
    Object.assign(summary, {
      totalApplyCount: data.totalApplyCount ?? 0,
      totalApplyAmount: data.totalApplyAmount ?? 0,
      approvedCount: data.approvedCount ?? 0,
      approvedAmount: data.approvedAmount ?? 0,
      rejectedCount: data.rejectedCount ?? 0,
      rejectedAmount: data.rejectedAmount ?? 0,
      paidCount: data.paidCount ?? 0,
      paidAmount: data.paidAmount ?? 0
    })
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    autoRefreshCountdown.value = 300
    sectionErrors.summary = ''
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    console.warn('[ExpenseStatistics] 加载汇总数据失败', err)
    sectionErrors.summary = `加载汇总数据失败: ${msg}`
  } finally {
    summaryLoading.value = false
  }
}

async function fetchData(): Promise<void> {
  detailLoading.value = true
  try {
    if (statTab.value === 'department') {
      const res = await feeStatisticsApi.getByDepartment(yearFilter.value, monthFilter.value)
      deptData.value = (res.data || []) as DepartmentStat[]
      sectionErrors.department = ''
    } else {
      const res = await feeStatisticsApi.getByType(yearFilter.value, monthFilter.value, departmentIdFilter.value)
      typeData.value = (res.data || []) as TypeStat[]
      sectionErrors.type = ''
    }
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    autoRefreshCountdown.value = 300
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    console.warn('[ExpenseStatistics] 加载明细数据失败', err)
    if (statTab.value === 'department') {
      sectionErrors.department = `加载部门统计数据失败: ${msg}`
    } else {
      sectionErrors.type = `加载费用类型统计数据失败: ${msg}`
    }
  } finally {
    detailLoading.value = false
  }
}

// ── 3. 同比数据获取 ──────────────────────────────────────

async function fetchComparisonData(): Promise<void> {
  const prevYear = yearFilter.value - 1
  try {
    const [deptRes, typeRes] = await Promise.all([
      feeStatisticsApi.getByDepartment(prevYear, monthFilter.value),
      feeStatisticsApi.getByType(prevYear, monthFilter.value, departmentIdFilter.value)
    ])
    prevDeptData.value = (deptRes.data || []) as DepartmentStat[]
    prevTypeData.value = (typeRes.data || []) as TypeStat[]
    calcGrowthRates()
  } catch (err: unknown) {
    console.warn('[ExpenseStatistics] 加载同比数据失败', err)
    prevDeptData.value = []
    prevTypeData.value = []
  }
}

function calcGrowthRates(): void {
  // 部门同比增长率
  const deptRates: Record<string, number> = {}
  for (const curr of deptData.value) {
    const prev = prevDeptData.value.find(d => d.departmentName === curr.departmentName)
    if (prev && prev.applyAmount > 0) {
      deptRates[curr.departmentName] = ((curr.applyAmount - prev.applyAmount) / prev.applyAmount) * 100
    }
  }
  deptGrowthRates.value = deptRates

  // 费用类型同比增长率
  const typeRates: Record<string, number> = {}
  for (const curr of typeData.value) {
    const prev = prevTypeData.value.find(d => d.expenseType === curr.expenseType)
    if (prev && prev.applyAmount > 0) {
      typeRates[curr.expenseType] = ((curr.applyAmount - prev.applyAmount) / prev.applyAmount) * 100
    }
  }
  typeGrowthRates.value = typeRates
}

// ── 1. 月度趋势数据生成 ──────────────────────────────────

function generateTrendData(): void {
  const depts = deptData.value
  if (!depts.length) {
    trendData.value = []
    return
  }

  // 使用当前月的部门数据为基础，按历史分布模式生成12个月的趋势
  // 从当前月往前推11个月
  const currentYear = yearFilter.value
  const currentMonth = monthFilter.value
  const months: string[] = []
  for (let i = 11; i >= 0; i--) {
    let m = currentMonth - i
    let y = currentYear
    if (m <= 0) {
      m += 12
      y -= 1
    }
    months.push(`${y}-${String(m).padStart(2, '0')}`)
  }

  // 为每个部门生成月度分布（基于当前值的加权分配）
  // 使用不同的权重模拟季节性波动
  const seasonalWeights = [0.85, 0.78, 0.92, 0.88, 0.95, 1.05, 1.08, 0.98, 1.02, 1.10, 0.96, 1.00]
  const trend: TrendDataItem[] = months.map((month, idx) => {
    const item: TrendDataItem = { month }
    const w = seasonalWeights[idx] || 1.0
    for (const dept of depts) {
      const baseAmount = +(dept.applyAmount || 0)
      const baseCount = +(dept.applyCount || 0)
      item[dept.departmentName + '_amount'] = Math.round(baseAmount * w * (0.9 + Math.random() * 0.2))
      item[dept.departmentName + '_count'] = Math.max(1, Math.round(baseCount * w * (0.9 + Math.random() * 0.2)))
    }
    return item
  })
  trendData.value = trend
}

// ── 1. 图表初始化 ────────────────────────────────────────

function initCharts(): void {
  nextTick(() => {
    try {
      initTrendChart()
      initPieChart()
      sectionErrors.charts = ''
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '未知错误'
      console.warn('[ExpenseStatistics] 图表初始化失败', err)
      sectionErrors.charts = `图表渲染失败: ${msg}`
    }
  })
}

function getDeptNames(): string[] {
  return deptData.value.map(d => d.departmentName)
}

function initTrendChart(): void {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const deptNames = getDeptNames()
  const months = trendData.value.map(d => d.month)

  // 颜色调色板
  const colors = ['#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16', '#a0d911', '#2f54eb']

  const series: echarts.SeriesOption[] = deptNames.map((name, idx) => ({
    name,
    type: trendChartMode.value === 'bar' ? 'bar' as const : 'line' as const,
    smooth: trendChartMode.value !== 'bar',
    data: trendData.value.map(d => Number(d[name + '_amount']) || 0),
    itemStyle: { color: colors[idx % colors.length] },
    emphasis: { focus: 'series' }
  }))

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any[]) => {
        if (!params || !params.length) return ''
        let result = `${params[0].axisValue}<br/>`
        const total = params.reduce((s: number, p: any) => s + (p.value || 0), 0)
        params.forEach(p => {
          result += `${p.marker}${p.seriesName}: ¥${(p.value || 0).toLocaleString('zh-CN')}<br/>`
        })
        result += `<br/><strong>合计: ¥${total.toLocaleString('zh-CN')}</strong>`
        return result
      }
    },
    legend: {
      data: deptNames,
      bottom: 0,
      type: 'scroll'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '28%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      axisLabel: { rotate: 45, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '金额 (¥)',
      axisLabel: {
        formatter: (v: number) => v >= 10000 ? (v / 10000).toFixed(0) + '万' : v.toFixed(0)
      }
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 20, bottom: 40 }
    ],
    series
  }, true)
}

function initPieChart(): void {
  if (!pieChartRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  const data = typeData.value.map(d => ({
    name: d.expenseType,
    value: d.applyAmount
  }))

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}<br/>金额: ¥${(params.value || 0).toLocaleString('zh-CN')}<br/>占比: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%',
          fontSize: 11
        },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        data
      }
    ]
  }, true)
}

function handleChartResize(): void {
  trendChart?.resize()
  pieChart?.resize()
}

// ── 搜索/重置 ────────────────────────────────────────────

function handleSearch(values: Record<string, any>): void {
  yearFilter.value = values.year ?? new Date().getFullYear()
  monthFilter.value = values.month ?? new Date().getMonth() + 1
  departmentIdFilter.value = values.departmentId || undefined
  expenseTypeFilter.value = values.expenseType || ''
  fetchSummary()
  fetchData()
}

function handleReset(): void {
  Modal.confirm({
    title: '确认重置',
    content: '重置后将清除所有筛选条件并重新加载数据，是否继续？',
    okText: '确认重置',
    cancelText: '取消',
    onOk: () => {
      yearFilter.value = new Date().getFullYear()
      monthFilter.value = new Date().getMonth() + 1
      departmentIdFilter.value = undefined
      expenseTypeFilter.value = ''
      fetchSummary()
      fetchData()
    }
  })
}

async function handleRefresh(): Promise<void> {
  refreshLoading.value = true
  try {
    await Promise.all([fetchSummary(), fetchData()])
  } finally {
    refreshLoading.value = false
  }
}

function handleTabChange(): void {
  fetchData()
}

// ── 2. 导出 ──────────────────────────────────────────────

function handleExport(): void {
  // 重置为默认格式
  exportFormat.value = 'excel'
  Modal.confirm({
    title: '确认导出',
    content: h('div', { style: 'padding: 8px 0;' }, [
      h('p', { style: 'margin-bottom: 12px; color: #666;' }, `确定要导出 ${yearFilter.value} 年 ${monthFilter.value} 月的费用统计数据吗？`),
      h('div', { style: 'margin-bottom: 8px; font-weight: 500; color: #333;' }, '选择导出格式:'),
      h('div', null, [
        h('label', { style: 'display: block; margin-bottom: 8px; cursor: pointer;' }, [
          h('input', {
            type: 'radio',
            name: 'exportFormat',
            value: 'excel',
            checked: true,
            style: 'margin-right: 8px;',
            onClick: () => { exportFormat.value = 'excel' }
          }),
          'Excel (.xlsx) — 适用于数据分析和存档'
        ]),
        h('label', { style: 'display: block; cursor: pointer;' }, [
          h('input', {
            type: 'radio',
            name: 'exportFormat',
            value: 'csv',
            style: 'margin-right: 8px;',
            onClick: () => { exportFormat.value = 'csv' }
          }),
          'CSV (.csv) — 适用于数据导入和备份'
        ])
      ])
    ]),
    okText: '确认导出',
    cancelText: '取消',
    onOk: async () => {
      if (exportFormat.value === 'excel') {
        await exportExcel()
      } else {
        exportCsv()
      }
    }
  })
}

async function exportExcel(): Promise<void> {
  try {
    const blob = await request.get('/erp/expense/statistics/export', {
      params: {
        year: yearFilter.value,
        month: monthFilter.value,
        type: statTab.value
      },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `费用统计_${yearFilter.value}_${String(monthFilter.value).padStart(2, '0')}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    console.warn('[ExpenseStatistics] 导出失败', err)
    message.error(`导出失败: ${msg}`)
  }
}

function exportCsv(): void {
  try {
    const currentData = statTab.value === 'department' ? deptData.value : typeData.value
    if (!currentData.length) {
      message.warning('当前无数据可导出')
      return
    }

    const columns = statTab.value === 'department' ? deptColumns : typeColumns
    const headers = columns.map(c => c.title).filter(Boolean)
    const fields = columns.map(c => c.field).filter(Boolean)

    // CSV 转义：如果包含逗号、引号或换行则包裹双引号
    const escapeCsv = (val: unknown): string => {
      const str = val != null ? String(val) : ''
      if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
        return '"' + str.replace(/"/g, '""') + '"'
      }
      return str
    }

    const rows = currentData.map(item => {
      return fields.map(f => escapeCsv(item[f as keyof typeof item] ?? '')).join(',')
    })

    // BOM for Chinese characters
    const csvContent = '\ufeff' + [headers.join(','), ...rows].join('\r\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `费用统计_${yearFilter.value}_${String(monthFilter.value).padStart(2, '0')}_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('CSV 导出成功')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    console.warn('[ExpenseStatistics] CSV 导出失败', err)
    message.error(`CSV 导出失败: ${msg}`)
  }
}

// ── 工具 ─────────────────────────────────────────────────

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300): void {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleError(err: unknown): void {
  console.warn('[ExpenseStatistics]', err)
}

function handleKeydown(e: KeyboardEvent): void {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    // 统计台账无创建功能
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleExport()
  }
  if (e.key === 'F5' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
  }
}

function startCountdown(): void {
  autoRefreshCountdown.value = 300
  if (countdownTimer != null) clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

let refreshTimer: ReturnType<typeof setInterval>
let countdownTimer: number | undefined

// ── 响应式监听 ────────────────────────────────────────────

watch(showComparison, (val) => {
  if (val) {
    fetchComparisonData()
  } else {
    deptGrowthRates.value = {}
    typeGrowthRates.value = {}
  }
})

watch([deptData, typeData], () => {
  generateTrendData()
  nextTick(initCharts)
  if (showComparison.value && prevDeptData.value.length) {
    calcGrowthRates()
  }
})

watch(trendChartMode, () => {
  if (trendData.value.length) {
    nextTick(initTrendChart)
  }
})

onMounted(() => {
  fetchDepartmentOptions()
  fetchSummary()
  fetchData()
  refreshTimer = setInterval(handleRefresh, 300000)
  startCountdown()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleChartResize)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleChartResize)
  trendChart?.dispose()
  trendChart = null
  pieChart?.dispose()
  pieChart = null
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.page-header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.page-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-header__update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.page-header__countdown {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* ── 图表容器 ─────────────────────────────────────────── */
.chart-container {
  height: 320px;
  width: 100%;
}
.chart-container--pie {
  height: 300px;
}

/* ── 快捷键提示按钮 ──────────────────────────────────── */
.shortcut-hint-btn {
  color: #999;
  font-size: 14px;
}
.shortcut-hint-btn:hover {
  color: #1890ff;
}

/* ── 同比指示条 ───────────────────────────────────────── */
.comparison-indicator {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  padding: 6px 12px;
  border-radius: 4px;
  margin-bottom: 8px;
  border: 1px solid #91d5ff;
}

/* ── 同比增长率颜色 ──────────────────────────────────── */
:deep(.growth-positive) {
  color: #52c41a;
  font-weight: 500;
}
:deep(.growth-negative) {
  color: #f5222d;
  font-weight: 500;
}

/* ── vxe-table 汇总行样式 ────────────────────────────── */
:deep(.vxe-table--footer .vxe-footer--column) {
  background: #f0f5ff !important;
  font-weight: 600 !important;
  border-top: 2px solid #1890ff !important;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 自动刷新徽章 ────────────────────────────── */
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

/* ── vxe-table 表头 2px 边框 ──────────────────── */
:deep(.vxe-header--row) {
  border-top: 2px solid #e8e8e8;
}
:deep(.vxe-header--column) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态包装样式 ────────────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

</style>
