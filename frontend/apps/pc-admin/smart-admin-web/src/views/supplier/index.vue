<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface Supplier {
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
  bankName: string
  bankAccount: string
  taxNumber: string
  comprehensiveScore: number
  totalPoints: number
  portalStatus: number
  createTime: string
  updateTime: string
}

const suppliers = ref<Supplier[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterLevel = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const levelOptions = ['A', 'B', 'C', 'D', 'E']
const statusOptions = [
  { value: 1, label: '正常合作' },
  { value: 2, label: '暂停合作' },
  { value: 3, label: '终止合作' },
  { value: 4, label: '潜在供应商' }
]

const portalStatusOptions = [
  { value: 0, label: '未激活' },
  { value: 1, label: '已激活' },
  { value: 2, label: '已禁用' }
]

onMounted(async () => {
  loadSuppliers()
})

const loadSuppliers = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/supplier/page', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
        supplierLevel: filterLevel.value,
        cooperationStatus: filterStatus.value
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      suppliers.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadSuppliers()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadSuppliers()
}

const handleDetail = (supplier: Supplier) => {
  router.push(`/supplier/detail/${supplier.id}`)
}

const handlePerformance = (supplier: Supplier) => {
  router.push(`/supplier/performance/${supplier.id}`)
}

const handlePortal = (supplier: Supplier) => {
  router.push(`/supplier/portal/${supplier.id}`)
}

const handleActivatePortal = async (supplier: Supplier) => {
  if (confirm(`确定激活供应商 ${supplier.supplierName} 的门户账户？`)) {
    try {
      const response = await fetch(`/api/supplier/${supplier.id}/activate-portal?portalAccountId=${supplier.supplierCode}`, {
        method: 'POST'
      })
      const data = await response.json()
      if (data.code === 200) {
        alert('门户激活成功')
        loadSuppliers()
      }
    } catch {
      alert('激活失败')
    }
  }
}

const handleDisablePortal = async (supplier: Supplier) => {
  const reason = prompt('请输入禁用原因')
  if (reason) {
    try {
      const response = await fetch(`/api/supplier/${supplier.id}/disable-portal?reason=${encodeURIComponent(reason)}`, {
        method: 'POST'
      })
      const data = await response.json()
      if (data.code === 200) {
        alert('门户已禁用')
        loadSuppliers()
      }
    } catch {
      alert('禁用失败')
    }
  }
}

const handleCreate = () => {
  router.push('/supplier/create')
}

const handleEdit = (supplier: Supplier) => {
  router.push(`/supplier/edit/${supplier.id}`)
}

const handleDelete = async (supplier: Supplier) => {
  if (confirm(`确定删除供应商 ${supplier.supplierName}？`)) {
    try {
      const response = await fetch(`/api/supplier/${supplier.id}`, {
        method: 'DELETE'
      })
      const data = await response.json()
      if (data.code === 200) {
        alert('删除成功')
        loadSuppliers()
      }
    } catch {
      alert('删除失败')
    }
  }
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
  return statusOptions.find(s => s.value === status)?.label || '未知'
}

const getPortalStatusLabel = (status: number) => {
  return portalStatusOptions.find(s => s.value === status)?.label || '未知'
}

const formatScore = (score: number) => {
  return score ? score.toFixed(1) : '0.0'
}
</script>

<template>
  <div class="supplier-list-page">
    <div class="page-header">
      <h1>供应商管理</h1>
      <button class="create-btn" @click="handleCreate">
        + 新增供应商
      </button>
    </div>

    <div class="search-bar">
      <input 
        v-model="searchKeyword"
        type="text"
        placeholder="搜索供应商编码/名称"
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <select v-model="filterLevel" class="filter-select" @change="handleSearch">
        <option value="">全部等级</option>
        <option v-for="level in levelOptions" :key="level" :value="level">
          {{ level }}级
        </option>
      </select>
      <select v-model="filterStatus" class="filter-select" @change="handleSearch">
        <option value="">全部状态</option>
        <option v-for="status in statusOptions" :key="status.value" :value="status.value">
          {{ status.label }}
        </option>
      </select>
      <button class="search-btn" @click="handleSearch">搜索</button>
    </div>

    <div class="supplier-stats">
      <div class="stat-card">
        <span class="stat-value">{{ total }}</span>
        <span class="stat-label">供应商总数</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ suppliers.filter(s => s.supplierLevel === 'A').length }}</span>
        <span class="stat-label">A级供应商</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ suppliers.filter(s => s.cooperationStatus === 1).length }}</span>
        <span class="stat-label">正常合作</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ suppliers.filter(s => s.portalStatus === 1).length }}</span>
        <span class="stat-label">门户已激活</span>
      </div>
    </div>

    <div class="supplier-table" v-if="!loading">
      <table>
        <thead>
          <tr>
            <th>供应商编码</th>
            <th>供应商名称</th>
            <th>等级</th>
            <th>综合评分</th>
            <th>积分</th>
            <th>合作状态</th>
            <th>门户状态</th>
            <th>联系人</th>
            <th>联系电话</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="supplier in suppliers" :key="supplier.id">
            <td>{{ supplier.supplierCode }}</td>
            <td>{{ supplier.supplierName }}</td>
            <td>
              <span class="level-badge" :style="{ color: getLevelColor(supplier.supplierLevel) }">
                {{ supplier.supplierLevel }}
              </span>
            </td>
            <td>{{ formatScore(supplier.comprehensiveScore) }}</td>
            <td>{{ supplier.totalPoints }}</td>
            <td>{{ getStatusLabel(supplier.cooperationStatus) }}</td>
            <td>{{ getPortalStatusLabel(supplier.portalStatus) }}</td>
            <td>{{ supplier.contactPerson }}</td>
            <td>{{ supplier.contactPhone }}</td>
            <td class="actions">
              <button class="action-btn detail" @click="handleDetail(supplier)">详情</button>
              <button class="action-btn performance" @click="handlePerformance(supplier)">绩效</button>
              <button class="action-btn portal" @click="handlePortal(supplier)">门户</button>
              <button class="action-btn edit" @click="handleEdit(supplier)">编辑</button>
              <button 
                v-if="supplier.portalStatus !== 1"
                class="action-btn activate"
                @click="handleActivatePortal(supplier)"
              >激活门户</button>
              <button 
                v-if="supplier.portalStatus === 1"
                class="action-btn disable"
                @click="handleDisablePortal(supplier)"
              >禁用门户</button>
              <button class="action-btn delete" @click="handleDelete(supplier)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="suppliers.length === 0" class="empty-state">
        <p>暂无供应商数据</p>
      </div>
    </div>

    <div class="pagination" v-if="total > pageSize">
      <button 
        :disabled="currentPage === 1"
        @click="handlePageChange(currentPage - 1)"
      >上一页</button>
      <span>第 {{ currentPage }} 页 / 共 {{ Math.ceil(total / pageSize) }} 页</span>
      <button 
        :disabled="currentPage >= Math.ceil(total / pageSize)"
        @click="handlePageChange(currentPage + 1)"
      >下一页</button>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.supplier-list-page {
  padding: 20px;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h1 {
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }

  .create-btn {
    padding: 10px 20px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;

  .search-input {
    flex: 1;
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
  }

  .filter-select {
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #fff;
  }

  .search-btn {
    padding: 10px 20px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
}

.supplier-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;

  .stat-card {
    flex: 1;
    padding: 15px;
    background: #fff;
    border-radius: 8px;
    text-align: center;

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #1988fa;
    }

    .stat-label {
      font-size: 12px;
      color: #969799;
    }
  }
}

.supplier-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;

  table {
    width: 100%;
    border-collapse: collapse;

    th, td {
      padding: 12px 15px;
      text-align: left;
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

    .level-badge {
      font-weight: 600;
    }

    .actions {
      display: flex;
      gap: 5px;
      flex-wrap: wrap;

      .action-btn {
        padding: 4px 8px;
        border: none;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;

        &.detail { background: #1988fa; color: #fff; }
        &.performance { background: #07c160; color: #fff; }
        &.portal { background: #7232dd; color: #fff; }
        &.edit { background: #f7f8fa; color: #333; border: 1px solid #dcdfe6; }
        &.activate { background: #ff976a; color: #fff; }
        &.disable { background: #f44; color: #fff; }
        &.delete { background: #f7f8fa; color: #f44; border: 1px solid #f44; }
      }
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;

  button {
    padding: 8px 15px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  span {
    color: #666;
  }
}

.empty-state {
  padding: 40px;
  text-align: center;
  color: #969799;
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