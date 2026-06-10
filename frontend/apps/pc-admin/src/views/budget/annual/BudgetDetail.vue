<template>
  <PageContainer full-height>
    <template #header>
      <div class="detail-page-header">
        <div class="detail-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item><router-link to="/budget/annual">年度预算</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>预算详情</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="detail-page-header-title">{{ budget?.budgetNo ? `预算详情 - ${budget.budgetNo}` : '预算详情' }}</h2>
        </div>
        <div class="detail-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button size="small" @click="router.push('/budget/annual')">
            <template #icon><ArrowLeftOutlined /></template>
            返回
          </a-button>
        </div>
      </div>
    </template>

    <div class="budget-detail-body">
      <!-- 错误状态 -->
      <div v-if="hasError" class="detail-error-state">
        <WarningOutlined class="detail-error-icon" />
        <p class="detail-error-text">数据加载失败，请重试</p>
        <a-button type="primary" @click="loadData">重新加载</a-button>
      </div>

      <template v-else>
        <!-- 统计卡片 -->
        <div class="stat-cards">
          <div class="stat-card stat-total">
            <div class="stat-card-body">
              <div class="stat-card-value">¥{{ formatAmount(budget?.totalAmount) }}</div>
              <div class="stat-card-label">预算总额</div>
            </div>
            <FundOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-used">
            <div class="stat-card-body">
              <div class="stat-card-value">¥{{ formatAmount(budget?.totalUsedAmount) }}</div>
              <div class="stat-card-label">已使用</div>
            </div>
            <ShoppingOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-remaining">
            <div class="stat-card-body">
              <div class="stat-card-value">¥{{ formatAmount(budget?.totalRemainingAmount) }}</div>
              <div class="stat-card-label">剩余预算</div>
            </div>
            <WalletOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-rate">
            <div class="stat-card-body">
              <div class="stat-card-value">{{ (budget?.executionRate || 0).toFixed(1) }}%</div>
              <div class="stat-card-label">执行率</div>
            </div>
            <LineChartOutlined class="stat-card-icon" />
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="detail-actions">
          <a-space>
            <a-button v-if="budget?.status === 'draft'" size="small" @click="debounceClick('edit', handleEdit)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-button v-if="budget?.status === 'draft'" type="primary" size="small" @click="debounceClick('submit', handleSubmit)">
              提交审批
            </a-button>
            <a-button v-if="budget?.status === 'submitted'" type="primary" size="small" @click="debounceClick('approve', handleApprove)">
              审批通过
            </a-button>
            <a-button v-if="budget?.status === 'submitted'" danger size="small" @click="debounceClick('reject', handleReject)">
              拒绝
            </a-button>
            <a-button v-if="budget?.status === 'executing'" size="small" @click="debounceClick('close', handleClose)">
              关闭
            </a-button>
          </a-space>
        </div>

        <!-- 基本信息 -->
        <a-card title="基本信息" class="detail-section-card">
          <a-descriptions :column="{ xs: 1, sm: 2, md: 4 }" bordered size="small">
            <a-descriptions-item label="预算单号">{{ budget?.budgetNo }}</a-descriptions-item>
            <a-descriptions-item label="年度">{{ budget?.fiscalYear }}</a-descriptions-item>
            <a-descriptions-item label="部门">{{ budget?.departmentName }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusColor(budget?.status)">{{ statusText(budget?.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="预算总额">¥{{ budget?.totalAmount?.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="已使用">¥{{ budget?.totalUsedAmount?.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="剩余">¥{{ budget?.totalRemainingAmount?.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="执行率">{{ budget?.executionRate?.toFixed(2) }}%</a-descriptions-item>
            <a-descriptions-item label="已审批金额">¥{{ budget?.totalApprovedAmount?.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="模板">{{ budget?.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建人">{{ budget?.createdBy }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ budget?.createdAt }}</a-descriptions-item>
            <a-descriptions-item label="描述" :span="{ xs: 1, sm: 2, md: 4 }">{{ budget?.description || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="{ xs: 1, sm: 2, md: 4 }">{{ budget?.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 科目明细 -->
        <a-card title="预算科目明细" class="detail-section-card">
          <VxeTableList
            :data-source="budgetItems"
            :columns="itemVxeColumns"
            :loading="itemLoading"
            :pagination="false"
            row-key="id"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          />
        </a-card>

        <!-- 执行记录 + 调整历史 -->
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :md="12">
            <a-card title="执行记录" class="detail-section-card">
              <a-timeline v-if="executionLogs.length > 0">
                <a-timeline-item
                  v-for="log in executionLogs"
                  :key="log.id"
                  :color="log.executionType === 'consume' ? 'red' : log.executionType === 'freeze' ? 'blue' : log.executionType === 'unfreeze' ? 'orange' : 'green'"
                >
                  <template #label>
                    <small>{{ log.executionDate }}</small>
                  </template>
                  <div>{{ log.description }}</div>
                  <div v-if="log.amount">
                    <strong>金额: ¥{{ log.amount?.toFixed(2) }}</strong>
                  </div>
                  <div>
                    <small>类型: {{ log.executionType }} | 来源: {{ log.sourceType }} {{ log.sourceNo }}</small>
                  </div>
                </a-timeline-item>
              </a-timeline>
              <a-empty v-else description="暂无执行记录" />
            </a-card>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-card title="调整历史" class="detail-section-card">
              <VxeTableList
                :data-source="adjustments"
                :columns="adjustVxeColumns"
                :loading="adjLoading"
                :pagination="false"
                row-key="id"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #adjustmentTypeCell="{ record }">
                  <a-tag :color="record.adjustmentType === 'increase' ? 'green' : record.adjustmentType === 'decrease' ? 'red' : 'blue'">
                    {{ record.adjustmentType === 'increase' ? '增加' : record.adjustmentType === 'decrease' ? '减少' : '调剂' }}
                  </a-tag>
                </template>
                <template #amountCell="{ record }">
                  ¥{{ record.amount?.toFixed(2) }}
                </template>
                <template #statusCell="{ record }">
                  <a-tag :color="adjStatusColor(record.status)">{{ adjStatusText(record.status) }}</a-tag>
                </template>
              </VxeTableList>
            </a-card>
          </a-col>
        </a-row>
      </template>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { annualBudgetApi, budgetItemApi, budgetAdjustmentApi } from '@/api/budget'
import { message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import {
  FundOutlined, ShoppingOutlined, WalletOutlined, LineChartOutlined,
  SyncOutlined, ReloadOutlined, WarningOutlined, ArrowLeftOutlined, EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const budgetId = ref(Number(route.params.id))

const budget = ref<any>(null)
const budgetItems = ref<any[]>([])
const itemLoading = ref(false)
const executionLogs = ref<any[]>([])
const adjustments = ref<any[]>([])
const adjLoading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function formatAmount(amount: number | undefined): string {
  return (amount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const amountFmt = ({ cellValue }: any) => `¥${(cellValue ?? 0).toFixed(2)}`
const rateFmt = ({ cellValue }: any) => `${(cellValue ?? 0).toFixed(2)}%`

const itemVxeColumns = [
  { field: 'subjectCode', title: '科目编码' },
  { field: 'subjectName', title: '科目名称' },
  { field: 'budgetAmount', title: '预算金额', align: 'right', formatter: amountFmt },
  { field: 'usedAmount', title: '已使用', align: 'right', formatter: amountFmt },
  { field: 'remainingAmount', title: '剩余', align: 'right', formatter: amountFmt },
  { field: 'frozenAmount', title: '冻结', align: 'right', formatter: amountFmt },
  { field: 'executionRate', title: '执行率', align: 'right', width: 80, formatter: rateFmt },
]

const adjustVxeColumns = [
  { field: 'adjustmentNo', title: '调整单号' },
  { field: 'adjustmentType', title: '类型', width: 70, slotName: 'adjustmentTypeCell' },
  { field: 'amount', title: '金额', align: 'right', slotName: 'amountCell' },
  { field: 'status', title: '状态', width: 70, slotName: 'statusCell' },
  { field: 'reason', title: '原因', showOverflow: 'tooltip' as const },
]

const statusColor = (s: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'cyan', rejected: 'red', executing: 'green', closed: 'orange' }
  return map[s] || 'default'
}
const statusText = (s: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已审批', rejected: '已拒绝', executing: '执行中', closed: '已关闭' }
  return map[s] || s
}
const adjStatusColor = (s: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'green', rejected: 'red' }
  return map[s] || 'default'
}
const adjStatusText = (s: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[s] || s
}

const loadData = async () => {
  try {
    hasError.value = false
    const res = await annualBudgetApi.getById(budgetId.value)
    if (res.success) {
      budget.value = res.data
      budgetItems.value = res.data.items || []
    }
  } catch (_) {
    hasError.value = true
    console.warn('[预算详情] 加载预算详情失败')
  }

  itemLoading.value = true
  try {
    const res = await budgetItemApi.listByBudget(budgetId.value)
    if (res.success) {
      budgetItems.value = res.data || []
    }
  } catch (_) {
    console.warn('[预算详情] 加载预算科目失败')
  }
  itemLoading.value = false

  adjLoading.value = true
  try {
    const res = await budgetAdjustmentApi.page({ budgetId: budgetId.value, pageNum: 0, pageSize: 100 })
    if (res.success) {
      adjustments.value = res.data.records || []
    }
  } catch (_) {
    console.warn('[预算详情] 加载调整记录失败')
  }
  adjLoading.value = false

  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  refreshLoading.value = false
}

const handleEdit = () => {
  router.push(`/budget/annual?edit=${budget.value?.id}`)
}

const handleSubmit = async () => {
  try {
    await annualBudgetApi.submit(budgetId.value)
    message.success('已提交审批')
    loadData()
  } catch (e: any) {
    console.warn('[预算详情] 提交失败', e)
    message.error(e?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async () => {
  try {
    await annualBudgetApi.approve(budgetId.value)
    message.success('审批通过')
    loadData()
  } catch (e: any) {
    console.warn('[预算详情] 审批失败', e)
    message.error(e?.response?.data?.message || '审批失败')
  }
}

const handleReject = async () => {
  try {
    await annualBudgetApi.reject(budgetId.value)
    message.success('已拒绝')
    loadData()
  } catch (e: any) {
    console.warn('[预算详情] 拒绝失败', e)
    message.error(e?.response?.data?.message || '拒绝失败')
  }
}

const handleClose = async () => {
  try {
    await annualBudgetApi.close(budgetId.value)
    message.success('已关闭')
    loadData()
  } catch (e: any) {
    console.warn('[预算详情] 关闭失败', e)
    message.error(e?.response?.data?.message || '关闭失败')
  }
}

// ── 快捷键 ────────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    e.preventDefault()
    router.push('/budget/annual')
    return
  }
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', loadData)
    return
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('budget:refresh', loadData)
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('budget:refresh', loadData)
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: loadData })
</script>

<style scoped>
.detail-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.detail-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.detail-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.detail-page-header-right {
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

.budget-detail-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow-y: auto;
  min-height: 0;
}

/* 错误状态 */
.detail-error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}
.detail-error-icon {
  font-size: 48px;
  color: #faad14;
}
.detail-error-text {
  color: #999;
  margin: 16px 0;
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

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-used { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-remaining { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-rate { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

/* 操作栏 */
.detail-actions {
  margin-bottom: 16px;
}

/* 详情区块卡片 */
.detail-section-card {
  margin-bottom: 16px;
}

/* 紧凑尺寸覆盖 */
.budget-detail-body :deep(.ant-input-sm),
.budget-detail-body :deep(.ant-input-number-sm),
.budget-detail-body :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.budget-detail-body :deep(.ant-picker-small),
.budget-detail-body :deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
.budget-detail-body :deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
.budget-detail-body :deep(.ant-input-number-sm input) {
  height: 26px;
}

.detail-section-card :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
