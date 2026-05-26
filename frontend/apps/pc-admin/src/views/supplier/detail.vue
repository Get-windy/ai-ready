<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const supplierId = ref(route.params.id as string)

interface SupplierDetail {
  id: number
  supplierCode: string
  supplierName: string
  shortName: string
  supplierType: number
  supplierLevel: string
  cooperationStatus: number
  contactPerson: string
  contactPhone: string
  email: string
  address: string
  province: string
  city: string
  bankName: string
  bankAccount: string
  taxNumber: string
  comprehensiveScore: number
  totalPoints: number
  portalStatus: number
  portalAccountId: string
  createTime: string
  updateTime: string
  remark: string
}

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

interface InquiryRecord {
  id: number
  inquiryNo: string
  inquiryTitle: string
  inquiryStatus: number
  createTime: string
  quotationAmount: number
  quotationStatus: number
}

const supplier = ref<SupplierDetail | null>(null)
const performances = ref<PerformanceRecord[]>([])
const inquiries = ref<InquiryRecord[]>([])
const pointsRecords = ref<any[]>([])
const loading = ref(false)
const activeTab = ref('basic')

onMounted(async () => {
  loadSupplierDetail()
  loadPerformances()
  loadInquiries()
  loadPointsRecords()
})

const loadSupplierDetail = async () => {
  loading.value = true
  try {
    const response = await fetch(`/api/supplier/${supplierId.value}`)
    const data = await response.json()
    if (data.code === 200) {
      supplier.value = data.data
    }
  } finally {
    loading.value = false
  }
}

const loadPerformances = async () => {
  try {
    const response = await fetch(`/api/supplier/${supplierId.value}/performance/history`)
    const data = await response.json()
    if (data.code === 200) {
      performances.value = data.data || []
    }
  } catch {
    performances.value = []
  }
}

const loadInquiries = async () => {
  try {
    const response = await fetch(`/api/v1/supplier-portal/inquiries/supplier/${supplierId.value}`)
    const data = await response.json()
    if (data.code === 200) {
      inquiries.value = data.data || []
    }
  } catch {
    inquiries.value = []
  }
}

const loadPointsRecords = async () => {
  try {
    const response = await fetch(`/api/v1/supplier-portal/points/${supplierId.value}/records`)
    const data = await response.json()
    if (data.code === 200) {
      pointsRecords.value = data.data || []
    }
  } catch {
    pointsRecords.value = []
  }
}

const handleEdit = () => {
  router.push(`/supplier/edit/${supplierId.value}`)
}

const handleEvaluate = () => {
  router.push(`/supplier/performance/${supplierId.value}/evaluate`)
}

const handleAddPoints = async () => {
  const points = prompt('请输入增加积分数量')
  if (points) {
    const reason = prompt('请输入增加原因')
    if (reason) {
      try {
        const response = await fetch(`/api/v1/supplier-portal/points/${supplierId.value}/add?points=${points}&reason=${encodeURIComponent(reason)}`, {
          method: 'POST'
        })
        const data = await response.json()
        if (data.code === 200) {
          alert('积分增加成功')
          loadSupplierDetail()
          loadPointsRecords()
        }
      } catch {
        alert('操作失败')
      }
    }
  }
}

const handleConsumePoints = async () => {
  const points = prompt('请输入消费积分数量')
  if (points) {
    const reason = prompt('请输入消费原因')
    if (reason) {
      try {
        const response = await fetch(`/api/v1/supplier-portal/points/${supplierId.value}/consume?points=${points}&reason=${encodeURIComponent(reason)}`, {
          method: 'POST'
        })
        const data = await response.json()
        if (data.code === 200) {
          alert('积分消费成功')
          loadSupplierDetail()
          loadPointsRecords()
        }
      } catch {
        alert('操作失败')
      }
    }
  }
}

const handleBack = () => {
  router.push('/supplier')
}

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    'A': '#07c160',
    'B': '#1988fa',
    'C': '#ff976a',
    'D': '#f44',
    'E': '#969799'
  }
  return colors[level] || '#969799'
}

const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = {
    1: '正常合作',
    2: '暂停合作',
    3: '终止合作',
    4: '潜在供应商'
  }
  return labels[status] || '未知'
}

const getInquiryStatusLabel = (status: number) => {
  const labels: Record<number, string> = {
    0: '待报价',
    1: '已报价',
    2: '已接受',
    3: '已拒绝'
  }
  return labels[status] || '未知'
}

const formatScore = (score: number) => {
  return score ? score.toFixed(1) : '0.0'
}
</script>

<template>
  <div class="supplier-detail-page">
    <div class="page-header">
      <button class="back-btn" @click="handleBack">← 返回</button>
      <h1>供应商详情</h1>
      <div class="header-actions">
        <button class="edit-btn" @click="handleEdit">编辑</button>
        <button class="evaluate-btn" @click="handleEvaluate">绩效评估</button>
      </div>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div class="detail-content" v-if="supplier && !loading">
      <div class="supplier-summary">
        <div class="summary-header">
          <span class="supplier-name">{{ supplier.supplierName }}</span>
          <span class="supplier-code">{{ supplier.supplierCode }}</span>
          <span class="level-badge" :style="{ color: getLevelColor(supplier.supplierLevel) }">
            {{ supplier.supplierLevel }}级
          </span>
        </div>
        <div class="summary-stats">
          <div class="stat-item">
            <span class="label">综合评分</span>
            <span class="value">{{ formatScore(supplier.comprehensiveScore) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">总积分</span>
            <span class="value">{{ supplier.totalPoints }}</span>
          </div>
          <div class="stat-item">
            <span class="label">合作状态</span>
            <span class="value">{{ getStatusLabel(supplier.cooperationStatus) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">门户状态</span>
            <span class="value">{{ supplier.portalStatus === 1 ? '已激活' : '未激活' }}</span>
          </div>
        </div>
      </div>

      <div class="tabs">
        <button 
          :class="{ active: activeTab === 'basic' }"
          @click="activeTab = 'basic'"
        >基本信息</button>
        <button 
          :class="{ active: activeTab === 'performance' }"
          @click="activeTab = 'performance'"
        >绩效记录</button>
        <button 
          :class="{ active: activeTab === 'inquiry' }"
          @click="activeTab = 'inquiry'"
        >询价报价</button>
        <button 
          :class="{ active: activeTab === 'points' }"
          @click="activeTab = 'points'"
        >积分记录</button>
      </div>

      <div class="tab-content">
        <div v-if="activeTab === 'basic'" class="basic-info">
          <div class="info-section">
            <h3>基本信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">简称</span>
                <span class="value">{{ supplier.shortName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">供应商类型</span>
                <span class="value">{{ supplier.supplierType === 1 ? '生产型' : '贸易型' }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系人</span>
                <span class="value">{{ supplier.contactPerson }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系电话</span>
                <span class="value">{{ supplier.contactPhone }}</span>
              </div>
              <div class="info-item">
                <span class="label">邮箱</span>
                <span class="value">{{ supplier.email || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">地址</span>
                <span class="value">{{ supplier.province }} {{ supplier.city }} {{ supplier.address }}</span>
              </div>
            </div>
          </div>

          <div class="info-section">
            <h3>财务信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">开户银行</span>
                <span class="value">{{ supplier.bankName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">银行账号</span>
                <span class="value">{{ supplier.bankAccount || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">税号</span>
                <span class="value">{{ supplier.taxNumber || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="info-section">
            <h3>其他信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">创建时间</span>
                <span class="value">{{ supplier.createTime }}</span>
              </div>
              <div class="info-item">
                <span class="label">更新时间</span>
                <span class="value">{{ supplier.updateTime }}</span>
              </div>
              <div class="info-item">
                <span class="label">备注</span>
                <span class="value">{{ supplier.remark || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'performance'" class="performance-list">
          <table>
            <thead>
              <tr>
                <th>评估周期</th>
                <th>质量评分</th>
                <th>交付评分</th>
                <th>价格评分</th>
                <th>服务评分</th>
                <th>综合评分</th>
                <th>评估人</th>
                <th>评估时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="perf in performances" :key="perf.id">
                <td>{{ perf.period }}</td>
                <td>{{ formatScore(perf.qualityScore) }}</td>
                <td>{{ formatScore(perf.deliveryScore) }}</td>
                <td>{{ formatScore(perf.priceScore) }}</td>
                <td>{{ formatScore(perf.serviceScore) }}</td>
                <td>{{ formatScore(perf.comprehensiveScore) }}</td>
                <td>{{ perf.evaluator }}</td>
                <td>{{ perf.evaluateTime }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="performances.length === 0" class="empty-state">
            <p>暂无绩效记录</p>
          </div>
        </div>

        <div v-if="activeTab === 'inquiry'" class="inquiry-list">
          <table>
            <thead>
              <tr>
                <th>询价单号</th>
                <th>标题</th>
                <th>报价金额</th>
                <th>状态</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="inquiry in inquiries" :key="inquiry.id">
                <td>{{ inquiry.inquiryNo }}</td>
                <td>{{ inquiry.inquiryTitle }}</td>
                <td>{{ inquiry.quotationAmount || '-' }}</td>
                <td>{{ getInquiryStatusLabel(inquiry.inquiryStatus) }}</td>
                <td>{{ inquiry.createTime }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="inquiries.length === 0" class="empty-state">
            <p>暂无询价记录</p>
          </div>
        </div>

        <div v-if="activeTab === 'points'" class="points-list">
          <div class="points-actions">
            <button class="add-btn" @click="handleAddPoints">增加积分</button>
            <button class="consume-btn" @click="handleConsumePoints">消费积分</button>
          </div>
          <table>
            <thead>
              <tr>
                <th>记录时间</th>
                <th>积分变动</th>
                <th>变动类型</th>
                <th>原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in pointsRecords" :key="record.id">
                <td>{{ record.createTime }}</td>
                <td>{{ record.points }}</td>
                <td>{{ record.points > 0 ? '增加' : '消费' }}</td>
                <td>{{ record.reason }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="pointsRecords.length === 0" class="empty-state">
            <p>暂无积分记录</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.supplier-detail-page {
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

  .header-actions {
    display: flex;
    gap: 10px;

    .edit-btn, .evaluate-btn {
      padding: 8px 15px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }

    .edit-btn {
      background: #f7f8fa;
      color: #333;
      border: 1px solid #dcdfe6;
    }

    .evaluate-btn {
      background: #07c160;
      color: #fff;
    }
  }
}

.supplier-summary {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;

  .summary-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 15px;

    .supplier-name {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }

    .supplier-code {
      font-size: 14px;
      color: #969799;
    }

    .level-badge {
      font-weight: 600;
    }
  }

  .summary-stats {
    display: flex;
    gap: 20px;

    .stat-item {
      .label {
        font-size: 12px;
        color: #969799;
      }

      .value {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-left: 5px;
      }
    }
  }
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;

  button {
    padding: 10px 20px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;

    &.active {
      background: #1988fa;
      color: #fff;
      border-color: #1988fa;
    }
  }
}

.tab-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;

  .basic-info {
    .info-section {
      margin-bottom: 20px;

      h3 {
        font-size: 14px;
        font-weight: 600;
        color: #333;
        margin-bottom: 10px;
        padding-bottom: 10px;
        border-bottom: 1px solid #ebedf0;
      }

      .info-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 15px;

        .info-item {
          .label {
            font-size: 12px;
            color: #969799;
          }

          .value {
            font-size: 14px;
            color: #333;
            margin-top: 5px;
          }
        }
      }
    }
  }

  .performance-list, .inquiry-list, .points-list {
    table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: 10px 15px;
        text-align: left;
        border-bottom: 1px solid #ebedf0;
      }

      th {
        background: #f7f8fa;
        font-weight: 600;
      }
    }

    .points-actions {
      display: flex;
      gap: 10px;
      margin-bottom: 15px;

      .add-btn, .consume-btn {
        padding: 8px 15px;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }

      .add-btn {
        background: #07c160;
        color: #fff;
      }

      .consume-btn {
        background: #ff976a;
        color: #fff;
      }
    }
  }

  .empty-state {
    padding: 30px;
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

  p {
    margin-top: 10px;
    color: #969799;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>