<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="purchase-contract-header">
          <div class="purchase-contract-header__left">
            <span class="purchase-contract-header__breadcrumb">ERP / 采购管理 / 采购合同</span>
            <h2 class="purchase-contract-header__title">
              采购合同管理
            </h2>
          </div>
          <div class="purchase-contract-header__right">
            <a-space :size="12">
              <a-switch
                v-model:checked="autoRefreshEnabled"
                size="small"
                checked-children="自动"
                un-checked-children="手动"
                @change="handleAutoRefreshChange"
              />
              <span
                v-if="autoRefreshEnabled && autoRefreshCountdown > 0"
                class="auto-refresh-badge"
              >
                <SyncOutlined /> {{ autoRefreshCountdown }}s
              </span>
              <span class="data-status">
                <a-badge :status="loading ? 'processing' : 'success'" />
                <span
                  v-if="lastUpdateTime"
                  class="update-time"
                >
                  数据更新: {{ lastUpdateTime }}
                </span>
              </span>
              <a-button
                size="small"
                :loading="refreshLoading"
                @click="debounceClick('refresh', fetchData)"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
                刷新
              </a-button>
              <span class="shortcut-hints">
                <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              </span>
            </a-space>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <a-row
        :gutter="16"
        style="margin-bottom: 16px;"
      >
        <a-col :span="4">
          <div class="summary-card">
            <div
              class="summary-icon"
              style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"
            >
              <FileTextOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">
                合同总数
              </div>
              <div class="summary-value">
                {{ statistics.totalCount }}
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="4">
          <div class="summary-card">
            <div
              class="summary-icon"
              style="background: linear-gradient(135deg, #d9d9d9 0%, #8c8c8c 100%);"
            >
              <EditOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">
                草稿
              </div>
              <div class="summary-value">
                {{ statistics.draftCount }}
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="4">
          <div class="summary-card">
            <div
              class="summary-icon"
              style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"
            >
              <ClockCircleOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">
                待审批
              </div>
              <div class="summary-value warning">
                {{ statistics.pendingCount }}
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="4">
          <div class="summary-card">
            <div
              class="summary-icon"
              style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"
            >
              <CheckCircleOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">
                生效中
              </div>
              <div class="summary-value success">
                {{ statistics.activeCount }}
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="4">
          <div class="summary-card">
            <div
              class="summary-icon"
              style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"
            >
              <DollarOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">
                合同金额
              </div>
              <div class="summary-value">
                ¥{{ formatAmount(statistics.totalAmount) }}
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="4">
          <div class="summary-card">
            <div
              class="summary-icon"
              style="background: linear-gradient(135deg, #13c2c2 0%, #08979c 100%);"
            >
              <AuditOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">
                已完成
              </div>
              <div class="summary-value">
                {{ statistics.completedCount }}
              </div>
            </div>
          </div>
        </a-col>
      </a-row>

      <a-card
        title="采购合同管理"
        style="flex: 1; overflow: hidden;"
        :body-style="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }"
      >
        <!-- 搜索栏 -->
        <SearchBar
          :fields="searchFields"
          :loading="loading"
          @search="handleSearch"
          @reset="handleReset"
        />

        <!-- 操作按钮 -->
        <div class="action-area">
          <a-space>
            <a-button
              v-permission="'purchase:contract:create'"
              type="primary"
              @click="handleCreate"
            >
              <template #icon>
                <PlusOutlined />
              </template>新建合同
            </a-button>
            <a-button
              v-permission="'purchase:contract:export'"
              @click="debounceClick('export', handleExport)"
            >
              <template #icon>
                <ExportOutlined />
              </template>导出
            </a-button>
          </a-space>
        </div>

        <!-- 数据表格 -->
        <BillTableList
          ref="tableRef"
          :columns="vxeColumns"
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
          @cell-dblclick="handleView"
          @page-change="handlePageChange"
        >
          <template #empty>
            <div
              v-if="hasError"
              class="table-empty"
            >
              <WarningOutlined class="table-empty-icon" />
              <p class="table-empty-text">
                数据加载异常，请重试
              </p>
              <a-button
                type="primary"
                @click="fetchData"
              >
                <ReloadOutlined /> 重试
              </a-button>
            </div>
            <EmptyState
              v-else
              title="暂无数据"
              description="暂无采购合同数据"
              size="small"
              :show-actions="false"
            />
          </template>
          <template #contractNoCell="{ record }">
            <a-button
              type="link"
              size="small"
              @click="handleView(record)"
            >
              {{ record.contractNo }}
            </a-button>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.contractStatus)">
              {{ getStatusText(record.contractStatus) }}
            </a-tag>
          </template>
          <template #totalAmountCell="{ record }">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
          <template #executedAmountCell="{ record }">
            ¥{{ (record.executedAmount || 0).toFixed(2) }}
            <span v-if="record.executedPercent">({{ record.executedPercent }}%)</span>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleView(record)"
              >
                查看
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.DRAFT"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.DRAFT"
                type="link"
                size="small"
                @click="handleSubmit(record)"
              >
                提交
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.PENDING_APPROVAL"
                type="link"
                size="small"
                @click="handleApprove(record)"
              >
                审批
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.APPROVED"
                type="link"
                size="small"
                @click="handleActivate(record)"
              >
                激活
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.ACTIVE"
                type="link"
                size="small"
                danger
                @click="handleTerminate(record)"
              >
                终止
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.ACTIVE || record.contractStatus === ContractStatus.COMPLETED"
                type="link"
                size="small"
                @click="handleArchive(record)"
              >
                归档
              </a-button>
              <a-button
                v-if="record.contractStatus === ContractStatus.DRAFT || record.contractStatus === ContractStatus.REJECTED"
                type="link"
                size="small"
                danger
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </BillTableList>
      </a-card>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SyncOutlined, ExportOutlined,
  FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined,
  DollarOutlined, WarningOutlined, EditOutlined, AuditOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import {
  purchaseContractApi, ContractStatus,
  type PurchaseContract, type ContractStatistics,
} from '@/api/purchase-contract'

defineOptions({ name: 'PurchaseContractList' })
const router = useRouter()

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); handleExport(); return }
}

function handleError(err: any) {
  hasError.value = true
  console.warn('[采购合同] ErrorBoundary 捕获异常:', err)
}

const handleAutoRefreshChange = (checked: boolean) => {
  if (checked) {
    autoRefreshCountdown.value = 30
  } else {
    autoRefreshCountdown.value = 0
  }
}

const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
const dataSource = ref<PurchaseContract[]>([])
const tableRef = ref()

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计数据
const statistics = computed<ContractStatistics>(() => ({
  totalCount: dataSource.value.length,
  draftCount: dataSource.value.filter(r => r.contractStatus === ContractStatus.DRAFT).length,
  pendingCount: dataSource.value.filter(r => r.contractStatus === ContractStatus.PENDING_APPROVAL).length,
  activeCount: dataSource.value.filter(r => r.contractStatus === ContractStatus.ACTIVE).length,
  completedCount: dataSource.value.filter(r => r.contractStatus === ContractStatus.COMPLETED).length,
  totalAmount: dataSource.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0),
}))

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const searchFields: SearchField[] = [
  { name: 'contractNo', label: '合同编号', type: 'input', placeholder: '请输入合同编号' },
  { name: 'supplierName', label: '供应商', type: 'input', placeholder: '请输入供应商名称' },
  { name: 'contractTitle', label: '合同名称', type: 'input', placeholder: '请输入合同名称' },
  { name: 'contractStatus', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '草稿', value: ContractStatus.DRAFT },
    { label: '待审批', value: ContractStatus.PENDING_APPROVAL },
    { label: '已审批', value: ContractStatus.APPROVED },
    { label: '生效中', value: ContractStatus.ACTIVE },
    { label: '已完成', value: ContractStatus.COMPLETED },
    { label: '已驳回', value: ContractStatus.REJECTED },
    { label: '已终止', value: ContractStatus.TERMINATED },
    { label: '已归档', value: ContractStatus.ARCHIVED },
  ]},
  { name: 'dateRange', label: '签订日期', type: 'dateRange', placeholder: '请选择日期范围' },
]

const queryParams = reactive({
  contractNo: '',
  supplierName: '',
  contractTitle: '',
  contractStatus: undefined as ContractStatus | undefined,
  dateStart: '',
  dateEnd: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const vxeColumns = computed(() => [
  { field: 'contractNo', title: '合同编号', width: 160, slotName: 'contractNoCell' },
  { field: 'contractTitle', title: '合同名称', width: 200 },
  { field: 'supplierName', title: '供应商', width: 160 },
  { field: 'totalAmount', title: '合同金额', width: 120, align: 'right', slotName: 'totalAmountCell' },
  { field: 'executedAmount', title: '已执行金额', width: 140, align: 'right', slotName: 'executedAmountCell' },
  { field: 'startDate', title: '开始日期', width: 110 },
  { field: 'endDate', title: '结束日期', width: 110 },
  { field: 'contractStatus', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'createdAt', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 280, fixed: 'right', type: 'action' },
])

const getStatusText = (status: ContractStatus): string => {
  const map: Record<ContractStatus, string> = {
    [ContractStatus.DRAFT]: '草稿',
    [ContractStatus.PENDING_APPROVAL]: '待审批',
    [ContractStatus.APPROVED]: '已审批',
    [ContractStatus.ACTIVE]: '生效中',
    [ContractStatus.COMPLETED]: '已完成',
    [ContractStatus.REJECTED]: '已驳回',
    [ContractStatus.TERMINATED]: '已终止',
    [ContractStatus.ARCHIVED]: '已归档',
  }
  return map[status] || '未知'
}

const getStatusColor = (status: ContractStatus): string => {
  const map: Record<ContractStatus, string> = {
    [ContractStatus.DRAFT]: 'default',
    [ContractStatus.PENDING_APPROVAL]: 'orange',
    [ContractStatus.APPROVED]: 'blue',
    [ContractStatus.ACTIVE]: 'green',
    [ContractStatus.COMPLETED]: 'cyan',
    [ContractStatus.REJECTED]: 'red',
    [ContractStatus.TERMINATED]: 'volcano',
    [ContractStatus.ARCHIVED]: 'purple',
  }
  return map[status] || 'default'
}

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await purchaseContractApi.page({
      ...queryParams,
      current: pagination.current,
      size: pagination.pageSize,
    })
    dataSource.value = (res as any).data?.records || (res as any).records || []
    pagination.total = (res as any).data?.total || (res as any).total || 0
  } catch (error) {
    hasError.value = true
    console.warn('[采购合同] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    Object.assign(queryParams, {
      contractNo: values.contractNo || '',
      supplierName: values.supplierName || '',
      contractTitle: values.contractTitle || '',
      contractStatus: values.contractStatus,
      dateStart: values.dateRange?.[0] || '',
      dateEnd: values.dateRange?.[1] || '',
    })
  }
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(queryParams, {
    contractNo: '',
    supplierName: '',
    contractTitle: '',
    contractStatus: undefined,
    dateStart: '',
    dateEnd: '',
  })
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: { current: number; pageSize: number }) => {
  pagination.current = page.current
  pagination.pageSize = page.pageSize
  fetchData()
}

// 行操作
const handleCreate = () => {
  router.push('/erp/purchase-contract/form')
}

const handleView = (record: PurchaseContract) => {
  router.push(`/erp/purchase-contract/form?id=${record.id}`)
}

const handleEdit = (record: PurchaseContract) => {
  router.push(`/erp/purchase-contract/form?id=${record.id}`)
}

const handleSubmit = (record: PurchaseContract) => {
  Modal.confirm({
    title: '提交审批',
    content: `确认提交合同 ${record.contractNo} 进行审批吗？`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseContractApi.submit(record.id, '提交审批')
        message.success('已提交审批')
        fetchData()
      } catch {
        message.error('提交失败')
      }
    },
  })
}

const handleApprove = (record: PurchaseContract) => {
  Modal.confirm({
    title: '审批确认',
    content: `确认审批通过合同 ${record.contractNo} 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseContractApi.approve(record.id, 0, '审批通过', true)
        message.success('审批通过')
        fetchData()
      } catch {
        message.error('审批失败')
      }
    },
  })
}

const handleActivate = (record: PurchaseContract) => {
  Modal.confirm({
    title: '激活合同',
    content: `确认激活合同 ${record.contractNo} 吗？激活后将正式生效。`,
    okText: '确认激活',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseContractApi.activate(record.id)
        message.success('合同已激活')
        fetchData()
      } catch {
        message.error('激活失败')
      }
    },
  })
}

const handleTerminate = (record: PurchaseContract) => {
  Modal.confirm({
    title: '终止合同',
    content: `确认终止合同 ${record.contractNo} 吗？此操作不可恢复。`,
    okText: '确认终止',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseContractApi.terminate(record.id, '手动终止')
        message.success('合同已终止')
        fetchData()
      } catch {
        message.error('终止失败')
      }
    },
  })
}

const handleArchive = (record: PurchaseContract) => {
  Modal.confirm({
    title: '归档合同',
    content: `确认归档合同 ${record.contractNo} 吗？`,
    okText: '确认归档',
    cancelText: '取消',
    onOk: async () => {
      try {
        const archiveNo = `ARC-${Date.now()}`
        await purchaseContractApi.archive(record.id, archiveNo, '归档')
        message.success('合同已归档')
        fetchData()
      } catch {
        message.error('归档失败')
      }
    },
  })
}

const handleDelete = (record: PurchaseContract) => {
  Modal.confirm({
    title: '删除合同',
    content: `确认删除合同 ${record.contractNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseContractApi.delete(record.id)
        message.success('已删除')
        fetchData()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

const handleExport = async () => {
  try {
    const blob = await purchaseContractApi.export(queryParams)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `采购合同_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ── 自动刷新 ──────────────────────────────────────────
const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    if (autoRefreshEnabled.value && !loading.value) {
      fetchData()
    }
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshEnabled.value && autoRefreshCountdown.value > 0) {
      autoRefreshCountdown.value--
      if (autoRefreshCountdown.value === 0) autoRefreshCountdown.value = 30
    }
  }, 1000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onMounted(() => {
  fetchData()
  startAutoRefresh()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  stopAutoRefresh()
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.purchase-contract-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px;
}
.purchase-contract-header__breadcrumb {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}
.purchase-contract-header__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 10px;
  font-size: 12px;
  color: #1890ff;
}
.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #595959;
}
.update-time {
  color: #8c8c8c;
}
.shortcut-hints {
  display: inline-flex;
  gap: 8px;
  font-size: 12px;
  color: #8c8c8c;
}
.shortcut-hint kbd {
  padding: 1px 4px;
  background: #fafafa;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  font-family: monospace;
  font-size: 11px;
}

.summary-card {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  transition: all 0.3s;
}
.summary-card:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: #fff;
  font-size: 20px;
  margin-right: 12px;
}
.summary-content {
  flex: 1;
}
.summary-title {
  font-size: 13px;
  color: #595959;
  margin-bottom: 4px;
}
.summary-value {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}
.summary-value.warning {
  color: #faad14;
}
.summary-value.success {
  color: #52c41a;
}

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
}
.table-empty-icon {
  font-size: 48px;
  color: #faad14;
  margin-bottom: 12px;
}
.table-empty-text {
  color: #595959;
  font-size: 14px;
  margin-bottom: 16px;
}
</style>
