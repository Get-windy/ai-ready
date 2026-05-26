<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const supplierId = route.params.id as string

interface PerformanceRecord {
  id: number
  supplierId: number
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

interface SupplierInfo {
  supplierCode: string
  supplierName: string
  supplierLevel: string
  comprehensiveScore: number
}

const supplier = ref<SupplierInfo | null>(null)
const performances = ref<PerformanceRecord[]>([])
const loading = ref(false)
const showEvaluateModal = ref(false)
const evaluateForm = ref({
  period: '',
  periodType: 1,
  qualityScore: 80,
  deliveryScore: 80,
  priceScore: 80,
  serviceScore: 80,
  remark: ''
})

const periodTypeOptions = [
  { value: 1, label: '月度' },
  { value: 2, label: '季度' },
  { value: 3, label: '年度' }
]

const avgScores = computed(() => {
  if (performances.value.length === 0) return { quality: 0, delivery: 0, price: 0, service: 0, comprehensive: 0 }
  const sum = performances.value.reduce((acc, p) => ({
    quality: acc.quality + p.qualityScore,
    delivery: acc.delivery + p.deliveryScore,
    price: acc.price + p.priceScore,
    service: acc.service + p.serviceScore,
    comprehensive: acc.comprehensive + p.comprehensiveScore
  }), { quality: 0, delivery: 0, price: 0, service: 0, comprehensive: 0 })
  const len = performances.value.length
  return {
    quality: sum.quality / len,
    delivery: sum.delivery / len,
    price: sum.price / len,
    service: sum.service / len,
    comprehensive: sum.comprehensive / len
  }
})

onMounted(async () => {
  await loadSupplier()
  await loadPerformances()
})

const loadSupplier = async () => {
  try {
    const response = await fetch(`/api/supplier/${supplierId}`)
    const data = await response.json()
    if (data.code === 200) {
      supplier.value = data.data
    }
  } catch {
    supplier.value = { supplierCode: 'SUP001', supplierName: '示例供应商', supplierLevel: 'A', comprehensiveScore: 85 }
  }
}

const loadPerformances = async () => {
  loading.value = true
  try {
    const response = await fetch(`/api/supplier/${supplierId}/performance/history`)
    const data = await response.json()
    if (data.code === 200) {
      performances.value = data.data || []
    }
  } catch {
    performances.value = [
      { id: 1, supplierId: 1, period: '2024-01', periodType: 1, qualityScore: 90, deliveryScore: 85, priceScore: 80, serviceScore: 88, comprehensiveScore: 85.5, evaluateTime: '2024-01-31', evaluator: '采购员张', remark: '质量稳定' },
      { id: 2, supplierId: 1, period: '2024-02', periodType: 1, qualityScore: 88, deliveryScore: 90, priceScore: 82, serviceScore: 85, comprehensiveScore: 86.25, evaluateTime: '2024-02-28', evaluator: '采购员李', remark: '交付及时' }
    ]
  } finally {
    loading.value = false
  }
}

const handleEvaluate = () => {
  const now = new Date()
  evaluateForm.value.period = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  showEvaluateModal.value = true
}

const submitEvaluate = async () => {
  try {
    const comprehensive = (evaluateForm.value.qualityScore + evaluateForm.value.deliveryScore + evaluateForm.value.priceScore + evaluateForm.value.serviceScore) / 4
    const response = await fetch(`/api/supplier/${supplierId}/performance`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...evaluateForm.value,
        comprehensiveScore: comprehensive
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      alert('绩效评估提交成功')
      showEvaluateModal.value = false
      loadPerformances()
    }
  } catch {
    alert('提交失败')
  }
}

const handleBack = () => {
  router.push(`/supplier/detail/${supplierId}`)
}

const formatScore = (score: number) => score.toFixed(1)

const getScoreColor = (score: number) => {
  if (score >= 90) return '#07c160'
  if (score >= 80) return '#1988fa'
  if (score >= 70) return '#ff976a'
  return '#f44'
}

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = { 'A': '#07c160', 'B': '#1988fa', 'C': '#ff976a', 'D': '#f44', 'E': '#969799' }
  return colors[level] || '#969799'
}
</script>

<template>
  <div class="performance-page">
    <div class="page-header">
      <button class="back-btn" @click="handleBack">← 返回</button>
      <h1>供应商绩效评估</h1>
      <button class="evaluate-btn" @click="handleEvaluate">+ 新增评估</button>
    </div>

    <div class="supplier-summary" v-if="supplier">
      <div class="summary-item">
        <span class="label">供应商</span>
        <span class="value">{{ supplier.supplierName }}</span>
      </div>
      <div class="summary-item">
        <span class="label">编码</span>
        <span class="value">{{ supplier.supplierCode }}</span>
      </div>
      <div class="summary-item">
        <span class="label">等级</span>
        <span class="value level" :style="{ color: getLevelColor(supplier.supplierLevel) }">{{ supplier.supplierLevel }}级</span>
      </div>
      <div class="summary-item">
        <span class="label">当前评分</span>
        <span class="value score" :style="{ color: getScoreColor(supplier.comprehensiveScore) }">{{ formatScore(supplier.comprehensiveScore) }}</span>
      </div>
    </div>

    <div class="score-summary">
      <div class="score-card">
        <div class="score-title">质量评分</div>
        <div class="score-value" :style="{ color: getScoreColor(avgScores.quality) }">{{ formatScore(avgScores.quality) }}</div>
        <div class="score-label">平均分</div>
      </div>
      <div class="score-card">
        <div class="score-title">交付评分</div>
        <div class="score-value" :style="{ color: getScoreColor(avgScores.delivery) }">{{ formatScore(avgScores.delivery) }}</div>
        <div class="score-label">平均分</div>
      </div>
      <div class="score-card">
        <div class="score-title">价格评分</div>
        <div class="score-value" :style="{ color: getScoreColor(avgScores.price) }">{{ formatScore(avgScores.price) }}</div>
        <div class="score-label">平均分</div>
      </div>
      <div class="score-card">
        <div class="score-title">服务评分</div>
        <div class="score-value" :style="{ color: getScoreColor(avgScores.service) }">{{ formatScore(avgScores.service) }}</div>
        <div class="score-label">平均分</div>
      </div>
      <div class="score-card comprehensive">
        <div class="score-title">综合评分</div>
        <div class="score-value" :style="{ color: getScoreColor(avgScores.comprehensive) }">{{ formatScore(avgScores.comprehensive) }}</div>
        <div class="score-label">平均分</div>
      </div>
    </div>

    <div class="performance-table" v-if="!loading">
      <table>
        <thead>
          <tr>
            <th>评估周期</th>
            <th>周期类型</th>
            <th>质量评分</th>
            <th>交付评分</th>
            <th>价格评分</th>
            <th>服务评分</th>
            <th>综合评分</th>
            <th>评估人</th>
            <th>评估时间</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="perf in performances" :key="perf.id">
            <td>{{ perf.period }}</td>
            <td>{{ periodTypeOptions.find(p => p.value === perf.periodType)?.label }}</td>
            <td :style="{ color: getScoreColor(perf.qualityScore) }">{{ formatScore(perf.qualityScore) }}</td>
            <td :style="{ color: getScoreColor(perf.deliveryScore) }">{{ formatScore(perf.deliveryScore) }}</td>
            <td :style="{ color: getScoreColor(perf.priceScore) }">{{ formatScore(perf.priceScore) }}</td>
            <td :style="{ color: getScoreColor(perf.serviceScore) }">{{ formatScore(perf.serviceScore) }}</td>
            <td :style="{ color: getScoreColor(perf.comprehensiveScore), fontWeight: 600 }">{{ formatScore(perf.comprehensiveScore) }}</td>
            <td>{{ perf.evaluator }}</td>
            <td>{{ perf.evaluateTime }}</td>
            <td>{{ perf.remark || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="performances.length === 0" class="empty-state">
        <p>暂无绩效评估记录</p>
      </div>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div class="modal" v-if="showEvaluateModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>新增绩效评估</h3>
          <button class="close-btn" @click="showEvaluateModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label>评估周期</label>
            <input v-model="evaluateForm.period" type="text" placeholder="如: 2024-01" />
          </div>
          <div class="form-item">
            <label>周期类型</label>
            <select v-model="evaluateForm.periodType">
              <option v-for="opt in periodTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <div class="form-item">
            <label>质量评分 (0-100)</label>
            <input v-model.number="evaluateForm.qualityScore" type="number" min="0" max="100" />
          </div>
          <div class="form-item">
            <label>交付评分 (0-100)</label>
            <input v-model.number="evaluateForm.deliveryScore" type="number" min="0" max="100" />
          </div>
          <div class="form-item">
            <label>价格评分 (0-100)</label>
            <input v-model.number="evaluateForm.priceScore" type="number" min="0" max="100" />
          </div>
          <div class="form-item">
            <label>服务评分 (0-100)</label>
            <input v-model.number="evaluateForm.serviceScore" type="number" min="0" max="100" />
          </div>
          <div class="form-item">
            <label>备注</label>
            <textarea v-model="evaluateForm.remark" rows="2"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showEvaluateModal = false">取消</button>
          <button class="submit-btn" @click="submitEvaluate">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.performance-page {
  padding: 20px;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;

  .back-btn {
    padding: 8px 15px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
  }

  h1 {
    flex: 1;
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }

  .evaluate-btn {
    padding: 10px 20px;
    background: #07c160;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
}

.supplier-summary {
  display: flex;
  gap: 20px;
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 20px;

  .summary-item {
    .label {
      font-size: 12px;
      color: #969799;
    }
    .value {
      font-size: 14px;
      color: #333;
      margin-left: 8px;
      &.level, &.score { font-weight: 600; }
    }
  }
}

.score-summary {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;

  .score-card {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 15px;
    text-align: center;

    &.comprehensive {
      background: #1988fa;
      .score-title, .score-label { color: #fff; }
    }

    .score-title {
      font-size: 12px;
      color: #666;
    }

    .score-value {
      font-size: 24px;
      font-weight: 600;
      margin: 10px 0;
    }

    .score-label {
      font-size: 12px;
      color: #969799;
    }
  }
}

.performance-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;

  table {
    width: 100%;
    border-collapse: collapse;

    th, td {
      padding: 12px 15px;
      text-align: center;
      border-bottom: 1px solid #ebedf0;
    }

    th {
      background: #f7f8fa;
      font-weight: 600;
      color: #333;
    }

    td {
      color: #666;
    }
  }

  .empty-state {
    padding: 40px;
    text-align: center;
    color: #969799;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;

  .spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #ebedf0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  p { margin-top: 10px; color: #969799; }
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;

  .modal-content {
    background: #fff;
    border-radius: 8px;
    width: 500px;
    max-height: 80vh;
    overflow: auto;

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px 20px;
      border-bottom: 1px solid #ebedf0;

      h3 { font-size: 16px; color: #333; }
      .close-btn {
        background: none;
        border: none;
        font-size: 20px;
        cursor: pointer;
      }
    }

    .modal-body {
      padding: 20px;

      .form-item {
        margin-bottom: 15px;

        label {
          display: block;
          font-size: 12px;
          color: #666;
          margin-bottom: 8px;
        }

        input, select, textarea {
          width: 100%;
          padding: 10px;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
        }
      }
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      padding: 15px 20px;
      border-top: 1px solid #ebedf0;

      .cancel-btn, .submit-btn {
        padding: 10px 20px;
        border-radius: 4px;
        cursor: pointer;
      }

      .cancel-btn {
        background: #f7f8fa;
        color: #333;
        border: 1px solid #dcdfe6;
      }

      .submit-btn {
        background: #1988fa;
        color: #fff;
        border: none;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>