<template>
  <div class="finance-report-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-trial" :class="{ 'stat-success': trialBalanceBalanced === true, 'stat-error': trialBalanceBalanced === false }">
        <div class="stat-card-body">
          <div class="stat-card-value">
            <span v-if="trialBalanceBalanced === null">-</span>
            <span v-else-if="trialBalanceBalanced">平衡</span>
            <span v-else>不平衡</span>
          </div>
          <div class="stat-card-label">试算平衡</div>
        </div>
        <CheckCircleOutlined v-if="trialBalanceBalanced" class="stat-card-icon" />
        <CloseCircleOutlined v-else-if="trialBalanceBalanced === false" class="stat-card-icon" />
        <LoadingOutlined v-else class="stat-card-icon" />
      </div>
      <div class="stat-card stat-balance" :class="{ 'stat-success': balanceSheetBalanced }">
        <div class="stat-card-body">
          <div class="stat-card-value">
            <span v-if="balanceSheetBalanced">平衡</span>
            <span v-else>待检查</span>
          </div>
          <div class="stat-card-label">资产负债</div>
        </div>
        <SyncOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-profit" :class="{ 'stat-profit-negative': incomeNetProfit && incomeNetProfit.current < 0 }">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(incomeNetProfit?.current || 0) }}</div>
          <div class="stat-card-label">本期净利润</div>
        </div>
        <RiseOutlined v-if="incomeNetProfit && incomeNetProfit.current >= 0" class="stat-card-icon" />
        <FallOutlined v-else class="stat-card-icon" />
      </div>
      <div class="stat-card stat-cumulative">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(incomeNetProfit?.cumulative || 0) }}</div>
          <div class="stat-card-label">累计净利润</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
    </div>

    <div class="report-filter-bar">
      <a-form layout="inline">
        <a-form-item label="年度">
          <a-input-number
            v-model:value="filterYear"
            :min="2020"
            :max="2099"
            style="width: 100px"
            size="small"
          />
        </a-form-item>
        <a-form-item label="期间">
          <a-select v-model:value="filterPeriod" style="width: 80px" size="small">
            <a-select-option v-for="p in 12" :key="p" :value="p">{{ p }}月</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="small" @click="handleGenerate">
            <template #icon><SearchOutlined /></template>
            生成
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <!-- 试算平衡表 -->
      <a-tab-pane key="trial-balance" tab="试算平衡表">
        <a-alert
          v-if="trialBalanceBalanced !== null"
          :type="trialBalanceBalanced ? 'success' : 'error'"
          :message="trialBalanceBalanced ? '试算平衡 - 借贷相等' : '试算不平衡'"
          show-icon
          style="margin-bottom: 16px"
        />
        <VxeTableList
          :columns="trialBalanceColumns"
          :data-source="trialBalanceData"
          :loading="trialLoading"
          :pagination="false"
          :row-key="'id'"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :show-export="false"
          :selectable="true"
          size="small"
          @selection-change="handleSelectionChange"
        />
      </a-tab-pane>

      <!-- 资产负债表 -->
      <a-tab-pane key="balance-sheet" tab="资产负债表">
        <a-alert
          :type="balanceSheetBalanced ? 'success' : 'warning'"
          :message="balanceSheetBalanced ? '资产 = 负债 + 所有者权益' : '资产 不等于 负债 + 所有者权益'"
          show-icon
          style="margin-bottom: 16px"
        />
        <VxeTableList
          :columns="balanceSheetColumns"
          :data-source="balanceSheetData"
          :loading="bsLoading"
          :pagination="false"
          :row-key="'id'"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :show-export="false"
          :selectable="true"
          size="small"
          @selection-change="handleSelectionChange"
        />
      </a-tab-pane>

      <!-- 利润表 -->
      <a-tab-pane key="income-statement" tab="利润表">
        <VxeTableList
          :columns="incomeStatementColumns"
          :data-source="incomeStatementData"
          :loading="isLoading"
          :pagination="false"
          :row-key="'id'"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :show-export="false"
          :selectable="true"
          :show-summary="!!incomeNetProfit"
          :summary-data="incomeSummaryData"
          size="small"
          @selection-change="handleSelectionChange"
        />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined,
  SyncOutlined, RiseOutlined, FallOutlined, DollarOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { reportApi } from '@/api/finance'

const activeTab = ref('trial-balance')
const filterYear = ref(dayjs().year())
const filterPeriod = ref(dayjs().month() + 1)

// 试算平衡表
const trialBalanceData = ref<any[]>([])
const trialLoading = ref(false)
const trialBalanceBalanced = ref<boolean | null>(null)

// 资产负债表
const balanceSheetData = ref<any[]>([])
const bsLoading = ref(false)
const balanceSheetBalanced = ref(false)

// 利润表
const incomeStatementData = ref<any[]>([])
const isLoading = ref(false)
const incomeNetProfit = ref<{ current: number; cumulative: number } | null>(null)

const incomeSummaryData = computed(() => {
  if (!incomeNetProfit.value) return undefined
  return [
    { label: '净利润（本期金额）', value: incomeNetProfit.value.current, type: 'currency' as const },
    { label: '净利润（本年累计）', value: incomeNetProfit.value.cumulative, type: 'currency' as const }
  ]
})

const trialBalanceColumns = computed(() => [
  { title: '科目', field: 'subjectName', width: 220, formatter: ({ row }) => `${row.subjectCode || ''} ${row.subjectName || ''}` },
  { title: '期初借方', field: 'openingDebit', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '期初贷方', field: 'openingCredit', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '本期借方', field: 'periodDebit', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '本期贷方', field: 'periodCredit', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '期末借方', field: 'closingDebit', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '期末贷方', field: 'closingCredit', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' }
])

const balanceSheetColumns = computed(() => [
  { title: '项目', field: 'itemName', width: 250, formatter: ({ cellValue, row }) => `${'  '.repeat(row.level || 0)}${cellValue}` },
  { title: '行次', field: 'lineNo', width: 60, align: 'center' },
  { title: '期末余额', field: 'endingBalance', width: 150, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '年初余额', field: 'beginningBalance', width: 150, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' }
])

const incomeStatementColumns = computed(() => [
  { title: '项目', field: 'itemName', width: 250, formatter: ({ cellValue, row }) => `${'  '.repeat(row.level || 0)}${cellValue}` },
  { title: '行次', field: 'lineNo', width: 60, align: 'center' },
  { title: '本期金额', field: 'currentAmount', width: 150, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' },
  { title: '本年累计', field: 'cumulativeAmount', width: 150, align: 'right', formatter: ({ cellValue }) => cellValue ? formatAmount(cellValue) : '-' }
])

const handleTabChange = (key: string) => {
  activeTab.value = key
  loadReport(key)
}

const handleGenerate = () => {
  loadReport(activeTab.value)
}

const loadReport = async (tab: string) => {
  const period = `${filterYear.value}-${String(filterPeriod.value).padStart(2, '0')}`

  if (tab === 'trial-balance') {
    await loadTrialBalance(period)
  } else if (tab === 'balance-sheet') {
    await loadBalanceSheet(period)
  } else if (tab === 'income-statement') {
    await loadIncomeStatement(period)
  }
}

const loadTrialBalance = async (period: string) => {
  trialLoading.value = true
  try {
    const res = await reportApi.getTrialBalance({ fiscalPeriod: period })
    if (res.data) {
      const items = res.data.items || res.data || []
      trialBalanceData.value = Array.isArray(items) ? items : []

      // Calculate totals for balance check
      const totalOpenDebit = trialBalanceData.value.reduce((s: number, r: any) => s + (r.openingDebit || 0), 0)
      const totalOpenCredit = trialBalanceData.value.reduce((s: number, r: any) => s + (r.openingCredit || 0), 0)
      const totalPeriodDebit = trialBalanceData.value.reduce((s: number, r: any) => s + (r.periodDebit || 0), 0)
      const totalPeriodCredit = trialBalanceData.value.reduce((s: number, r: any) => s + (r.periodCredit || 0), 0)
      const totalCloseDebit = trialBalanceData.value.reduce((s: number, r: any) => s + (r.closingDebit || 0), 0)
      const totalCloseCredit = trialBalanceData.value.reduce((s: number, r: any) => s + (r.closingCredit || 0), 0)

      // Add summary row
      trialBalanceData.value.push({
        subjectCode: '',
        subjectName: '合计',
        openingDebit: totalOpenDebit,
        openingCredit: totalOpenCredit,
        periodDebit: totalPeriodDebit,
        periodCredit: totalPeriodCredit,
        closingDebit: totalCloseDebit,
        closingCredit: totalCloseCredit
      })

      trialBalanceBalanced.value =
        Math.abs(totalOpenDebit - totalOpenCredit) < 0.01 &&
        Math.abs(totalPeriodDebit - totalPeriodCredit) < 0.01 &&
        Math.abs(totalCloseDebit - totalCloseCredit) < 0.01
    }
  } catch {
    message.error('获取试算平衡表失败')
    trialBalanceBalanced.value = null
  } finally {
    trialLoading.value = false
  }
}

const loadBalanceSheet = async (period: string) => {
  bsLoading.value = true
  try {
    const res = await reportApi.getBalanceSheet({ fiscalPeriod: period })
    if (res.data) {
      const items = res.data.items || res.data || []
      balanceSheetData.value = Array.isArray(items) ? items : []

      // Check asset = liability + equity
      const assets = balanceSheetData.value
        .filter((r: any) => r.type === 'asset' || r.category === 'asset')
        .reduce((s: number, r: any) => s + (r.endingBalance || 0), 0)
      const liabilities = balanceSheetData.value
        .filter((r: any) => r.type === 'liability' || r.category === 'liability')
        .reduce((s: number, r: any) => s + (r.endingBalance || 0), 0)
      const equity = balanceSheetData.value
        .filter((r: any) => r.type === 'equity' || r.category === 'equity')
        .reduce((s: number, r: any) => s + (r.endingBalance || 0), 0)

      balanceSheetBalanced.value = Math.abs(assets - (liabilities + equity)) < 0.01
    }
  } catch {
    message.error('获取资产负债表失败')
  } finally {
    bsLoading.value = false
  }
}

const loadIncomeStatement = async (period: string) => {
  isLoading.value = true
  try {
    const res = await reportApi.getIncomeStatement({ fiscalPeriod: period })
    if (res.data) {
      const items = res.data.items || res.data || []
      incomeStatementData.value = Array.isArray(items) ? items : []

      // Calculate net profit (last row)
      const lastItem = incomeStatementData.value[incomeStatementData.value.length - 1]
      if (lastItem) {
        incomeNetProfit.value = {
          current: lastItem.currentAmount || lastItem.amount || 0,
          cumulative: lastItem.cumulativeAmount || lastItem.total || 0
        }
      }
    }
  } catch {
    message.error('获取利润表失败')
  } finally {
    isLoading.value = false
  }
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  loadReport(activeTab.value)
})
</script>

<style scoped>
.finance-report-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-trial { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-trial.stat-success { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-trial.stat-error { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-balance { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-balance.stat-success { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-profit { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-profit.stat-profit-negative { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-cumulative { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 24px;
  color: rgba(0, 0, 0, 0.15);
}

.report-filter-bar {
  margin-bottom: 0;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
}

.amount-value {
  font-family: monospace;
  font-weight: 500;
}

.text-muted {
  color: #bbb;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>
