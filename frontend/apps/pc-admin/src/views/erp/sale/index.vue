<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="订单处理中心" full-height>
      <!-- 状态栏 -->
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
            <span v-if="lastUpdateTime" class="update-time">
              最后更新: {{ lastUpdateTime }}
            </span>
          </span>
          <a-tooltip title="手动刷新">
            <a-button size="small" @click="fetchData">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </template>

      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-pending-process">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.pendingProcessCount }}</div>
            <div class="stat-card-label">待处理订单</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending-approval">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.pendingApprovalCount }}</div>
            <div class="stat-card-label">待审核订单</div>
          </div>
          <FileSearchOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-today">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.todayOrderCount }}</div>
            <div class="stat-card-label">今日新增</div>
          </div>
          <CalendarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-month">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.monthOrderCount }}</div>
            <div class="stat-card-label">本月新增</div>
          </div>
          <BarChartOutlined class="stat-card-icon" />
        </div>
      </div>

      <!-- 搜索筛选区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="searchParams" class="search-form">
          <a-row :gutter="16" align="middle">
            <a-col :span="6">
              <a-form-item label="订单编号">
                <a-input v-model:value="searchParams.orderNo" placeholder="请输入订单编号" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="客户名称">
                <a-select
                  v-model:value="searchParams.customerId"
                  placeholder="请选择客户"
                  allow-clear
                  show-search
                  :filter-option="filterOption"
                >
                  <a-select-option v-for="c in customerOptions" :key="c.id" :value="c.id">
                    {{ c.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="订单状态">
                <a-select v-model:value="searchParams.status" placeholder="请选择状态" allow-clear>
                  <a-select-option v-for="[key, val] in Object.entries(ORDER_STATUS_MAP)" :key="key" :value="Number(key)">
                    <a-tag :color="val.color" style="margin-right: 4px">{{ val.text }}</a-tag>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="时间范围">
                <a-range-picker
                  v-model:value="dateRange"
                  :placeholder="['开始日期', '结束日期']"
                  @change="handleDateChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="2">
              <a-form-item>
                <a-space>
                  <a-button type="primary" @click="handleSearch">
                    <template #icon><SearchOutlined /></template>
                    搜索
                  </a-button>
                  <a-button @click="handleReset">
                    <template #icon><ClearOutlined /></template>
                    重置
                  </a-button>
                </a-space>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </div>

      <!-- 使用系统表格组件 -->
      <div class="table-area">
        <BillTableList
          :columns="billColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="false"
          row-key="id"
          @page-change="handleBillPageChange"
        >
          <!-- 订单编号 -->
          <template #orderNoCell="{ record }">
            <a-button type="link" size="small" @click="handleView(record)">
              {{ record.orderNo }}
            </a-button>
          </template>
          <!-- 订单金额 -->
          <template #totalAmountCell="{ record }">
            <span class="currency-value">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <!-- 订单状态 -->
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <!-- 操作列 -->
          <template #actionCell="{ record }">
            <a-space :size="4">
              <a-tooltip title="查看详情">
                <a-button type="link" size="small" @click="handleView(record)">
                  详情
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === OrderStatus.DRAFT || record.status === OrderStatus.PENDING" title="编辑订单">
                <a-button type="link" size="small" @click="handleEdit(record)">
                  编辑
                </a-button>
              </a-tooltip>
              <a-tooltip v-else title="已审批订单不可编辑">
                <a-button type="link" size="small" disabled>编辑</a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === OrderStatus.DRAFT" title="删除订单">
                <a-button type="link" size="small" danger @click="handleDelete(record)">
                  删除
                </a-button>
              </a-tooltip>
              <a-tooltip v-else title="非草稿状态不可删除">
                <a-button type="link" size="small" disabled danger>删除</a-button>
              </a-tooltip>
            </a-space>
          </template>
        </BillTableList>
      </div>

      <!-- 详情弹窗 -->
      <a-modal
        v-model:open="detailVisible"
        title="订单详情"
        width="800px"
        :footer="null"
        destroy-on-close
      >
        <a-spin :spinning="detailLoading">
          <a-descriptions bordered :column="2" v-if="detailData">
            <a-descriptions-item label="订单编号">{{ detailData.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="客户名称">{{ detailData.customerName }}</a-descriptions-item>
            <a-descriptions-item label="订单金额">
              <span class="currency-value">¥{{ formatAmount(detailData.totalAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="订单状态">
              <a-tag :color="getStatusColor(detailData.status)">
                {{ getStatusText(detailData.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="备注">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 订单明细 -->
          <a-divider>订单明细</a-divider>
          <a-table
            v-if="detailData?.items && detailData.items.length > 0"
            :columns="detailColumns"
            :data-source="detailData.items"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'unitPrice'">
                <span class="currency-value">¥{{ formatAmount(record.unitPrice) }}</span>
              </template>
              <template v-if="column.dataIndex === 'amount'">
                <span class="currency-value">¥{{ formatAmount(record.amount) }}</span>
              </template>
            </template>
          </a-table>
          <a-empty v-else description="暂无订单明细" />
        </a-spin>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  ReloadOutlined,
  SearchOutlined,
  ClearOutlined,
  ClockCircleOutlined,
  FileSearchOutlined,
  CalendarOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue'
import { salesOrderApi, OrderStatus, type SalesOrder } from '@/api/order'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

// ── 状态定义 ────────────────────────────────────────

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<SalesOrder[]>([])
const lastUpdateTime = ref<string>('')

// 统计数据
const stats = reactive({
  pendingProcessCount: 0,
  pendingApprovalCount: 0,
  todayOrderCount: 0,
  monthOrderCount: 0
})

// 搜索参数
const searchParams = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined,
  startDate: '',
  endDate: ''
})
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 选择
const selectedRowKeys = ref<number[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref<(SalesOrder & { items?: any[] }) | null>(null)
const detailLoading = ref(false)

// 客户选项
const customerOptions = ref<{ id: number; name: string }[]>([])

// ── 状态映射（对标截图）────────────────────────────────

const ORDER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'orange' },
  2: { text: '待处理', color: 'blue' },
  3: { text: '部分发货', color: 'processing' },
  4: { text: '已完成', color: 'green' },
  5: { text: '已取消', color: 'default' }
}

function getStatusText(status: number): string {
  return ORDER_STATUS_MAP[status]?.text || '未知'
}

function getStatusColor(status: number): string {
  return ORDER_STATUS_MAP[status]?.color || 'default'
}

// ── 表格列配置（BillTableList格式）────────────────────────────────

const billColumns = [
  { title: '订单编号', field: 'orderNo', key: 'orderNo', width: 160, type: 'slot', slotName: 'orderNoCell' },
  { title: '客户名称', field: 'customerName', key: 'customerName', width: 180 },
  { title: '订单金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell' },
  { title: '订单状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' }
]

// BillTableList 分页配置
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right' as const },
  { title: '金额', dataIndex: 'amount', width: 120, align: 'right' as const }
]

// ── 数据加载 ────────────────────────────────────────

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await salesOrderApi.getPage({
      tenantId: userStore.tenantId,
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    // 处理两种响应格式：Page对象格式或wrapper格式
    if (res) {
      tableData.value = res.records || res.data?.records || []
      pagination.total = res.total || res.data?.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售订单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await salesOrderApi.getStatistics({ tenantId: userStore.tenantId })
    if (res) {
      // 处理两种响应格式
      const data = res.data || res
      Object.assign(stats, data)
    }
  } catch (error: any) {
    console.warn('[销售订单] 获取统计失败', error)
  }
}

const loadCustomerOptions = async () => {
  try {
    const { customerApi } = await import('@/api/customer')
    const res = await customerApi.getOptions()
    const data = res.data || res || []
    customerOptions.value = data.map((c: any) => ({ id: c.id, name: c.partnerName || c.name }))
  } catch (e) {
    console.warn('[销售订单] 加载客户选项失败', e)
    customerOptions.value = []
  }
}

// ── 事件处理 ────────────────────────────────────────

const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchParams, {
    orderNo: '',
    customerId: undefined,
    status: undefined,
    startDate: '',
    endDate: ''
  })
  dateRange.value = null
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

// BillTableList 分页处理
const handleBillPageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const filterOption = (input: string, option: any) => {
  if (!option) return false
  const text = option.label || option.name || option.children?.[0]?.children || ''
  if (!text) return false
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

// ── 单条操作 ────────────────────────────────────────

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await salesOrderApi.getById(id) as any
    detailData.value = res.data || res || null
  } catch (error: any) {
    console.warn('[销售订单] 获取详情失败', error)
    message.error(error?.response?.data?.message || '获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: SalesOrder) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleEdit = (record: SalesOrder) => {
  router.push(`/erp/sale/form/${record.id}`)
}

const handleDelete = (record: SalesOrder) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除订单 ${record.orderNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await salesOrderApi.delete(record.id)
        message.success('删除成功')
        fetchData()
        fetchStats()
      } catch (error: any) {
        console.warn('[销售订单] 删除失败', error)
        message.error(error?.response?.data?.message || '删除失败')
      }
    }
  })
}

// ── 辅助函数 ────────────────────────────────────────

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleError = (error: Error) => {
  hasError.value = true
  console.warn('[销售订单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ── 初始化 ────────────────────────────────────────

onMounted(() => {
  fetchData()
  fetchStats()
  loadCustomerOptions()
})

onUnmounted(() => {
  // 清理
})
</script>

<style scoped>
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
  padding: 16px 20px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stat-pending-process {
  border-left: 4px solid #1890ff;
}

.stat-pending-approval {
  border-left: 4px solid #fa8c16;
}

.stat-today {
  border-left: 4px solid #52c41a;
}

.stat-month {
  border-left: 4px solid #722ed1;
}

.stat-card-value {
  font-size: 24px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 32px;
  color: rgba(0, 0, 0, 0.15);
}

/* 搜索区域 */
.search-area {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.search-form {
  width: 100%;
}

.search-form .ant-form-item {
  margin-bottom: 0;
  margin-right: 0;
}

.search-form .ant-form-item-label {
  padding-right: 8px;
}

.search-form .ant-form-item-label > label {
  font-size: 14px;
  color: #333;
}

.search-form .ant-input,
.search-form .ant-select,
.search-form .ant-picker {
  width: 100%;
  height: 28px;
}

.search-form .ant-select-selector,
.search-form .ant-picker-input {
  height: 28px !important;
}

.search-form .ant-select-selection-item,
.search-form .ant-picker-input > input {
  line-height: 26px !important;
}

.search-form .ant-btn {
  height: 28px;
  padding: 0 12px;
  font-size: 13px;
}

/* 表格区域 */
.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.currency-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 140px;
  }
}
</style>