<template>
  <PageContainer full-height>
    <template #header>
      <div class="performance-header">
        <div class="performance-header__left">
          <a-button type="text" class="performance-header__back" v-permission="'supplier:performance:back'" @click="handleBack">
            <template #icon><LeftOutlined /></template>
          </a-button>
<span class="shortcut-hints">
          <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
        </span>
          <div class="performance-header__titles">
            <span class="performance-header__breadcrumb">供应商 / 绩效评估</span>
            <h2 class="performance-header__title">供应商绩效评估</h2>
          </div>
        </div>
        <div class="performance-header__right">
          <a-space :size="12">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">
                数据更新: {{ lastUpdateTime }}
              </span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadPerformances)()">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button type="primary" v-permission="'supplier:performance:evaluate'" @click="handleEvaluate" :disabled="!supplierIdNum || supplierIdNum === null">
              <template #icon><PlusOutlined /></template>
              新增评估
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <ErrorBoundary>
    <div class="page-content">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-quality">
        <div class="stat-card-body">
          <div class="stat-card-value" :style="{ color: getScoreColor(avgScores.quality) }">{{ avgScores.quality.toFixed(1) }}</div>
          <div class="stat-card-label">质量评分</div>
        </div>
        <SafetyOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-delivery">
        <div class="stat-card-body">
          <div class="stat-card-value" :style="{ color: getScoreColor(avgScores.delivery) }">{{ avgScores.delivery.toFixed(1) }}</div>
          <div class="stat-card-label">交付评分</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-price">
        <div class="stat-card-body">
          <div class="stat-card-value" :style="{ color: getScoreColor(avgScores.price) }">{{ avgScores.price.toFixed(1) }}</div>
          <div class="stat-card-label">价格评分</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-service">
        <div class="stat-card-body">
          <div class="stat-card-value" :style="{ color: getScoreColor(avgScores.service) }">{{ avgScores.service.toFixed(1) }}</div>
          <div class="stat-card-label">服务评分</div>
        </div>
        <SmileOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-comprehensive">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ avgScores.comprehensive.toFixed(1) }}</div>
          <div class="stat-card-label">综合评分</div>
        </div>
        <StarOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card :bordered="false" v-if="supplier" style="margin-bottom: 16px">
      <a-descriptions size="small" :column="4">
        <a-descriptions-item label="供应商名称">{{ supplier.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ supplier.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="供应商等级">
          <a-tag :color="getLevelColor(supplier.supplierLevel)">{{ supplier.supplierLevel }}级</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="综合评分">
          <span :style="{ color: getScoreColor(supplier.comprehensiveScore), fontWeight: 600 }">
            {{ formatScore(supplier.comprehensiveScore) }}
          </span>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :bordered="false" class="table-card">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="performances"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` } as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        :min-empty-rows="12"
        @cell-dblclick="handleView"
      >
        <template #periodTypeCell="{ record }">
          {{ periodTypeLabel(record.periodType) }}
        </template>
        <template #qualityScoreCell="{ record }">
          <span :style="{ color: getScoreColor(record.qualityScore) }">{{ formatScore(record.qualityScore) }}</span>
        </template>
        <template #deliveryScoreCell="{ record }">
          <span :style="{ color: getScoreColor(record.deliveryScore) }">{{ formatScore(record.deliveryScore) }}</span>
        </template>
        <template #priceScoreCell="{ record }">
          <span :style="{ color: getScoreColor(record.priceScore) }">{{ formatScore(record.priceScore) }}</span>
        </template>
        <template #serviceScoreCell="{ record }">
          <span :style="{ color: getScoreColor(record.serviceScore) }">{{ formatScore(record.serviceScore) }}</span>
        </template>
        <template #comprehensiveScoreCell="{ record }">
          <span :style="{ color: getScoreColor(record.comprehensiveScore), fontWeight: 600 }">{{ formatScore(record.comprehensiveScore) }}</span>
        </template>
        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="loadPerformances" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else-if="!supplierIdNum || supplierIdNum === null">
              <InboxOutlined class="table-empty-icon" />
              <p class="table-empty-text">请先从供应商列表中选择供应商</p>
              <a-button type="primary" size="small" @click="handleBack" class="table-empty-action">前往供应商列表</a-button>
            </template>
            <template v-else>
              <InboxOutlined class="table-empty-icon" />
              <p class="table-empty-text">暂无绩效评估记录</p>
            </template>
          </div>
        </template>
      </VxeTableList>
    </a-card>

    <a-modal
      v-model:open="showEvaluateModal"
      title="新增绩效评估"
      @ok="submitEvaluate"
      :confirm-loading="submitLoading"
    >
      <a-form ref="formRef" :model="evaluateForm" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="评估周期" name="period">
          <a-input v-model:value="evaluateForm.period" size="small" placeholder="如: 2024-01" />
        </a-form-item>
        <a-form-item label="周期类型" name="periodType">
          <a-select v-model:value="evaluateForm.periodType" size="small">
            <a-select-option v-for="opt in periodTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="质量评分" name="qualityScore">
          <a-slider v-model:value="evaluateForm.qualityScore" :min="0" :max="100" />
        </a-form-item>
        <a-form-item label="交付评分" name="deliveryScore">
          <a-slider v-model:value="evaluateForm.deliveryScore" :min="0" :max="100" />
        </a-form-item>
        <a-form-item label="价格评分" name="priceScore">
          <a-slider v-model:value="evaluateForm.priceScore" :min="0" :max="100" />
        </a-form-item>
        <a-form-item label="服务评分" name="serviceScore">
          <a-slider v-model:value="evaluateForm.serviceScore" :min="0" :max="100" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="evaluateForm.remark" size="small" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
  </ErrorBoundary>
</PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SafetyOutlined, ClockCircleOutlined, DollarOutlined, SmileOutlined, StarOutlined, ReloadOutlined, SyncOutlined, LeftOutlined, WarningOutlined, InboxOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRouter, useRoute } from 'vue-router'
import { supplierApi } from '@/api/supplier'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { requiredRule } from '@/utils/formRules'
import type { FormInstance } from 'ant-design-vue'
import request from '@/utils/request'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const router = useRouter()
const route = useRoute()
const supplierId = (route.params.id as string) || (route.query.id as string) || ''
const supplierIdNum = computed(() => { const n = Number(supplierId); return isNaN(n) ? null : n })

interface PerformanceRecord {
  id: number
  period: string
  periodType: number
  qualityScore: number
  deliveryScore: number
  priceScore: number
  serviceScore: number
  comprehensiveScore: number
  evaluateTime: string
  evaluator: string
  remark: string
}

const supplier = ref<{ supplierCode: string; supplierName: string; supplierLevel: string; comprehensiveScore: number } | null>(null)
const performances = ref<PerformanceRecord[]>([])
const loading = ref(false)
const submitLoading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const showEvaluateModal = ref(false)
const formRef = ref<FormInstance>()

const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const periodTypeOptions = [
  { value: 1, label: '月度' },
  { value: 2, label: '季度' },
  { value: 3, label: '年度' }
]

const evaluateForm = ref({
  period: '',
  periodType: 1,
  qualityScore: 80,
  deliveryScore: 80,
  priceScore: 80,
  serviceScore: 80,
  remark: ''
})

const formRules = {
  period: [requiredRule('评估周期')],
  periodType: [{ required: true, message: '请选择周期类型' }]
}

const avgScores = computed(() => {
  const len = performances.value.length
  if (len === 0) return { quality: 0, delivery: 0, price: 0, service: 0, comprehensive: 0 }
  const sum = performances.value.reduce((acc, p) => ({
    quality: acc.quality + p.qualityScore,
    delivery: acc.delivery + p.deliveryScore,
    price: acc.price + p.priceScore,
    service: acc.service + p.serviceScore,
    comprehensive: acc.comprehensive + p.comprehensiveScore
  }), { quality: 0, delivery: 0, price: 0, service: 0, comprehensive: 0 })
  return {
    quality: sum.quality / len,
    delivery: sum.delivery / len,
    price: sum.price / len,
    service: sum.service / len,
    comprehensive: sum.comprehensive / len
  }
})



const vxeColumns = [
  { field: 'period', title: '评估周期', width: 110 },
  { field: 'periodType', title: '周期类型', width: 90, slotName: 'periodTypeCell' },
  { field: 'qualityScore', title: '质量评分', width: 100, slotName: 'qualityScoreCell' },
  { field: 'deliveryScore', title: '交付评分', width: 100, slotName: 'deliveryScoreCell' },
  { field: 'priceScore', title: '价格评分', width: 100, slotName: 'priceScoreCell' },
  { field: 'serviceScore', title: '服务评分', width: 100, slotName: 'serviceScoreCell' },
  { field: 'comprehensiveScore', title: '综合评分', width: 100, slotName: 'comprehensiveScoreCell' },
  { field: 'evaluator', title: '评估人', width: 100 },
  { field: 'evaluateTime', title: '评估时间', width: 120 },
  { field: 'remark', title: '备注', width: 150 },
]

const periodTypeLabel = (type: number) => periodTypeOptions.find(p => p.value === type)?.label || '未知'

const formatScore = (score: number) => score?.toFixed(1) || '0.0'

const getScoreColor = (score: number) => {
  if (score >= 90) return '#52c41a'
  if (score >= 80) return '#1890ff'
  if (score >= 70) return '#faad14'
  return '#ff4d4f'
}

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = { A: '#52c41a', B: '#1890ff', C: '#faad14', D: '#ff4d4f', E: '#969799' }
  return colors[level] || '#969799'
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', loadPerformances)(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleEvaluate(); return }
}

function handleParentCreate() { handleEvaluate() }

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  await Promise.allSettled([loadSupplier(), loadPerformances()])
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadPerformances()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('supplier:create', handleParentCreate)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('supplier:create', handleParentCreate)
})

defineExpose({ handleQuery: loadPerformances })

async function loadSupplier() {
  try {
    const id = supplierIdNum.value
    if (id === null) return
    const res = await supplierApi.getById(id)
    supplier.value = res as any
  } catch (err) {
    console.warn('[供应商] 加载供应商信息失败', err)
    supplier.value = { supplierCode: '-', supplierName: '-', supplierLevel: '-', comprehensiveScore: 0 }
  }
}

async function loadPerformances() {
  loading.value = true
  hasError.value = false
  try {
    const id = supplierIdNum.value
    if (id === null) return
    const res = await supplierApi.getPerformanceHistory(id)
    performances.value = (res as any)?.data || (res as any) || []
  } catch (err: any) {
    hasError.value = true
    console.warn('[供应商] 获取绩效记录失败', err)
    message.error(err?.message || '获取绩效记录失败')
    performances.value = []
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleEvaluate = () => {
  const now = new Date()
  evaluateForm.value = {
    period: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
    periodType: 1,
    qualityScore: 80,
    deliveryScore: 80,
    priceScore: 80,
    serviceScore: 80,
    remark: ''
  }
  showEvaluateModal.value = true
}

const submitEvaluate = async () => {
  try {
    await formRef.value?.validate()
    const id = supplierIdNum.value
    if (id === null) { message.error('无效的供应商ID'); return }
    submitLoading.value = true
    const comprehensive = (evaluateForm.value.qualityScore + evaluateForm.value.deliveryScore + evaluateForm.value.priceScore + evaluateForm.value.serviceScore) / 4
    await request.post('/supplier/performance/evaluate', {
      supplierId: id,
      evaluationPeriod: evaluateForm.value.period,
      evaluationType: evaluateForm.value.periodType,
      qualityScore: evaluateForm.value.qualityScore,
      deliveryScore: evaluateForm.value.deliveryScore,
      priceScore: evaluateForm.value.priceScore,
      serviceScore: evaluateForm.value.serviceScore,
      comprehensiveScore: comprehensive
    })
    console.warn('[供应商] 操作成功: 绩效评估提交成功')
    message.success('绩效评估提交成功')
    showEvaluateModal.value = false
    await loadPerformances()
  } catch (err: any) {
    if (err?.errorFields) { console.warn('[供应商] 表单验证失败', err); return }
    console.warn('[供应商] 提交绩效评估失败', err)
    message.error(err?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

const handleView = (record: any) => {
  const row = record?.row ?? record
  // double-click to view detail - currently navigates back to supplier detail
  handleBack()
}

const handleBack = () => {
  if (!supplierId) { router.push('/supplier'); return }
  router.push(`/supplier/detail/${supplierId}`)
}

</script>

<style scoped>
/* ── 让 VxeTableList 填满剩余空间 ──────────────────────── */
.page-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* ── 空状态 ── */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.performance-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.performance-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.performance-header__back {
  color: #303133;
  font-size: 16px;
  padding: 0 4px;
}

.performance-header__titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.performance-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.performance-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.performance-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
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

.stat-quality { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-delivery { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-price { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-service { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-comprehensive {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
}
.stat-comprehensive .stat-card-value,
.stat-comprehensive .stat-card-label {
  color: #fff;
}
.stat-comprehensive .stat-card-icon {
  color: rgba(255, 255, 255, 0.3);
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

.table-card {
  flex: 1;
  border-radius: 8px;
}

/* 空行占位符 */




/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
  }
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

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
