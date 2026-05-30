<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
    empty-text="暂无订单数据"
    search-placeholder="搜索订单号 / 客户 / 供应商..."
    @clear-selection="handleClearSelection"
    @search-submit="handleSearchSubmit"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <!-- 类型筛选 Tab -->
    <template #breadcrumb>
      <div class="order-center-breadcrumb">
        <span class="order-center-breadcrumb-text">订单中心</span>
        <a-radio-group
          v-model:value="filterTab"
          size="small"
          button-style="solid"
          style="margin-left: 16px"
          @change="handleFilterTabChange"
        >
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="purchase">采购订单</a-radio-button>
          <a-radio-button value="sales">销售订单</a-radio-button>
        </a-radio-group>
      </div>
    </template>

    <template #actions>
      <a-space>
        <a-select
          v-model:value="filterStatus"
          placeholder="筛选状态"
          allow-clear
          style="width: 140px"
          @change="handleFilterStatusChange"
        >
          <a-select-option :value="0">草稿</a-select-option>
          <a-select-option :value="1">待审批</a-select-option>
          <a-select-option :value="2">已审批</a-select-option>
          <a-select-option :value="4">执行中</a-select-option>
          <a-select-option :value="5">已完成</a-select-option>
          <a-select-option :value="6">已取消</a-select-option>
        </a-select>
      </a-space>
    </template>

    <template #list-view>
      <!-- 统计卡片 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic title="订单总数" :value="pagination.total" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="采购订单"
              :value="dataSource.filter(o => o.orderType === 'purchase').length"
              value-style="color: #1890ff"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="销售订单"
              :value="dataSource.filter(o => o.orderType === 'sales').length"
              value-style="color: #52c41a"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="待审批"
              :value="dataSource.filter(o => o.orderStatus === 1).length"
              value-style="color: #faad14"
            />
          </a-card>
        </a-col>
      </a-row>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderType'">
            <a-tag :color="record.orderType === 'purchase' ? 'blue' : 'green'">
              {{ record.orderType === 'purchase' ? '采购' : '销售' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'orderStatus'">
            <a-tag :color="getStatusColor(record.orderStatus)">
              {{ getStatusText(record.orderStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            <span style="font-weight: 600">¥{{ record.totalAmount?.toFixed(2) || '0.00' }}</span>
          </template>
          <template v-else-if="column.key === 'partyName'">
            {{ record.customerName || record.supplierName || '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="handleView(record)">查看详情</a>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ModuleLayout } from '@ai-ready/components'
import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'
import { salesOrderApi } from '@/api/order'

const router = useRouter()

interface UnifiedOrder {
  id: number
  orderNo: string
  orderType: 'purchase' | 'sales'
  orderStatus: number
  customerName?: string
  supplierName?: string
  totalAmount: number
  createTime: string
}

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<UnifiedOrder[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const filterTab = ref('all')
const filterStatus = ref<number | undefined>(undefined)

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const breadcrumbItems = computed(() => [{ text: '订单中心' }])

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '类型', key: 'orderType', width: 80 },
  { title: '往来单位', key: 'partyName' },
  { title: '金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'orderStatus', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'blue', 3: 'red', 4: 'cyan', 5: 'green', 6: 'default' }
  return colors[status] || 'default'
}
const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '已拒绝', 4: '执行中', 5: '已完成', 6: '已取消' }
  return texts[status] || '未知'
}

const fetchData = async () => {
  loading.value = true; error.value = null
  try {
    const params: any = {
      current: pagination.current,
      size: pagination.pageSize,
      tenantId: 1
    }
    if (filterStatus.value !== undefined) {
      params.status = filterStatus.value
    }

    // 根据筛选类型获取：采购订单 + 销售订单
    const allOrders: UnifiedOrder[] = []

    if (filterTab.value === 'all' || filterTab.value === 'purchase') {
      try {
        const res = await purchaseOrderApi.page(params)
        const records = res.data?.records || res.records || []
        records.forEach((o: any) => allOrders.push({
          id: o.id, orderNo: o.orderNo, orderType: 'purchase',
          orderStatus: o.status, supplierName: o.supplierName,
          totalAmount: o.totalAmountWithTax || o.totalAmount || 0,
          createTime: o.createTime
        }))
      } catch { /* purchase API may not be available */ }
    }

    // Sales orders - use purchase API as temporary fallback
    if (filterTab.value === 'all' || filterTab.value === 'sales') {
      try {
        const res = await salesOrderApi.getPage({
          ...params,
          pageNum: params.current,
          pageSize: params.size
        } as any)
        const records = res.data?.records || res.records || []
        records.forEach((o: any) => allOrders.push({
          id: o.id, orderNo: o.orderNo, orderType: 'sales',
          orderStatus: o.status, customerName: o.customerName,
          totalAmount: o.finalAmount || o.totalAmount || 0,
          createTime: o.createTime
        }))
      } catch { /* sales API may not be available */ }
    }

    dataSource.value = allOrders
    pagination.total = allOrders.length
  } catch (err: any) {
    error.value = err?.message || '获取数据失败'
  } finally {
    loading.value = false
  }
}

const handleSearchSubmit = (value: string) => {
  (window as any).__orderSearchKeyword = value
  pagination.current = 1
  fetchData()
}
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handlePageChange = (page: number) => { pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; fetchData() }
const handleFilterTabChange = () => { pagination.current = 1; fetchData() }
const handleFilterStatusChange = () => { pagination.current = 1; fetchData() }
const handleView = (record: UnifiedOrder) => {
  if (record.orderType === 'purchase') {
    router.push(`/purchase/order/${record.id}`)
  } else {
    message.info('销售订单详情页开发中')
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.order-center-breadcrumb {
  display: flex;
  align-items: center;
}
.order-center-breadcrumb-text {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.stat-card {
  text-align: center;
}
.stat-card :deep(.ant-card-body) {
  padding: 16px;
}
</style>
