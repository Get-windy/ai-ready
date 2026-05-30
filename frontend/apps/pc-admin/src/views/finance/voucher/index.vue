<template>
  <div class="voucher-management">
    <!-- 搜索区域 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchForm" class="search-form">
        <a-row :gutter="16" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="凭证号">
              <a-input
                v-model:value="searchForm.voucherNo"
                placeholder="请输入凭证号"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="日期范围">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                style="width: 100%"
                :placeholder="['开始日期', '结束日期']"
                format="YYYY-MM-DD"
                :valueFormat="['startDate', 'endDate']"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="审核状态">
              <a-select
                v-model:value="searchForm.auditStatus"
                placeholder="请选择审核状态"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="0">待审核</a-select-option>
                <a-select-option :value="1">已审核</a-select-option>
                <a-select-option :value="2">已驳回</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="过账状态">
              <a-select
                v-model:value="searchForm.postStatus"
                placeholder="请选择过账状态"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="0">未过账</a-select-option>
                <a-select-option :value="1">已过账</a-select-option>
              </a-select>
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

    <!-- 表格区域 -->
    <a-card class="table-card" :bordered="false">
      <template #title>
        <div class="table-header">
          <span class="title">会计凭证</span>
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              新增凭证
            </a-button>
            <a-button danger :disabled="!selectedRowKeys.length" @click="handleBatchDelete">
              <template #icon><DeleteOutlined /></template>
              批量删除
            </a-button>
          </a-space>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
        row-key="id"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'auditStatus'">
            <a-tag :color="getAuditStatusColor(record.auditStatus)">
              {{ getAuditStatusName(record.auditStatus) }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'postStatus'">
            <a-tag :color="record.postStatus === 1 ? 'success' : 'default'">
              {{ record.postStatus === 1 ? '已过账' : '未过账' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'debitAmount'">
            {{ formatAmount(record.debitAmount) }}
          </template>

          <template v-else-if="column.key === 'creditAmount'">
            {{ formatAmount(record.creditAmount) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                :disabled="record.auditStatus !== 0"
                @click="handleAudit(record)"
              >
                审核
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="record.auditStatus !== 1 || record.postStatus === 1"
                @click="handlePost(record)"
              >
                过账
              </a-button>
              <a-dropdown>
                <a-button type="link" size="small">
                  更多<DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item
                      :disabled="record.postStatus === 1"
                      @click="handleEdit(record)"
                    >
                      <EditOutlined /> 编辑
                    </a-menu-item>
                    <a-menu-item @click="handleDetail(record)">
                      <FileTextOutlined /> 详情
                    </a-menu-item>
                    <a-menu-item
                      :disabled="record.auditStatus !== 1"
                      @click="handleReject(record)"
                    >
                      <CloseCircleOutlined /> 驳回
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item danger @click="handleDelete(record)">
                      <DeleteOutlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 凭证表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="800px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="凭证日期" name="voucherDate">
          <a-date-picker
            v-model:value="formState.voucherDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            placeholder="请选择日期"
          />
        </a-form-item>
        <a-form-item label="摘要" name="summary">
          <a-input
            v-model:value="formState.summary"
            placeholder="请输入摘要"
          />
        </a-form-item>
      </a-form>

      <a-divider orientation="left">凭证分录</a-divider>

      <div class="entry-section">
        <a-button type="dashed" block @click="handleAddEntry" style="margin-bottom: 12px">
          <template #icon><PlusOutlined /></template>
          添加分录行
        </a-button>

        <a-table
          :columns="entryColumns"
          :data-source="formState.entries"
          :pagination="false"
          row-key="sortOrder"
          size="small"
        >
          <template #bodyCell="{ column, record: entry, index }">
            <template v-if="column.key === 'accountCode'">
              <a-input
                v-model:value="entry.accountCode"
                placeholder="科目编码"
                size="small"
              />
            </template>
            <template v-else-if="column.key === 'accountName'">
              <a-input
                v-model:value="entry.accountName"
                placeholder="科目名称"
                size="small"
              />
            </template>
            <template v-else-if="column.key === 'summary'">
              <a-input
                v-model:value="entry.summary"
                placeholder="摘要"
                size="small"
              />
            </template>
            <template v-else-if="column.key === 'debitAmount'">
              <a-input-number
                v-model:value="entry.debitAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
                size="small"
                placeholder="0.00"
              />
            </template>
            <template v-else-if="column.key === 'creditAmount'">
              <a-input-number
                v-model:value="entry.creditAmount"
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
              <a-table-summary-cell :index="0" :col-span="3">合计</a-table-summary-cell>
              <a-table-summary-cell :index="3">
                <strong>{{ getTotalDebit() }}</strong>
              </a-table-summary-cell>
              <a-table-summary-cell :index="4">
                <strong>{{ getTotalCredit() }}</strong>
              </a-table-summary-cell>
              <a-table-summary-cell :index="5" />
            </a-table-summary-row>
          </template>
        </a-table>
      </div>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="凭证详情"
      :footer="null"
      width="800px"
    >
      <a-descriptions v-if="currentVoucher" :column="2" bordered size="small">
        <a-descriptions-item label="凭证号">{{ currentVoucher.voucherNo }}</a-descriptions-item>
        <a-descriptions-item label="凭证日期">{{ currentVoucher.voucherDate }}</a-descriptions-item>
        <a-descriptions-item label="摘要" :span="2">{{ currentVoucher.summary }}</a-descriptions-item>
        <a-descriptions-item label="审核状态">
          <a-tag :color="getAuditStatusColor(currentVoucher.auditStatus)">
            {{ getAuditStatusName(currentVoucher.auditStatus) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="过账状态">
          <a-tag :color="currentVoucher.postStatus === 1 ? 'success' : 'default'">
            {{ currentVoucher.postStatus === 1 ? '已过账' : '未过账' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="制单人">{{ currentVoucher.creatorName }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentVoucher.createTime }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>分录明细</a-divider>
      <a-table
        v-if="currentVoucher"
        :columns="entryColumns"
        :data-source="currentVoucher.entries || []"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record: entry }">
          <template v-if="column.key === 'accountCode'">{{ entry.accountCode }}</template>
          <template v-else-if="column.key === 'accountName'">{{ entry.accountName }}</template>
          <template v-else-if="column.key === 'summary'">{{ entry.summary }}</template>
          <template v-else-if="column.key === 'debitAmount'">{{ formatAmount(entry.debitAmount) }}</template>
          <template v-else-if="column.key === 'creditAmount'">{{ formatAmount(entry.creditAmount) }}</template>
        </template>
      </a-table>
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
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  FileTextOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import { voucherApi, type VoucherInfo, type VoucherEntry } from '@/api/voucher'

// 搜索表单
const searchForm = reactive({
  voucherNo: '',
  dateRange: undefined as [string, string] | undefined,
  auditStatus: undefined as number | undefined,
  postStatus: undefined as number | undefined
})

// 表格数据
const tableData = ref<VoucherInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列
const columns: TableProps['columns'] = [
  { title: '凭证号', dataIndex: 'voucherNo', width: 140 },
  { title: '凭证日期', dataIndex: 'voucherDate', width: 120 },
  { title: '摘要', dataIndex: 'summary', width: 200, ellipsis: true },
  { title: '借方金额', key: 'debitAmount', width: 120 },
  { title: '贷方金额', key: 'creditAmount', width: 120 },
  { title: '制单人', dataIndex: 'creatorName', width: 100 },
  { title: '审核状态', key: 'auditStatus', width: 100 },
  { title: '过账状态', key: 'postStatus', width: 100 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]

// 分录列的表格列定义
const entryColumns: TableProps['columns'] = [
  { title: '科目编码', key: 'accountCode', width: 120 },
  { title: '科目名称', key: 'accountName', width: 150 },
  { title: '摘要', key: 'summary', width: 150 },
  { title: '借方金额', key: 'debitAmount', width: 120 },
  { title: '贷方金额', key: 'creditAmount', width: 120 },
  { title: '操作', key: 'action', width: 60 }
]

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => isEdit.value ? '编辑凭证' : '新增凭证')

const formState = reactive({
  id: 0,
  voucherDate: '',
  summary: '',
  entries: [] as VoucherEntry[]
})

const formRules = {
  voucherDate: { required: true, message: '请选择凭证日期', trigger: 'change' },
  summary: { required: true, message: '请输入摘要', trigger: 'blur' }
}

// 详情弹窗
const detailVisible = ref(false)
const currentVoucher = ref<VoucherInfo | null>(null)

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    delete params.dateRange

    const res = await voucherApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    voucherNo: '',
    dateRange: undefined,
    auditStatus: undefined,
    postStatus: undefined
  })
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

const onSelectChange = (keys: (string | number)[]) => {
  selectedRowKeys.value = keys as number[]
}

// 新增凭证
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    voucherDate: '',
    summary: '',
    entries: []
  })
  modalVisible.value = true
}

// 编辑凭证
const handleEdit = (record: VoucherInfo) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    voucherDate: record.voucherDate,
    summary: record.summary,
    entries: record.entries ? [...record.entries] : []
  })
  modalVisible.value = true
}

// 删除凭证
const handleDelete = (record: VoucherInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除凭证 "${record.voucherNo}" 吗？`,
    async onOk() {
      try {
        await voucherApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个凭证吗？`,
    async onOk() {
      try {
        await voucherApi.batchDelete(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

// 审核凭证
const handleAudit = async (record: VoucherInfo) => {
  Modal.confirm({
    title: '确认审核',
    content: `确定要通过凭证 "${record.voucherNo}" 的审核吗？`,
    async onOk() {
      try {
        await voucherApi.audit(record.id)
        message.success('审核通过')
        fetchData()
      } catch {
        message.error('审核失败')
      }
    }
  })
}

// 驳回凭证
const handleReject = async (record: VoucherInfo) => {
  Modal.confirm({
    title: '驳回凭证',
    content: `确定要驳回凭证 "${record.voucherNo}" 吗？`,
    async onOk() {
      try {
        await voucherApi.reject(record.id, '')
        message.success('已驳回')
        fetchData()
      } catch {
        message.error('驳回失败')
      }
    }
  })
}

// 过账
const handlePost = async (record: VoucherInfo) => {
  Modal.confirm({
    title: '确认过账',
    content: `确定要将凭证 "${record.voucherNo}" 过账吗？过账后将无法修改。`,
    async onOk() {
      try {
        await voucherApi.post(record.id)
        message.success('过账成功')
        fetchData()
      } catch {
        message.error('过账失败')
      }
    }
  })
}

// 详情
const handleDetail = async (record: VoucherInfo) => {
  try {
    const res = await voucherApi.getById(record.id)
    if (res.data) {
      currentVoucher.value = res.data
      detailVisible.value = true
    }
  } catch {
    message.error('加载详情失败')
  }
}

// 分录操作
const handleAddEntry = () => {
  formState.entries.push({
    accountCode: '',
    accountName: '',
    summary: '',
    debitAmount: 0,
    creditAmount: 0,
    sortOrder: formState.entries.length + 1
  })
}

const handleRemoveEntry = (index: number) => {
  formState.entries.splice(index, 1)
}

const getTotalDebit = () => {
  return formState.entries.reduce((sum, e) => sum + (e.debitAmount || 0), 0).toFixed(2)
}

const getTotalCredit = () => {
  return formState.entries.reduce((sum, e) => sum + (e.creditAmount || 0), 0).toFixed(2)
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    const data = {
      voucherDate: formState.voucherDate,
      summary: formState.summary,
      entries: formState.entries
    }

    if (isEdit.value) {
      await voucherApi.update({ id: formState.id, ...data })
      message.success('更新成功')
    } else {
      await voucherApi.create(data)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 辅助函数
const getAuditStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'error' }
  return colors[status] || 'default'
}

const getAuditStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '待审核', 1: '已审核', 2: '已驳回' }
  return names[status] || '未知'
}

const formatAmount = (amount: number) => {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.voucher-management {
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

@media (max-width: 768px) {
  .search-form :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  .table-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
