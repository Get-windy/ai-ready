<template>
  <div class="performance-page">
    <a-page-header
      title="供应商绩效评估"
      @back="handleBack"
    >
      <template #extra>
        <a-button type="primary" @click="handleEvaluate">
          <template #icon><PlusOutlined /></template>
          新增评估
        </a-button>
      </template>
    </a-page-header>

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
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
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
          <a-input v-model:value="evaluateForm.period" placeholder="如: 2024-01" />
        </a-form-item>
        <a-form-item label="周期类型" name="periodType">
          <a-select v-model:value="evaluateForm.periodType">
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
          <a-textarea v-model:value="evaluateForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SafetyOutlined, ClockCircleOutlined, DollarOutlined, SmileOutlined, StarOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { supplierApi } from '@/api/supplier'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { requiredRule } from '@/utils/formRules'
import type { FormInstance } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const supplierId = (route.params.id as string) || (route.query.id as string) || ''
const supplierIdNum = computed(() => { const n = Number(supplierId); return isNaN(n) ? null : n })

if (!supplierId) {
  console.warn('[供应商绩效] 缺少供应商ID参数，将返回列表')
  router.replace('/supplier/index')
}

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
const showEvaluateModal = ref(false)
const formRef = ref<FormInstance>()

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

onMounted(async () => {
  await Promise.allSettled([loadSupplier(), loadPerformances()])
})

const loadSupplier = async () => {
  try {
    const id = supplierIdNum.value
    if (id === null) return
    const res = await supplierApi.getById(id)
    supplier.value = res as any
  } catch {
    supplier.value = { supplierCode: '-', supplierName: '-', supplierLevel: '-', comprehensiveScore: 0 }
  }
}

const loadPerformances = async () => {
  loading.value = true
  try {
    const id = supplierIdNum.value
    if (id === null) return
    const res = await supplierApi.getPerformanceHistory(id)
    performances.value = (res as any)?.data || (res as any) || []
  } catch (err: any) {
    message.error(err?.message || '获取绩效记录失败')
    performances.value = []
  } finally {
    loading.value = false
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
    message.success('绩效评估提交成功')
    showEvaluateModal.value = false
    await loadPerformances()
  } catch (err: any) {
    if (err?.errorFields) return
    message.error(err?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

const handleBack = () => {
  router.push(`/supplier/detail/${supplierId}`)
}

</script>

<style scoped>
.performance-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
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
</style>
