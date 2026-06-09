<template>
  <div class="inventory-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.pending }}</div>
          <div class="stat-card-label">待盘点</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.completed }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-mismatch">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ resultCounts.mismatch }}</div>
          <div class="stat-card-label">差异记录</div>
        </div>
        <ExclamationCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">盘点记录数</div>
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
      :table-key="'fixed-asset-inventory-list'"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新增盘点"
      @add="showCreateModal"
      @edit="editRecord"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>
      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的盘点记录，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无盘点记录，点击右上角「新增盘点」开始创建
          </p>
        </div>
      </template>
      <template #inventoryNoCell="{ record }">
        <a @click="viewDetail(record)" class="inventory-no">{{ record.inventoryNo }}</a>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
          {{ record.status === 'completed' ? '已完成' : '待盘点' }}
        </a-tag>
      </template>
      <template #checkResultCell="{ record }">
        <a-tag :color="resultColorMap[record.checkResult]">
          {{ resultMap[record.checkResult] || record.checkResult }}
        </a-tag>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="viewDetail(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 'pending'" title="编辑">
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
                <a-menu-item v-if="record.status === 'pending'" key="complete">
                  <CheckCircleOutlined /> 完成盘点
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
    </VxeTableList>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑盘点记录' : '新增盘点记录'"
      :width="700"
      @ok="handleModalOk"
      :confirmLoading="modalLoading"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="资产ID" required>
          <a-input-number v-model:value="formData.assetId" :min="1" style="width: 100%" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="资产编码">
              <a-input v-model:value="formData.assetCode" placeholder="资产编码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="资产名称">
              <a-input v-model:value="formData.assetName" placeholder="资产名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="盘点日期">
              <a-date-picker v-model:value="formData.inventoryDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门">
              <a-input v-model:value="formData.departmentName" placeholder="部门名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="预期位置">
              <a-input v-model:value="formData.expectedLocation" placeholder="预期位置" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实际位置">
              <a-input v-model:value="formData.actualLocation" placeholder="实际位置" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="预期状态">
              <a-select v-model:value="formData.expectedStatus" placeholder="预期状态">
                <a-select-option value="in_use">使用中</a-select-option>
                <a-select-option value="idle">闲置</a-select-option>
                <a-select-option value="maintenance">维修中</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实际状态">
              <a-select v-model:value="formData.actualStatus" placeholder="实际状态">
                <a-select-option value="in_use">使用中</a-select-option>
                <a-select-option value="idle">闲置</a-select-option>
                <a-select-option value="maintenance">维修中</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="预期保管人">
              <a-input v-model:value="formData.expectedCustodian" placeholder="预期保管人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实际保管人">
              <a-input v-model:value="formData.actualCustodian" placeholder="实际保管人" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="盘点结果">
          <a-select v-model:value="formData.checkResult" placeholder="选择结果">
            <a-select-option value="consistent">一致</a-select-option>
            <a-select-option value="mismatch">不符</a-select-option>
            <a-select-option value="missing">盘亏</a-select-option>
            <a-select-option value="surplus">盘盈</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, EyeOutlined, EditOutlined,
  ClockCircleOutlined, CheckCircleOutlined, ExclamationCircleOutlined,
  FileTextOutlined, EllipsisOutlined, PrinterOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inventoryApi } from '@/api/fixed-asset'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const selectedRowKeys = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const formData = reactive({
  assetId: undefined as number | undefined,
  assetCode: '',
  assetName: '',
  inventoryDate: undefined as any,
  departmentId: '',
  departmentName: '',
  expectedLocation: '',
  actualLocation: '',
  expectedStatus: 'in_use',
  actualStatus: 'in_use',
  expectedCustodian: '',
  actualCustodian: '',
  checkResult: 'consistent',
  remark: '',
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
  const pending = tableData.value.filter(r => r.status === 'pending').length
  const completed = tableData.value.filter(r => r.status === 'completed').length
  return { pending, completed }
})

const resultCounts = computed(() => {
  const mismatch = tableData.value.filter(r => r.checkResult === 'mismatch' || r.checkResult === 'missing' || r.checkResult === 'surplus').length
  return { mismatch }
})

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'inventoryNo', title: '盘点单号', width: 150, slotName: 'inventoryNoCell' },
  { field: 'assetCode', title: '资产编码', width: 120 },
  { field: 'assetName', title: '资产名称', width: 160 },
  { field: 'inventoryDate', title: '盘点日期', width: 120 },
  { field: 'departmentName', title: '部门', width: 120 },
  { field: 'expectedLocation', title: '预期位置', width: 130 },
  { field: 'actualLocation', title: '实际位置', width: 130 },
  { field: 'expectedStatus', title: '预期状态', width: 100 },
  { field: 'actualStatus', title: '实际状态', width: 100 },
  { field: 'checkResult', title: '盘点结果', width: 100, slotName: 'checkResultCell' },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { type: 'action', title: '操作', width: 120, fixed: 'right' },
])

const filterFields = [
  { key: 'inventoryNo', label: '盘点单号', type: 'input' as const, placeholder: '盘点单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待盘点', value: 'pending' },
    { label: '已完成', value: 'completed' },
  ]},
  { key: 'checkResult', label: '盘点结果', type: 'select' as const, options: [
    { label: '一致', value: 'consistent' },
    { label: '不符', value: 'mismatch' },
    { label: '盘亏', value: 'missing' },
    { label: '盘盈', value: 'surplus' },
  ]},
]

const resultMap: Record<string, string> = {
  consistent: '一致', mismatch: '不符', missing: '盘亏', surplus: '盘盈',
}
const resultColorMap: Record<string, string> = {
  consistent: 'green', mismatch: 'orange', missing: 'red', surplus: 'blue',
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.inventoryNo) params.inventoryNo = searchFilters.inventoryNo
  if (searchFilters.status) params.status = searchFilters.status
  if (searchFilters.checkResult) params.checkResult = searchFilters.checkResult

  inventoryApi.getPage(params).then((res: any) => {
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
    lastUpdated.value = new Date().toISOString()
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
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
    assetId: undefined,
    assetCode: '',
    assetName: '',
    inventoryDate: undefined,
    departmentId: '',
    departmentName: '',
    expectedLocation: '',
    actualLocation: '',
    expectedStatus: 'in_use',
    actualStatus: 'in_use',
    expectedCustodian: '',
    actualCustodian: '',
    checkResult: 'consistent',
    remark: '',
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
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? inventoryApi.update(editId.value, formData)
    : inventoryApi.create(formData)

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

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.inventory-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
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

.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-mismatch { background: linear-gradient(135deg, #fff1f0 0%, #ffa39e 100%); }
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

.inventory-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
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
