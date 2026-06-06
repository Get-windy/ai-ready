<template>
  <div class="inventory-list">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-inventory-list'"
      :filter-fields="filterFields"
      add-text="新增盘点"
      @add="showCreateModal"
      @edit="editRecord"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>
      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配盘点记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无盘点记录">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button @click="showCreateModal">新增盘点</a-button>
        </a-empty>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
            {{ record.status === 'completed' ? '已完成' : '待盘点' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'checkResult'">
          <a-tag :color="resultColorMap[record.checkResult]">
            {{ resultMap[record.checkResult] || record.checkResult }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space :size="0" class="action-cell-inner">
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="viewDetail(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'pending'" title="编辑">
              <a-button type="link" size="small" @click="editRecord(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </template>
    </TableList>

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
import { SearchOutlined, InboxOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import { inventoryApi } from '@/api/fixed-asset'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref([])
const tableRef = ref()
const lastUpdated = ref('')

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

const columns = [
  { title: '盘点单号', dataIndex: 'inventoryNo', width: 150 },
  { title: '资产编码', dataIndex: 'assetCode', width: 120 },
  { title: '资产名称', dataIndex: 'assetName', width: 160 },
  { title: '盘点日期', dataIndex: 'inventoryDate', width: 120 },
  { title: '部门', dataIndex: 'departmentName', width: 120 },
  { title: '预期位置', dataIndex: 'expectedLocation', width: 130 },
  { title: '实际位置', dataIndex: 'actualLocation', width: 130 },
  { title: '预期状态', dataIndex: 'expectedStatus', width: 100 },
  { title: '实际状态', dataIndex: 'actualStatus', width: 100 },
  { title: '盘点结果', dataIndex: 'checkResult', key: 'checkResult', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

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
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-more-btn {
  border: none; box-shadow: none; padding: 4px 8px;
}
.action-cell-inner {
  display: inline-flex; align-items: center;
}
</style>
