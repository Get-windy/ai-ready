<template>
  <div class="dashboard-page">
    <div class="dashboard-main">
      <!-- 左侧主内容区 -->
      <div class="left-col">
        <!-- 实时数据 -->
        <div class="dash-section realtime-section">
          <div class="section-header">
            <h2 class="section-title"><span class="title-bar" /> 实时数据</h2>
            <span class="section-hint">统计截止当前时间</span>
          </div>
          <a-row :gutter="24" class="realtime-cards">
            <a-col :span="8">
              <div class="realtime-card">
                <div class="rt-label">本月收入</div>
                <div class="rt-value rt-income">{{ formatMoney(realtime.monthlyIncome) }}</div>
                <div class="rt-today">今日{{ formatMoney(realtime.todayIncome) }}</div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="realtime-card">
                <div class="rt-label">本月支出</div>
                <div class="rt-value rt-expense">{{ formatMoney(realtime.monthlyExpense) }}</div>
                <div class="rt-today">今日{{ formatMoney(realtime.todayExpense) }}</div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="realtime-card">
                <div class="rt-label">本月营业利润</div>
                <div class="rt-value" :class="realtime.monthlyProfit >= 0 ? 'rt-income' : 'rt-expense'">{{ formatMoney(realtime.monthlyProfit) }}</div>
                <div class="rt-today">今日{{ formatMoney(realtime.todayProfit) }}</div>
              </div>
            </a-col>
          </a-row>
        </div>

        <!-- 业绩概览 -->
        <div class="dash-section overview-section">
          <div class="section-header">
            <div class="section-title-wrap">
              <h2 class="section-title"><span class="title-bar" /> 业绩概览</h2>
              <span class="section-hint">本月默认统计1号到当前日期的数据</span>
            </div>
            <div class="section-actions">
              <a-select v-model:value="periodType" size="small" style="width: 100px" @change="handlePeriodChange">
                <a-select-option value="month">本月</a-select-option>
                <a-select-option value="week">本周</a-select-option>
                <a-select-option value="quarter">本季度</a-select-option>
              </a-select>
            </div>
          </div>

          <!-- KPI 卡片横向滚动 -->
          <div class="kpi-scroll-wrapper">
            <button v-if="kpiScrollable" class="kpi-arrow kpi-arrow-left" @click="scrollKpi(-1)">
              <LeftOutlined />
            </button>
            <div ref="kpiScrollRef" class="kpi-scroll" @scroll="onKpiScroll">
              <div
                v-for="ind in currentIndicators"
                :key="ind.id"
                class="kpi-card"
              >
                <div class="kpi-header">
                  <span class="kpi-label">{{ ind.label }}</span>
                  <span v-if="ind.id === 'visit_count'" class="kpi-rate">0%</span>
                </div>
                <div class="kpi-value" :style="{ color: ind.color }">
                  {{ ind.prefix || '' }}{{ formatKpiValue(ind) }}
                </div>
                <div class="kpi-unit">{{ ind.unit || '' }}</div>
              </div>
            </div>
            <button v-if="kpiScrollable" class="kpi-arrow kpi-arrow-right" @click="scrollKpi(1)">
              <RightOutlined />
            </button>
          </div>

          <!-- 趋势图 -->
          <div ref="trendChartRef" class="trend-chart" />
        </div>

        <!-- 排行榜 -->
        <a-row :gutter="4" class="rank-row">
          <a-col :span="12">
            <div class="dash-section rank-section">
              <div class="section-header">
                <h2 class="section-title"><span class="title-bar" /> 本月客户销售排行</h2>
                <a class="detail-link">详情 &gt;</a>
              </div>
              <table class="rank-table">
                <thead>
                  <tr><th style="width:50px">排名</th><th>客户名称</th><th style="width:100px">销售金额</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(r, i) in customerRanking" :key="i">
                    <td>{{ i + 1 }}</td>
                    <td>{{ r.name }}</td>
                    <td class="rank-amount">{{ formatMoney(r.amount) }}</td>
                  </tr>
                  <tr v-if="customerRanking.length === 0">
                    <td colspan="3" class="empty-text">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="dash-section rank-section">
              <div class="section-header">
                <h2 class="section-title"><span class="title-bar" /> 本月商品销售排行</h2>
                <a class="detail-link">详情 &gt;</a>
              </div>
              <table class="rank-table">
                <thead>
                  <tr><th style="width:50px">排名</th><th>商品名称</th><th style="width:100px">销售金额</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(r, i) in productRanking" :key="i">
                    <td>{{ i + 1 }}</td>
                    <td>{{ r.name }}</td>
                    <td class="rank-amount">{{ formatMoney(r.amount) }}</td>
                  </tr>
                  <tr v-if="productRanking.length === 0">
                    <td colspan="3" class="empty-text">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 右侧栏：公告 + 广告 -->
      <div class="right-col">
        <div class="dash-section notice-section">
          <div class="section-header">
            <h2 class="section-title"><span class="title-bar" /> 公告</h2>
          </div>
          <div class="notice-list">
            <div v-for="n in notices" :key="n.id" class="notice-item">
              <span class="notice-title">{{ n.title }}</span>
              <span class="notice-date">{{ n.date }}</span>
            </div>
            <div v-if="notices.length === 0" class="empty-text">暂无公告</div>
          </div>
        </div>
        <div class="ad-banner">
          <div class="ad-placeholder">聚合在线支付<br/>抄底费率</div>
        </div>
      </div>
    </div>

    <!-- 指标自定义弹窗 -->
    <a-modal
      v-model:open="customModalVisible"
      title="自定义显示指标"
      :footer="null"
      width="500px"
    >
      <p class="custom-tip">勾选需要在工作台显示的指标（至少保留1个）</p>
      <div class="custom-indicator-list">
        <a-checkbox
          v-for="ind in ALL_INDICATORS"
          :key="ind.id"
          :checked="customSelectedIds.includes(ind.id)"
          @change="onCustomToggle(ind.id)"
          class="custom-item"
        >
          <component :is="ind.icon" :style="{ color: ind.color, marginRight: 6 }" />
          {{ ind.label }}
        </a-checkbox>
      </div>
      <div class="custom-actions">
        <a-button @click="handleResetIndicators">恢复默认</a-button>
        <a-button type="primary" @click="handleSaveCustom">确定</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  ALL_INDICATORS,
  getCurrentIndicators,
  getCustomIndicators,
  saveCustomIndicators,
  clearCustomIndicators,
  hasCustomIndicators,
  mapUserTypeToRole,
  type KpiIndicator,
  type UserRole
} from '@/utils/dashboardIndicators'
import { dashboardApi, type TrendChartData } from '@/api/dashboard'

const userStore = useUserStore()

// ── 角色 ──────────────────────────────────────
const currentRole = computed<UserRole>(() => mapUserTypeToRole(userStore.userType))

// ── 指标 ──────────────────────────────────────
const currentIndicators = ref<KpiIndicator[]>([])
const customModalVisible = ref(false)
const customSelectedIds = ref<string[]>([])

function refreshIndicators() {
  currentIndicators.value = getCurrentIndicators(currentRole.value)
  customSelectedIds.value = getCustomIndicators() || currentIndicators.value.map(i => i.id)
}

function openCustomModal() {
  customSelectedIds.value = getCustomIndicators() || currentIndicators.value.map(i => i.id)
  customModalVisible.value = true
}

function onCustomToggle(id: string) {
  const idx = customSelectedIds.value.indexOf(id)
  if (idx >= 0) {
    if (customSelectedIds.value.length > 1) {
      customSelectedIds.value.splice(idx, 1)
    }
  } else {
    customSelectedIds.value.push(id)
  }
}

function handleSaveCustom() {
  saveCustomIndicators(customSelectedIds.value, currentRole.value)
  refreshIndicators()
  customModalVisible.value = false
}

function handleResetIndicators() {
  clearCustomIndicators()
  refreshIndicators()
  customModalVisible.value = false
}

// ── 实时数据 ─────────────────────────────────
const realtime = ref({
  monthlyIncome: 0,
  todayIncome: 0,
  monthlyExpense: 0,
  todayExpense: 0,
  monthlyProfit: 0,
  todayProfit: 0
})

// ── 排行 ─────────────────────────────────────
const customerRanking = ref<{ name: string; amount: number }[]>([])
const productRanking = ref<{ name: string; amount: number }[]>([])

// ── 公告 ──────────────────────────────────────
const notices = ref<{ id: number; title: string; date: string }[]>([
  { id: 1, title: '系统维护通知', date: '2026-02-09' },
  { id: 2, title: '关于短信服务调整的重要通知', date: '2025-05-29' },
  { id: 3, title: '售后服务升级公告', date: '2025-02-21' },
  { id: 4, title: '来肯企汇春节放假通知', date: '2025-01-22' },
  { id: 5, title: '发版公告', date: '2024-11-29' },
  { id: 6, title: '紧急通知：关于短信发送签名的通知', date: '2024-11-15' },
  { id: 7, title: '系统维护通知', date: '2024-03-25' },
  { id: 8, title: '系统维护通知', date: '2024-02-04' },
  { id: 9, title: '短信签名备案通知', date: '2024-01-04' }
])

// ── KPI 滚动 ──────────────────────────────────
const kpiScrollRef = ref<HTMLElement>()
const kpiScrollable = ref(false)

function scrollKpi(dir: number) {
  const el = kpiScrollRef.value
  if (!el) return
  el.scrollBy({ left: dir * 280, behavior: 'smooth' })
}

function onKpiScroll() {
  const el = kpiScrollRef.value
  if (!el) return
  kpiScrollable.value = el.scrollWidth > el.clientWidth
}

// ── 趋势图 ────────────────────────────────────
const trendChartRef = ref<HTMLElement>()
let chartInstance: any = null
const periodType = ref('month')
let trendData: TrendChartData | null = null

// ── 格式化 ────────────────────────────────────
function formatMoney(n: number): string {
  if (n === 0) return '0'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatKpiValue(ind: KpiIndicator): string {
  const val = (realtime.value as Record<string, any>)[ind.apiField]
  if (val == null) return '0'
  if (typeof val === 'number') {
    if (ind.prefix === '¥') {
      return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
    return val.toLocaleString('zh-CN')
  }
  return String(val)
}

// ── 数据加载 ──────────────────────────────────
async function loadDashboardData() {
  // 实时数据（使用已有 API 模拟）
  try {
    const statsRes = await dashboardApi.getStats()
    if (statsRes.data) {
      realtime.value = {
        monthlyIncome: 570185.94,
        todayIncome: 33869.51,
        monthlyExpense: 656103.03,
        todayExpense: 27589.94,
        monthlyProfit: -85917.09,
        todayProfit: 6279.57
      }
    }
  } catch {
    // API 未就绪时使用默认值
  }

  // 趋势图
  await loadTrend()
}

async function loadTrend() {
  try {
    const res = await dashboardApi.getTrend()
    trendData = res.data || null
  } catch {
    trendData = null
  }
  await nextTick()
  initChart()
}

async function initChart() {
  await nextTick()
  if (!trendChartRef.value) return

  try {
    const echartsModule: any = await import('echarts')
    const echartsInst = echartsModule.default || echartsModule
    if (!chartInstance) {
      chartInstance = echartsInst.init(trendChartRef.value)
    }

    // 生成当月日期序列
    const now = new Date()
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
    const categories: string[] = []
    const data: number[] = []
    for (let d = 1; d <= daysInMonth; d++) {
      categories.push(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`)
      data.push(0)
    }

    chartInstance.setOption({
      grid: { left: 40, right: 20, top: 10, bottom: 30 },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: categories,
        axisLine: { lineStyle: { color: '#e8e8e8' } },
        axisLabel: { color: '#999', fontSize: 10, interval: Math.floor(daysInMonth / 8) }
      },
      yAxis: {
        type: 'value',
        max: 1,
        axisLine: { show: false },
        splitLine: { lineStyle: { type: 'dashed', color: '#e8e8e8' } },
        axisLabel: { color: '#999', fontSize: 10 }
      },
      series: [{
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        data,
        itemStyle: { color: '#ff4d4f' },
        lineStyle: { color: '#ff4d4f', width: 1.5 }
      }]
    })
  } catch {
    // ECharts 加载失败
  }
}

function handlePeriodChange() {
  loadTrend()
}

// ── 生命周期 ──────────────────────────────────
const handleResize = () => {
  chartInstance?.resize()
  if (kpiScrollRef.value) {
    kpiScrollable.value = kpiScrollRef.value.scrollWidth > kpiScrollRef.value.clientWidth
  }
}

onMounted(() => {
  refreshIndicators()
  loadDashboardData()
  window.addEventListener('resize', handleResize)
  nextTick(() => {
    if (kpiScrollRef.value) {
      kpiScrollable.value = kpiScrollRef.value.scrollWidth > kpiScrollRef.value.clientWidth
    }
  })
})

onBeforeUnmount(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-page {
  padding: 0 4px 4px 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #d5d8dc;
}

.dashboard-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  gap: 4px;
}

.left-col {
  flex: 3;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.right-col {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

/* ── 通用区块 ── */
.dash-section {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  flex: none;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-bar {
  display: inline-block;
  width: 3px;
  height: 16px;
  background: #ff4d4f;
  border-radius: 2px;
}

.section-hint {
  font-size: 12px;
  color: #999;
}

.detail-link {
  font-size: 12px;
  color: #999;
}

/* ── 实时数据 ── */
.realtime-cards {
  text-align: center;
}

.realtime-card {
  padding: 8px 0;
}

.rt-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.rt-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.rt-income { color: #52c41a; }
.rt-expense { color: #ff4d4f; }

.rt-today {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* ── KPI 滚动 ── */
.kpi-scroll-wrapper {
  display: flex;
  align-items: center;
  position: relative;
  margin-bottom: 2px;
}

.kpi-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 4px 0;
  flex: 1;
}

.kpi-scroll::-webkit-scrollbar { display: none; }

.kpi-arrow {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
  transition: all 0.2s;
  z-index: 2;
}

.kpi-arrow:hover {
  border-color: #1890ff;
  color: #1890ff;
  background: #e6f7ff;
}

.kpi-card {
  min-width: 160px;
  padding: 12px 16px;
  background: #f0f2f5;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.kpi-label {
  font-size: 13px;
  color: #666;
}

.kpi-rate {
  font-size: 12px;
  color: #ff4d4f;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.kpi-unit {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

/* ── 趋势图 ── */
.trend-chart {
  height: 200px;
  width: 100%;
}

/* ── 排行 ─ */
.rank-row {
  margin-bottom: 0;
}

.rank-section {
  margin-bottom: 0;
}

.rank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.rank-table th {
  background: #f0f2f5;
  padding: 8px 12px;
  text-align: center;
  font-weight: 500;
  color: #666;
  border-bottom: 1px solid #f0f0f0;
}

.rank-table td {
  padding: 8px 12px;
  text-align: center;
  border-bottom: 1px solid #f5f5f5;
  color: #333;
}

.rank-amount {
  font-weight: 600;
}

.empty-text {
  text-align: center;
  color: #ccc;
  padding: 20px 0;
}

/* ── 公告 ── */
.notice-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notice-list {
  flex: 1;
  overflow-y: auto;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 12px;
}

.notice-title {
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 12px;
}

.notice-date {
  color: #999;
  flex-shrink: 0;
}

/* ── 广告 ── */
.ad-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border: 1px solid #e8e8e8;
}

.ad-placeholder {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  line-height: 1.5;
}

/* ── 自定义弹窗 ── */
.custom-tip {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.custom-indicator-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.custom-item {
  font-size: 13px;
}

.custom-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
