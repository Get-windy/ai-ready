<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/mall">商城管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>用户审核</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <a-space :size="8">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
            <a-tooltip title="F5: 刷新">
              <a-button size="small" @click="debounceClick('refresh', fetchData)">
                <template #icon><ReloadOutlined /></template>刷新
              </a-button>
            </a-tooltip>
          </a-space>
        </div>
      </div>
    </template>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      @refresh="debounceClick('refresh', fetchData)"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #empty>
        <a-empty v-if="!hasError" description="暂无数据" />
        <a-result v-else status="error" title="数据加载失败">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>重新加载
            </a-button>
          </template>
        </a-result>
      </template>

      <template #auditStatusCell="{ record }">
        <a-tag v-if="record.auditStatus === 0" color="orange">待审核</a-tag>
        <a-tag v-else-if="record.auditStatus === 1" color="green">已通过</a-tag>
        <a-tag v-else-if="record.auditStatus === 2" color="red">已驳回</a-tag>
        <span v-else>{{ record.auditStatus }}</span>
      </template>

      <template #statusCell="{ record }">
        <a-switch
          :checked="record.status === 1"
          size="small"
          @change="handleToggleStatus(record)"
        />
      </template>

      <template #action="{ record }">
        <a-space>
          <template v-if="record.auditStatus === 0">
            <a-button v-permission="'erp:mall:user:approve'" type="link" size="small" @click="handleApprove(record)">通过</a-button>
            <a-button v-permission="'erp:mall:user:reject'" type="link" size="small" danger @click="handleReject(record)">驳回</a-button>
          </template>
          <span v-else style="color: #999; font-size: 12px;">
            {{ record.auditStatus === 1 ? '已审核' : '已驳回' }}
          </span>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 驳回原因弹窗 -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="驳回原因"
      :confirm-loading="rejectLoading"
      @ok="handleRejectConfirm"
    >
      <a-textarea
        v-model:value="rejectReason"
        placeholder="请输入驳回原因"
        :rows="3"
        :maxlength="500"
      />
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallUserAuditList' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { shopUserApi, type ShopUser } from '@/api/erp/mall'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<ShopUser[]>([])
const selectedRowKeys = ref<number[]>([])

const tableDataSource = tableData

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const searchForm = reactive({
  keyword: undefined as string | undefined,
  auditStatus: undefined as number | undefined,
  status: undefined as number | undefined
})

const vxeColumns: any = computed(() => [
  { type: 'checkbox', width: 40 },
  { field: 'username', title: '用户名', width: 120 },
  { field: 'companyName', title: '公司名称', width: 180, showOverflow: 'tooltip' },
  { field: 'nickname', title: '昵称', width: 120 },
  { field: 'phone', title: '手机号', width: 130 },
  { field: 'source', title: '来源', width: 80 },
  { field: 'auditStatus', title: '审核状态', width: 100, slotName: 'auditStatusCell' },
  { field: 'status', title: '状态', width: 70, slotName: 'statusCell' },
  { field: 'createTime', title: '注册时间', width: 160 },
  { type: 'action', title: '操作', width: 160, fixed: 'right' }
])

const filterFields = computed<FilterField[]>(() => [
  { key: 'keyword', label: '关键词', type: 'input', placeholder: '用户名/昵称/手机号/公司' },
  { key: 'auditStatus', label: '审核状态', type: 'select', options: [
    { label: '全部', value: undefined },
    { label: '待审核', value: 0 },
    { label: '已通过', value: 1 },
    { label: '已驳回', value: 2 }
  ]},
  { key: 'status', label: '状态', type: 'select', options: [
    { label: '全部', value: undefined },
    { label: '正常', value: 1 },
    { label: '禁用', value: 0 }
  ]}
])

// ── 驳回弹窗 ──
const rejectModalVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const currentRejectRecord = ref<ShopUser | null>(null)

// ── 防抖 ──
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await shopUserApi.page({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res?.data) {
      tableData.value = res.records || []
      pagination.total = res.total || 0
    }
  } catch (err) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[用户审核] 加载用户列表失败', err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { keyword: undefined, auditStatus: undefined, status: undefined })
  } else {
    searchForm.keyword = filters.keyword
    searchForm.auditStatus = filters.auditStatus !== undefined ? Number(filters.auditStatus) : undefined
    searchForm.status = filters.status !== undefined ? Number(filters.status) : undefined
  }
  pagination.current = 1
  fetchData()
}

const handleApprove = async (record: ShopUser) => {
  try {
    await shopUserApi.approve(record.id)
    message.success(`用户 "${record.username}" 已审核通过`)
    fetchData()
  } catch (err) {
    console.warn('[用户审核] 审核通过失败', err)
  }
}

const handleReject = (record: ShopUser) => {
  currentRejectRecord.value = record
  rejectReason.value = ''
  rejectModalVisible.value = true
}

const handleRejectConfirm = async () => {
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  if (!currentRejectRecord.value) return
  rejectLoading.value = true
  try {
    await shopUserApi.reject(currentRejectRecord.value.id, rejectReason.value)
    message.success(`用户 "${currentRejectRecord.value.username}" 已驳回`)
    rejectModalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[用户审核] 驳回失败', err)
  } finally {
    rejectLoading.value = false
  }
}

const handleToggleStatus = async (record: ShopUser) => {
  const newStatus = record.status === 1 ? 0 : 1
  try {
    await shopUserApi.toggleStatus(record.id, newStatus)
    message.success(newStatus === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch (err) {
    console.warn('[用户审核] 切换状态失败', err)
  }
}

// ── 键盘快捷键 & 自动刷新 ──
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (e.key === 'F5') {
    e.preventDefault(); debounceClick('refresh', fetchData)
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) { console.warn('[MallUserAuditList]', err) }

defineExpose({ fetchData })
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
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
