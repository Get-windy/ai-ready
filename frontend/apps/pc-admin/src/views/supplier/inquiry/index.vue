<template>
  <PageContainer full-height>
    <template #header>
      <div class="inquiry-header">
        <div class="inquiry-header__left">
          <a-button type="text" class="inquiry-header__back" v-permission="'supplier:inquiry:back'" @click="handleBack">
            <template #icon><LeftOutlined /></template>
          </a-button>
<span class="shortcut-hints">
          <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
        </span>
          <div class="inquiry-header__titles">
            <span class="inquiry-header__breadcrumb">供应商 / 询价报价</span>
            <h2 class="inquiry-header__title">供应商询价报价</h2>
          </div>
        </div>
        <div class="inquiry-header__right">
          <a-space :size="12">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">
                数据更新: {{ lastUpdateTime }}
              </span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadInquiries)()">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button type="primary" v-permission="'supplier:inquiry:createinquiry'" @click="handleCreateInquiry" :disabled="!supplierId">
              <template #icon><PlusOutlined /></template>
              发起询价
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <ErrorBoundary>
    <div class="page-content">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ inquiries.length }}</div>
          <div class="stat-card-label">询价总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pendingCount }}</div>
          <div class="stat-card-label">待报价</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-quoted">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ quotedCount }}</div>
          <div class="stat-card-label">已报价</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-accepted">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ acceptedCount }}</div>
          <div class="stat-card-label">已接受</div>
        </div>
        <CheckOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card :bordered="false" v-if="supplier" style="margin-bottom: 16px">
      <a-descriptions size="small" :column="4">
        <a-descriptions-item label="供应商名称">{{ supplier.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ supplier.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="询价总数">{{ inquiries.length }}</a-descriptions-item>
        <a-descriptions-item label="待报价">{{ pendingCount }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :bordered="false" class="table-card">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="inquiries"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` } as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        :min-empty-rows="12"
        @cell-dblclick="handleView"
      >
        <template #inquiryStatusCell="{ record }">
          <a-tag :color="getStatusColor(record.inquiryStatus)">{{ getStatusLabel(record.inquiryStatus) }}</a-tag>
        </template>
        <template #quotationStatusCell="{ record }">
          <a-tag v-if="record.quotationStatus === 1" color="success">已报价</a-tag>
          <a-tag v-else color="default">待报价</a-tag>
        </template>
        <template #quotationAmountCell="{ record }">
          {{ record.quotationAmount ? `¥${record.quotationAmount.toLocaleString()}` : '-' }}
        </template>
        <template #action="{ record }">
          <a-space>
            <a-button
              v-if="record.quotationStatus === 1"
              size="small"
 v-permission="'supplier:inquiry:viewquotation'" @click="handleViewQuotation(record)"
            >查看报价</a-button>
            <a-button
              v-if="record.quotationStatus === 1 && record.inquiryStatus === 1"
              size="small"
              type="primary"
 v-permission="'supplier:inquiry:acceptquotation'" @click="handleAcceptQuotation(record)"
            >接受</a-button>
            <a-button
              v-if="record.quotationStatus === 1 && record.inquiryStatus === 1"
              size="small"
              danger
 v-permission="'supplier:inquiry:rejectquotation'" @click="handleRejectQuotation(record)"
            >拒绝</a-button>
          </a-space>
        </template>
        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="loadInquiries" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else-if="!supplierId">
	              <InboxOutlined class="table-empty-icon" />
	              <p class="table-empty-text">请先从供应商列表中选择供应商</p>
	              <a-button type="primary" size="small" @click="handleBack" class="table-empty-action">前往供应商列表</a-button>
	            </template>
	            <template v-else>
              <InboxOutlined class="table-empty-icon" />
              <p class="table-empty-text">暂无询价报价记录</p>
            </template>
          </div>
        </template>
      </VxeTableList>
    </a-card>

    <a-modal
      v-model:open="showCreateModal"
      title="发起询价"
      @ok="submitInquiry"
      :confirm-loading="submitLoading"
    >
      <a-form ref="formRef" :model="createForm" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="询价标题" name="inquiryTitle">
          <a-input v-model:value="createForm.inquiryTitle" size="small" placeholder="请输入询价标题" />
        </a-form-item>
        <a-form-item label="截止日期" name="deadline">
          <a-date-picker v-model:value="createForm.deadline" size="small" style="width: 100%" placeholder="选择截止日期" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="createForm.remark" size="small" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
      </a-modal>
    </div>
    </ErrorBoundary>
</PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, CheckOutlined, ReloadOutlined, SyncOutlined, LeftOutlined, WarningOutlined, InboxOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRouter, useRoute } from 'vue-router'
import { supplierApi } from '@/api/supplier'
import { requiredRule } from '@/utils/formRules'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import request from '@/utils/request'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const route = useRoute()
const router = useRouter()
const supplierId = (route.params.id as string) || (route.query.id as string) || ''

interface InquiryRecord {
  id: number
  inquiryNo: string
  inquiryTitle: string
  inquiryStatus: number
  createTime: string
  deadline: string
  quotationAmount: number
  quotationStatus: number
  quotationTime: string
  remark: string
}

const supplier = ref<{ supplierCode: string; supplierName: string } | null>(null)
const inquiries = ref<InquiryRecord[]>([])
const loading = ref(false)
const submitLoading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const showCreateModal = ref(false)
const formRef = ref<FormInstance>()

const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const createForm = ref({
  inquiryTitle: '',
  deadline: undefined as dayjs.Dayjs | undefined,
  remark: ''
})

const formRules = {
  inquiryTitle: [requiredRule('询价标题')],
  deadline: [{ required: true, message: '请选择截止日期' }]
}

const pendingCount = computed(() => inquiries.value.filter(i => i.inquiryStatus === 0).length)
const quotedCount = computed(() => inquiries.value.filter(i => i.quotationStatus === 1).length)
const acceptedCount = computed(() => inquiries.value.filter(i => i.inquiryStatus === 2).length)



const vxeColumns = [
  { field: 'inquiryNo', title: '询价单号', width: 160 },
  { field: 'inquiryTitle', title: '询价标题', width: 180 },
  { field: 'inquiryStatus', title: '状态', width: 100, slotName: 'inquiryStatusCell' },
  { field: 'quotationAmount', title: '报价金额', width: 130, slotName: 'quotationAmountCell' },
  { field: 'quotationStatus', title: '报价状态', width: 100, slotName: 'quotationStatusCell' },
  { field: 'deadline', title: '截止日期', width: 120 },
  { field: 'createTime', title: '创建时间', width: 120 },
  { field: 'action', title: '操作', width: 220, fixed: 'right', type: 'action' },
]

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待报价', 1: '已报价', 2: '已接受', 3: '已拒绝', 4: '已过期' }
  return map[status] || '未知'
}

const getStatusColor = (status: number) => {
  const map: Record<number, string> = { 0: 'processing', 1: 'success', 2: 'success', 3: 'error', 4: 'default' }
  return map[status] || 'default'
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', loadInquiries)(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreateInquiry(); return }
}

function handleParentCreate() { handleCreateInquiry() }

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  await Promise.allSettled([loadSupplier(), loadInquiries()])
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadInquiries()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('supplier:create', handleParentCreate)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('supplier:create', handleParentCreate)
})

defineExpose({ handleQuery: loadInquiries })

async function loadSupplier() {
  if (!supplierId) return
  try {
    const res = await supplierApi.getById(Number(supplierId))
    supplier.value = res as any
  } catch (err) {
    console.warn('[供应商] 加载供应商信息失败', err)
  }
}

async function loadInquiries() {
  if (!supplierId) return
  loading.value = true
  hasError.value = false
  try {
    const res = await supplierApi.getInquiries(Number(supplierId))
    inquiries.value = (res as any)?.data || (res as any) || []
  } catch (err: any) {
    hasError.value = true
    console.warn('[供应商] 获取询价记录失败', err)
    message.error(err?.message || '获取询价记录失败')
    inquiries.value = []
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleCreateInquiry = () => {
  createForm.value = { inquiryTitle: '', deadline: undefined, remark: '' }
  showCreateModal.value = true
}

const handleView = (record: any) => {
  const row = record?.row ?? record
  handleViewQuotation(row)
}

const submitInquiry = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    await request.post('/v1/supplier-portal/inquiries', {
      supplierId: Number(supplierId || 0),
      inquiryTitle: createForm.value.inquiryTitle,
      deadline: createForm.value.deadline?.format('YYYY-MM-DD'),
      remark: createForm.value.remark
    })
    console.warn('[供应商] 操作成功: 询价单创建成功')
    message.success('询价单创建成功')
    showCreateModal.value = false
    await loadInquiries()
  } catch (err: any) {
    if (err?.errorFields) { console.warn('[供应商] 表单验证失败', err); return }
    console.warn('[供应商] 创建询价失败', err)
    message.error(err?.message || '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const handleViewQuotation = (inquiry: InquiryRecord) => {
  Modal.info({
    title: '报价详情',
    content: `报价金额: ¥${inquiry.quotationAmount?.toLocaleString() || '0'}`
  })
}

const handleAcceptQuotation = async (inquiry: InquiryRecord) => {
  Modal.confirm({
    title: '确认接受报价',
    content: `确定接受报价 ¥${inquiry.quotationAmount?.toLocaleString() || '0'}？`,
    onOk: async () => {
      try {
        await request.post(`/v1/supplier-portal/quotations/${inquiry.id}/accept`)
        console.warn('[供应商] 操作成功: 报价已接受')
        message.success('报价已接受')
        await loadInquiries()
      } catch (err: any) {
        console.warn('[供应商] 接受报价失败', err)
        message.error(err?.message || '操作失败')
      }
    }
  })
}

const handleRejectQuotation = async (inquiry: InquiryRecord) => {
  let reason = ''
  Modal.confirm({
    title: '拒绝报价',
    content: '请输入拒绝原因',
    onOk: async () => {
      if (!reason.trim()) {
        message.warning('请输入拒绝原因')
        throw new Error('请输入拒绝原因')
      }
      try {
        await request.post(`/v1/supplier-portal/quotations/${inquiry.id}/reject`, null, { params: { reason } })
        console.warn('[供应商] 操作成功: 报价已拒绝')
        message.success('报价已拒绝')
        await loadInquiries()
      } catch (err: any) {
        console.warn('[供应商] 拒绝报价失败', err)
        message.error(err?.message || '操作失败')
      }
    }
  })
}

const handleBack = () => {
  if (!supplierId) { router.push('/supplier'); return }
  router.push(`/supplier/detail/${supplierId}`)
}

</script>

<style scoped>
/* ── 让 VxeTableList 填满剩余空间 ──────────────────────── */
.page-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* ── 空状态 ── */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.inquiry-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.inquiry-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inquiry-header__back {
  color: #303133;
  font-size: 16px;
  padding: 0 4px;
}

.inquiry-header__titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.inquiry-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.inquiry-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.inquiry-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

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
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-quoted { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-accepted { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.table-card {
  flex: 1;
  border-radius: 8px;
}

/* 空行占位符 */





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
