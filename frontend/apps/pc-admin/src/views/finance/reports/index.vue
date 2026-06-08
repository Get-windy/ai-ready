<template>
  <div class="reports-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-assets">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData.totalAssets) }}</div>
          <div class="stat-card-label">总资产</div>
        </div>
        <FundOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-liabilities">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData.totalLiabilities) }}</div>
          <div class="stat-card-label">总负债</div>
        </div>
        <CreditCardOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-income">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData.netIncome) }}</div>
          <div class="stat-card-label">净利润</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-date">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ periodDate.format('YYYY-MM') }}</div>
          <div class="stat-card-label">报表期间</div>
        </div>
        <CalendarOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card title="财务报表">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="balance-sheet"
          tab="资产负债表"
        >
          <BalanceSheet :period="periodDate.format('YYYY-MM')" @loaded="handleBalanceLoaded" />
        </a-tab-pane>
        <a-tab-pane
          key="profit-statement"
          tab="利润表"
        >
          <ProfitStatement :period="periodDate.format('YYYY-MM')" @loaded="handleProfitLoaded" />
        </a-tab-pane>
        <a-tab-pane
          key="cash-flow"
          tab="现金流量表"
        >
          <CashFlowStatement :period="periodDate.format('YYYY-MM')" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs from 'dayjs'
import { FundOutlined, CreditCardOutlined, DollarOutlined, CalendarOutlined } from '@ant-design/icons-vue'
import BalanceSheet from './components/BalanceSheet.vue'
import ProfitStatement from './components/ProfitStatement.vue'
import CashFlowStatement from './components/CashFlowStatement.vue'

const activeTab = ref('balance-sheet')
const periodDate = ref(dayjs())

const summaryData = reactive({
  totalAssets: 1250000.00,
  totalLiabilities: 350000.00,
  netIncome: 100000.00
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

function handleBalanceLoaded(data: any) {
  if (data) {
    summaryData.totalAssets = data.totalAssets || 0
    summaryData.totalLiabilities = data.totalLiabilities || 0
  }
}

function handleProfitLoaded(data: any) {
  if (data) {
    summaryData.netIncome = data.netProfit || 0
  }
}
</script>

<style scoped>
.reports-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
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

.stat-assets { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-liabilities { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-income { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-date { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-body {
  flex: 1;
}

.stat-card-value {
  font-size: 20px;
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
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>