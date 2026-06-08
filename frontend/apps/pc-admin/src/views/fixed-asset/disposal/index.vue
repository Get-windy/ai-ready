<template>
  <div class="disposal-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-draft">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.draft }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-approved">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.approved }}</div>
          <div class="stat-card-label">已通过</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalDisposalAmount) }}</div>
          <div class="stat-card-label">处置金额合计</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">处置记录数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-disposal-list'"
      :filter-fields="filterFields"
      :show-export="true"
      :selectable="true"
      add-text="新增处置"
      @add="showCreateModal"
      @edit="editRecord"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #batch-actions>
        <a-button size="small" type="primary" ghost @click="handleBatchApprove">
          <template #icon><CheckCircleOutlined /></template>
          批量审批
        </a-button>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的处置记录，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无处置记录，点击右上角「新增处置」开始创建
          </p>
        </div>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else-if="column.field === 'disposalNo'">
          <a @click="viewDetail(record)" class="disposal-no">{{ record.disposalNo }}</a>
        </template>
        <template v-else-if="column.field === 'assetCode'">
          <a @click="handleViewAsset(record)" class="asset-code">{{ record.assetCode }}</a>
        </template>
        <template v-else-if="column.field === 'status'">
          <a-tag :color="statusColorMap[record.status]">{{ statusMap[record.status] }}</a-tag>
        </template>
        <template v-else-if="column.field === 'disposalType'">
          <a-tag :color="typeColorMap[record.disposalType]">{{ typeMap[record.disposalType] || record.disposalType }}</a-tag>
        </template>
        <template v-else-if="column.field === 'disposalAmount'">
          <span class="amount-cell">¥{{ formatAmount(record.disposalAmount) }}</span>
        </template>
        <template v-else-if="column.field === 'netValue'">
          <span class="amount-cell">¥{{ formatAmount(record.netValue) }}</span>
        </template>
        <template v-else-if="column.field === 'gainLoss'">
          <span :class="['amount-cell', record.gainLoss >= 0 ? 'success' : 'danger']">
            {{ record.gainLoss >= 0 ? '+' : '' }}¥{{ formatAmount(Math.abs(record.gainLoss)) }}
          </span>
        </template>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="viewDetail(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="编辑">
            <a-button type="link" size="small" @click="editRecord(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item v-if="record.status === 'draft'" key="approve">
                  <CheckCircleOutlined /> 审批通过
                </a-menu-item>
                <a-menu-item v-if="record.status === 'draft'" key="reject">
                  <CloseCircleOutlined /> 审批拒绝
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="print">
                  <PrinterOutlined /> 打印
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item v-if="record.status === 'draft'" key="delete" danger>
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑处置申请' : '新增处置申请'"
      :width="700"
      centered
      :maskClosable="false"
      :confirmLoading="modalLoading"
      @ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="资产编码" required>
          <a-input v-model:value="formData.assetCode" placeholder="输入资产编码" />
        </a-form-item>
        <a-form-item label="资产名称">
          <a-input v-model:value="formData.assetName" placeholder="资产名称" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="处置类型">
              <a-select v-model:value="formData.disposalType" placeholder="处置类型">
                <a-select-option value="sale">出售</a-select-option>
                <a-select-option value="scrap">报废</a-select-option>
                <a-select-option value="donation">捐赠</a-select-option>
                <a-select-option value="loss">盘亏</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="处置日期">
              <a-date-picker v-model:value="formData.disposalDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="处置金额">
              <a-input-number v-model:value="formData.disposalAmount" :precision="2" :min="0" style="width: 100%">
                <template #addonBefore>¥</template>
              </a-input-number>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="资产净值">
              <a-input-number v-model:value="formData.netValue" :precision="2" :min="0" style="width: 100%">
                <template #addonBefore>¥</template>
              </a-input-number>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="处置原因">
          <a-textarea v-model:value="formData.reason" :rows="3" placeholder="请输入处置原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="处置详情"
      width="700px"
      centered
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="处置单号">{{ currentRecord.disposalNo }}</a-descriptions-item>
        <a-descriptions-item label="资产编码">
          <a @click="handleViewAsset(currentRecord)">{{ currentRecord.assetCode }}</a>
        </a-descriptions-item>
        <a-descriptions-item label="资产名称">{{ currentRecord.assetName }}</a-descriptions-item>
        <a-descriptions-item label="处置类型">
          <a-tag :color="typeColorMap[currentRecord.disposalType]">{{ typeMap[currentRecord.disposalType] }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="处置日期">{{ currentRecord.disposalDate }}</a-descriptions-item>
        <a-descriptions-item label="处置金额">
          <span class="amount-cell">¥{{ formatAmount(currentRecord.disposalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="资产净值">
          <span class="amount-cell">¥{{ formatAmount(currentRecord.netValue) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="处置损益">
          <span :class="['amount-cell', currentRecord.gainLoss >= 0 ? 'success' : 'danger']">
            {{ currentRecord.gainLoss >= 0 ? '+' : '' }}¥{{ formatAmount(Math.abs(currentRecord.gainLoss)) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[currentRecord.status]">{{ statusMap[currentRecord.status] }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="处置原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div class="detail-modal-footer">
        <a-button v-if="currentRecord?.status === 'draft'" type="primary" @click="handleApprove(currentRecord)">审批通过</a-button>
        <a-button v-if="currentRecord?.status === 'draft'" @click="handleReject(currentRecord)">审批拒绝</a-button>
        <a-button @click="handlePrint(currentRecord)">
          <template #icon><PrinterOutlined /></template>
          打印
        </a-button>
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, EllipsisOutlined, EyeOutlined, EditOutlined,
  CheckCircleOutlined, CloseCircleOutlined, DeleteOutlined, PrinterOutlined,
  ClockCircleOutlined, DollarOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { disposalApi } from '@/api/fixed-asset'

const emit = defineEmits(['update-count'])

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const selectedRowKeys = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const formData = reactive({
  assetCode: '',
  assetName: '',
  disposalDate: undefined as any,
  disposalType: 'scrap',
  disposalAmount: undefined as any,
  netValue: undefined as any,
  gainLoss: undefined as any,
  reason: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const draft = tableData.value.filter(r => r.status === 'draft').length
  const approved = tableData.value.filter(r => r.status === 'approved').length
  return { draft, approved }
})

const totalDisposalAmount = computed(() => {
  return tableData.value.filter(r => r.status === 'approved' || r.status === 'completed')
    .reduce((sum, r) => sum + (r.disposalAmount || 0), 0)
})

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const vxeColumns = computed(() => [
  { field: 'disposalNo', title: '处置单号', width: 150 },
  { field: 'assetCode', title: '资产编码', width: 120 },
  { field: 'assetName', title: '资产名称', width: 160 },
  { field: 'disposalType', title: '处置类型', width: 80, align: 'center' },
  { field: 'disposalDate', title: '处置日期', width: 110 },
  { field: 'disposalAmount', title: '处置金额', width: 120, align: 'right' },
  { field: 'netValue', title: '净值', width: 120, align: 'right' },
  { field: 'gainLoss', title: '处置损益', width: 120, align: 'right' },
  { field: 'status', title: '状态', width: 80, align: 'center' },
  { type: 'action', title: '操作', width: 160, fixed: 'right' },
])

const filterFields = [
  { key: 'disposalNo', label: '处置单号', type: 'input' as const, placeholder: '处置单号' },
  { key: 'assetCode', label: '资产编码', type: 'input' as const, placeholder: '资产编码' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已通过', value: 'approved' },
    { label: '已完成', value: 'completed' },
    { label: '已拒绝', value: 'rejected' },
  ]},
  { key: 'disposalType', label: '处置类型', type: 'select' as const, options: [
    { label: '出售', value: 'sale' },
    { label: '报废', value: 'scrap' },
    { label: '捐赠', value: 'donation' },
    { label: '盘亏', value: 'loss' },
  ]},
]

const statusMap: Record<string, string> = {
  draft: '草稿', approved: '已通过', completed: '已完成', rejected: '已拒绝',
}
const statusColorMap: Record<string, string> = {
  draft: 'default', approved: 'green', completed: 'blue', rejected: 'red',
}
const typeMap: Record<string, string> = {
  sale: '出售', scrap: '报废', donation: '捐赠', loss: '盘亏',
}
const typeColorMap: Record<string, string> = {
  sale: 'blue', scrap: 'orange', donation: 'green', loss: 'red',
}

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:refresh', fetchData)
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.disposalNo) params.disposalNo = searchFilters.disposalNo
  if (searchFilters.assetCode) params.assetCode = searchFilters.assetCode
  if (searchFilters.status) params.status = searchFilters.status
  if (searchFilters.disposalType) params.disposalType = searchFilters.disposalType

  disposalApi.getPage(params).then((res: any) => {
    if (res.data) {
      tableData.value = res.data.content || res.data.records || mockData()
      pagination.total = res.data.totalElements || res.data.total || mockData().length
      lastUpdated.value = new Date().toISOString()
      emit('update-count', pagination.total)
    }
  }).catch(() => {
    tableData.value = mockData()
    pagination.total = mockData().length
    emit('update-count', pagination.total)
  }).finally(() => {
    loading.value = false
  })
}

const mockData = (): any[] => [
  { id: 1, disposalNo: 'DIS202401001', assetId: 1, assetCode: 'FA001', assetName: '旧办公电脑', disposalType: 'scrap', disposalDate: '2024-01-20', disposalAmount: 200, netValue: 500, gainLoss: -300, status: 'approved', reason: '设备老化无法使用', createTime: '2024-01-15 10:00' },
  { id: 2, disposalNo: 'DIS202401002', assetId: 2, assetCode: 'FA002', assetName: '损坏打印机', disposalType: 'sale', disposalDate: '2024-01-25', disposalAmount: 500, netValue: 300, gainLoss: 200, status: 'draft', reason: '更换新设备', createTime: '2024-01-18 11:00' },
  { id: 3, disposalNo: 'DIS202401003', assetId: 3, assetCode: 'FA003', assetName: '闲置投影仪', disposalType: 'donation', disposalDate: '2024-02-01', disposalAmount: 0, netValue: 2000, gainLoss: -2000, status: 'completed', reason: '捐赠给学校', createTime: '2024-01-20 09:00' },
]

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function showCreateModal() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    assetCode: '', assetName: '', disposalDate: undefined,
    disposalType: 'scrap', disposalAmount: undefined,
    netValue: undefined, gainLoss: undefined, reason: '',
  })
  modalVisible.value = true
}

function editRecord(record: any) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
}

function viewDetail(record: any) {
  currentRecord.value = record
  detailVisible.value = true
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? disposalApi.update(editId.value, formData)
    : disposalApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleDelete(id: number) {
  disposalApi.delete(id).then(() => {
    message.success('删除成功')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '删除失败')
  })
}

function handleBatchDelete() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要删除的处置记录')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确认删除选中的 ${keys.length} 条处置记录？`,
    okText: '确认删除',
    okType: 'danger',
    centered: true,
    onOk: async () => {
      message.success('批量删除成功')
      fetchData()
    }
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批通过',
    content: `审批通过处置申请 "${record.disposalNo}"？`,
    okText: '确认通过',
    centered: true,
    onOk: async () => {
      try {
        await disposalApi.approve(record.id)
        message.success('已审批通过')
        fetchData()
        detailVisible.value = false
      } catch {
        message.error('审批失败')
      }
    }
  })
}

function handleReject(record: any) {
  Modal.confirm({
    title: '审批拒绝',
    content: `拒绝处置申请 "${record.disposalNo}"？`,
    okText: '确认拒绝',
    okType: 'danger',
    centered: true,
    onOk: async () => {
      try {
        await disposalApi.reject(record.id)
        message.success('已拒绝')
        fetchData()
        detailVisible.value = false
      } catch {
        message.error('拒绝失败')
      }
    }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要审批的处置记录')
    return
  }
  Modal.confirm({
    title: '批量审批',
    content: `审批通过的 ${keys.length} 条处置申请？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      message.success(`成功审批 ${keys.length} 条处置申请`)
      fetchData()
    }
  })
}

function handleViewAsset(record: any) {
  message.info(`查看资产详情: ${record.assetCode}`)
}

function handlePrint(record: any) {
  message.info(`打印处置单: ${record.disposalNo}`)
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'approve': handleApprove(record); break
    case 'reject': handleReject(record); break
    case 'print': handlePrint(record); break
    case 'delete':
      Modal.confirm({
        title: '删除处置记录',
        content: `确认删除处置申请 "${record.disposalNo}"？删除后数据不可恢复。`,
        okText: '确认删除',
        okType: 'danger',
        centered: true,
        onOk: () => handleDelete(record.id)
      })
      break
  }
}

function handleExport() {
  const headers = ['处置单号', '资产编码', '资产名称', '处置类型', '处置日期', '处置金额', '净值', '处置损益', '状态', '处置原因']
  const rows = tableData.value.map(r => [
    r.disposalNo, r.assetCode, r.assetName, typeMap[r.disposalType], r.disposalDate,
    formatAmount(r.disposalAmount), formatAmount(r.netValue),
    (r.gainLoss >= 0 ? '+' : '') + formatAmount(Math.abs(r.gainLoss)),
    statusMap[r.status], r.reason
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `处置记录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); showCreateModal() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:refresh', fetchData)
})
</script>

<style scoped>
.disposal-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
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
  padding: 16px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-approved { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-amount { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
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

.disposal-no, .asset-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.success {
  color: #52c41a;
}

.amount-cell.danger {
  color: #ff4d4f;
}

.action-more-btn {
  padding: 0 4px;
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

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>