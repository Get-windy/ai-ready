<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface Order {
  id: number
  orderNo: string
  orderType: 'purchase' | 'sales'
  orderStatus: number
  customerName?: string
  supplierName?: string
  totalAmount: number
  createTime: string
  updateTime: string
  buyerName?: string
  salesPersonName?: string
}

const orders = ref<Order[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const orderTypeOptions = [
  { value: 'purchase', label: '采购订单' },
  { value: 'sales', label: '销售订单' }
]

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '待审批' },
  { value: 2, label: '已审批' },
  { value: 3, label: '已拒绝' },
  { value: 4, label: '执行中' },
  { value: 5, label: '已完成' },
  { value: 6, label: '已取消' }
]

onMounted(async () => {
  loadOrders()
})

const loadOrders = async () => {
  loading.value = true
  try {
    const purchaseResponse = await fetch(`/api/erp/purchase-orders/page?pageNum=${currentPage.value}&pageSize=${pageSize.value}&tenantId=1`)
    const purchaseData = await purchaseResponse.json()
    
    const purchaseOrders = (purchaseData.data?.records || []).map((o: any) => ({
      ...o,
      orderType: 'purchase',
      orderNo: o.orderNo || o.purchaseOrderNo,
      orderStatus: o.status,
      supplierName: o.supplierName,
      buyerName: o.buyerName
    }))

    orders.value = purchaseOrders
    total.value = purchaseData.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadOrders()
}

const handleDetail = (order: Order) => {
  if (order.orderType === 'purchase') {
    router.push(`/purchase/order/detail/${order.id}`)
  } else {
    router.push(`/sales/order/detail/${order.id}`)
  }
}

const getStatusLabel = (status: number) => {
  return statusOptions.find(s => s.value === status)?.label || '未知'
}

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: '#969799',
    1: '#1988fa',
    2: '#07c160',
    3: '#f44',
    4: '#ff976a',
    5: '#07c160',
    6: '#969799'
  }
  return colors[status] || '#969799'
}

const formatAmount = (amount: number) => {
  return amount ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const getTypeLabel = (type: string) => {
  return type === 'purchase' ? '采购' : '销售'
}

const getTypeColor = (type: string) => {
  return type === 'purchase' ? '#1988fa' : '#07c160'
}
</script>

<template>
  <div class="order-center-page">
    <div class="page-header">
      <h1>订单中心</h1>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-value">{{ orders.filter(o => o.orderType === 'purchase').length }}</span>
          <span class="stat-label">采购订单</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ orders.filter(o => o.orderType === 'sales').length }}</span>
          <span class="stat-label">销售订单</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ orders.filter(o => o.orderStatus === 1).length }}</span>
          <span class="stat-label">待审批</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ orders.filter(o => o.orderStatus === 4).length }}</span>
          <span class="stat-label">执行中</span>
        </div>
      </div>
    </div>

    <div class="search-bar">
      <input 
        v-model="searchKeyword"
        type="text"
        placeholder="搜索订单号/供应商/客户"
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <select v-model="filterType" class="filter-select" @change="handleSearch">
        <option value="">全部类型</option>
        <option v-for="type in orderTypeOptions" :key="type.value" :value="type.value">
          {{ type.label }}
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

    <div class="order-tabs">
      <button :class="{ active: filterType === '' }" @click="filterType = ''; handleSearch()">全部订单</button>
      <button :class="{ active: filterType === 'purchase' }" @click="filterType = 'purchase'; handleSearch()">采购订单</button>
      <button :class="{ active: filterType === 'sales' }" @click="filterType = 'sales'; handleSearch()">销售订单</button>
      <button :class="{ active: filterType === 'pending' }" @click="filterType = 'pending'; handleSearch()">待审批</button>
    </div>

    <div class="order-table" v-if="!loading">
      <table>
        <thead>
          <tr>
            <th>订单号</th>
            <th>订单类型</th>
            <th>供应商/客户</th>
            <th>总金额</th>
            <th>状态</th>
            <th>负责人</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>
              <span class="type-badge" :style="{ color: getTypeColor(order.orderType) }">
                {{ getTypeLabel(order.orderType) }}
              </span>
            </td>
            <td>{{ order.supplierName || order.customerName || '-' }}</td>
            <td>{{ formatAmount(order.totalAmount) }}</td>
            <td>
              <span class="status-badge" :style="{ color: getStatusColor(order.orderStatus) }">
                {{ getStatusLabel(order.orderStatus) }}
              </span>
            </td>
            <td>{{ order.buyerName || order.salesPersonName || '-' }}</td>
            <td>{{ order.createTime }}</td>
            <td class="actions">
              <button class="action-btn detail" @click="handleDetail(order)">详情</button>
              <button 
                v-if="order.orderStatus === 1"
                class="action-btn approve"
                @click="router.push(`/approval/order/${order.id}`)"
              >审批</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="orders.length === 0" class="empty-state">
        <p>暂无订单数据</p>
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
.order-center-page {
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

  .header-stats {
    display: flex;
    gap: 15px;

    .stat-item {
      padding: 10px 15px;
      background: #fff;
      border-radius: 8px;
      text-align: center;

      .stat-value {
        font-size: 18px;
        font-weight: 600;
        color: #1988fa;
      }

      .stat-label {
        font-size: 12px;
        color: #969799;
      }
    }
  }
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;

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

.order-tabs {
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

.order-table {
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

    .type-badge, .status-badge {
      font-weight: 600;
    }

    .actions {
      display: flex;
      gap: 5px;

      .action-btn {
        padding: 4px 8px;
        border: none;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;

        &.detail { background: #1988fa; color: #fff; }
        &.approve { background: #07c160; color: #fff; }
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