<template>
  <PageContainer full-height>
    <template #header>
      <div class="payment-page-header">
        <div class="payment-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>支付管理</a-breadcrumb-item>
            <a-breadcrumb-item>退款管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="payment-page-header-title">退款管理</h2>
        </div>
        <div class="payment-page-header-right">
          <a-space :size="8">
            <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
            <a-button size="small" :loading="loading" @click="() => fetchData()"><template #icon><ReloadOutlined /></template>刷新</a-button>
          </a-space>
        </div>
      </div>
    </template>

    <ErrorBoundary @error="handleError" @reset="fetchData">
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"><ClockCircleOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">待审批</div>
                <div class="stat-value">{{ stats.pending }}</div>
                <div class="stat-desc">等待审批</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">已批准</div>
                <div class="stat-value">{{ stats.approved }}</div>
                <div class="stat-desc">已批准退款</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-red">
              <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);"><CloseCircleOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">已拒绝</div>
                <div class="stat-value">{{ stats.rejected }}</div>
                <div class="stat-desc">已拒绝</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><DollarOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">退款金额</div>
                <div class="stat-value">¥{{ formatAmount(stats.totalAmount) }}</div>
                <div class="stat-desc">累计退款</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <div class="table-card">
        <BillTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :selectable="true"
          :min-empty-rows="10"
          row-key="id"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @selection-change="handleSelectionChange"
          @cell-dblclick="handleView"
        >
          <template #batch-actions>
            <a-button v-if="selectedRowKeys.length > 0" size="small" type="primary" ghost @click="handleBatchApprove(true)">批量批准 ({{ selectedRowKeys.length }})</a-button>
            <a-button v-if="selectedRowKeys.length > 0" size="small" danger ghost @click="handleBatchApprove(false)">批量拒绝 ({{ selectedRowKeys.length }})</a-button>
          </template>
          <template #empty>
            <div class="table-empty">
              <InboxOutlined v-if="!hasError" class="table-empty-icon" />
              <WarningOutlined v-else class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">{{ hasError ? '数据加载失败' : '暂无退款记录' }}</p>
            </div>
          </template>
          <template #amountCell="{ record }">
            <span style="color: #f5222d; font-weight: bold">¥{{ formatAmount(record.amount) }}</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="REFUND_STATUS_MAP[record.status]?.color">{{ REFUND_STATUS_MAP[record.status]?.text || record.status }}</a-tag>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-button size="small" type="link" @click="handleView(record)">详情</a-button>
              <a-button v-if="record.status === 0" size="small" type="link" @click="handleApprove(record, true)">批准</a-button>
              <a-button v-if="record.status === 0" size="small" type="link" danger @click="handleApprove(record, false)">拒绝</a-button>
            </a-space>
          </template>
        </BillTableList>
      </div>
    </ErrorBoundary>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="退款详情" placement="right" width="560px" :footer="null">
      <a-descriptions v-if="detailData" :column="2" bordered size="small">
        <a-descriptions-item label="支付请求ID">{{ detailData.paymentId }}</a-descriptions-item>
        <a-descriptions-item label="退款金额"><span style="color:#f5222d;font-weight:bold">¥{{ formatAmount(detailData.amount) }}</span></a-descriptions-item>
        <a-descriptions-item label="退款原因">{{ detailData.reason || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ detailData.applicantName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="REFUND_STATUS_MAP[detailData.status]?.color">{{ REFUND_STATUS_MAP[detailData.status]?.text }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="渠道退款号">{{ detailData.channelRefundNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
        <a-descriptions-item label="处理时间">{{ detailData.processTime || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, DollarOutlined, CheckCircleOutlined, CloseCircleOutlined,
  ClockCircleOutlined, WarningOutlined, InboxOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { refundApi, REFUND_STATUS_MAP } from '@/api/payment'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const detailVisible = ref(false)
const detailData = ref<any>(null)

const stats = reactive({ pending: 0, approved: 0, rejected: 0, totalAmount: 0 })

const vxeColumns = computed(() => [
  { field: 'paymentId', title: '支付请求ID', width: 100 },
  { field: 'amount', title: '退款金额', width: 110, align: 'right', slotName: 'amountCell' },
  { field: 'reason', title: '退款原因', width: 200, ellipsis: true },
  { field: 'applicantName', title: '申请人', width: 100 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'channelRefundNo', title: '渠道退款号', width: 150 },
  { field: 'createTime', title: '创建时间', width: 150 },
  { field: 'action', title: '操作', width: 150, fixed: 'right', type: 'action' }
])

function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const result = await refundApi.pageRequest({ pageNum: pagination.current, pageSize: pagination.pageSize })
    const data = (result as any).data || result
    tableData.value = data.records || data.content || []
    pagination.total = data.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    updateStats()
  } catch (err) {
    if (!silent) { hasError.value = true; message.error('获取退款列表失败') }
    tableData.value = []
  } finally { if (!silent) loading.value = false }
}

function updateStats() {
  const list = tableData.value
  stats.pending = list.filter(r => r.status === 0).length
  stats.approved = list.filter(r => r.status === 1 || r.status === 2).length
  stats.rejected = list.filter(r => r.status === 3).length
  stats.totalAmount = list.reduce((s, r) => s + (r.amount || 0), 0)
}

function handleView(record: any) { detailData.value = record; detailVisible.value = true }

async function handleApprove(record: any, approved: boolean) {
  Modal.confirm({
    title: approved ? '确认批准' : '确认拒绝',
    content: approved ? `确定批准退款请求吗？` : `确定拒绝退款请求吗？`,
    okType: approved ? undefined : 'danger',
    onOk: async () => {
      try {
        await refundApi.approve(record.id, approved)
        message.success(approved ? '已批准' : '已拒绝')
        fetchData()
      } catch (e: any) { message.error(e?.message || '操作失败') }
    }
  })
}

function handleBatchApprove(approved: boolean) {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择退款请求'); return }
  Modal.confirm({
    title: approved ? '批量批准' : '批量拒绝',
    content: `确定${approved ? '批准' : '拒绝'} ${selectedRowKeys.value.length} 个退款请求？`,
    okType: approved ? undefined : 'danger',
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          await refundApi.approve(id, approved)
        }
        message.success(`批量${approved ? '批准' : '拒绝'}成功`)
        selectedRowKeys.value = []
        fetchData()
      } catch (e: any) { message.error(e?.message || '批量操作失败') }
    }
  })
}

function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSelectionChange(rows: any[], ids: any[]) { selectedRowKeys.value = ids }
function handleError(err: any) { console.warn('[退款管理]', err) }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); fetchData() }
}

onMounted(() => { fetchData(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.payment-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.payment-page-header-left { display: flex; align-items: center; gap: 12px; }
.payment-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.payment-page-header-right { display: flex; align-items: center; gap: 12px; }
.stats-cards { flex-shrink: 0; margin-bottom: 16px; }
.update-time { font-size: 12px; color: #999; }
.stat-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.stat-content { flex: 1; }
.stat-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; }
.stat-desc { font-size: 12px; color: #999; margin-top: 4px; }
.table-card { background: #fff; border-radius: 8px; }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }
</style>
