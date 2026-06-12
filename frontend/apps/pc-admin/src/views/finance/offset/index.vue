<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="offset-page-header">
        <div class="offset-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>往来对冲</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="offset-page-header-title">往来对冲</h2>
        </div>
        <div class="offset-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <div class="finance-offset-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.receivableAmount) }}</div>
            <div class="stat-card-label">应收总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-written-off">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.payableAmount) }}</div>
            <div class="stat-card-label">应付总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-balance">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.offsetAmount) }}</div>
            <div class="stat-card-label">对冲金额</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.balanceAmount) }}</div>
            <div class="stat-card-label">余额</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :min-empty-rows="12"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :show-search="false"
        export-permission="finance:offset:list"
        :show-add="false"
        :show-edit="false"
        :show-delete="false"
        :selectable="false"
        @refresh="fetchData"
        @cell-dblclick="handleView"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
      >
        <template #toolbar-actions>
          <a-button size="small" type="primary" v-permission="'finance:offset:create'" @click="debounceClick('add', handleAdd)">
            <template #icon><PlusOutlined /></template>
            新增对冲
          </a-button>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>
        <template #batch-actions>
          <!-- 预留批量操作 -->
        </template>

        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else>
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">
                没有符合条件的对冲记录，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无往来对冲数据
              </p>
            </template>
          </div>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusLabelMap[record.status] || record.status }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space :size="0" class="action-cell-inner">
            <PrintButton
              template-type="offset"
              :business-id="record.id"
              business-type="offset"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-button type="link" size="small" v-permission="'finance:offset:view'" @click="handleView(record)">
              查看
            </a-button>
            <a-button v-if="record.status === 'draft'" type="link" size="small" v-permission="'finance:offset:edit'" @click="handleComplete(record)">
              完成
            </a-button>
            <a-button v-if="record.status === 'draft'" type="link" size="small" v-permission="'finance:offset:delete'" danger @click="handleCancel(record)">
              取消
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 新增对冲弹窗 -->
      <FullScreenDetail
        :visible="createVisible"
        title="新增往来对冲"
        :save-loading="createLoading"
        :dirty="formDirty"
        @save="handleCreateConfirm"
        @close="handleCreateCancel"
      >
        <a-form layout="vertical">
          <a-form-item label="对方类型" required>
            <a-radio-group v-model:value="createForm.partyType">
              <a-radio value="customer">客户</a-radio>
              <a-radio value="supplier">供应商</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="对方名称" required>
            <a-select
              v-model:value="createForm.partyId"
              :options="partyOptions"
              :loading="partyLoading"
              show-search
              :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
              placeholder="请选择对方名称"
              @change="handlePartyChange"
            />
          </a-form-item>
          <a-descriptions v-if="partyInfo" :column="1" bordered size="small">
            <a-descriptions-item label="应收金额">
              ¥{{ formatAmount(partyInfo.receivableAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="应付金额">
              ¥{{ formatAmount(partyInfo.payableAmount) }}
            </a-descriptions-item>
          </a-descriptions>
          <a-form-item label="对冲金额" required style="margin-top: 16px">
            <a-input-number
              v-model:value="createForm.offsetAmount"
              :min="0.01"
              :precision="2"
              size="small"
              style="width: 100%"
              placeholder="请输入对冲金额"
            />
          </a-form-item>
          <a-form-item label="对冲日期">
            <a-date-picker
              v-model:value="createForm.offsetDate"
              size="small"
              style="width: 100%"
              placeholder="选择对冲日期"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="createForm.remark"
              :rows="3"
              size="small"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 查看详情弹窗 -->
      <FullScreenDetail
        :visible="detailVisible"
        :title="`对冲详情 - ${detailData?.offsetNo || ''}`"
        @close="handleDetailClose"
      >
        <a-descriptions v-if="detailData" :column="1" bordered size="small">
          <a-descriptions-item label="对冲单号">{{ detailData.offsetNo }}</a-descriptions-item>
          <a-descriptions-item label="对方名称">{{ detailData.partyName }}</a-descriptions-item>
          <a-descriptions-item label="应收金额">¥{{ formatAmount(detailData.receivableAmount) }}</a-descriptions-item>
          <a-descriptions-item label="应付金额">¥{{ formatAmount(detailData.payableAmount) }}</a-descriptions-item>
          <a-descriptions-item label="对冲金额">¥{{ formatAmount(detailData.offsetAmount) }}</a-descriptions-item>
          <a-descriptions-item label="余额">¥{{ formatAmount(detailData.balanceAmount) }}</a-descriptions-item>
          <a-descriptions-item label="对冲日期">{{ detailData.offsetDate }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[detailData.status] || 'default'">{{ statusLabelMap[detailData.status] || detailData.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailData.remark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
        </a-descriptions>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, PlusOutlined, InboxOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined,
  SyncOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { offsetApi, preReceiptApi, prePaymentApi } from '@/api/finance'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchForm = reactive({
  partyName: '',
  offsetDate: undefined as string | undefined,
  status: undefined as string | undefined
})

const filterFields = [
  { key: 'partyName', label: '对方名称', type: 'input' as const, placeholder: '对方名称' },
  { key: 'offsetDate', label: '对冲日期', type: 'date' as const },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已完成', value: 'completed' },
    { label: '已取消', value: 'cancelled' }
  ]}
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const stats = reactive({
  receivableAmount: 0,
  payableAmount: 0,
  offsetAmount: 0,
  balanceAmount: 0
})

const statusColorMap: Record<string, string> = {
  draft: 'warning',
  completed: 'success',
  cancelled: 'error'
}

const statusLabelMap: Record<string, string> = {
  draft: '草稿',
  completed: '已完成',
  cancelled: '已取消'
}

const columns = computed(() => [
  { title: '对冲单号', field: 'offsetNo', width: 160 },
  { title: '对方名称', field: 'partyName', width: 150 },
  { title: '应收金额', field: 'receivableAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '应付金额', field: 'payableAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '对冲金额', field: 'offsetAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '余额', field: 'balanceAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '对冲日期', field: 'offsetDate', width: 110 },
  { title: '状态', field: 'status', width: 90, align: 'center', slotName: 'statusCell' },
  { title: '操作', type: 'action', width: 180, fixed: 'right' }
])

// 新增对冲
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  partyType: 'customer' as string,
  partyId: undefined as number | undefined,
  offsetAmount: undefined as number | undefined,
  offsetDate: undefined as string | undefined,
  remark: ''
})
const partyLoading = ref(false)
const partyOptions = ref<any[]>([])
const partyInfo = ref<any>(null)

const initialFormSnapshot = ref('')
const formDirty = computed(() => {
  if (!createVisible.value) return false
  return JSON.stringify({ ...createForm }) !== initialFormSnapshot.value
})

// 查看详情
const detailVisible = ref(false)
const detailData = ref<any>(null)

const fetchPartyOptions = async () => {
  partyLoading.value = true
  try {
    // 根据对方类型加载可选项
    if (createForm.partyType === 'customer') {
      // 从应收账款获取客户列表作为选项
      const res = await preReceiptApi.getPage({ pageNum: 1, pageSize: 999 })
      const list = res.data?.records || (res.data as any)?.list || []
      const seen = new Set<number>()
      partyOptions.value = list
        .filter((item: any) => { const dup = seen.has(item.customerId); seen.add(item.customerId); return !dup })
        .map((item: any) => ({ label: item.customerName, value: item.customerId }))
    } else {
      const res = await prePaymentApi.getPage({ pageNum: 1, pageSize: 999 })
      const list = res.data?.records || (res.data as any)?.list || []
      const seen = new Set<number>()
      partyOptions.value = list
        .filter((item: any) => { const dup = seen.has(item.supplierId); seen.add(item.supplierId); return !dup })
        .map((item: any) => ({ label: item.supplierName, value: item.supplierId }))
    }
  } catch (err) {
    console.warn('[往来对冲] 获取对方选项失败', err)
    partyOptions.value = []
  } finally {
    partyLoading.value = false
  }
}

const handlePartyChange = (val: number) => {
  // 根据选择的对方加载应收/应付金额
  partyInfo.value = null
  // 模拟或查询实际数据
  fetchPartyDetail(val)
}

const fetchPartyDetail = async (partyId: number) => {
  try {
    if (createForm.partyType === 'customer') {
      const res = await preReceiptApi.getPage({ customerId: partyId, pageNum: 1, pageSize: 999 })
      const list = res.data?.records || (res.data as any)?.list || []
      const receivableAmount = list.reduce((sum: number, item: any) => sum + (item.remainingAmount || 0), 0)
      partyInfo.value = { receivableAmount, payableAmount: 0 }
    } else {
      const res = await prePaymentApi.getPage({ supplierId: partyId, pageNum: 1, pageSize: 999 })
      const list = res.data?.records || (res.data as any)?.list || []
      const payableAmount = list.reduce((sum: number, item: any) => sum + (item.remainingAmount || 0), 0)
      partyInfo.value = { receivableAmount: 0, payableAmount }
    }
  } catch (err) {
    console.warn('[往来对冲] 获取对方明细失败', err)
    partyInfo.value = null
  }
}

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.partyName) params.partyName = searchForm.partyName
    if (searchForm.offsetDate) params.offsetDate = searchForm.offsetDate
    if (searchForm.status) params.status = searchForm.status

    const res = await offsetApi.getPage(params)
    if (res.data) {
      tableData.value = res.records || (res.data as any).list || []
      pagination.total = res.total || 0
      lastUpdated.value = new Date().toISOString()
            // 更新统计（优先使用后端汇总数据）
            if ((res.data as any).totalReceivableAmount !== undefined) {
              stats.receivableAmount = (res.data as any).totalReceivableAmount
              stats.payableAmount = (res.data as any).totalPayableAmount || 0
              stats.offsetAmount = (res.data as any).totalOffsetAmount || 0
              stats.balanceAmount = (res.data as any).totalBalanceAmount || 0
            } else {
              stats.receivableAmount = tableData.value.reduce((sum, item) => sum + (item.receivableAmount || 0), 0)
              stats.payableAmount = tableData.value.reduce((sum, item) => sum + (item.payableAmount || 0), 0)
              stats.offsetAmount = tableData.value.reduce((sum, item) => sum + (item.offsetAmount || 0), 0)
              stats.balanceAmount = tableData.value.reduce((sum, item) => sum + (item.balanceAmount || 0), 0)
            }
    }
    hasError.value = false
  } catch (err) {
    console.warn('[往来对冲] 获取数据失败', err)
    message.error('获取往来对冲数据失败')
    tableData.value = []
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleView = (record: any) => {
  detailData.value = record
  detailVisible.value = true
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.partyName = ''
  searchForm.offsetDate = undefined
  searchForm.status = undefined
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.partyName = filters.partyName || ''
  searchForm.offsetDate = filters.offsetDate || undefined
  searchForm.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleAdd = () => {
  createForm.partyType = 'customer'
  createForm.partyId = undefined
  createForm.offsetAmount = undefined
  createForm.offsetDate = undefined
  createForm.remark = ''
  initialFormSnapshot.value = JSON.stringify({ ...createForm })
  partyInfo.value = null
  partyOptions.value = []
  createVisible.value = true
  fetchPartyOptions()
}

const handleCreateConfirm = async () => {
  if (!createForm.partyId) {
    message.warning('请选择对方名称')
    return
  }
  if (!createForm.offsetAmount || createForm.offsetAmount <= 0) {
    message.warning('请输入有效的对冲金额')
    return
  }
  createLoading.value = true
  try {
    const payload: any = {
      partyType: createForm.partyType,
      partyId: createForm.partyId,
      offsetAmount: createForm.offsetAmount
    }
    if (createForm.offsetDate) payload.offsetDate = createForm.offsetDate
    if (createForm.remark) payload.remark = createForm.remark

    await offsetApi.create(payload)
    message.success('创建对冲成功')
    createVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[往来对冲] 创建失败', err)
    message.error('创建对冲失败')
  } finally {
    createLoading.value = false
  }
}

const handleCreateCancel = () => {
  createVisible.value = false
}

const handleComplete = (record: any) => {
  Modal.confirm({
    title: '确认完成',
    content: `确定完成对冲单 "${record.offsetNo}" 吗？`,
    async onOk() {
      try {
        await offsetApi.complete(record.id)
        message.success('对冲已完成')
        fetchData()
      } catch (err) {
        console.warn('[往来对冲] 完成操作失败', err)
        message.error('操作失败')
      }
    }
  })
}

const handleCancel = (record: any) => {
  Modal.confirm({
    title: '确认取消',
    content: `确定取消对冲单 "${record.offsetNo}" 吗？`,
    async onOk() {
      try {
        await offsetApi.cancel(record.id, '手动取消')
        message.success('对冲已取消')
        fetchData()
      } catch (err) {
        console.warn('[往来对冲] 取消操作失败', err)
        message.error('操作失败')
      }
    }
  })
}

const handleDetailClose = () => {
  detailVisible.value = false
  detailData.value = null
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function isInput(el: Element | null): boolean {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (el as HTMLElement)?.isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleParentCreate() { handleAdd() }

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onBeforeRouteLeave((to, from, next) => {
  if (createVisible.value && formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的修改，确定要离开吗？',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.offset-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.offset-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.offset-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.offset-page-header-right {
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

.finance-offset-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  gap: 16px;
}

.finance-offset-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-written-off { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-balance { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 18px;
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
  font-size: 24px;
  color: rgba(0, 0, 0, 0.15);
}

/* 空状态 */
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

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.amount-cell.success {
  color: #52c41a;
}

.amount-cell.warning {
  color: #faad14;
}

.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}

.action-cell-inner {
  display: inline-flex;
  align-items: center;
}

/* 响应式 */

.table-empty-action {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
  }
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
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
