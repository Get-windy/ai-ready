<template>
  <PageContainer title="凭证管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="fetchData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
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
      :data-source="tableDataSource"
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
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 0" title="审核">
            <a-button type="link" size="small" @click="handleAudit(record)">
              <template #icon><CheckCircleOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 1" title="过账">
            <a-button type="link" size="small" @click="handlePost(record)">
              <template #icon><SendOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item v-if="record.status === 0" key="audit">
                  <CheckCircleOutlined /> 审核
                </a-menu-item>
                <a-menu-item v-if="record.status === 1" key="post">
                  <SendOutlined /> 过账
                </a-menu-item>
                <a-menu-item v-if="record.status === 2" key="reverse">
                  <RollbackOutlined /> 冲销
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="print">
                  <PrinterOutlined /> 打印
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的凭证，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无凭证数据，点击右上角「新增凭证」开始创建
          </p>
        </div>
      </template>
    </VxeTableList>

      <!-- 新增凭证弹窗 -->
      <a-modal
        v-model:open="addModalVisible"
        title="新增凭证"
        :confirm-loading="addModalLoading"
        width="900px"
        centered
        :maskClosable="false"
        @ok="handleAddModalOk"
        @cancel="handleAddModalCancel"
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
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="年度">
                <a-input :value="addForm.fiscalYear" disabled />
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

          <a-table
            :columns="entryColumns"
            :data-source="formTableItems"
            :pagination="false"
            row-key="tempId"
            size="small"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="record.__empty_row">
                <span class="empty-placeholder">&nbsp;</span>
              </template>
              <template v-else-if="column.key === 'summary'">
                <a-input v-model:value="record.summary" placeholder="摘要" size="small" />
              </template>
              <template v-else-if="column.key === 'subject'">
                <a-input v-model:value="record.subjectName" placeholder="科目名称" size="small" />
              </template>
              <template v-else-if="column.key === 'debitAmount'">
                <a-input-number
                  v-model:value="record.debitAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  size="small"
                  placeholder="0.00"
                />
              </template>
              <template v-else-if="column.key === 'creditAmount'">
                <a-input-number
                  v-model:value="record.creditAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  size="small"
                  placeholder="0.00"
                />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" danger @click="handleRemoveEntry(index)">
                  <DeleteOutlined />
                </a-button>
              </template>
            </template>
            <template #summary>
              <a-table-summary-row>
                <a-table-summary-cell :index="0" :col-span="2">合计</a-table-summary-cell>
                <a-table-summary-cell :index="2">
                  <strong class="amount-cell debit">¥{{ getTotalDebit() }}</strong>
                </a-table-summary-cell>
                <a-table-summary-cell :index="3">
                  <strong class="amount-cell credit">¥{{ getTotalCredit() }}</strong>
                </a-table-summary-cell>
                <a-table-summary-cell :index="4" />
              </a-table-summary-row>
            </template>
          </a-table>
        </div>
      </a-modal>

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

          <a-table
            :columns="entryViewColumns"
            :data-source="currentVoucher.entries || []"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'debitAmount'">
                <span class="amount-cell debit">¥{{ formatAmount(record.debitAmount) }}</span>
              </template>
              <template v-else-if="column.key === 'creditAmount'">
                <span class="amount-cell credit">¥{{ formatAmount(record.creditAmount) }}</span>
              </template>
            </template>
            <template #summary>
              <a-table-summary-row>
                <a-table-summary-cell :index="0" :col-span="2">合计</a-table-summary-cell>
                <a-table-summary-cell :index="2">
                  <strong class="amount-cell debit">¥{{ formatAmount(currentVoucher.debitTotal) }}</strong>
                </a-table-summary-cell>
                <a-table-summary-cell :index="3">
                  <strong class="amount-cell credit">¥{{ formatAmount(currentVoucher.creditTotal) }}</strong>
                </a-table-summary-cell>
              </a-table-summary-row>
            </template>
          </a-table>

          <div class="detail-modal-footer">
            <a-button v-if="currentVoucher.status === 0" type="primary" @click="handleAudit(currentVoucher)">
              审核
            </a-button>
            <a-button v-if="currentVoucher.status === 1" type="primary" @click="handlePost(currentVoucher)">
              过账
            </a-button>
            <a-button v-if="currentVoucher.status === 2" type="primary" danger @click="handleReverse(currentVoucher)">
              冲销
            </a-button>
            <a-button @click="handlePrint(currentVoucher)">
              <template #icon><PrinterOutlined /></template>
              打印
            </a-button>
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
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SearchOutlined, PlusOutlined, DeleteOutlined, EyeOutlined,
  CheckCircleOutlined, SendOutlined, RollbackOutlined, EllipsisOutlined,
  InboxOutlined, PrinterOutlined, EditOutlined, DollarOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import { voucherApi } from '@/api/finance'

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
  status: number
  debitTotal: number
  creditTotal: number
  createdBy: string
  summary?: string
  entries?: VoucherEntry[]
}

const tableRef = ref()
const loading = ref(false)
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

const searchForm = reactive({
  fiscalYear: dayjs().year(),
  fiscalPeriod: undefined as number | undefined,
  status: undefined as number | undefined,
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
  const draft = tableData.value.filter(r => r.status === 0).length
  const audited = tableData.value.filter(r => r.status === 1).length
  const posted = tableData.value.filter(r => r.status === 2).length
  const reversed = tableData.value.filter(r => r.status === 3).length
  return { draft, audited, posted, reversed }
})

const totalDebit = computed(() => {
  return tableData.value.filter(r => r.status === 2).reduce((sum, r) => sum + r.debitTotal, 0)
})

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` } as any)
  }
  return data
})

// 表单空行填充
const MIN_FORM_ROWS = 8
const formTableItems = computed(() => {
  const data = [...addForm.entries]
  const emptyCount = Math.max(0, MIN_FORM_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, tempId: `__empty_form_${i}` } as any)
  }
  return data
})

const filterFields = [
  { key: 'fiscalYear', label: '年度', type: 'input' as const, placeholder: '年度', defaultValue: dayjs().year() },
  { key: 'fiscalPeriod', label: '期间', type: 'select' as const, options: Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })), placeholder: '期间' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已过账', value: 2 },
    { label: '已冲销', value: 3 }
  ]},
  { key: 'voucherNo', label: '凭证号', type: 'input' as const, placeholder: '凭证号' }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'processing', 2: 'success', 3: 'error' }
const statusLabelMap: Record<number, string> = { 0: '草稿', 1: '已审核', 2: '已过账', 3: '已冲销' }

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
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${statusColorMap[cellValue] || 'default'}">${statusLabelMap[cellValue] || '未知'}</span>`,
  },
  { field: 'createdBy', title: '制单人', width: 100 },
  { field: 'action', title: '操作', width: 180, fixed: 'right', type: 'action' },
])

const entryColumns = [
  { title: '摘要', key: 'summary', width: 180 },
  { title: '会计科目', key: 'subject', width: 180 },
  { title: '借方金额', key: 'debitAmount', width: 130 },
  { title: '贷方金额', key: 'creditAmount', width: 130 },
  { title: '操作', key: 'action', width: 60 }
]

const entryViewColumns = [
  { title: '摘要', dataIndex: 'summary' },
  { title: '会计科目', dataIndex: 'subjectName' },
  { title: '借方金额', key: 'debitAmount', width: 130, align: 'right' },
  { title: '贷方金额', key: 'creditAmount', width: 130, align: 'right' }
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

function formatAmount(val: number): string {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const fetchData = async () => {
  loading.value = true
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
      tableData.value = res.data.records || res.data.list || mockData()
      pagination.total = res.data.total || mockData().length
      lastUpdated.value = new Date().toISOString()
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    message.error('加载凭证数据失败')
    tableData.value = mockData()
    pagination.total = mockData().length
  } finally {
    loading.value = false
  }
}

const mockData = (): Voucher[] => [
  { id: 1, voucherNo: 'V2024010001', voucherDate: '2024-01-10', fiscalYear: 2024, fiscalPeriod: 1, status: 2, debitTotal: 50000, creditTotal: 50000, createdBy: '张三', summary: '采购入库' },
  { id: 2, voucherNo: 'V2024010002', voucherDate: '2024-01-15', fiscalYear: 2024, fiscalPeriod: 1, status: 1, debitTotal: 32000, creditTotal: 32000, createdBy: '李四', summary: '销售收款' },
  { id: 3, voucherNo: 'V2024010003', voucherDate: '2024-01-20', fiscalYear: 2024, fiscalPeriod: 1, status: 0, debitTotal: 15000, creditTotal: 15000, createdBy: '王五', summary: '费用报销' },
  { id: 4, voucherNo: 'V2024010004', voucherDate: '2024-01-12', fiscalYear: 2024, fiscalPeriod: 1, status: 3, debitTotal: 8000, creditTotal: 8000, createdBy: '张三', summary: '已冲销凭证' },
  { id: 5, voucherNo: 'V2024010005', voucherDate: '2024-01-22', fiscalYear: 2024, fiscalPeriod: 1, status: 2, debitTotal: 28000, creditTotal: 28000, createdBy: '李四', summary: '工资发放' }
]

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
  try { await addFormRef.value?.validate() } catch { return }
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
  } catch {
    message.error('创建凭证失败')
  } finally {
    addModalLoading.value = false
  }
}

const handleAddModalCancel = () => {
  addModalVisible.value = false
  addFormRef.value?.resetFields()
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
      } catch {
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
      } catch {
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
  } catch {
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
  } catch {
    currentVoucher.value = {
      ...record,
      entries: [
        { id: 1, summary: record.summary, subjectName: '银行存款', debitAmount: record.debitTotal, creditAmount: 0 },
        { id: 2, summary: record.summary, subjectName: '主营业务收入', debitAmount: 0, creditAmount: record.creditTotal }
      ]
    }
    detailVisible.value = true
  }
}

const handlePrint = (record: Voucher) => {
  message.info(`打印凭证: ${record.voucherNo}`)
}

const handleActionMenuClick = (key: string, record: Voucher) => {
  switch (key) {
    case 'audit': handleAudit(record); break
    case 'post': handlePost(record); break
    case 'reverse': handleReverse(record); break
    case 'print': handlePrint(record); break
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
  message.success('导出成功')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.finance-voucher-page {
  height: 100%;
  display: flex;
  flex-direction: column;
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

.empty-placeholder {
  color: transparent;
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

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
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
</style>