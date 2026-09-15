<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="售后处理" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
            <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          </span>
          <a-tooltip title="手动刷新"><a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button></a-tooltip>
        </a-space>
      </template>

      <div class="stat-cards">
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.pending }}</div>
            <div class="stat-card-label">待处理</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-approved">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.approved }}</div>
            <div class="stat-card-label">已同意</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-rejected">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.rejected }}</div>
            <div class="stat-card-label">已拒绝</div>
          </div>
          <CloseCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.total }}</div>
            <div class="stat-card-label">总数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="售后单号">
            <a-input v-model:value="searchParams.returnNo" placeholder="请输入售后单号" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchParams.status" placeholder="全部状态" allow-clear style="width: 150px">
              <a-select-option v-for="[key, val] in Object.entries(STATUS_MAP)" :key="key" :value="Number(key)">
                <a-tag :color="val.color" style="margin-right: 4px">{{ val.text }}</a-tag>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="类型">
            <a-select v-model:value="searchParams.type" placeholder="全部类型" allow-clear style="width: 150px">
              <a-select-option :value="1">退货退款</a-select-option>
              <a-select-option :value="2">仅退款</a-select-option>
              <a-select-option :value="3">换货</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>搜索</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
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
          :show-add="false"
          :selectable="true"
          row-key="id"
          @page-change="handlePageChange"
          @selection-change="handleSelectionChange"
        >
          <template #batch-actions>
            <a-button v-if="selectedRowKeys.length > 0" size="small" type="primary" ghost @click="handleBatchApprove">批量同意 ({{ selectedRowKeys.length }})</a-button>
            <a-button v-if="selectedRowKeys.length > 0" size="small" danger ghost @click="handleBatchReject">批量拒绝 ({{ selectedRowKeys.length }})</a-button>
          </template>
          <template #returnNoCell="{ record }">
            <a-button type="link" size="small" @click="handleView(record)">{{ record.returnNo }}</a-button>
          </template>
          <template #amountCell="{ record }">
            <span class="currency-value">¥{{ formatAmount(record.refundAmount) }}</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template #actionCell="{ record }">
            <a-space :size="4">
              <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleApprove(record)">同意</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleReject(record)">拒绝</a-button>
            </a-space>
          </template>
        </BillTableList>
      </div>

      <!-- 详情抽屉 -->
      <a-drawer v-model:open="detailVisible" title="售后详情" placement="right" width="640px" :footer="null">
        <a-spin :spinning="detailLoading">
          <template v-if="detailData">
            <a-descriptions bordered :column="2" size="small">
              <a-descriptions-item label="售后单号">{{ detailData.returnNo }}</a-descriptions-item>
              <a-descriptions-item label="关联订单">{{ detailData.orderNo }}</a-descriptions-item>
              <a-descriptions-item label="售后类型">{{ typeText(detailData.type) }}</a-descriptions-item>
              <a-descriptions-item label="状态"><a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag></a-descriptions-item>
              <a-descriptions-item label="退款金额"><span class="currency-value">¥{{ formatAmount(detailData.refundAmount) }}</span></a-descriptions-item>
              <a-descriptions-item label="申请人">{{ detailData.applicantName }}</a-descriptions-item>
              <a-descriptions-item label="原因" :span="2">{{ detailData.reason || '-' }}</a-descriptions-item>
              <a-descriptions-item label="说明" :span="2">{{ detailData.description || '-' }}</a-descriptions-item>
              <a-descriptions-item label="申请时间">{{ detailData.createTime }}</a-descriptions-item>
              <a-descriptions-item label="处理时间">{{ detailData.processTime || '-' }}</a-descriptions-item>
            </a-descriptions>

            <a-divider>售后商品</a-divider>
            <a-table :columns="itemColumns" :data-source="detailData.items || []" :pagination="false" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'subtotal'"><span class="currency-value">¥{{ formatAmount(record.subtotal) }}</span></template>
              </template>
            </a-table>

            <!-- 审批流程 -->
            <a-divider>处理记录</a-divider>
            <a-timeline>
              <a-timeline-item v-for="(r, idx) in detailData.processRecords || []" :key="idx" :color="r.action === 'approved' ? 'green' : 'red'">
                <div class="timeline-content">
                  <div class="timeline-title">{{ r.action === 'approved' ? '同意' : '拒绝' }}</div>
                  <div class="timeline-desc">{{ r.comment || '-' }}</div>
                  <div class="timeline-time">{{ r.createTime }} - {{ r.operatorName }}</div>
                </div>
              </a-timeline-item>
            </a-timeline>

            <div class="detail-footer">
              <a-space>
                <a-button v-if="detailData.status === 0" type="primary" @click="handleApprove(detailData)">同意售后</a-button>
                <a-button v-if="detailData.status === 0" danger @click="handleReject(detailData)">拒绝售后</a-button>
              </a-space>
            </div>
          </template>
        </a-spin>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, ClearOutlined,
  ClockCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { mallReturnApi } from '@/api/erp/mall'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const selectedRowKeys = ref<any[]>([])

const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '已同意', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
  3: { text: '已完成', color: 'blue' },
  4: { text: '已取消', color: 'default' }
}

function getStatusText(status: number) { return STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number) { return STATUS_MAP[status]?.color || 'default' }
function typeText(type: number): string {
  const m: Record<number, string> = { 1: '退货退款', 2: '仅退款', 3: '换货' }
  return m[type] || `类型${type}`
}

const stats = reactive({ pending: 0, approved: 0, rejected: 0, total: 0 })
const searchParams = reactive({ returnNo: '', status: undefined as number | undefined, type: undefined as number | undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const detailVisible = ref(false)
const detailData = ref<any>(null)
const detailLoading = ref(false)

const billColumns = [
  { title: '售后单号', field: 'returnNo', key: 'returnNo', width: 160, type: 'slot', slotName: 'returnNoCell' },
  { title: '关联订单', field: 'orderNo', key: 'orderNo', width: 160 },
  { title: '售后类型', field: 'type', key: 'type', width: 100, formatter: ({ cellValue }: any) => typeText(cellValue) },
  { title: '退款金额', field: 'refundAmount', key: 'refundAmount', width: 120, align: 'right', type: 'slot', slotName: 'amountCell' },
  { title: '状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '申请人', field: 'applicantName', key: 'applicantName', width: 100 },
  { title: '申请时间', field: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' }
]

const itemColumns: any[] = [
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 200 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '退款金额', dataIndex: 'subtotal', key: 'subtotal', width: 100, align: 'right' }
]

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.returnNo) params.returnNo = searchParams.returnNo
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.type !== undefined) params.type = searchParams.type
    const res: any = await mallReturnApi.page(params)
    tableData.value = res.records || res.data?.records || []
    pagination.total = res.total || res.data?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (error: any) { hasError.value = true; console.warn('[售后处理] 获取列表失败', error) }
  finally { loading.value = false }
}

const fetchStats = async () => {
  try {
    const res: any = await mallReturnApi.stats()
    const data = res.data || res
    Object.assign(stats, {
      pending: data.pendingCount || 0, approved: data.approvedCount || 0,
      rejected: data.rejectedCount || 0, total: data.totalCount || 0
    })
  } catch (error: any) { console.warn('[售后处理] 获取统计失败', error) }
}

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res: any = await mallReturnApi.getById(id)
    detailData.value = res.data || res || null
  } catch (error: any) { console.warn('[售后处理] 获取详情失败', error) }
  finally { detailLoading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.returnNo = ''; searchParams.status = undefined; searchParams.type = undefined; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleSelectionChange = (_rows: any[], ids: any[]) => { selectedRowKeys.value = ids }
const handleView = (record: any) => { detailVisible.value = true; fetchDetail(record.id) }

const handleApprove = async (record: any) => {
  Modal.confirm({
    title: '同意售后', content: `确定同意售后单 "${record.returnNo}" 吗？`,
    onOk: async () => {
      try { await mallReturnApi.approve(record.id); message.success('已同意'); fetchData(); fetchStats(); if (detailData.value?.id === record.id) fetchDetail(record.id) }
      catch { message.error('操作失败') }
    }
  })
}

const handleReject = async (record: any) => {
  Modal.confirm({
    title: '拒绝售后', content: `确定拒绝售后单 "${record.returnNo}" 吗？`, okType: 'danger',
    onOk: async () => {
      try { await mallReturnApi.reject(record.id); message.success('已拒绝'); fetchData(); fetchStats(); if (detailData.value?.id === record.id) fetchDetail(record.id) }
      catch { message.error('操作失败') }
    }
  })
}

function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择售后单'); return }
  Modal.confirm({
    title: '批量同意', content: `确定同意 ${selectedRowKeys.value.length} 个售后单？`,
    onOk: async () => {
      try { await mallReturnApi.batchApprove(selectedRowKeys.value); message.success('批量同意成功'); selectedRowKeys.value = []; fetchData(); fetchStats() }
      catch { message.error('批量操作失败') }
    }
  })
}

function handleBatchReject() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择售后单'); return }
  Modal.confirm({
    title: '批量拒绝', content: `确定拒绝 ${selectedRowKeys.value.length} 个售后单？`, okType: 'danger',
    onOk: async () => {
      try { await mallReturnApi.batchReject(selectedRowKeys.value); message.success('批量拒绝成功'); selectedRowKeys.value = []; fetchData(); fetchStats() }
      catch { message.error('批量操作失败') }
    }
  })
}

function handleError(error: Error) { hasError.value = true; console.warn('[售后处理] 页面错误', error) }

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => { fetchData(); fetchStats() })
</script>

<style scoped>
.stat-cards { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card { flex: 1; display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-radius: 8px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.stat-pending { border-left: 4px solid #fa8c16; }
.stat-approved { border-left: 4px solid #52c41a; }
.stat-rejected { border-left: 4px solid #f5222d; }
.stat-total { border-left: 4px solid #1890ff; }
.stat-card-value { font-size: 24px; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; color: #333; }
.stat-card-label { font-size: 13px; color: #666; margin-top: 4px; }
.stat-card-icon { font-size: 32px; color: rgba(0,0,0,.15); }
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; font-variant-numeric: tabular-nums; }
.data-status { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; }
.update-time { color: #999; }
.detail-footer { display: flex; justify-content: flex-end; margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.timeline-content .timeline-title { font-size: 14px; font-weight: 500; color: #303133; }
.timeline-content .timeline-desc { font-size: 12px; color: #606266; margin-top: 4px; }
.timeline-content .timeline-time { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
