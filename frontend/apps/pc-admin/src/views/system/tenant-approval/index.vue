<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="tenant-approval-page-header">
        <div class="tenant-approval-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>租户注册审批</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="tenant-approval-page-header-title">租户注册审批</h2>
        </div>
        <div class="tenant-approval-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchList)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="tenant-approval-page-body">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pendingList.length }}</div>
            <div class="stat-card-label">待审核</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-approved">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ approvedCount }}</div>
            <div class="stat-card-label">已通过</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-rejected">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ rejectedCount }}</div>
            <div class="stat-card-label">已驳回</div>
          </div>
          <CloseCircleOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-card>
        <template #title>
          <div class="card-header">
            <span>待审核列表</span>
            <a-badge :count="pendingList.length" :overflow-count="99">
              <a-tag color="orange">待审核</a-tag>
            </a-badge>
          </div>
        </template>

        <!-- 骨架加载 -->
        <a-skeleton active v-if="loading" :paragraph="{ rows: 6 }" style="padding: 24px;" />

        <!-- 错误状态 -->
        <a-result v-else-if="hasError" status="error" title="数据加载失败">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchList)()">
              <template #icon><ReloadOutlined /></template>
              重新加载
            </a-button>
          </template>
        </a-result>

        <!-- 无数据 -->
        <a-empty v-else-if="pendingList.length === 0" description="暂无待审核的租户注册申请">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchList)()">刷新</a-button>
          </template>
        </a-empty>

        <!-- 审核列表 -->
        <a-list v-else :data-source="pendingList" item-layout="vertical" size="large">
          <template #renderItem="{ item, index }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <div class="item-title">
                    <span>{{ item.tenantName }}</span>
                    <a-tag color="blue">{{ item.tenantCode }}</a-tag>
                  </div>
                </template>
                <template #description>
                  <a-descriptions :column="2" size="small">
                    <a-descriptions-item label="联系人">{{ item.contactPerson }}</a-descriptions-item>
                    <a-descriptions-item label="联系电话">{{ item.contactPhone }}</a-descriptions-item>
                    <a-descriptions-item label="邮箱">{{ item.contactEmail }}</a-descriptions-item>
                    <a-descriptions-item label="申请时间">{{ item.createTime }}</a-descriptions-item>
                  </a-descriptions>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button type="primary" ghost size="small" v-permission="'system:tenant:approve'" @click="handleApprove(item)">
                  <template #icon><CheckOutlined /></template>
                  通过
                </a-button>
                <a-button danger ghost size="small" v-permission="'system:tenant:reject'" @click="handleReject(item)">
                  <template #icon><CloseOutlined /></template>
                  驳回
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-card>

      <!-- 通过审批确认 -->
      <a-modal
        v-model:open="approveModalVisible"
        title="确认通过审批"
        @ok="confirmApprove"
        :confirm-loading="approving"
      >
        <p>确认通过 <strong>{{ currentItem?.tenantName }}</strong> 的注册申请？</p>
        <p style="color: #666; font-size: 13px">
          通过后将自动创建管理员账号并初始化租户环境。
        </p>
        <a-textarea
          v-model:value="approveRemark"
          placeholder="备注（可选）"
          :rows="3"
        />
      </a-modal>

      <!-- 驳回确认 -->
      <a-modal
        v-model:open="rejectModalVisible"
        title="驳回注册申请"
        @ok="confirmReject"
        :confirm-loading="rejecting"
        :ok-button-props="{ danger: true }"
      >
        <p>确认驳回 <strong>{{ currentItem?.tenantName }}</strong> 的注册申请？</p>
        <a-form-item
          label="驳回原因"
          :validate-status="rejectReasonError ? 'error' : undefined"
          :help="rejectReasonError"
        >
          <a-textarea
            v-model:value="rejectReason"
            placeholder="请填写驳回原因（必填）"
            :rows="3"
          />
        </a-form-item>
      </a-modal>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, CloseOutlined, SyncOutlined, ReloadOutlined, ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { tenantApprovalApi, type SysTenant } from '@/api/tenant'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

const loading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const pendingList = ref<SysTenant[]>([])
const hasError = ref(false)

const approveModalVisible = ref(false)
const rejectModalVisible = ref(false)
const currentItem = ref<SysTenant | null>(null)
const approveRemark = ref('')
const rejectReason = ref('')
const rejectReasonError = ref('')

// ── 自动刷新 ────────────────────────────────────────────
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const approvedCount = ref(0)
const rejectedCount = ref(0)

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// SSE 监听：当收到缓存失效通知时刷新列表
// 其他管理员审批后，当前用户会收到 SSE 通知
import { getSseClient } from '@/utils/sseClient'

onMounted(() => {
  fetchList()
  const sse = getSseClient()
  sse.on('cache-invalidate', () => {
    // 延迟一下再刷新，等后端处理完成
    setTimeout(() => fetchList(), 1000)
  })
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchList()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchList)()
  }
}

const fetchList = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await tenantApprovalApi.getPending()
    pendingList.value = (res as any).data || []
    // 统计数据仅用于展示
    approvedCount.value = pendingList.value.filter((item: any) => item.status === 1).length
    rejectedCount.value = pendingList.value.filter((item: any) => item.status === 2).length
  } catch (error: any) {
    hasError.value = true
    message.error('获取待审核列表失败')
    console.error(error)
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleApprove = (item: SysTenant) => {
  currentItem.value = item
  approveRemark.value = ''
  approveModalVisible.value = true
}

const confirmApprove = async () => {
  if (!currentItem.value) return
  approving.value = true
  try {
    await tenantApprovalApi.approve(currentItem.value.id, approveRemark.value || undefined)
    message.success(`已通过 ${currentItem.value.tenantName} 的注册申请`)
    approveModalVisible.value = false
    await fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审批失败')
  } finally {
    approving.value = false
  }
}

const handleReject = (item: SysTenant) => {
  currentItem.value = item
  rejectReason.value = ''
  rejectReasonError.value = ''
  rejectModalVisible.value = true
}

const confirmReject = async () => {
  if (!currentItem.value) return
  if (!rejectReason.value.trim()) {
    rejectReasonError.value = '请填写驳回原因'
    return
  }
  rejectReasonError.value = ''
  rejecting.value = true
  try {
    await tenantApprovalApi.reject(currentItem.value.id, rejectReason.value.trim())
    message.success(`已驳回 ${currentItem.value.tenantName} 的注册申请`)
    rejectModalVisible.value = false
    await fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  } finally {
    rejecting.value = false
  }
}

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.tenant-approval-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.tenant-approval-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.tenant-approval-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.tenant-approval-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.tenant-approval-page-body {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tenant-approval-page-body > .ant-card {
  flex: 1;
  min-height: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
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

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-approved { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-rejected { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }

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

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
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

</style>
