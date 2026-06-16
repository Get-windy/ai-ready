<template>
  <ErrorBoundary @error="handleError">
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
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadStats)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
          <a-button type="primary" size="small" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增对账
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

      <!-- 新增对账弹窗 -->
      <FullScreenDetail
        :visible="addVisible"
        title="新增对账"
        :dirty="addFormDirty"
        :save-loading="addLoading"
        @save="handleAddConfirm"
        @close="handleAddCancel"
      >
        <a-form
          ref="addFormRef"
          :model="addForm"
          :rules="addFormRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="对账类型" name="type">
            <a-radio-group v-model:value="addForm.type">
              <a-radio value="bank">银行对账</a-radio>
              <a-radio value="customer">客户对账</a-radio>
              <a-radio value="supplier">供应商对账</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="对方名称" name="partyName">
            <a-input v-model:value="addForm.partyName" placeholder="请输入银行/客户/供应商名称" />
          </a-form-item>
          <a-form-item label="对账期间" name="period">
            <a-input v-model:value="addForm.period" placeholder="例如: 2026-06" />
          </a-form-item>
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="addForm.remark" :rows="3" placeholder="备注信息" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, BankOutlined, UserOutlined, TeamOutlined, WarningOutlined, SyncOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import BankReconciliation from './components/BankReconciliation.vue'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}
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
      stats.bankPending = res.bankPending || 0
      stats.customerPending = res.customerPending || 0
      stats.supplierPending = res.supplierPending || 0
      stats.differenceCount = res.differenceCount || 0
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
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', loadStats)
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
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', loadStats)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: loadStats })

function handleParentCreate() { handleAdd() }

// ── 新增对账 ────────────────────────────────────────────
const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref()
const addFormDirty = ref(false)
const addForm = reactive({
  type: 'bank',
  partyName: '',
  period: '',
  remark: ''
})
const addFormRules: any = {
  partyName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}
function handleAdd() {
  addForm.type = activeTab.value
  addForm.partyName = ''
  addForm.period = ''
  addForm.remark = ''
  addVisible.value = true
  addFormDirty.value = false
  setTimeout(() => { addFormDirty.value = true }, 500)
}
async function handleAddConfirm() {
  try {
    await addFormRef.value?.validate()
    addLoading.value = true
    await reconciliationApi.create({
      type: addForm.type,
      partyName: addForm.partyName,
      period: addForm.period || '',
      remark: addForm.remark || ''
    })
    message.success('对账创建成功')
    addVisible.value = false
    loadStats()
  } catch (err: any) {
    if (err?.message) message.error(err.message)
  } finally {
    addLoading.value = false
  }
}
function handleAddCancel() {
  addVisible.value = false
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', loadStats); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
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