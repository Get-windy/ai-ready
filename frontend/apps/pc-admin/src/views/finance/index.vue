<template>
  <PageContainer title="财务管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="loadDashboard">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <div class="finance-dashboard">
      <!-- KPI 卡片 -->
      <div class="kpi-cards">
        <div class="kpi-card kpi-assets">
          <div class="kpi-card-content">
            <div class="kpi-card-value">
              <span class="kpi-prefix">¥</span>
              <span class="kpi-number">{{ formatAmount(dashboardData.totalAssets) }}</span>
            </div>
            <div class="kpi-card-label">总资产</div>
          </div>
          <FundOutlined class="kpi-card-icon" />
        </div>
        <div class="kpi-card kpi-liabilities">
          <div class="kpi-card-content">
            <div class="kpi-card-value">
              <span class="kpi-prefix">¥</span>
              <span class="kpi-number">{{ formatAmount(dashboardData.totalLiabilities) }}</span>
            </div>
            <div class="kpi-card-label">总负债</div>
          </div>
          <CreditCardOutlined class="kpi-card-icon" />
        </div>
        <div class="kpi-card kpi-income">
          <div class="kpi-card-content">
            <div class="kpi-card-value">
              <span class="kpi-prefix">¥</span>
              <span class="kpi-number">{{ formatAmount(dashboardData.monthlyIncome) }}</span>
            </div>
            <div class="kpi-card-label">本月收入</div>
          </div>
          <RiseOutlined class="kpi-card-icon" />
        </div>
        <div class="kpi-card kpi-expense">
          <div class="kpi-card-content">
            <div class="kpi-card-value">
              <span class="kpi-prefix">¥</span>
              <span class="kpi-number">{{ formatAmount(dashboardData.monthlyExpense) }}</span>
            </div>
            <div class="kpi-card-label">本月费用</div>
          </div>
          <FallOutlined class="kpi-card-icon" />
        </div>
        <div class="kpi-card kpi-profit" :class="{ 'profit-negative': dashboardData.netProfit < 0 }">
          <div class="kpi-card-content">
            <div class="kpi-card-value">
              <span class="kpi-prefix">¥</span>
              <span class="kpi-number">{{ formatAmount(dashboardData.netProfit) }}</span>
            </div>
            <div class="kpi-card-label">净利润</div>
          </div>
          <DollarOutlined class="kpi-card-icon" />
        </div>
      </div>

      <!-- 快速入口 -->
      <div class="quick-links-section">
        <div class="quick-links-title">快速入口</div>
        <div class="quick-links-grid">
          <div class="quick-link-card" @click="navigateTo('subject')">
            <FileTextOutlined class="quick-link-icon subject-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">科目管理</div>
              <div class="quick-link-desc">管理会计科目体系</div>
            </div>
          </div>
          <div class="quick-link-card" @click="navigateTo('voucher')">
            <FileTextOutlined class="quick-link-icon voucher-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">凭证管理</div>
              <div class="quick-link-desc">录入和审核会计凭证</div>
            </div>
          </div>
          <div class="quick-link-card" @click="navigateTo('receivable')">
            <DollarOutlined class="quick-link-icon receivable-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">应收账款</div>
              <div class="quick-link-desc">管理客户应收款项</div>
            </div>
          </div>
          <div class="quick-link-card" @click="navigateTo('payable')">
            <DollarOutlined class="quick-link-icon payable-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">应付账款</div>
              <div class="quick-link-desc">管理供应商应付款项</div>
            </div>
          </div>
          <div class="quick-link-card" @click="navigateTo('report')">
            <BarChartOutlined class="quick-link-icon report-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">财务报表</div>
              <div class="quick-link-desc">查看财务报表分析</div>
            </div>
          </div>
          <div class="quick-link-card" @click="navigateTo('reconciliation')">
            <CheckSquareOutlined class="quick-link-icon reconciliation-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">对账管理</div>
              <div class="quick-link-desc">银行及往来对账</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 待办事项 -->
      <div class="todo-section">
        <div class="todo-title">待办事项</div>
        <div class="todo-grid">
          <div class="todo-card todo-unaudited" @click="navigateTo('voucher')">
            <div class="todo-count">{{ todoCounts.unaudited }}</div>
            <div class="todo-label">待审核凭证</div>
          </div>
          <div class="todo-card todo-unposted" @click="navigateTo('voucher')">
            <div class="todo-count">{{ todoCounts.unposted }}</div>
            <div class="todo-label">待过账凭证</div>
          </div>
          <div class="todo-card todo-overdue-receivable" @click="navigateTo('receivable')">
            <div class="todo-count todo-warning">{{ todoCounts.overdueReceivable }}</div>
            <div class="todo-label">逾期应收</div>
          </div>
          <div class="todo-card todo-overdue-payable" @click="navigateTo('payable')">
            <div class="todo-count todo-danger">{{ todoCounts.overduePayable }}</div>
            <div class="todo-label">逾期应付</div>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  FileTextOutlined, DollarOutlined, BarChartOutlined, CheckSquareOutlined,
  FundOutlined, CreditCardOutlined, RiseOutlined, FallOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import { reportApi } from '@/api/finance'

const router = useRouter()
const loading = ref(false)
const lastUpdateTime = ref('')

const dashboardData = reactive({
  totalAssets: 0,
  totalLiabilities: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  netProfit: 0
})

const todoCounts = reactive({
  unaudited: 5,
  unposted: 3,
  overdueReceivable: 2,
  overduePayable: 1
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const navigateTo = (page: string) => {
  const routes: Record<string, string> = {
    subject: '/erp/finance/subject',
    voucher: '/erp/finance/voucher',
    receivable: '/erp/finance/accounts-receivable',
    payable: '/erp/finance/accounts-payable',
    report: '/erp/finance/reports',
    reconciliation: '/erp/finance/reconciliation'
  }
  router.push(routes[page])
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await reportApi.getDashboard()
    if (res.data) {
      dashboardData.totalAssets = res.data.totalAssets || 0
      dashboardData.totalLiabilities = res.data.totalLiabilities || 0
      dashboardData.monthlyIncome = res.data.monthlyIncome || 0
      dashboardData.monthlyExpense = res.data.monthlyExpense || 0
      dashboardData.netProfit = (res.data.monthlyIncome || 0) - (res.data.monthlyExpense || 0)

      // 更新待办数据
      if (res.data.todoCounts) {
        todoCounts.unaudited = res.data.todoCounts.unaudited || 0
        todoCounts.unposted = res.data.todoCounts.unposted || 0
        todoCounts.overdueReceivable = res.data.todoCounts.overdueReceivable || 0
        todoCounts.overduePayable = res.data.todoCounts.overduePayable || 0
      }
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    // 使用默认数据
    dashboardData.totalAssets = 1250000.00
    dashboardData.totalLiabilities = 350000.00
    dashboardData.monthlyIncome = 280000.00
    dashboardData.monthlyExpense = 180000.00
    dashboardData.netProfit = 100000.00
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } finally {
    loading.value = false
  }
}

// 定时刷新
let refreshTimer: number | null = null

onMounted(() => {
  loadDashboard()
  refreshTimer = window.setInterval(() => {
    loadDashboard()
  }, 60000) // 60秒静默刷新
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.finance-dashboard {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  overflow-y: auto;
}

.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

/* KPI 卡片 */
.kpi-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.kpi-card {
  flex: 1;
  min-width: 180px;
  max-width: 250px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.kpi-assets { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.kpi-liabilities { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.kpi-income { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.kpi-expense { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.kpi-profit { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.kpi-profit-negative { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }

.kpi-card-content {
  flex: 1;
}

.kpi-card-value {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.kpi-prefix {
  font-size: 14px;
  color: #666;
}

.kpi-number {
  font-size: 24px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.kpi-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.kpi-card-icon {
  font-size: 32px;
  color: rgba(0, 0, 0, 0.15);
}

/* 快速入口 */
.quick-links-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.quick-links-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.quick-link-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-link-card:hover {
  background: #f0f0f0;
  transform: translateY(-1px);
}

.quick-link-icon {
  font-size: 28px;
}

.subject-icon { color: #1890ff; }
.voucher-icon { color: #722ed1; }
.receivable-icon { color: #52c41a; }
.payable-icon { color: #fa8c16; }
.report-icon { color: #eb2f96; }
.reconciliation-icon { color: #13c2c2; }

.quick-link-text {
  flex: 1;
}

.quick-link-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.quick-link-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 待办事项 */
.todo-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.todo-grid {
  display: flex;
  gap: 12px;
}

.todo-card {
  flex: 1;
  min-width: 100px;
  padding: 12px;
  border-radius: 6px;
  background: #fafafa;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.todo-card:hover {
  background: #f0f0f0;
}

.todo-unaudited { border-left: 3px solid #1890ff; }
.todo-unposted { border-left: 3px solid #722ed1; }
.todo-overdue-receivable { border-left: 3px solid #faad14; }
.todo-overdue-payable { border-left: 3px solid #ff4d4f; }

.todo-count {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.todo-warning { color: #faad14; }
.todo-danger { color: #ff4d4f; }

.todo-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .kpi-card {
    flex: 1 1 45%;
    max-width: none;
  }

  .quick-links-grid {
    grid-template-columns: 1fr;
  }

  .todo-grid {
    flex-wrap: wrap;
  }

  .todo-card {
    flex: 1 1 45%;
  }
}
</style>