<template>
  <PageContainer full-height>
    <template #header>
      <div class="voucher-header">
        <div class="voucher-header-left">
          <a-breadcrumb class="voucher-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>凭证管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="voucher-header-title">凭证管理</h2>
        </div>
        <div class="voucher-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="finance-voucher-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-draft">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.draft }}</div>
            <div class="stat-card-label">草稿凭证</div>
          </div>
          <EditOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-audited">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.audited }}</div>
            <div class="stat-card-label">待过账</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-posted">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.posted }}</div>
            <div class="stat-card-label">已过账</div>
          </div>
          <SendOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-reversed">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.reversed }}</div>
            <div class="stat-card-label">已冲销</div>
          </div>
          <RollbackOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(totalDebit) }}</div>
            <div class="stat-card-label">借方总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
      </div>

          <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :filter-fields="filterFields"
      :show-search="false"
      :show-export="true"
      :show-add="true"
      :selectable="false"
      add-text="新增凭证"
      :show-edit="false"
      :show-delete="false"
      @add="handleAdd"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #statusCell="{ record }">
        <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusLabelMap[record.status] || '未知' }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="审核">
            <a-button type="link" size="small" @click="handleAudit(record)">
              <template #icon><CheckCircleOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 'audited'" title="过账">
            <a-button type="link" size="small" @click="handlePost(record)">
              <template #icon><SendOutlined /></template>
            </a-button>
          </a-tooltip>
          <PrintButton :record="record" :business-id="record.id" business-type="voucher" button-type="link" button-size="small" tooltip="打印" />
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item v-if="record.status === 'draft'" key="audit">
                  <CheckCircleOutlined /> 审核
                </a-menu-item>
                <a-menu-item v-if="record.status === 'audited'" key="post">
                  <SendOutlined /> 过账
                </a-menu-item>
                <a-menu-item v-if="record.status === 'posted'" key="reverse">
                  <RollbackOutlined /> 冲销
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
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
              没有符合条件的凭证，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无凭证数据，点击右上角「新增凭证」开始创建
            </p>
          </template>
        </div>
      </template>
    </VxeTableList>

      <!-- 新增凭证弹窗 -->
      <FullScreenDetail
        :visible="addModalVisible"
        title="新增凭证"
        :save-loading="addModalLoading"
        :show-save-and-new="true"
        width="900px"
        @save="handleAddModalOk"
        @close="handleAddFormClose"
        @save-and-new="handleAddFormSaveAndNew"
      >
        <a-form
          ref="addFormRef"
          :model="addForm"
          :rules="addFormRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="凭证日期" name="voucherDate">
                <a-date-picker
                  v-model:value="addForm.voucherDate"
                  style="width: 100%"
                  format="YYYY-MM-DD"
                  placeholder="选择日期"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="年度">
                <a-input :value="addForm.fiscalYear" disabled size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>

        <a-divider orientation="left">凭证分录</a-divider>

        <div class="entry-section">
          <a-button type="dashed" block @click="handleAddEntry" style="margin-bottom: 12px">
            <template #icon><PlusOutlined /></template>
            添加分录行
          </a-button>

          <VxeTableList
            :columns="entryColumns"
            :data-source="addForm.entries"
            :pagination="false"
            row-key="tempId"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #summaryCell="{ record }">
              <a-input v-model:value="record.summary" placeholder="摘要" size="small" />
            </template>
            <template #subjectCell="{ record }">
              <a-input v-model:value="record.subjectName" placeholder="科目名称" size="small" />
            </template>
            <template #debitAmountCell="{ record }">
              <a-input-number
                v-model:value="record.debitAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
                size="small"
                placeholder="0.00"
              />
            </template>
            <template #creditAmountCell="{ record }">
              <a-input-number
                v-model:value="record.creditAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
                size="small"
                placeholder="0.00"
              />
            </template>
            <template #actionCell="{ record, rowIndex }">
              <a-button type="link" size="small" danger @click="handleRemoveEntry(rowIndex)">
                <DeleteOutlined />
              </a-button>
            </template>
            <template #footer>
              <div class="voucher-summary-row">
                <span class="voucher-summary-label">合计</span>
                <span class="amount-cell debit">¥{{ getTotalDebit() }}</span>
                <span class="amount-cell credit">¥{{ getTotalCredit() }}</span>
              </div>
            </template>
          </VxeTableList>
        </div>
      </FullScreenDetail>

      <!-- 查看详情弹窗 -->
      <a-modal
        v-model:open="detailVisible"
        title="凭证详情"
        :footer="null"
        width="900px"
        centered
      >
        <template v-if="currentVoucher">
          <a-descriptions :column="3" bordered size="small">
            <a-descriptions-item label="凭证号">
              <a-tag color="blue">{{ currentVoucher.voucherNo }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="日期">{{ currentVoucher.voucherDate }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusColorMap[currentVoucher.status] || 'default'">
                {{ statusLabelMap[currentVoucher.status] || '未知' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="年度">{{ currentVoucher.fiscalYear }}</a-descriptions-item>
            <a-descriptions-item label="期间">{{ currentVoucher.fiscalPeriod }}月</a-descriptions-item>
            <a-descriptions-item label="制单人">{{ currentVoucher.createdBy || '-' }}</a-descriptions-item>
          </a-descriptions>

          <p style="margin-top: 12px">
            <strong>摘要：</strong>{{ currentVoucher.summary || currentVoucher.entries?.[0]?.summary || '-' }}
          </p>

          <a-divider>分录明细</a-divider>

          <VxeTableList
            :columns="entryViewColumns"
            :data-source="currentVoucher.entries || []"
            :pagination="false"
            row-key="id"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #debitAmountCell="{ record }">
              <span class="amount-cell debit">¥{{ formatAmount(record.debitAmount) }}</span>
            </template>
            <template #creditAmountCell="{ record }">
              <span class="amount-cell credit">¥{{ formatAmount(record.creditAmount) }}</span>
            </template>
            <template #footer>
              <div class="voucher-summary-row voucher-summary-row-view">
                <span class="voucher-summary-label">合计</span>
                <span class="amount-cell debit">¥{{ formatAmount(currentVoucher.debitTotal) }}</span>
                <span class="amount-cell credit">¥{{ formatAmount(currentVoucher.creditTotal) }}</span>
              </div>
            </template>
          </VxeTableList>

          <div class="detail-modal-footer">
            <a-button v-if="currentVoucher.status === 'draft'" type="primary" @click="handleAudit(currentVoucher)">
              审核
            </a-button>
            <a-button v-if="currentVoucher.status === 'audited'" type="primary" @click="handlePost(currentVoucher)">
              过账
            </a-button>
            <a-button v-if="currentVoucher.status === 'posted'" type="primary" danger @click="handleReverse(currentVoucher)">
              冲销
            </a-button>
            <PrintButton :business-id="currentVoucher.id" business-type="voucher" button-size="small" tooltip="打印" />
            <a-button @click="detailVisible = false">关闭</a-button>
          </div>
        </template>
      </a-modal>

      <!-- 冲销原因弹窗 -->
      <a-modal
        v-model:open="reverseModalVisible"
        title="冲销凭证"
        :confirm-loading="reverseLoading"
        centered
        @ok="handleReverseConfirm"
        @cancel="reverseModalVisible = false"
      >
        <a-form layout="vertical">
          <a-form-item label="冲销原因" required>
            <a-textarea
              v-model:value="reverseReason"
              placeholder="请输入冲销原因"
              :rows="3"
              size="small"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SearchOutlined, PlusOutlined, DeleteOutlined, EyeOutlined,
  CheckCircleOutlined, SendOutlined, RollbackOutlined, EllipsisOutlined,
  InboxOutlined, EditOutlined, DollarOutlined, ReloadOutlined, SyncOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer, FullScreenDetail } from '@/components'
import { voucherApi } from '@/api/finance'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

interface VoucherEntry {
  tempId?: number
  id?: number
  summary: string
  subjectName: string
  debitAmount: number
  creditAmount: number
}

interface Voucher {
  id: number
  voucherNo: string
  voucherDate: string
  fiscalYear: number
  fiscalPeriod: number
  status: string
  debitTotal: number
  creditTotal: number
  createdBy: string
  summary?: string
  entries?: VoucherEntry[]
}

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<Voucher[]>([])
const addModalVisible = ref(false)
const addModalLoading = ref(false)
const detailVisible = ref(false)
const currentVoucher = ref<Voucher | null>(null)
const reverseModalVisible = ref(false)
const reverseLoading = ref(false)
const reverseTarget = ref<Voucher | null>(null)
const reverseReason = ref('')
const addFormRef = ref<FormInstance>()
const lastUpdateTime = ref('')
const lastUpdated = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)

const searchForm = reactive({
  fiscalYear: dayjs().year(),
  fiscalPeriod: undefined as number | undefined,
  status: undefined as string | undefined,
  voucherNo: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const draft = tableData.value.filter(r => r.status === 'draft').length
  const audited = tableData.value.filter(r => r.status === 'audited').length
  const posted = tableData.value.filter(r => r.status === 'posted').length
  const reversed = tableData.value.filter(r => r.status === 'reversed').length
  return { draft, audited, posted, reversed }
})

const totalDebit = computed(() => {
  return tableData.value.filter(r => r.status === 'posted').reduce((sum, r) => sum + r.debitTotal, 0)
})

const filterFields = [
  { key: 'fiscalYear', label: '年度', type: 'input' as const, placeholder: '年度', defaultValue: dayjs().year() },
  { key: 'fiscalPeriod', label: '期间', type: 'select' as const, options: Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })), placeholder: '期间' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已审核', value: 'audited' },
    { label: '已过账', value: 'posted' },
    { label: '已冲销', value: 'reversed' }
  ]},
  { key: 'voucherNo', label: '凭证号', type: 'input' as const, placeholder: '凭证号' }
]

const statusColorMap: Record<string, string> = { draft: 'default', audited: 'processing', posted: 'success', reversed: 'error' }
const statusLabelMap: Record<string, string> = { draft: '草稿', audited: '已审核', posted: '已过账', reversed: '已冲销' }

// vxe-table 列定义
const vxeColumns = computed(() => [
  {
    field: 'voucherNo',
    title: '凭证号',
    width: 140,
    formatter: ({ cellValue }: any) => `<a style="color: #1890ff; cursor: pointer;">${cellValue}</a>`,
  },
  { field: 'voucherDate', title: '日期', width: 110 },
  { field: 'summary', title: '摘要', width: 200 },
  {
    field: 'debitTotal',
    title: '借方总额',
    width: 130,
    align: 'right',
    formatter: ({ cellValue }: any) => `¥${(cellValue || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
  },
  {
    field: 'creditTotal',
    title: '贷方总额',
    width: 130,
    align: 'right',
    formatter: ({ cellValue }: any) => `¥${(cellValue || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
  },
  { field: 'status', title: '状态', width: 100, align: 'center', slotName: 'statusCell' },
  { field: 'createdBy', title: '制单人', width: 100 },
  { field: 'action', title: '操作', width: 180, fixed: 'right', type: 'action' },
])

const entryColumns = [
  { title: '摘要', field: 'summary', width: 180, slotName: 'summaryCell' },
  { title: '会计科目', field: 'subject', width: 180, slotName: 'subjectCell' },
  { title: '借方金额', field: 'debitAmount', width: 130, slotName: 'debitAmountCell' },
  { title: '贷方金额', field: 'creditAmount', width: 130, slotName: 'creditAmountCell' },
  { title: '操作', field: 'action', width: 60, slotName: 'actionCell' }
]

const entryViewColumns = [
  { title: '摘要', field: 'summary' },
  { title: '会计科目', field: 'subjectName' },
  { title: '借方金额', field: 'debitAmount', width: 130, align: 'right', slotName: 'debitAmountCell' },
  { title: '贷方金额', field: 'creditAmount', width: 130, align: 'right', slotName: 'creditAmountCell' }
]

let entryTempIdCounter = 0
const addForm = reactive({
  voucherDate: undefined as any,
  fiscalYear: dayjs().year(),
  entries: [] as any[]
})

const addFormRules = {
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }]
}

const initialFormSnapshot = ref('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({
    voucherDate: addForm.voucherDate,
    fiscalYear: addForm.fiscalYear,
    entries: addForm.entries.map(e => ({ ...e }))
  })
}
const formDirty = computed(() => {
  const current = JSON.stringify({
    voucherDate: addForm.voucherDate,
    fiscalYear: addForm.fiscalYear,
    entries: addForm.entries.map(e => ({ ...e }))
  })
  return current !== initialFormSnapshot.value
})

onBeforeRouteLeave((to, from, next) => {
  if (addModalVisible.value && formDirty.value) {
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

function formatAmount(val: number): string {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.fiscalYear) params.fiscalYear = searchForm.fiscalYear
    if (searchForm.fiscalPeriod) params.fiscalPeriod = searchForm.fiscalPeriod
    if (searchForm.status !== undefined) params.status = searchForm.status
    if (searchForm.voucherNo) params.voucherNo = searchForm.voucherNo

    const res = await voucherApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    hasError.value = false
  } catch (err) {
    console.warn('加载凭证数据失败', err)
    message.error('加载凭证数据失败')
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
  }
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.fiscalYear = filters.fiscalYear ?? dayjs().year()
  searchForm.fiscalPeriod = filters.fiscalPeriod
  searchForm.status = filters.status
  searchForm.voucherNo = filters.voucherNo || ''
  pagination.current = 1
  fetchData()
}

const handleResetFilters = () => {
  searchForm.fiscalYear = dayjs().year()
  searchForm.fiscalPeriod = undefined
  searchForm.status = undefined
  searchForm.voucherNo = ''
  pagination.current = 1
  fetchData()
}

const handleAdd = () => {
  addForm.voucherDate = undefined
  addForm.fiscalYear = dayjs().year()
  addForm.entries = []
  entryTempIdCounter = 0
  addModalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

const handleAddEntry = () => {
  entryTempIdCounter++
  addForm.entries.push({
    tempId: entryTempIdCounter,
    summary: '',
    subjectName: '',
    debitAmount: 0,
    creditAmount: 0
  })
}

const handleRemoveEntry = (index: number) => {
  addForm.entries.splice(index, 1)
}

const getTotalDebit = () => {
  return addForm.entries.reduce((sum: number, e) => sum + (e.debitAmount || 0), 0).toFixed(2)
}

const getTotalCredit = () => {
  return addForm.entries.reduce((sum: number, e) => sum + (e.creditAmount || 0), 0).toFixed(2)
}

const handleAddModalOk = async () => {
  try { await addFormRef.value?.validate() } catch (err) { console.warn('[凭证管理] 表单校验失败', err); return }
  if (addForm.entries.length === 0) { message.warning('请至少添加一条分录'); return }
  const debit = addForm.entries.reduce((s, e) => s + (e.debitAmount || 0), 0)
  const credit = addForm.entries.reduce((s, e) => s + (e.creditAmount || 0), 0)
  if (debit !== credit) { message.warning('借贷金额不平，请检查'); return }
  addModalLoading.value = true
  try {
    const data = {
      voucherDate: addForm.voucherDate ? dayjs(addForm.voucherDate).format('YYYY-MM-DD') : '',
      fiscalYear: addForm.fiscalYear,
      entries: addForm.entries.map(e => ({
        summary: e.summary,
        subjectName: e.subjectName,
        debitAmount: e.debitAmount || 0,
        creditAmount: e.creditAmount || 0
      }))
    }
    await voucherApi.create(data)
    message.success('凭证创建成功')
    addModalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[凭证管理] 创建凭证失败', err)
    message.error('创建凭证失败')
  } finally {
    addModalLoading.value = false
  }
}

const handleAddModalCancel = () => {
  addModalVisible.value = false
  addFormRef.value?.resetFields()
}

function handleAddFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的修改，确定要关闭吗？',
      onOk: () => { addModalVisible.value = false }
    })
  } else {
    addModalVisible.value = false
  }
}

let _savedAndNew = false
function handleAddFormSaveAndNew() {
  _savedAndNew = true
  handleAddModalOk().then(() => {
    if (_savedAndNew && !addModalVisible.value) {
      _savedAndNew = false
      addForm.voucherDate = undefined
      addForm.fiscalYear = dayjs().year()
      addForm.entries = []
      entryTempIdCounter = 0
      addModalVisible.value = true
      nextTick(() => saveFormSnapshot())
    }
  })
}

const handleAudit = async (record: Voucher) => {
  Modal.confirm({
    title: '确认审核',
    content: `确定要审核凭证 "${record.voucherNo}" 吗？`,
    okText: '确认审核',
    centered: true,
    async onOk() {
      try {
        await voucherApi.audit(record.id)
        message.success('审核成功')
        detailVisible.value = false
        fetchData()
      } catch (err) {
        console.warn('[凭证管理] 审核失败', err)
        message.error('审核失败')
      }
    }
  })
}

const handlePost = async (record: Voucher) => {
  Modal.confirm({
    title: '确认过账',
    content: `确定要将凭证 "${record.voucherNo}" 过账吗？过账后不可修改。`,
    okText: '确认过账',
    centered: true,
    async onOk() {
      try {
        await voucherApi.post(record.id)
        message.success('过账成功')
        detailVisible.value = false
        fetchData()
      } catch (err) {
        console.warn('[凭证管理] 过账失败', err)
        message.error('过账失败')
      }
    }
  })
}

const handleReverse = (record: Voucher) => {
  reverseTarget.value = record
  reverseReason.value = ''
  reverseModalVisible.value = true
}

const handleReverseConfirm = async () => {
  if (!reverseReason.value.trim()) {
    message.warning('请输入冲销原因')
    return
  }
  reverseLoading.value = true
  try {
    await voucherApi.reverse(reverseTarget.value!.id, reverseReason.value)
    message.success('冲销成功')
    reverseModalVisible.value = false
    detailVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[凭证管理] 冲销失败', err)
    message.error('冲销失败')
  } finally {
    reverseLoading.value = false
  }
}

const handleView = async (record: Voucher) => {
  try {
    const res = await voucherApi.getById(record.id)
    if (res.data) {
      currentVoucher.value = res.data
      detailVisible.value = true
    }
  } catch (err) {
    console.warn('获取凭证详情失败', err)
    message.error('获取凭证详情失败')
  }
}

const handleActionMenuClick = (key: string, record: Voucher) => {
  switch (key) {
    case 'audit': handleAudit(record); break
    case 'post': handlePost(record); break
    case 'reverse': handleReverse(record); break
  }
}

const handleExport = () => {
  const headers = ['凭证号', '日期', '摘要', '借方总额', '贷方总额', '状态', '制单人']
  const rows = tableData.value.map(r => [
    r.voucherNo, r.voucherDate, r.summary || '', formatAmount(r.debitTotal),
    formatAmount(r.creditTotal), statusLabelMap[r.status], r.createdBy
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `凭证列表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  console.warn('[凭证管理] 导出成功', tableData.value.length)
  message.success('导出成功')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleParentCreate() { handleAdd() }

function handleEditVoucher(e: CustomEvent) {
  const data = e.detail
  if (!data?.entries) return
  addForm.voucherDate = data.voucherDate ? dayjs(data.voucherDate) : undefined
  addForm.fiscalYear = data.fiscalYear || dayjs().year()
  addForm.entries = (data.entries || []).map((entry: any) => ({
    tempId: ++entryTempIdCounter,
    summary: entry.summary || '',
    subjectName: entry.subjectName || entry.subject || '',
    debitAmount: entry.debitAmount || 0,
    creditAmount: entry.creditAmount || 0
  }))
  addModalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:edit-voucher', handleEditVoucher)
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
  window.removeEventListener('finance:edit-voucher', handleEditVoucher)
  window.removeEventListener('finance:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.finance-voucher-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.finance-voucher-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

.voucher-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.voucher-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.voucher-breadcrumb {
  font-size: 13px;
}
.voucher-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.voucher-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
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

.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-audited { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-posted { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-reversed { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
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

.voucher-summary-row {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 8px 16px;
  font-weight: 600;
  font-size: 13px;
}

.voucher-summary-row.voucher-summary-row-view {
  padding: 4px 16px;
}

.voucher-summary-label {
  margin-right: 8px;
  color: #333;
}

.voucher-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.amount-cell.debit {
  color: #1890ff;
}

.amount-cell.credit {
  color: #52c41a;
}

.action-more-btn {
  padding: 0 4px;
}

.entry-section {
  margin: 0 -24px;
  padding: 0 24px;
}

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}





/* 响应式 */
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
</style>
