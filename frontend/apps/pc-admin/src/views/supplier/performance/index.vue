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

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="4" v-for="(score, key) in scoreCards" :key="key">
        <a-card :class="{ 'comprehensive-card': key === 'comprehensive' }">
          <a-statistic
            :title="score.title"
            :value="score.value"
            :precision="1"
            :value-style="{ color: score.color }"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="performances"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'periodType'">
            {{ periodTypeLabel(record.periodType) }}
          </template>
          <template v-else-if="column.key === 'qualityScore'">
            <span :style="{ color: getScoreColor(record.qualityScore) }">{{ formatScore(record.qualityScore) }}</span>
          </template>
          <template v-else-if="column.key === 'deliveryScore'">
            <span :style="{ color: getScoreColor(record.deliveryScore) }">{{ formatScore(record.deliveryScore) }}</span>
          </template>
          <template v-else-if="column.key === 'priceScore'">
            <span :style="{ color: getScoreColor(record.priceScore) }">{{ formatScore(record.priceScore) }}</span>
          </template>
          <template v-else-if="column.key === 'serviceScore'">
            <span :style="{ color: getScoreColor(record.serviceScore) }">{{ formatScore(record.serviceScore) }}</span>
          </template>
          <template v-else-if="column.key === 'comprehensiveScore'">
            <span :style="{ color: getScoreColor(record.comprehensiveScore), fontWeight: 600 }">{{ formatScore(record.comprehensiveScore) }}</span>
          </template>
        </template>
      </a-table>
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
import { PlusOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { supplierApi } from '@/api/supplier'
import { requiredRule } from '@/utils/formRules'
import type { FormInstance } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()
const supplierId = route.params.id as string
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

const scoreCards = computed(() => [
  { title: '质量评分', value: avgScores.value.quality, color: getScoreColor(avgScores.value.quality) },
  { title: '交付评分', value: avgScores.value.delivery, color: getScoreColor(avgScores.value.delivery) },
  { title: '价格评分', value: avgScores.value.price, color: getScoreColor(avgScores.value.price) },
  { title: '服务评分', value: avgScores.value.service, color: getScoreColor(avgScores.value.service) },
  { title: '综合评分', value: avgScores.value.comprehensive, color: '#fff' }
])

const columns = [
  { title: '评估周期', dataIndex: 'period', key: 'period', width: 110 },
  { title: '周期类型', key: 'periodType', width: 90 },
  { title: '质量评分', key: 'qualityScore', width: 100 },
  { title: '交付评分', key: 'deliveryScore', width: 100 },
  { title: '价格评分', key: 'priceScore', width: 100 },
  { title: '服务评分', key: 'serviceScore', width: 100 },
  { title: '综合评分', key: 'comprehensiveScore', width: 100 },
  { title: '评估人', dataIndex: 'evaluator', key: 'evaluator', width: 100 },
  { title: '评估时间', dataIndex: 'evaluateTime', key: 'evaluateTime', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark' }
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
    const res = await supplierApi.getById(id)
    await request.post(`/supplier/${id}/performance`, {
      supplierId: id,
      ...evaluateForm.value,
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

import request from '@/utils/request'
</script>

<style scoped>
.comprehensive-card {
  background: #1890ff;
}
.comprehensive-card :deep(.ant-statistic-title),
.comprehensive-card :deep(.ant-statistic-content) {
  color: #fff;
}
</style>
