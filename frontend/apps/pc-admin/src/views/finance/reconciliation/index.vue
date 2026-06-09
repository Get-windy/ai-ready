<template>
  <PageContainer full-height>
    <template #header>
      <div class="reconciliation-page-header">
        <div class="reconciliation-page-header-left">
          <a-breadcrumb class="reconciliation-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>对账管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="reconciliation-page-header-title">对账管理</h2>
        </div>
        <div class="reconciliation-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="loadStats">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>
    <div class="reconciliation-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-bank">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.bankPending }}</div>
          <div class="stat-card-label">待银行对账</div>
        </div>
        <BankOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-customer">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.customerPending }}</div>
          <div class="stat-card-label">待客户对账</div>
        </div>
        <UserOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-supplier">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.supplierPending }}</div>
          <div class="stat-card-label">待供应商对账</div>
        </div>
        <TeamOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-difference">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.differenceCount }}</div>
          <div class="stat-card-label">差异待处理</div>
        </div>
        <WarningOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card title="财务对账" :bordered="false">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="bank"
          tab="银行对账"
        >
          <BankReconciliation />
        </a-tab-pane>
        <a-tab-pane
          key="customer"
          tab="客户对账"
        >
          <CustomerReconciliation />
        </a-tab-pane>
        <a-tab-pane
          key="supplier"
          tab="供应商对账"
        >
          <SupplierReconciliation />
        </a-tab-pane>
        <a-tab-pane
          key="difference"
          tab="差异处理"
        >
          <DifferenceHandling />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { BankOutlined, UserOutlined, TeamOutlined, WarningOutlined, SyncOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import BankReconciliation from './components/BankReconciliation.vue'
import CustomerReconciliation from './components/CustomerReconciliation.vue'
import SupplierReconciliation from './components/SupplierReconciliation.vue'
import DifferenceHandling from './components/DifferenceHandling.vue'
import { reconciliationApi } from '@/api/finance'

const activeTab = ref('bank')
const loading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

const stats = reactive({
  bankPending: 0,
  customerPending: 0,
  supplierPending: 0,
  differenceCount: 0
})

const loadStats = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const res = await reconciliationApi.getStats()
    if (res.data) {
      stats.bankPending = res.data.bankPending || 0
      stats.customerPending = res.data.customerPending || 0
      stats.supplierPending = res.data.supplierPending || 0
      stats.differenceCount = res.data.differenceCount || 0
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    console.warn('[对账管理] 获取对账统计数据失败', err)
    message.error('获取统计数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
  }
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadStats()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadStats()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: loadStats })
</script>

<style scoped>
.reconciliation-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.reconciliation-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.reconciliation-breadcrumb {
  font-size: 13px;
}
.reconciliation-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.reconciliation-page-header-right {
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

.reconciliation-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.stat-bank { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-customer { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-supplier { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-difference { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
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