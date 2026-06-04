<template>
  <div class="finance-voucher-page">
    <!-- 搜索区域 -->
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="searchForm" class="search-form">
        <a-row :gutter="16" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="年度">
              <a-input-number
                v-model:value="searchForm.fiscalYear"
                :min="2020"
                :max="2099"
                placeholder="年度"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="期间">
              <a-select v-model:value="searchForm.fiscalPeriod" placeholder="期间" allow-clear style="width: 100%">
                <a-select-option v-for="p in 12" :key="p" :value="p">{{ p }}月</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="状态">
              <a-select v-model:value="searchForm.status" placeholder="状态" allow-clear style="width: 100%">
                <a-select-option :value="0">草稿</a-select-option>
                <a-select-option :value="1">已审核</a-select-option>
                <a-select-option :value="2">已过账</a-select-option>
                <a-select-option :value="3">已冲销</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="凭证号">
              <a-input v-model:value="searchForm.voucherNo" placeholder="凭证号" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 操作栏 -->
    <a-card :bordered="false" class="table-card">
      <template #title>
        <div class="table-header">
          <span class="title">会计凭证</span>
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              新增凭证
            </a-button>
          </a-space>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColorMap[record.status] || 'default'">
              {{ statusLabelMap[record.status] || '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'debitTotal'">
            {{ formatAmount(record.debitTotal) }}
          </template>
          <template v-else-if="column.key === 'creditTotal'">
            {{ formatAmount(record.creditTotal) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                :disabled="record.status !== 0"
                @click="handleAudit(record)"
              >
                审核
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="record.status !== 1"
                @click="handlePost(record)"
              >
                过账
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="record.status !== 2"
                @click="handleReverse(record)"
              >
                冲销
              </a-button>
              <a-button type="link" size="small" @click="handleView(record)">
                查看
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增凭证弹窗 -->
    <a-modal
      v-model:open="addModalVisible"
      title="新增凭证"
      :confirm-loading="addModalLoading"
      width="900px"
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
          :data-source="addForm.entries"
          :pagination="false"
          row-key="tempId"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'summary'">
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
                <strong>{{ getTotalDebit() }}</strong>
              </a-table-summary-cell>
              <a-table-summary-cell :index="3">
                <strong>{{ getTotalCredit() }}</strong>
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

        <!-- 摘要 -->
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
              {{ formatAmount(record.debitAmount) }}
            </template>
            <template v-else-if="column.key === 'creditAmount'">
              {{ formatAmount(record.creditAmount) }}
            </template>
          </template>
          <template #summary>
            <a-table-summary-row>
              <a-table-summary-cell :index="0" :col-span="2">合计</a-table-summary-cell>
              <a-table-summary-cell :index="2">
                <strong>{{ formatAmount(currentVoucher.debitTotal) }}</strong>
              </a-table-summary-cell>
              <a-table-summary-cell :index="3">
                <strong>{{ formatAmount(currentVoucher.creditTotal) }}</strong>
              </a-table-summary-cell>
            </a-table-summary-row>
          </template>
        </a-table>

        <div style="text-align: center; margin-top: 16px">
          <a-space>
            <a-button
              type="primary"
              :disabled="currentVoucher.status !== 0"
              @click="handleAudit(currentVoucher)"
            >
              审核
            </a-button>
            <a-button
              type="primary"
              :disabled="currentVoucher.status !== 1"
              @click="handlePost(currentVoucher)"
            >
              过账
            </a-button>
            <a-button
              type="primary"
              danger
              :disabled="currentVoucher.status !== 2"
              @click="handleReverse(currentVoucher)"
            >
              冲销
            </a-button>
          </a-space>
        </div>
      </template>
    </a-modal>

    <!-- 冲销原因弹窗 -->
    <a-modal
      v-model:open="reverseModalVisible"
      title="冲销凭证"
      :confirm-loading="reverseLoading"
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
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
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

const loading = ref(false)
const tableData = ref<Voucher[]>([])
const addModalVisible = ref(false)
const addModalLoading = ref(false)
const detailVisible = ref(false)
const currentVoucher = ref<Voucher | null>(null)
const reverseModalVisible = ref(false)
const reverseLoading = ref(false)
const reverseTarget = ref<Voucher | Record<string, any> | null>(null)
const reverseReason = ref('')
const addFormRef = ref<FormInstance>()

const searchForm = reactive({
  fiscalYear: dayjs().year(),
  fiscalPeriod: undefined as number | undefined,
  status: undefined as number | undefined,
  voucherNo: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const statusColorMap: Record<number, string> = {
  0: 'default',
  1: 'processing',
  2: 'success',
  3: 'error'
}

const statusLabelMap: Record<number, string> = {
  0: '草稿',
  1: '已审核',
  2: '已过账',
  3: '已冲销'
}

const columns: TableProps['columns'] = [
  { title: '凭证号', dataIndex: 'voucherNo', key: 'voucherNo', width: 140 },
  { title: '日期', dataIndex: 'voucherDate', key: 'voucherDate', width: 110 },
  { title: '摘要', key: 'summary', dataIndex: 'summary', ellipsis: true, width: 200 },
  { title: '借方总额', key: 'debitTotal', dataIndex: 'debitTotal', width: 120, align: 'right' },
  { title: '贷方总额', key: 'creditTotal', dataIndex: 'creditTotal', width: 120, align: 'right' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '制单人', dataIndex: 'createdBy', key: 'createdBy', width: 100 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

const entryColumns: TableProps['columns'] = [
  { title: '摘要', key: 'summary', width: 180 },
  { title: '会计科目', key: 'subject', width: 180 },
  { title: '借方金额', key: 'debitAmount', width: 130 },
  { title: '贷方金额', key: 'creditAmount', width: 130 },
  { title: '操作', key: 'action', width: 60 }
]

const entryViewColumns: TableProps['columns'] = [
  { title: '摘要', dataIndex: 'summary', key: 'summary' },
  { title: '会计科目', dataIndex: 'subjectName', key: 'subjectName' },
  { title: '借方金额', key: 'debitAmount', width: 130, align: 'right' },
  { title: '贷方金额', key: 'creditAmount', width: 130, align: 'right' }
]

let entryTempIdCounter = 0
const addForm: {
  voucherDate: any
  fiscalYear: number
  entries: VoucherEntry[]
} = reactive({
  voucherDate: undefined as any,
  fiscalYear: dayjs().year(),
  entries: [] as any[]
})

const addFormRules = {
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }]
} as any

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
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch {
    message.error('加载凭证数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.fiscalYear = dayjs().year()
  searchForm.fiscalPeriod = undefined
  searchForm.status = undefined
  searchForm.voucherNo = ''
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
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
  try {
    await addFormRef.value?.validate()
  } catch {
    return
  }
  if (addForm.entries.length === 0) {
    message.warning('请至少添加一条分录')
    return
  }
  addModalLoading.value = true
  try {
    const data = {
      voucherDate: addForm.voucherDate ? dayjs(addForm.voucherDate).format('YYYY-MM-DD') : '',
      fiscalYear: addForm.fiscalYear,
      entries: addForm.entries.map((e: VoucherEntry) => ({
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

const handleAudit = async (record: Voucher | Record<string, any>) => {
  Modal.confirm({
    title: '确认审核',
    content: `确定要审核凭证 "${record.voucherNo}" 吗？`,
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

const handlePost = async (record: Voucher | Record<string, any>) => {
  Modal.confirm({
    title: '确认过账',
    content: `确定要将凭证 "${record.voucherNo}" 过账吗？`,
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

const handleReverse = (record: Voucher | Record<string, any>) => {
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
    await voucherApi.reverse(reverseTarget.value.id, reverseReason.value)
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

const handleView = async (record: Voucher | Record<string, any>) => {
  try {
    const res = await voucherApi.getById(record.id)
    if (res.data) {
      currentVoucher.value = res.data
      detailVisible.value = true
    }
  } catch {
    message.error('获取凭证详情失败')
  }
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.finance-voucher-page {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: -24px;
}

.table-card :deep(.ant-card-head) {
  border-bottom: none;
  padding-bottom: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-header .title {
  font-size: 16px;
  font-weight: 500;
}

.entry-section {
  margin: 0 -24px;
  padding: 0 24px;
}
</style>
