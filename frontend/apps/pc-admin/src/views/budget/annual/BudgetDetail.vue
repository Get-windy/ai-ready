<template>
  <div class="budget-detail-page">
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

    <a-page-header
      :title="`预算详情 - ${budget?.budgetNo ?? ''}`"
      @back="() => $router.push('/budget/annual')"
    >
      <template #extra>
        <a-space>
          <a-button v-if="budget?.status === 'draft'" @click="handleEdit">编辑</a-button>
          <a-button v-if="budget?.status === 'draft'" type="primary" @click="handleSubmit">提交审批</a-button>
          <a-button v-if="budget?.status === 'submitted'" type="primary" @click="handleApprove">审批通过</a-button>
          <a-button v-if="budget?.status === 'submitted'" danger @click="handleReject">拒绝</a-button>
          <a-button v-if="budget?.status === 'executing'" @click="handleClose">关闭</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-row :gutter="[16, 16]" class="mt-2">
      <!-- 基本信息 -->
      <a-col :span="24">
        <a-card title="基本信息">
          <a-descriptions :column="4" bordered size="small">
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
            <a-descriptions-item label="描述" :span="4">{{ budget?.description || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="4">{{ budget?.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-2">
      <!-- 科目明细 -->
      <a-col :span="24">
        <a-card title="预算科目明细">
          <a-table
            :data-source="budgetItems"
            :columns="itemColumns"
            :loading="itemLoading"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'budgetAmount' || column.key === 'usedAmount' || column.key === 'remainingAmount' || column.key === 'frozenAmount'">
                ¥{{ record[column.key]?.toFixed(2) ?? '0.00' }}
              </template>
              <template v-else-if="column.key === 'executionRate'">
                {{ record.executionRate?.toFixed(2) ?? '0.00' }}%
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-2">
      <!-- 执行日志 -->
      <a-col :span="12">
        <a-card title="执行记录">
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
      <!-- 调整历史 -->
      <a-col :span="12">
        <a-card title="调整历史">
          <a-table
            :data-source="adjustments"
            :columns="adjustColumns"
            :loading="adjLoading"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'adjustmentType'">
                <a-tag :color="record.adjustmentType === 'increase' ? 'green' : record.adjustmentType === 'decrease' ? 'red' : 'blue'">
                  {{ record.adjustmentType === 'increase' ? '增加' : record.adjustmentType === 'decrease' ? '减少' : '调剂' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'amount'">
                ¥{{ record.amount?.toFixed(2) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="adjStatusColor(record.status)">{{ adjStatusText(record.status) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { annualBudgetApi, budgetItemApi, budgetAdjustmentApi, budgetReportApi } from '@/api/budget'
import { message } from 'ant-design-vue'
import { FundOutlined, ShoppingOutlined, WalletOutlined, LineChartOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const budgetId = ref(Number(route.params.id))

const budget = ref<any>(null)
const budgetItems = ref<any[]>([])
const itemLoading = ref(false)
const executionLogs = ref<any[]>([])
const adjustments = ref<any[]>([])
const adjLoading = ref(false)

function formatAmount(amount: number | undefined): string {
  return (amount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const itemColumns = [
  { title: '科目编码', dataIndex: 'subjectCode', key: 'subjectCode' },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName' },
  { title: '预算金额', dataIndex: 'budgetAmount', key: 'budgetAmount', align: 'right' as const },
  { title: '已使用', dataIndex: 'usedAmount', key: 'usedAmount', align: 'right' as const },
  { title: '剩余', dataIndex: 'remainingAmount', key: 'remainingAmount', align: 'right' as const },
  { title: '冻结', dataIndex: 'frozenAmount', key: 'frozenAmount', align: 'right' as const },
  { title: '执行率', dataIndex: 'executionRate', key: 'executionRate', align: 'right' as const, width: 80 },
]

const adjustColumns = [
  { title: '调整单号', dataIndex: 'adjustmentNo', key: 'adjustmentNo' },
  { title: '类型', dataIndex: 'adjustmentType', key: 'adjustmentType', width: 70 },
  { title: '金额', dataIndex: 'amount', key: 'amount', align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 70 },
  { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
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
    const res = await annualBudgetApi.getById(budgetId.value)
    if (res.success) {
      budget.value = res.data
      budgetItems.value = res.data.items || []
    }
  } catch (_) { /* ignore */ }

  itemLoading.value = true
  try {
    const res = await budgetItemApi.listByBudget(budgetId.value)
    if (res.success) {
      budgetItems.value = res.data || []
    }
  } catch (_) { /* ignore */ }
  itemLoading.value = false

  adjLoading.value = true
  try {
    const res = await budgetAdjustmentApi.page({ budgetId: budgetId.value, pageNum: 0, pageSize: 100 })
    if (res.success) {
      adjustments.value = res.data.records || []
    }
  } catch (_) { /* ignore */ }
  adjLoading.value = false
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
    message.error(e?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async () => {
  try {
    await annualBudgetApi.approve(budgetId.value)
    message.success('审批通过')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '审批失败')
  }
}

const handleReject = async () => {
  try {
    await annualBudgetApi.reject(budgetId.value)
    message.success('已拒绝')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '拒绝失败')
  }
}

const handleClose = async () => {
  try {
    await annualBudgetApi.close(budgetId.value)
    message.success('已关闭')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '关闭失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.budget-detail-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.mt-2 { margin-top: 16px; }

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

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
