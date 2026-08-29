<template>
  <div class="supplier-reconciliation">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #faad14">
        <div class="summary-card-title">待供应商对账</div>
        <div class="summary-card-value">{{ summaryData.pendingCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #1890ff">
        <div class="summary-card-title">对账记录总数</div>
        <div class="summary-card-value">{{ summaryData.totalCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1">
        <div class="summary-card-title">差异待处理</div>
        <div class="summary-card-value">{{ summaryData.differenceCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a">
        <div class="summary-card-title">本页差异合计</div>
        <div class="summary-card-value">¥{{ summaryData.pageDifference.toFixed(2) }}</div>
      </div>
    </div>

    <div class="filter-area">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="供应商名称">
          <a-input
            v-model:value="queryParams.targetName"
            placeholder="请输入供应商名称"
            allow-clear
            style="width: 200px"
            size="small"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="queryParams.status"
            placeholder="全部状态"
            allow-clear
            style="width: 140px"
            size="small"
          >
            <a-select-option :value="0">待对账</a-select-option>
            <a-select-option :value="1">已对账</a-select-option>
            <a-select-option :value="2">有差异</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <BillTableList
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :show-toolbar="false"
      :selectable="false"
      :show-add="false"
      :show-search="false"
      :show-export="false"
      :show-batch-delete="false"
      @page-change="handlePageChange"
    >
      <template #periodCell="{ record }">
        <span>{{ record.startDate }} ~ {{ record.endDate }}</span>
      </template>
      <template #systemBalanceCell="{ record }">
        <span>¥{{ fmt(record.systemBalance) }}</span>
      </template>
      <template #actualBalanceCell="{ record }">
        <span>¥{{ fmt(record.actualBalance) }}</span>
      </template>
      <template #differenceCell="{ record }">
        <span :style="{ color: Number(record.difference) !== 0 ? '#f5222d' : '#52c41a' }">
          ¥{{ fmt(record.difference) }}
        </span>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
      </template>
      <template #actionCell="{ record }">
        <a-button
          v-if="record.status === 0"
          type="link"
          size="small"
          @click="handleReconcile(record)"
        >
          对账
        </a-button>
        <span v-else class="no-action">—</span>
      </template>
    </BillTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { message } from 'ant-design-vue'
import { reconciliationApi } from '@/api/finance'

interface RecordItem {
  id: number
  reconciliationNo: string
  targetName: string
  startDate: string
  endDate: string
  systemBalance: number | null
  actualBalance: number | null
  difference: number | null
  status: number
  reconciliationDate: string
}

const loading = ref(false)
const dataSource = ref<RecordItem[]>([])

const summaryData = reactive({
  pendingCount: 0,
  totalCount: 0,
  differenceCount: 0,
  pageDifference: 0
})

const queryParams = reactive({
  targetName: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { field: 'reconciliationNo', title: '对账编号', width: 160 },
  { field: 'targetName', title: '供应商名称', minWidth: 140 },
  { field: 'period', title: '对账期间', width: 200, slotName: 'periodCell' },
  { field: 'systemBalance', title: '系统应付', width: 120, align: 'right', slotName: 'systemBalanceCell' },
  { field: 'actualBalance', title: '供应商金额', width: 120, align: 'right', slotName: 'actualBalanceCell' },
  { field: 'difference', title: '差异', width: 110, align: 'right', slotName: 'differenceCell' },
  { field: 'status', title: '状态', width: 90, slotName: 'statusCell' },
  { field: 'reconciliationDate', title: '对账日期', width: 110 },
  { field: 'action', title: '操作', width: 80, fixed: 'right', slotName: 'actionCell' }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'error' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '待对账', 1: '已对账', 2: '有差异' }
  return texts[status] || '未知'
}

const fmt = (v: number | null | undefined) => {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}

async function loadStats() {
  try {
    const res: any = await reconciliationApi.getStats()
    if (res) {
      summaryData.pendingCount = Number(res.supplierPending || 0)
      summaryData.differenceCount = Number(res.differenceCount || 0)
    }
  } catch (err) {
    console.warn('[供应商对账] 获取统计失败', err)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await reconciliationApi.page({
      reconciliationType: 'SUPPLIER',
      targetName: queryParams.targetName || undefined,
      status: queryParams.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const records = (res?.records || []) as RecordItem[]
    dataSource.value = records
    pagination.total = Number(res?.total || 0)
    summaryData.totalCount = pagination.total
    summaryData.pageDifference = records.reduce((sum, r) => sum + Number(r.difference || 0), 0)
  } catch (err) {
    console.warn('[供应商对账] 加载失败', err)
    message.error('加载供应商对账记录失败')
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadData()
}

async function handleReconcile(record: RecordItem) {
  try {
    await reconciliationApi.reconcile(record.id)
    message.success(`对账成功：${record.reconciliationNo}`)
    loadData()
    loadStats()
  } catch (err: any) {
    message.error(err?.message || '对账失败')
  }
}

function handleParentCreate() {
  loadData()
}

onMounted(() => {
  loadStats()
  loadData()
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', loadData)
})

onUnmounted(() => {
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', loadData)
})

defineExpose({})
</script>

<style scoped>
.supplier-reconciliation {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.supplier-reconciliation > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

.summary-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.summary-card {
  flex: 1;
  min-width: 180px;
  padding: 16px 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--card-color), color-mix(in srgb, var(--card-color) 70%, white));
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.summary-card-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.summary-card-value {
  font-size: 24px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.filter-area {
  margin-bottom: 16px;
}

.no-action {
  color: #bbb;
}
</style>
