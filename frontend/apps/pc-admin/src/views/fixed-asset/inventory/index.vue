<template>
  <div class="inventory-list">
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="盘点单号">
          <a-input v-model:value="searchForm.inventoryNo" placeholder="盘点单号" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="选择状态" allow-clear style="width: 120px">
            <a-select-option value="pending">待盘点</a-select-option>
            <a-select-option value="completed">已完成</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="盘点结果">
          <a-select v-model:value="searchForm.checkResult" placeholder="选择结果" allow-clear style="width: 120px">
            <a-select-option value="consistent">一致</a-select-option>
            <a-select-option value="mismatch">不符</a-select-option>
            <a-select-option value="missing">盘亏</a-select-option>
            <a-select-option value="surplus">盘盈</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button type="primary" ghost @click="showCreateModal">新增盘点</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        rowKey="id"
      >
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
            <a-space>
              <a @click="viewDetail(record)">查看</a>
              <a v-if="record.status === 'pending'" @click="editRecord(record)">编辑</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
import { ref, reactive, onMounted } from 'vue'
import { inventoryApi } from '@/api/fixed-asset'
import { message } from 'ant-design-vue'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref([])

const searchForm = reactive({
  inventoryNo: undefined as string | undefined,
  status: undefined as string | undefined,
  checkResult: undefined as string | undefined,
})

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

const resultMap: Record<string, string> = {
  consistent: '一致', mismatch: '不符', missing: '盘亏', surplus: '盘盈',
}
const resultColorMap: Record<string, string> = {
  consistent: 'green', mismatch: 'orange', missing: 'red', surplus: 'blue',
}

onMounted(() => {
  fetchData()
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchForm.inventoryNo) params.inventoryNo = searchForm.inventoryNo
  if (searchForm.status) params.status = searchForm.status
  if (searchForm.checkResult) params.checkResult = searchForm.checkResult

  inventoryApi.getPage(params).then((res: any) => {
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.inventoryNo = undefined
  searchForm.status = undefined
  searchForm.checkResult = undefined
  handleSearch()
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
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
</script>
