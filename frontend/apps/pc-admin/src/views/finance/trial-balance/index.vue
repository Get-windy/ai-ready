<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="trial-balance-header">
        <div class="trial-balance-header-left">
          <a-breadcrumb class="trial-balance-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>试算平衡表</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="trial-balance-header-title">试算平衡表</h2>
        </div>
        <div class="trial-balance-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>
    <div class="trial-balance-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-subjects">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ rawItems.length }}</div>
          <div class="stat-card-label">科目数量</div>
        </div>
        <AuditOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-opening">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(totals.totalOpeningDebit) }}</div>
          <div class="stat-card-label">期初余额</div>
        </div>
        <CalendarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-period">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(totals.totalPeriodDebit) }}</div>
          <div class="stat-card-label">本期发生额</div>
        </div>
        <LineChartOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-status">
        <div class="stat-card-body">
          <div class="stat-card-value" :class="{ 'balanced': isBalanced, 'unbalanced': !isBalanced }">
            {{ isBalanced === null ? '-' : (isBalanced ? '平衡' : '不平衡') }}
          </div>
          <div class="stat-card-label">试算状态</div>
        </div>
        <CheckCircleOutlined v-if="isBalanced" class="stat-card-icon success" />
        <CloseCircleOutlined v-else-if="isBalanced === false" class="stat-card-icon error" />
        <QuestionCircleOutlined v-else class="stat-card-icon" />
      </div>
    </div>

    <a-card>
      <template #title><AuditOutlined /> 试算平衡表</template>
      <template #extra>
        <a-space>
          会计期间：
          <a-date-picker
            v-model:value="periodDate"
            picker="month"
            style="width: 140px"
            size="small"
          />
          <a-button v-permission="'finance:trial-balance:view'" type="primary" size="small" @click="fetchData">
            <SearchOutlined /> 生成
          </a-button>
          <a-button v-permission="'finance:trial-balance:export'" size="small" @click="handleExport">
            <ExportOutlined /> 导出
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="isBalanced !== null"
        :type="isBalanced ? 'success' : 'error'"
        :message="isBalanced ? '试算平衡 ✓' : '试算不平衡 ✗'"
        :description="balanceMessage"
        show-icon
        style="margin-bottom: 16px"
      />

      <VxeTableList
        :min-empty-rows="12"
        :columns="vxeColumns"
        :data-source="tableData"
        :pagination="false as any"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @cell-dblclick="handleView"
      >
        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else>
              <InboxOutlined class="table-empty-icon" />
              <p class="table-empty-text">暂无数据</p>
            </template>
          </div>
        </template>
        <template #subjectNameCell="{ record }">
          <span :style="{ paddingLeft: (record._level || 0) * 20 + 'px' }">
            {{ record.subjectCode }} {{ record.subjectName }}
          </span>
        </template>
      </VxeTableList>
    </a-card>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import { AuditOutlined, SearchOutlined, ExportOutlined, CalendarOutlined, LineChartOutlined, CheckCircleOutlined, CloseCircleOutlined, QuestionCircleOutlined, SyncOutlined, ReloadOutlined, WarningOutlined, InboxOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import dayjs from 'dayjs'
import * as XLSX from 'xlsx'
import { reportApi } from '@/api/finance'
import type { TrialBalanceItem } from '@/api/finance/accounting'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const periodDate = ref(dayjs())
const rawItems = ref<TrialBalanceItem[]>([])
const totals = ref({
  totalOpeningDebit: 0,
  totalOpeningCredit: 0,
  totalPeriodDebit: 0,
  totalPeriodCredit: 0,
  totalClosingDebit: 0,
  totalClosingCredit: 0
})

const vxeColumns = computed(() => [
  { field: 'subjectName', title: '科目', width: 250, slotName: 'subjectNameCell' },
  { field: 'openingDebit', title: '期初借方', width: 130, align: 'right', formatter: ({ cellValue }: any) => (cellValue ?? 0).toFixed(2) },
  { field: 'openingCredit', title: '期初贷方', width: 130, align: 'right', formatter: ({ cellValue }: any) => (cellValue ?? 0).toFixed(2) },
  { field: 'periodDebit', title: '本期借方', width: 130, align: 'right', formatter: ({ cellValue }: any) => (cellValue ?? 0).toFixed(2) },
  { field: 'periodCredit', title: '本期贷方', width: 130, align: 'right', formatter: ({ cellValue }: any) => (cellValue ?? 0).toFixed(2) },
  { field: 'closingDebit', title: '期末借方', width: 130, align: 'right', formatter: ({ cellValue }: any) => (cellValue ?? 0).toFixed(2) },
  { field: 'closingCredit', title: '期末贷方', width: 130, align: 'right', formatter: ({ cellValue }: any) => (cellValue ?? 0).toFixed(2) },
])

const tableData = computed(() => {
  const data = rawItems.value.map((item, idx) => ({ ...item, _level: 0, _key: idx }))
  // 添加汇总行
  data.push({
    subjectCode: '',
    subjectName: '合计',
    openingDebit: totals.value.totalOpeningDebit,
    openingCredit: totals.value.totalOpeningCredit,
    periodDebit: totals.value.totalPeriodDebit,
    periodCredit: totals.value.totalPeriodCredit,
    closingDebit: totals.value.totalClosingDebit,
    closingCredit: totals.value.totalClosingCredit,
    _level: 0,
    _key: -1
  } as any)
  return data
})

const isBalanced = computed(() => {
  if (rawItems.value.length === 0) return null
  return (
    Math.abs(totals.value.totalOpeningDebit - totals.value.totalOpeningCredit) < 0.01 &&
    Math.abs(totals.value.totalPeriodDebit - totals.value.totalPeriodCredit) < 0.01 &&
    Math.abs(totals.value.totalClosingDebit - totals.value.totalClosingCredit) < 0.01
  )
})

const balanceMessage = computed(() => {
  if (isBalanced.value === null) return ''
  if (isBalanced.value) {
    return '所有科目借贷平衡，报表数据正确。'
  }
  return `期初差额: ${(totals.value.totalOpeningDebit - totals.value.totalOpeningCredit).toFixed(2)} | ` +
    `本期差额: ${(totals.value.totalPeriodDebit - totals.value.totalPeriodCredit).toFixed(2)} | ` +
    `期末差额: ${(totals.value.totalClosingDebit - totals.value.totalClosingCredit).toFixed(2)}`
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

async function fetchData() {
  loading.value = true
  refreshLoading.value = true
  try {
    const period = periodDate.value?.format('YYYY-MM') || ''
    const [year, p] = period.split('-')
    const res = await reportApi.getTrialBalance({ fiscalYear: parseInt(year), fiscalPeriod: parseInt(p) })
    const items: TrialBalanceItem[] = res.data || []
    rawItems.value = items
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')

    // 客户端计算合计数
    const totalOpeningDebit = items.reduce((s, r) => s + (r.openingDebit || 0), 0)
    const totalOpeningCredit = items.reduce((s, r) => s + (r.openingCredit || 0), 0)
    const totalPeriodDebit = items.reduce((s, r) => s + (r.periodDebit || 0), 0)
    const totalPeriodCredit = items.reduce((s, r) => s + (r.periodCredit || 0), 0)
    const totalClosingDebit = items.reduce((s, r) => s + (r.closingDebit || 0), 0)
    const totalClosingCredit = items.reduce((s, r) => s + (r.closingCredit || 0), 0)

    totals.value = {
      totalOpeningDebit,
      totalOpeningCredit,
      totalPeriodDebit,
      totalPeriodCredit,
      totalClosingDebit,
      totalClosingCredit
    }
    hasError.value = false
  } catch (err) {
    console.warn('获取试算平衡表失败', err)
    message.error('获取试算平衡表失败')
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
  }
}

function handleExport() {
  if (rawItems.value.length === 0) {
    message.warning('暂无数据可导出')
    return
  }
  const period = periodDate.value?.format('YYYY-MM') || 'unknown'
  const sheetData = tableData.value.map((item: any) => ({
    '科目': item.subjectName || '',
    '期初借方': item.openingDebit || 0,
    '期初贷方': item.openingCredit || 0,
    '本期借方': item.periodDebit || 0,
    '本期贷方': item.periodCredit || 0,
    '期末借方': item.closingDebit || 0,
    '期末贷方': item.closingCredit || 0
  }))
  const ws = XLSX.utils.json_to_sheet(sheetData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '试算平衡表')
  XLSX.writeFile(wb, `试算平衡表_${period}.xlsx`)
  message.success('导出成功')
}

function handleView(record: any) {
  message.info(`查看科目: ${record.subjectName || record.subjectCode || '-'}`)
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function handleParentCreate() { handleAdd() }
function handleAdd() {
  message.info('创建功能由父组件触发')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.trial-balance-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.trial-balance-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.trial-balance-breadcrumb {
  font-size: 13px;
}
.trial-balance-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.trial-balance-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.trial-balance-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.trial-balance-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-subjects { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-opening { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-period { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-status { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-value.balanced { color: #52c41a; }
.stat-card-value.unbalanced { color: #f5222d; }

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.stat-card-icon.success { color: rgba(82, 196, 26, 0.3); }
.stat-card-icon.error { color: rgba(245, 34, 45, 0.3); }

.amount-value {
  font-family: monospace;
  font-weight: 500;
}

.text-disabled {
  color: var(--color-text-disabled, #bbb);
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
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

</style>
