<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="商城订单"
      full-height
    >
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >最后更新: {{ lastUpdateTime }}</span>
          </span>
          <a-tooltip title="手动刷新">
            <a-button
              size="small"
              @click="fetchData"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
            </a-button>
          </a-tooltip>
        </a-space>
      </template>

      <div class="stat-cards">
        <div class="stat-card stat-pending-pay">
          <div class="stat-card-body">
            <div class="stat-card-value">
              {{ stats.pendingPay }}
            </div>
            <div class="stat-card-label">
              待付款
            </div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending-ship">
          <div class="stat-card-body">
            <div class="stat-card-value">
              {{ stats.pendingShip }}
            </div>
            <div class="stat-card-label">
              待发货
            </div>
          </div>
          <ShoppingCartOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-shipped">
          <div class="stat-card-body">
            <div class="stat-card-value">
              {{ stats.shipped }}
            </div>
            <div class="stat-card-label">
              已发货
            </div>
          </div>
          <CarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">
              ¥{{ formatAmount(stats.totalAmount) }}
            </div>
            <div class="stat-card-label">
              交易总额
            </div>
          </div>
          <BarChartOutlined class="stat-card-icon" />
        </div>
      </div>

      <div class="search-area">
        <a-form
          layout="inline"
          :model="searchParams"
        >
          <a-form-item label="订单编号">
            <a-input
              v-model:value="searchParams.orderNo"
              placeholder="请输入订单编号"
              allow-clear
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="订单状态">
            <a-select
              v-model:value="searchParams.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 150px"
            >
              <a-select-option
                v-for="[key, val] in Object.entries(ORDER_STATUS_MAP)"
                :key="key"
                :value="Number(key)"
              >
                <a-tag
                  :color="val.color"
                  style="margin-right: 4px"
                >
                  {{ val.text }}
                </a-tag>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>
                搜索
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <div class="table-area">
        <BillTableList
          :columns="billColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="true"
          :selectable="false"
          row-key="id"
          @page-change="handlePageChange"
        >
          <template #orderNoCell="{ record }">
            <a-button
              type="link"
              size="small"
              @click="handleView(record)"
            >
              {{ record.orderNo }}
            </a-button>
          </template>
          <template #totalAmountCell="{ record }">
            <span class="currency-value">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template #actionCell="{ record }">
            <a-space :size="4">
              <a-button
                type="link"
                size="small"
                @click="handleView(record)"
              >
                详情
              </a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleApprove(record)"
              >
                审核
              </a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                @click="handlePay(record)"
              >
                支付
              </a-button>
            </a-space>
          </template>
        </BillTableList>
      </div>

      <a-modal
        v-model:open="detailVisible"
        title="订单详情"
        width="800px"
        :footer="null"
        destroy-on-close
      >
        <a-spin :spinning="detailLoading">
          <a-descriptions
            v-if="detailData"
            bordered
            :column="2"
          >
            <a-descriptions-item label="订单编号">
              {{ detailData.orderNo }}
            </a-descriptions-item>
            <a-descriptions-item label="客户名称">
              {{ detailData.customerName }}
            </a-descriptions-item>
            <a-descriptions-item label="订单金额">
              <span class="currency-value">¥{{ formatAmount(detailData.totalAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="订单状态">
              <a-tag :color="getStatusColor(detailData.status)">
                {{ getStatusText(detailData.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="收货人">
              {{ detailData.consignee || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="联系电话">
              {{ detailData.consigneePhone || '-' }}
            </a-descriptions-item>
            <a-descriptions-item
              label="收货地址"
              :span="2"
            >
              {{ detailData.shippingAddress || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">
              {{ detailData.createTime }}
            </a-descriptions-item>
            <a-descriptions-item label="备注">
              {{ detailData.remark || '-' }}
            </a-descriptions-item>
          </a-descriptions>
        </a-spin>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined,
  SearchOutlined,
  ClearOutlined,
  DollarOutlined,
  ShoppingCartOutlined,
  CarOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import request from '@/utils/request'
import type { PageResult } from '@/api/common'

// ── 状态定义 ──
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const ORDER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '待付款', color: 'blue' },
  2: { text: '待发货', color: 'processing' },
  3: { text: '已发货', color: 'purple' },
  4: { text: '已完成', color: 'green' },
  5: { text: '已取消', color: 'default' }
}

function getStatusText(status: number) { return ORDER_STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number) { return ORDER_STATUS_MAP[status]?.color || 'default' }

// ── 统计 ──
const stats = reactive({ pendingPay: 0, pendingShip: 0, shipped: 0, totalAmount: 0 })

// ── 搜索 ──
const searchParams = reactive({ orderNo: '', status: undefined as number | undefined })

// ── 分页 ──
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ── 详情 ──
const detailVisible = ref(false)
const detailData = ref<any>(null)
const detailLoading = ref(false)

// ── 表格列 ──
const billColumns = [
  { title: '订单编号', field: 'orderNo', key: 'orderNo', width: 160, type: 'slot', slotName: 'orderNoCell' },
  { title: '客户名称', field: 'customerName', key: 'customerName', width: 180 },
  { title: '订单金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell' },
  { title: '订单状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' }
]

// ── 数据加载 ──
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await request.get('/api/v1/mall/orders', {
      params: { page: pagination.current, size: pagination.pageSize, ...searchParams }
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || data || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[商城订单] 获取列表失败', error)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res: any = await request.get('/erp/sale/order/stats')
    if (res) {
      const data = res.data || res
      Object.assign(stats, {
        pendingPay: data.pendingPayCount || data.pendingProcessCount || 0,
        pendingShip: data.pendingShipCount || 0,
        shipped: data.shippedCount || 0,
        totalAmount: data.totalAmount || data.monthOrderAmount || 0
      })
    }
  } catch (error: any) {
    console.warn('[商城订单] 获取统计失败', error)
  }
}

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res: any = await request.get(`/api/v1/mall/orders/${id}`)
    detailData.value = res.data || res || null
  } catch (error: any) {
    console.warn('[商城订单] 获取详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

// ── 事件处理 ──
const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => {
  searchParams.orderNo = ''
  searchParams.status = undefined
  pagination.current = 1
  fetchData()
}
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}
const handleView = (record: any) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleApprove = async (record: any) => {
  try {
    await request.put(`/api/v1/mall/orders/${record.id}/approve`)
    message.success('审核通过')
    fetchData()
  } catch { message.error('操作失败') }
}

const handlePay = async (record: any) => {
  try {
    await request.post(`/api/v1/mall/orders/${record.id}/pay`)
    message.success('支付成功')
    fetchData()
  } catch { message.error('支付失败') }
}

const handleError = (error: Error) => {
  hasError.value = true
  console.warn('[商城订单] 页面错误', error)
}

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => { fetchData(); fetchStats() })
</script>

<style scoped>
.stat-cards { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card { flex: 1; display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-radius: 8px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.stat-pending-pay { border-left: 4px solid #1890ff; }
.stat-pending-ship { border-left: 4px solid #fa8c16; }
.stat-shipped { border-left: 4px solid #722ed1; }
.stat-total { border-left: 4px solid #52c41a; }
.stat-card-value { font-size: 24px; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; color: #333; }
.stat-card-label { font-size: 13px; color: #666; margin-top: 4px; }
.stat-card-icon { font-size: 32px; color: rgba(0,0,0,.15); }
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; font-variant-numeric: tabular-nums; }
.data-status { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; }
.update-time { color: #999; }
</style>
