<template>
  <div class="finance-report-page">
    <a-card :bordered="false">
      <template #title>
        <a-space>财务报表</a-space>
      </template>
      <template #extra>
        <a-space>
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
        </a-space>
      </template>

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
          <a-table
            :columns="trialBalanceColumns"
            :data-source="trialBalanceData"
            :loading="trialLoading"
            :pagination="false"
            row-key="subjectCode"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'subjectName'">
                {{ record.subjectCode }} {{ record.subjectName }}
              </template>
              <template v-else-if="column.dataIndex && column.dataIndex !== 'subjectCode' && column.dataIndex !== 'subjectName'">
                <span :class="record[column.dataIndex] !== 0 ? 'amount-value' : 'text-muted'">
                  {{ formatAmount(record[column.dataIndex]) }}
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 资产负债表 -->
        <a-tab-pane key="balance-sheet" tab="资产负债表">
          <a-alert
            :type="balanceSheetBalanced ? 'success' : 'warning'"
            :message="balanceSheetBalanced ? '资产 = 负债 + 所有者权益' : '资产 不等于 负债 + 所有者权益'"
            show-icon
            style="margin-bottom: 16px"
          />
          <a-table
            :columns="balanceSheetColumns"
            :data-source="balanceSheetData"
            :loading="bsLoading"
            :pagination="false"
            row-key="id"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'itemName'">
                <span :style="{ paddingLeft: (record.level || 0) * 20 + 'px', fontWeight: record.level === 0 ? 600 : 'normal' }">
                  {{ record.itemName }}
                </span>
              </template>
              <template v-else-if="column.key === 'endingBalance' || column.key === 'beginningBalance'">
                {{ formatAmount(record[column.dataIndex]) }}
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 利润表 -->
        <a-tab-pane key="income-statement" tab="利润表">
          <a-table
            :columns="incomeStatementColumns"
            :data-source="incomeStatementData"
            :loading="isLoading"
            :pagination="false"
            row-key="id"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'itemName'">
                <span :style="{ paddingLeft: (record.level || 0) * 20 + 'px', fontWeight: record.level === 0 ? 600 : 'normal' }">
                  {{ record.itemName }}
                </span>
              </template>
              <template v-else-if="column.key === 'currentAmount' || column.key === 'cumulativeAmount'">
                {{ formatAmount(record[column.dataIndex]) }}
              </template>
            </template>
            <template #summary>
              <a-table-summary-row v-if="incomeNetProfit !== null">
                <a-table-summary-cell :index="0">
                  <strong>净利润</strong>
                </a-table-summary-cell>
                <a-table-summary-cell :index="1">
                  <strong>{{ formatAmount(incomeNetProfit.current) }}</strong>
                </a-table-summary-cell>
                <a-table-summary-cell :index="2">
                  <strong>{{ formatAmount(incomeNetProfit.cumulative) }}</strong>
                </a-table-summary-cell>
              </a-table-summary-row>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
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

const trialBalanceColumns: TableProps['columns'] = [
  { title: '科目', key: 'subjectName', dataIndex: 'subjectName', width: 220 },
  { title: '期初借方', dataIndex: 'openingDebit', key: 'openingDebit', width: 130, align: 'right' },
  { title: '期初贷方', dataIndex: 'openingCredit', key: 'openingCredit', width: 130, align: 'right' },
  { title: '本期借方', dataIndex: 'periodDebit', key: 'periodDebit', width: 130, align: 'right' },
  { title: '本期贷方', dataIndex: 'periodCredit', key: 'periodCredit', width: 130, align: 'right' },
  { title: '期末借方', dataIndex: 'closingDebit', key: 'closingDebit', width: 130, align: 'right' },
  { title: '期末贷方', dataIndex: 'closingCredit', key: 'closingCredit', width: 130, align: 'right' }
]

const balanceSheetColumns: TableProps['columns'] = [
  { title: '项目', key: 'itemName', dataIndex: 'itemName', width: 250 },
  { title: '行次', dataIndex: 'lineNo', key: 'lineNo', width: 60, align: 'center' },
  { title: '期末余额', key: 'endingBalance', dataIndex: 'endingBalance', width: 150, align: 'right' },
  { title: '年初余额', key: 'beginningBalance', dataIndex: 'beginningBalance', width: 150, align: 'right' }
]

const incomeStatementColumns: TableProps['columns'] = [
  { title: '项目', key: 'itemName', dataIndex: 'itemName', width: 250 },
  { title: '行次', dataIndex: 'lineNo', key: 'lineNo', width: 60, align: 'center' },
  { title: '本期金额', key: 'currentAmount', dataIndex: 'currentAmount', width: 150, align: 'right' },
  { title: '本年累计', key: 'cumulativeAmount', dataIndex: 'cumulativeAmount', width: 150, align: 'right' }
]

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
  padding: 0;
}

.amount-value {
  font-family: monospace;
  font-weight: 500;
}

.text-muted {
  color: #bbb;
}
</style>
