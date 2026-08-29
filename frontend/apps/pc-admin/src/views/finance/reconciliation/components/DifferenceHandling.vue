<template>
  <div class="difference-handling">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #722ed1">
        <div class="summary-card-title">差异待处理</div>
        <div class="summary-card-value">{{ summaryData.differenceCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #1890ff">
        <div class="summary-card-title">差异记录总数</div>
        <div class="summary-card-value">{{ summaryData.totalCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14">
        <div class="summary-card-title">本页差异合计</div>
        <div class="summary-card-value">¥{{ summaryData.pageDifference.toFixed(2) }}</div>
      </div>
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
      <template #typeCell="{ record }">
        <a-tag>{{ getTypeText(record.reconciliationType) }}</a-tag>
      </template>
      <template #differenceCell="{ record }">
        <span style="color: #f5222d">¥{{ fmt(record.difference) }}</span>
      </template>
      <template #reasonCell="{ record }">
        <span :title="record.differenceReason">{{ record.differenceReason || '—' }}</span>
      </template>
      <template #actionCell="{ record }">
        <a-button
          type="link"
          size="small"
          @click="handleResolve(record)"
        >
          处理
        </a-button>
      </template>
    </BillTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { message, Modal } from 'ant-design-vue'
import { reconciliationApi } from '@/api/finance'

interface DiffRecord {
  id: number
  reconciliationNo: string
  reconciliationType: string
  targetName: string
  difference: number | null
  differenceReason: string
}

const loading = ref(false)
const dataSource = ref<DiffRecord[]>([])

const summaryData = reactive({
  differenceCount: 0,
  totalCount: 0,
  pageDifference: 0
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { field: 'reconciliationNo', title: '对账编号', width: 160 },
  { field: 'reconciliationType', title: '类型', width: 100, slotName: 'typeCell' },
  { field: 'targetName', title: '对方名称', minWidth: 140 },
  { field: 'difference', title: '差异金额', width: 120, align: 'right', slotName: 'differenceCell' },
  { field: 'differenceReason', title: '差异原因', minWidth: 160, slotName: 'reasonCell' },
  { field: 'action', title: '操作', width: 80, fixed: 'right', slotName: 'actionCell' }
]

const getTypeText = (type: string) => {
  const texts: Record<string, string> = { BANK: '银行', CUSTOMER: '客户', SUPPLIER: '供应商' }
  return texts[type] || type || '未知'
}

const fmt = (v: number | null | undefined) => {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}

async function loadStats() {
  try {
    const res: any = await reconciliationApi.getStats()
    if (res) {
      summaryData.differenceCount = Number(res.differenceCount || 0)
    }
  } catch (err) {
    console.warn('[差异处理] 获取统计失败', err)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await reconciliationApi.page({
      status: 2,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const records = (res?.records || []) as DiffRecord[]
    dataSource.value = records
    pagination.total = Number(res?.total || 0)
    summaryData.totalCount = pagination.total
    summaryData.pageDifference = records.reduce((sum, r) => sum + Number(r.difference || 0), 0)
  } catch (err) {
    console.warn('[差异处理] 加载失败', err)
    message.error('加载差异记录失败')
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadData()
}

function handleResolve(record: DiffRecord) {
  Modal.confirm({
    title: '处理差异',
    content: `确认处理差异记录 ${record.reconciliationNo}（${record.targetName}）？处理后将标记为已对账。`,
    okText: '确认处理',
    cancelText: '取消',
    onOk: async () => {
      try {
        await reconciliationApi.handleDifference(record.id, record.differenceReason || '人工处理')
        message.success('差异已处理')
        loadData()
        loadStats()
      } catch (err: any) {
        message.error(err?.message || '处理失败')
      }
    }
  })
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
.difference-handling {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.difference-handling > :deep(.vxe-table-list-container) {
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
</style>
