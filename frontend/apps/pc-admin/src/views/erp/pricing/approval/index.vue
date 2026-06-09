<template>
  <PageContainer full-height>
    <template #header>
      <div class="approval-header">
        <div class="approval-header__left">
          <span class="approval-header__breadcrumb">ERP / 定价管理 / 审批</span>
          <h2 class="approval-header__title">价格审批</h2>
        </div>
        <div class="approval-header__right">
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
            <a-button size="small" :loading="refreshLoading" @click="loadData">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">总申请数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审批</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已通过</div>
            <div class="summary-value">{{ statistics.approvedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <CloseCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已拒绝</div>
            <div class="summary-value">{{ statistics.rejectedCount }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <template #extra>
        <a-button type="primary" @click="handleApply">
          <template #icon><PlusOutlined /></template>
          申请价格变更
        </a-button>
      </template>

      <a-tabs v-model:activeKey="activeTab" style="flex: 1; overflow: hidden;">
        <a-tab-pane key="pending" tab="待审批">
          <VxeTableList
            ref="pendingTableRef"
            :columns="pendingVxeColumns"
            :data-source="pendingList"
            :loading="loading"
            :pagination="pagination"
            show-toolbar
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="false"
            @refresh="loadData"
            @page-change="handlePageChange"
          >
            <template #priceChange="{ record }">
              <div class="price-change">
                <span class="old-price">原价: ¥{{ record.oldPrice }}</span>
                <span class="new-price">新价: ¥{{ record.newPrice }}</span>
                <span class="change" :class="record.priceChangeType">
                  {{ record.priceChangeType === 'increase' ? '+' : '-' }}¥{{ record.priceChange }}
                </span>
              </div>
            </template>
            <template #statusCell="{ record }">
              <StatusTag :status="record.status" :map="PRICE_APPROVAL_STATUS" />
            </template>
            <template #action="{ record }">
              <a-space :size="4">
                <a-button size="small" type="primary" @click="handleApprove(record)">通过</a-button>
                <a-button size="small" danger @click="handleReject(record)">拒绝</a-button>
                <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
              </a-space>
            </template>
          </VxeTableList>
        </a-tab-pane>
        <a-tab-pane key="approved" tab="已通过">
          <VxeTableList
            :columns="processedVxeColumns"
            :data-source="approvedList"
            :loading="loading"
            :show-toolbar="false"
            :selectable="false"
            :pagination="false"
          >
            <template #priceChange="{ record }">
              <div class="price-change">
                <span class="old-price">原价: ¥{{ record.oldPrice }}</span>
                <span class="new-price">新价: ¥{{ record.newPrice }}</span>
                <span class="change" :class="record.priceChangeType">
                  {{ record.priceChangeType === 'increase' ? '+' : '-' }}¥{{ record.priceChange }}
                </span>
              </div>
            </template>
            <template #statusCell="{ record }">
              <StatusTag :status="record.status" :map="PRICE_APPROVAL_STATUS" />
            </template>
            <template #approverCell="{ record }">
              {{ record.approverName }} / {{ formatDate(record.approveTime) }}
            </template>
          </VxeTableList>
        </a-tab-pane>
        <a-tab-pane key="rejected" tab="已拒绝">
          <VxeTableList
            :columns="processedVxeColumns"
            :data-source="rejectedList"
            :loading="loading"
            :show-toolbar="false"
            :selectable="false"
            :pagination="false"
          >
            <template #priceChange="{ record }">
              <div class="price-change">
                <span class="old-price">原价: ¥{{ record.oldPrice }}</span>
                <span class="new-price">新价: ¥{{ record.newPrice }}</span>
                <span class="change" :class="record.priceChangeType">
                  {{ record.priceChangeType === 'increase' ? '+' : '-' }}¥{{ record.priceChange }}
                </span>
              </div>
            </template>
            <template #statusCell="{ record }">
              <StatusTag :status="record.status" :map="PRICE_APPROVAL_STATUS" />
            </template>
            <template #approverCell="{ record }">
              {{ record.approverName }} / {{ formatDate(record.approveTime) }}
            </template>
          </VxeTableList>
        </a-tab-pane>
        <a-tab-pane key="my" tab="我的申请">
          <VxeTableList
            :columns="myVxeColumns"
            :data-source="myList"
            :loading="loading"
            :show-toolbar="false"
            :selectable="false"
            :pagination="false"
          >
            <template #priceChange="{ record }">
              <div class="price-change">
                <span class="old-price">原价: ¥{{ record.oldPrice }}</span>
                <span class="new-price">新价: ¥{{ record.newPrice }}</span>
                <span class="change" :class="record.priceChangeType">
                  {{ record.priceChangeType === 'increase' ? '+' : '-' }}¥{{ record.priceChange }}
                </span>
              </div>
            </template>
            <template #statusCell="{ record }">
              <StatusTag :status="record.status" :map="PRICE_APPROVAL_STATUS" />
            </template>
          </VxeTableList>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="applyVisible"
      title="申请价格变更"
      width="600px"
      :confirm-loading="submitLoading"
      @ok="handleApplySubmit"
      @cancel="applyVisible = false"
    >
      <a-form
        ref="applyFormRef"
        :model="applyForm"
        :rules="applyRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="产品" name="productId">
          <a-select
            v-model:value="applyForm.productId"
            placeholder="请选择产品"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="p in productList" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.code }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="客户" name="customerId">
          <a-select
            v-model:value="applyForm.customerId"
            placeholder="请选择客户(可选，不选则为通用价格)"
            show-search
            :filter-option="filterOption"
            allow-clear
          >
            <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">
              {{ c.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="当前价格">
          <span class="current-price">¥{{ currentPrice }}</span>
        </a-form-item>
        <a-form-item label="新价格" name="newPrice">
          <a-input-number v-model:value="applyForm.newPrice" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="价格变动">
          <span :class="{ increase: priceChangeType === 'increase', decrease: priceChangeType === 'decrease' }">
            {{ priceChangeType === 'increase' ? '+' : '-' }}¥{{ priceChangeAmount }}
          </span>
        </a-form-item>
        <a-form-item label="审批类型" name="approvalType">
          <a-select v-model:value="applyForm.approvalType" placeholder="请选择审批类型">
            <a-select-option value="price_adjustment">价格调整</a-select-option>
            <a-select-option value="promotion">促销价格</a-select-option>
            <a-select-option value="contract">合同价格</a-select-option>
            <a-select-option value="discount">折扣价格</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="生效时间">
          <a-range-picker v-model:value="applyForm.effectiveRange" show-time />
        </a-form-item>
        <a-form-item label="申请原因" name="approvalReason">
          <a-textarea v-model:value="applyForm.approvalReason" placeholder="请输入申请原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="approveVisible"
      title="审批通过"
      width="500px"
      @ok="handleApproveConfirm"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="产品">
          <span>{{ approveData.productName }}</span>
        </a-form-item>
        <a-form-item label="价格变更">
          <span>¥{{ approveData.oldPrice }} → ¥{{ approveData.newPrice }}</span>
        </a-form-item>
        <a-form-item label="审批备注">
          <a-textarea v-model:value="approveRemark" placeholder="请输入审批备注(可选)" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="rejectVisible"
      title="审批拒绝"
      width="500px"
      @ok="handleRejectConfirm"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="产品">
          <span>{{ rejectData.productName }}</span>
        </a-form-item>
        <a-form-item label="价格变更">
          <span>¥{{ rejectData.oldPrice }} → ¥{{ rejectData.newPrice }}</span>
        </a-form-item>
        <a-form-item label="拒绝原因">
          <a-textarea v-model:value="rejectReason" placeholder="请输入拒绝原因" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="detailVisible"
      title="审批详情"
      placement="right"
      width="80vw"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="产品名称">{{ detailData.productName }}</a-descriptions-item>
        <a-descriptions-item label="产品编码">{{ detailData.productCode }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ detailData.customerName || '通用价格' }}</a-descriptions-item>
        <a-descriptions-item label="审批类型">{{ detailData.approvalTypeLabel }}</a-descriptions-item>
        <a-descriptions-item label="原价格">¥{{ detailData.oldPrice }}</a-descriptions-item>
        <a-descriptions-item label="新价格">¥{{ detailData.newPrice }}</a-descriptions-item>
        <a-descriptions-item label="价格变动">
          <span :class="detailData.priceChangeType">{{ detailData.priceChangeType === 'increase' ? '+' : '-' }}¥{{ detailData.priceChange }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="审批状态">
          <StatusTag :status="detailData.status" :map="PRICE_APPROVAL_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="申请人">{{ detailData.applicantName }}</a-descriptions-item>
        <a-descriptions-item label="申请时间">{{ formatDate(detailData.applyTime) }}</a-descriptions-item>
        <a-descriptions-item label="审批人">{{ detailData.approverName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审批时间">{{ formatDate(detailData.approveTime) || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请原因" :span="2">{{ detailData.approvalReason }}</a-descriptions-item>
        <a-descriptions-item label="审批备注" :span="2">{{ detailData.approveRemark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { priceApprovalApi, type PriceApproval, type PriceApprovalStatistics } from '@/api/pricing-approval'
import { useUserStore } from '@/stores/user'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { PRICE_APPROVAL_STATUS } from '@/utils/statusConfig'

const userStore = useUserStore()
const loading = ref(false)
const submitLoading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const activeTab = ref('pending')
const applyVisible = ref(false)
const approveVisible = ref(false)
const rejectVisible = ref(false)
const detailVisible = ref(false)
const applyFormRef = ref<FormInstance>()

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 响应式数据 ──────────────────────────────────────────

const statistics = ref<PriceApprovalStatistics>({ totalCount: 0, pendingCount: 0, approvedCount: 0, rejectedCount: 0 })

const pendingList = ref<PriceApproval[]>([])
const approvedList = ref<PriceApproval[]>([])
const rejectedList = ref<PriceApproval[]>([])
const myList = ref<PriceApproval[]>([])

const productList = ref<{ id: number; name: string; code: string; basePrice: number }[]>([])
const customerList = ref<{ id: number; name: string }[]>([])

const applyForm = reactive({
  productId: undefined as number | undefined,
  customerId: undefined as number | undefined,
  newPrice: undefined as number | undefined,
  approvalType: undefined as string | undefined,
  effectiveRange: [] as any[],
  approvalReason: ''
})

const applyRules = {
  productId: [{ required: true, message: '请选择产品' }],
  newPrice: [{ required: true, message: '请输入新价格' }],
  approvalType: [{ required: true, message: '请选择审批类型' }],
  approvalReason: [{ required: true, message: '请输入申请原因' }]
}

const currentPrice = computed(() => {
  if (!applyForm.productId) return '0.00'
  const product = productList.value.find(p => p.id === applyForm.productId)
  return product ? product.basePrice.toFixed(2) : '0.00'
})

const priceChangeAmount = computed(() => {
  if (!applyForm.newPrice) return '0.00'
  return Math.abs(applyForm.newPrice - parseFloat(currentPrice.value)).toFixed(2)
})

const priceChangeType = computed(() => {
  if (!applyForm.newPrice) return ''
  return applyForm.newPrice >= parseFloat(currentPrice.value) ? 'increase' : 'decrease'
})

const approveData = ref<PriceApproval>({} as PriceApproval)
const approveRemark = ref('')
const rejectData = ref<PriceApproval>({} as PriceApproval)
const rejectReason = ref('')
const detailData = ref<PriceApproval>({} as PriceApproval)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const pendingTableRef = ref()

// ── VxeTableList 列定义 ──────────────────────────────────

const pendingVxeColumns = computed(() => [
  { field: 'productName', title: '产品', width: 150 },
  { field: 'customerName', title: '客户', width: 120 },
  { field: 'priceChange', title: '价格变更', width: 200, slotName: 'priceChange' },
  { field: 'approvalTypeLabel', title: '审批类型', width: 100 },
  { field: 'applicantName', title: '申请人', width: 100 },
  { field: 'applyTime', title: '申请时间', width: 160 },
  { field: 'status', title: '状态', width: 90, align: 'center', slotName: 'statusCell' },
  { field: 'action', title: '操作', width: 200, fixed: 'right', type: 'action' },
])

const processedVxeColumns = computed(() => [
  { field: 'productName', title: '产品', width: 150 },
  { field: 'customerName', title: '客户', width: 120 },
  { field: 'priceChange', title: '价格变更', width: 200, slotName: 'priceChange' },
  { field: 'status', title: '状态', width: 90, align: 'center', slotName: 'statusCell' },
  { field: 'approver', title: '审批人', width: 160, slotName: 'approverCell' },
])

const myVxeColumns = computed(() => [
  { field: 'productName', title: '产品', width: 150 },
  { field: 'customerName', title: '客户', width: 120 },
  { field: 'priceChange', title: '价格变更', width: 200, slotName: 'priceChange' },
  { field: 'status', title: '状态', width: 90, align: 'center', slotName: 'statusCell' },
  { field: 'applyTime', title: '申请时间', width: 160 },
])

// ── 数据加载 ────────────────────────────────────────────

const loadStatistics = async () => {
  try {
    const res = await priceApprovalApi.getStatistics()
    if (res.data) statistics.value = res.data
  } catch {
    console.warn('[价格审批] 统计加载失败，不影响列表')
  }
}

const loadPendingList = async () => {
  try {
    const res = await priceApprovalApi.getPendingList()
    pendingList.value = res.data || []
    pagination.total = pendingList.value.length
  } catch (err: any) {
    console.warn('[价格审批] 加载待审批列表失败', err)
    message.error('加载待审批列表失败: ' + (err?.message || ''))
  }
}

const loadApprovedList = async () => {
  try {
    const res = await priceApprovalApi.getListByStatus('approved')
    approvedList.value = res.data || []
  } catch {
    console.warn('[价格审批] 加载已通过列表失败')
    approvedList.value = []
  }
}

const loadRejectedList = async () => {
  try {
    const res = await priceApprovalApi.getListByStatus('rejected')
    rejectedList.value = res.data || []
  } catch {
    console.warn('[价格审批] 加载已拒绝列表失败')
    rejectedList.value = []
  }
}

const loadMyList = async () => {
  const uid = userStore.userInfo?.userId ?? userStore.userInfo?.id ?? 0
  if (!uid) { myList.value = []; return }
  try {
    const res = await priceApprovalApi.getMyApprovals(uid)
    myList.value = res.data || []
  } catch {
    console.warn('[价格审批] 加载我的申请列表失败')
    myList.value = []
  }
}

const loadData = async () => {
  loading.value = true
  await Promise.allSettled([loadStatistics(), loadPendingList(), loadApprovedList(), loadRejectedList(), loadMyList()])
  loading.value = false
  refreshLoading.value = false
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
}

// Tab 切换时按需刷新
watch(activeTab, (tab) => {
  if (tab === 'pending') loadPendingList()
  else if (tab === 'approved') loadApprovedList()
  else if (tab === 'rejected') loadRejectedList()
  else if (tab === 'my') loadMyList()
})

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  loadPendingList()
}

// ── 操作 ────────────────────────────────────────────────

const handleApply = () => {
  Object.assign(applyForm, {
    productId: undefined,
    customerId: undefined,
    newPrice: undefined,
    approvalType: undefined,
    effectiveRange: [],
    approvalReason: ''
  })
  applyVisible.value = true
}

const handleApplySubmit = async () => {
  try {
    await applyFormRef.value?.validate()
    submitLoading.value = true
    await priceApprovalApi.apply({
      productId: applyForm.productId!,
      customerId: applyForm.customerId,
      newPrice: applyForm.newPrice!,
      approvalType: applyForm.approvalType!,
      approvalReason: applyForm.approvalReason,
      effectiveStart: applyForm.effectiveRange?.[0]?.toISOString?.(),
      effectiveEnd: applyForm.effectiveRange?.[1]?.toISOString?.()
    })
    message.success('价格变更申请已提交')
    applyVisible.value = false
    loadData()
  } catch (err: any) {
    console.warn('[价格审批] 提交申请失败', err)
    if (err?.message) message.error(err.message)
  } finally {
    submitLoading.value = false
  }
}

const handleApprove = (record: PriceApproval) => {
  approveData.value = { ...record }
  approveRemark.value = ''
  approveVisible.value = true
}

const handleApproveConfirm = async () => {
  const approverId = userStore.userInfo?.userId ?? userStore.userInfo?.id ?? 0
  try {
    await priceApprovalApi.approve(approveData.value.id, approverId, approveRemark.value || undefined)
    message.success('审批已通过')
    approveVisible.value = false
    loadData()
  } catch (err: any) {
    console.warn('[价格审批] 审批失败', err)
    message.error('审批失败: ' + (err?.message || ''))
  }
}

const handleReject = (record: PriceApproval) => {
  rejectData.value = { ...record }
  rejectReason.value = ''
  rejectVisible.value = true
}

const handleRejectConfirm = async () => {
  if (!rejectReason.value.trim()) {
    message.warning('请输入拒绝原因')
    return
  }
  const approverId = userStore.userInfo?.userId ?? userStore.userInfo?.id ?? 0
  try {
    await priceApprovalApi.reject(rejectData.value.id, approverId, rejectReason.value)
    message.success('审批已拒绝')
    rejectVisible.value = false
    loadData()
  } catch (err: any) {
    console.warn('[价格审批] 拒绝审批失败', err)
    message.error('拒绝审批失败: ' + (err?.message || ''))
  }
}

const handleView = async (record: PriceApproval) => {
  try {
    const res = await priceApprovalApi.getById(record.id)
    detailData.value = res.data || record
  } catch {
    console.warn('[价格审批] 加载详情失败，使用本地数据')
    detailData.value = { ...record, approvalTypeLabel: getApprovalTypeText(record.approvalType) } as PriceApproval
  }
  detailVisible.value = true
}

// ── 辅助 ────────────────────────────────────────────────

const formatDate = (date: string) => date ? date.split('T')[0] : ''
const filterOption = (input: string, option: any) =>
  option?.name?.toLowerCase?.().includes(input.toLowerCase()) ?? false

const getApprovalTypeText = (type: string) => {
  return {
    price_adjustment: '价格调整', promotion: '促销价格',
    contract: '合同价格', discount: '折扣价格'
  }[type] || type
}

onMounted(() => {
  loadData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: loadData })
</script>

<style scoped lang="scss">
.approval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.approval-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.approval-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.approval-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.approval-header__right {
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

.pricing-approval-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  border: 1px solid #ffa39e;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}


.price-change {
  .old-price {
    font-size: 12px;
    color: #999;
  }

  .new-price {
    font-size: 12px;
    color: #333;
    margin-left: 8px;
  }

  .change {
    font-size: 12px;
    font-weight: 500;
    margin-left: 8px;

    &.increase {
      color: #f5222d;
    }

    &.decrease {
      color: #3f8600;
    }
  }
}

.current-price {
  font-size: 16px;
  color: #333;
}

.increase {
  color: #f5222d;
  font-weight: 500;
}

.decrease {
  color: #3f8600;
  font-weight: 500;
}
</style>
